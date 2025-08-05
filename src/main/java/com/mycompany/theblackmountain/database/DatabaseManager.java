package com.mycompany.theblackmountain.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

/**
 * Gestore centrale per la connessione e inizializzazione del database H2
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private boolean initialized = false;
    private Properties props;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void initialize() throws SQLException, IOException {
        if (initialized) return;

        System.out.println("\uD83D\uDC84 Inizializzazione database H2...");

        props = loadDatabaseProperties();

        // Carica il driver H2 (opzionale con JDBC 4.0+)
        try {
            Class.forName(props.getProperty("db.driver"));
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver non trovato: " + props.getProperty("db.driver"));
        }

        initializeSchema();

        initialized = true;
        System.out.println("✅ Database H2 inizializzato con successo!");
    }

    private Properties loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IOException("File db.properties non trovato nel classpath");
            }
            props.load(input);
        }
        return props;
    }

    private void initializeSchema() throws SQLException, IOException {
        System.out.println("📋 Creazione schema database...");
        executeScript("/database/schema.sql");
        if (!hasData()) {
            System.out.println("📊 Inserimento dati iniziali...");
            executeScript("/database/data.sql");
        } else {
            System.out.println("📊 Dati già presenti nel database");
        }
    }

    private void executeScript(String scriptPath) throws SQLException, IOException {
        String script = loadScript(scriptPath);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String[] statements = script.split(";");
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    try {
                        stmt.execute(sql);
                    } catch (SQLException e) {
                        System.err.println("Errore SQL: " + sql);
                        System.err.println("Errore: " + e.getMessage());
                    }
                }
            }
        }
    }

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

    private boolean hasData() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false; // Se la tabella non esiste
        }
        return false;
    }

    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database non inizializzato. Chiama initialize() prima.");
        }
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }

    public void shutdown() {
        System.out.println("\uD83D\uDD12 Chiusura database H2 (nessun pool da chiudere)");
        initialized = false;
    }

    public void resetDatabase() throws SQLException, IOException {
        System.out.println("\uD83D\uDD04 Reset database...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }
        initializeSchema();
        System.out.println("✅ Database resettato");
    }

    public void printDatabaseInfo() {
        System.out.println("=== INFO DATABASE ===");
        System.out.println("Inizializzato: " + initialized);
        if (props != null) {
            System.out.println("URL: " + props.getProperty("db.url"));
            System.out.println("User: " + props.getProperty("db.username"));
        }
        System.out.println("====================");
    }
}
