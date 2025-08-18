/*
 * Package database
 */
package com.mycompany.theblackmountain.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestore database semplificato per The Black Mountain.
 */

public class TBMDatabase {

    private static TBMDatabase instance;
    private static final String DB_URL = "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    private InitDatabase initializer;
    private boolean initialized = false;

    private TBMDatabase() {
        this.initializer = new InitDatabase();
    }

    /**
     * Ottiene l'istanza singleton.
     */
    public static synchronized TBMDatabase getInstance() {
        if (instance == null) {
            instance = new TBMDatabase();
        }
        return instance;
    }

    /**
     * Inizializza il database.
     */
    public void initialize() throws SQLException {
        if (initialized) {
            return;
        }
        
        System.out.println("Inizializzazione database...");
        
        // Testa la connessione
        if (!testConnection()) {
            throw new SQLException("Test di connessione fallito");
        }
        
        // Inizializza le tabelle e i dati
        initializer.initDatabase();
        
        initialized = true;
        System.out.println("Database inizializzato con successo!");
    }

    /**
     * Ottiene una connessione al database.
     */
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database non inizializzato. Chiama initialize() prima.");
        }
        
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Testa la connessione al database.
     */
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return conn.isValid(5); // timeout 5 secondi
        } catch (SQLException e) {
            System.err.println("Test connessione fallito: " + e.getMessage());
            return false;
        }
    }

    /**
     * Chiude il database.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        try (Connection conn = getConnection()) {
            conn.createStatement().execute("SHUTDOWN");
            System.out.println("Database chiuso correttamente");
        } catch (SQLException e) {
            System.err.println("Errore nella chiusura del database: " + e.getMessage());
        } finally {
            initialized = false;
        }
    }

    /**
     * Verifica se il database è inizializzato e funzionante.
     */
    public boolean isHealthy() {
        return initialized && testConnection();
    }

    /**
     * Ottiene informazioni di debug sul database.
     */
    public String getDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== DATABASE INFO ===\n");
        info.append("Inizializzato: ").append(initialized).append("\n");
        info.append("URL: ").append(DB_URL).append("\n");
        info.append("Connessione attiva: ").append(testConnection()).append("\n");
        
        if (initialized && testConnection()) {
            try (Connection conn = getConnection()) {
                var meta = conn.getMetaData();
                info.append("Database: ").append(meta.getDatabaseProductName())
                    .append(" ").append(meta.getDatabaseProductVersion()).append("\n");
                info.append("Driver: ").append(meta.getDriverName())
                    .append(" ").append(meta.getDriverVersion()).append("\n");
            } catch (SQLException e) {
                info.append("ERRORE nel recupero metadati: ").append(e.getMessage()).append("\n");
            }
        }
        
        info.append("====================");
        return info.toString();
    }
}