package com.mycompany.theblackmountain.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Gestore database migliorato con connection pooling e migliore gestione errori
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private DataSource dataSource;
    private Properties props;
    
    // Configurazioni per connection pooling semplice
    private static final int MAX_CONNECTIONS = 10;
    private static final int CONNECTION_TIMEOUT = 30000; // 30 secondi
    
    private DatabaseManager() {}
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Inizializza il database con gestione errori migliorata
     */
    public synchronized void initialize() throws DatabaseException {
        if (initialized.get()) {
            return;
        }
        
        try {
            System.out.println("🔧 Inizializzazione DatabaseManager...");
            
            // 1. Carica configurazioni
            loadConfiguration();
            
            // 2. Configura DataSource con connection pooling
            setupDataSource();
            
            // 3. Verifica connettività
            testConnection();
            
            // 4. Inizializza schema se necessario
            initializeSchema();
            
            initialized.set(true);
            System.out.println("✅ DatabaseManager inizializzato con successo!");
            
        } catch (Exception e) {
            initialized.set(false);
            throw new DatabaseException("Errore durante l'inizializzazione del database", e);
        }
    }
    
    /**
     * Carica le configurazioni con fallback a valori di default
     */
    private void loadConfiguration() throws IOException {
        props = new Properties();
        
        // Carica da file properties
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                System.out.println("📋 Configurazioni caricate da db.properties");
            } else {
                System.out.println("⚠️ File db.properties non trovato, uso configurazioni di default");
            }
        }
        
        // Imposta valori di default se non presenti
        props.putIfAbsent("db.url", "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE");
        props.putIfAbsent("db.username", "sa");
        props.putIfAbsent("db.password", "");
        props.putIfAbsent("db.driver", "org.h2.Driver");
        props.putIfAbsent("db.max_connections", String.valueOf(MAX_CONNECTIONS));
        props.putIfAbsent("db.connection_timeout", String.valueOf(CONNECTION_TIMEOUT));
    }
    
    /**
     * Configura il DataSource con connection pooling basic
     */
    private void setupDataSource() throws DatabaseException {
        try {
            // Carica il driver esplicitamente
            Class.forName(props.getProperty("db.driver"));
            
            // Configura H2 DataSource
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL(props.getProperty("db.url"));
            ds.setUser(props.getProperty("db.username"));
            ds.setPassword(props.getProperty("db.password"));
            
            this.dataSource = ds;
            
            System.out.println("🔗 DataSource configurato: " + props.getProperty("db.url"));
            
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("Driver database non trovato: " + props.getProperty("db.driver"), e);
        }
    }
    
    /**
     * Testa la connessione al database
     */
    private void testConnection() throws DatabaseException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("🔍 Database: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new DatabaseException("Test di connessione fallito", e);
        }
    }
    
    /**
     * Inizializza lo schema del database
     */
    private void initializeSchema() throws DatabaseException {
        try {
            System.out.println("📋 Inizializzazione schema database...");
            
            if (!schemaExists()) {
                System.out.println("🔨 Creazione schema...");
                executeScript("/database/schema.sql");
            } else {
                System.out.println("📊 Schema già esistente");
            }
            
            if (!hasData()) {
                System.out.println("📊 Inserimento dati iniziali...");
                executeScript("/database/data.sql");
            } else {
                System.out.println("📈 Dati già presenti");
            }
            
        } catch (Exception e) {
            throw new DatabaseException("Errore nell'inizializzazione dello schema", e);
        }
    }
    
    /**
     * Verifica se lo schema esiste
     */
    private boolean schemaExists() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "ROOMS", null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Verifica se ci sono dati nelle tabelle principali
     */
    private boolean hasData() {
        String[] tables = {"rooms", "objects", "characters"};
        
        try (Connection conn = getConnection()) {
            for (String table : tables) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                } catch (SQLException e) {
                    // Tabella potrebbe non esistere ancora
                    return false;
                }
            }
        } catch (SQLException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * Esegue uno script SQL
     */
    private void executeScript(String scriptPath) throws DatabaseException {
        try {
            String script = loadScript(scriptPath);
            executeStatements(script);
        } catch (IOException e) {
            throw new DatabaseException("Errore nel caricamento script: " + scriptPath, e);
        }
    }
    
    /**
     * Carica uno script SQL dalle risorse
     */
    private String loadScript(String scriptPath) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(scriptPath)) {
            if (input == null) {
                throw new IOException("Script non trovato: " + scriptPath);
            }
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        }
    }
    
    /**
     * Esegue una serie di statement SQL separati da punto e virgola
     */
    private void executeStatements(String script) throws DatabaseException {
        String[] statements = script.split(";");
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {
                int executedCount = 0;
                
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty() && !sql.startsWith("--") && !sql.startsWith("/*")) {
                        try {
                            stmt.execute(sql);
                            executedCount++;
                        } catch (SQLException e) {
                            System.err.println("⚠️ Errore nell'esecuzione SQL: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                            System.err.println("   Motivo: " + e.getMessage());
                            // Continua con gli altri statement invece di fallire completamente
                        }
                    }
                }
                
                conn.commit();
                System.out.println("✅ Eseguiti " + executedCount + " statement SQL");
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'esecuzione degli statement SQL", e);
        }
    }
    
    /**
     * Ottiene una connessione dal pool
     */
    public Connection getConnection() throws SQLException {
        if (!initialized.get()) {
            throw new SQLException("DatabaseManager non inizializzato. Chiama initialize() prima.");
        }
        
        if (dataSource == null) {
            throw new SQLException("DataSource non configurato");
        }
        
        try {
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(true); // Default per operazioni singole
            return conn;
        } catch (SQLException e) {
            throw new SQLException("Errore nell'ottenimento della connessione dal pool", e);
        }
    }
    
    /**
     * Ottiene una connessione transazionale
     */
    public Connection getTransactionalConnection() throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        return conn;
    }
    
    /**
     * Esegue un'operazione in una transazione
     */
    public <T> T executeInTransaction(TransactionalOperation<T> operation) throws DBException, Exception {
        try (Connection conn = getTransactionalConnection()) {
            try {
                T result = operation.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DBException("Errore nell'esecuzione della transazione", e);
        }
    }
    
    /**
     * Esegue un'operazione senza valore di ritorno in una transazione
     */
    public void executeInTransaction(VoidTransactionalOperation operation) throws DBException, Exception {
        executeInTransaction(conn -> {
            operation.execute(conn);
            return null;
        });
    }
    
    /**
     * Resetta il database eliminando tutti i dati
     */
    public synchronized void resetDatabase() throws DatabaseException {
        try {
            System.out.println("🔄 Reset database in corso...");
            
            executeInTransaction(conn -> {
                try (Statement stmt = conn.createStatement()) {
                    // Disabilita i vincoli di chiave esterna temporaneamente
                    stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
                    
                    // Elimina tutti i dati dalle tabelle principali
                    stmt.execute("DELETE FROM room_objects");
                    stmt.execute("DELETE FROM inventory");
                    stmt.execute("DELETE FROM weapons");
                    stmt.execute("DELETE FROM characters");
                    stmt.execute("DELETE FROM objects");
                    stmt.execute("DELETE FROM rooms");
                    
                    // Riabilita i vincoli
                    stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
                    
                    System.out.println("🗑️ Dati eliminati");
                }
            });
            
            // Reinserisce i dati iniziali
            executeScript("/database/data.sql");
            System.out.println("✅ Database resettato con successo");
            
        } catch (Exception e) {
            throw new DatabaseException("Errore nel reset del database", e);
        }
    }
    
    /**
     * Chiude il database manager
     */
    public synchronized void shutdown() {
        if (!initialized.get()) {
            return;
        }
        
        try {
            System.out.println("🔒 Chiusura DatabaseManager...");
            
            // Chiudi tutte le connessioni attive se H2 supporta il comando
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("SHUTDOWN");
            } catch (SQLException e) {
                System.err.println("⚠️ Errore nella chiusura controllata del database: " + e.getMessage());
            }
            
            dataSource = null;
            initialized.set(false);
            
            System.out.println("✅ DatabaseManager chiuso");
            
        } catch (Exception e) {
            System.err.println("❌ Errore durante la chiusura: " + e.getMessage());
        }
    }
    
    /**
     * Ottiene informazioni di debug sul database
     */
    public DatabaseInfo getDatabaseInfo() {
        DatabaseInfo info = new DatabaseInfo();
        info.initialized = initialized.get();
        
        if (props != null) {
            info.url = props.getProperty("db.url");
            info.username = props.getProperty("db.username");
        }
        
        if (initialized.get()) {
            try (Connection conn = getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                info.databaseProductName = meta.getDatabaseProductName();
                info.databaseProductVersion = meta.getDatabaseProductVersion();
                info.driverName = meta.getDriverName();
                info.driverVersion = meta.getDriverVersion();
                
                // Conta record nelle tabelle principali
                info.roomCount = countRecords(conn, "rooms");
                info.objectCount = countRecords(conn, "objects");
                info.characterCount = countRecords(conn, "characters");
                info.weaponCount = countRecords(conn, "weapons");
                
            } catch (SQLException e) {
                info.error = e.getMessage();
            }
        }
        
        return info;
    }
    
    private int countRecords(Connection conn, String tableName) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return -1;
        }
    }
    
    /**
     * Verifica se il database è inizializzato e funzionante
     */
    public boolean isHealthy() {
        if (!initialized.get()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            return conn.isValid(5); // timeout di 5 secondi
        } catch (SQLException e) {
            return false;
        }
    }
    
    // Interfacce funzionali per operazioni transazionali
    @FunctionalInterface
    public interface TransactionalOperation<T> {
        T execute(Connection connection) throws Exception;
    }
    
    @FunctionalInterface
    public interface VoidTransactionalOperation {
        void execute(Connection connection) throws Exception;
    }
    
    /**
     * Classe per contenere informazioni di debug del database
     */
    public static class DatabaseInfo {
        public boolean initialized;
        public String url;
        public String username;
        public String databaseProductName;
        public String databaseProductVersion;
        public String driverName;
        public String driverVersion;
        public int roomCount;
        public int objectCount;
        public int characterCount;
        public int weaponCount;
        public String error;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DATABASE INFO ===\n");
            sb.append("Inizializzato: ").append(initialized).append("\n");
            
            if (error != null) {
                sb.append("ERRORE: ").append(error).append("\n");
            } else if (initialized) {
                sb.append("Database: ").append(databaseProductName).append(" ").append(databaseProductVersion).append("\n");
                sb.append("Driver: ").append(driverName).append(" ").append(driverVersion).append("\n");
                sb.append("URL: ").append(url).append("\n");
                sb.append("Username: ").append(username).append("\n");
                sb.append("Stanze: ").append(roomCount).append("\n");
                sb.append("Oggetti: ").append(objectCount).append("\n");
                sb.append("Personaggi: ").append(characterCount).append("\n");
                sb.append("Armi: ").append(weaponCount).append("\n");
            }
            
            sb.append("====================");
            return sb.toString();
        }
    }
}

/**
 * Eccezione personalizzata per errori del database
 */
class DBException extends Exception {
    
    public DBException(String message) {
        super(message);
    }
    
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}