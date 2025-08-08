/*
 * Package database
 */
package com.mycompany.theblackmountain.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe di inizializzazione del database per The Black Mountain.
 * Approccio minimal e diretto senza DAO/Entity layers.
 */
public class InitDatabase {

    private static final String DB_URL = "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    /**
     * Metodo di inizializzazione del database di gioco.
     */
    public final void initDatabase() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // ==================
            // CREAZIONE TABELLE
            // ==================
            
            // Tabella ROOMS - Stanze del gioco
            Statement stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOMS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "LOOK_DESCRIPTION TEXT,"
                    + "VISIBLE BOOLEAN DEFAULT TRUE,"
                    + "IMAGE_PATH VARCHAR(255));");
            stm.close();
            
            // Tabella ROOM_CONNECTIONS - Connessioni tra stanze
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOM_CONNECTIONS("
                    + "ROOM_ID INT PRIMARY KEY,"
                    + "NORTH_ID INT,"
                    + "SOUTH_ID INT,"
                    + "EAST_ID INT,"
                    + "WEST_ID INT,"
                    + "FOREIGN KEY (ROOM_ID) REFERENCES ROOMS(ID));");
            stm.close();
            
            // Tabella OBJECTS - Oggetti del gioco
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS OBJECTS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "ALIASES VARCHAR(500),"
                    + "OPENABLE BOOLEAN DEFAULT FALSE,"
                    + "PICKUPABLE BOOLEAN DEFAULT TRUE,"
                    + "PUSHABLE BOOLEAN DEFAULT FALSE,"
                    + "IS_OPEN BOOLEAN DEFAULT FALSE,"
                    + "IS_PUSHED BOOLEAN DEFAULT FALSE,"
                    + "OBJECT_TYPE VARCHAR(50) DEFAULT 'NORMAL');");
            stm.close();
            
            // Tabella WEAPONS - Armi (estende oggetti)
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS WEAPONS("
                    + "OBJECT_ID INT PRIMARY KEY,"
                    + "WEAPON_TYPE VARCHAR(50),"
                    + "ATTACK_BONUS INT DEFAULT 0,"
                    + "CRITICAL_CHANCE DOUBLE DEFAULT 5.0,"
                    + "CRITICAL_MULTIPLIER DOUBLE DEFAULT 2.0,"
                    + "IS_POISONED BOOLEAN DEFAULT FALSE,"
                    + "POISON_DAMAGE INT DEFAULT 0,"
                    + "SPECIAL_EFFECT VARCHAR(255),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();
            
            // Tabella CHARACTERS - Personaggi del gioco
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS CHARACTERS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "CHARACTER_TYPE VARCHAR(50) NOT NULL,"
                    + "MAX_HP INT DEFAULT 100,"
                    + "CURRENT_HP INT DEFAULT 100,"
                    + "ATTACK INT DEFAULT 10,"
                    + "DEFENSE INT DEFAULT 5,"
                    + "IS_ALIVE BOOLEAN DEFAULT TRUE,"
                    + "ROOM_ID INT DEFAULT 1);");
            stm.close();
            
            // Tabella ROOM_OBJECTS - Oggetti presenti nelle stanze
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOM_OBJECTS("
                    + "ROOM_ID INT,"
                    + "OBJECT_ID INT,"
                    + "PRIMARY KEY (ROOM_ID, OBJECT_ID),"
                    + "FOREIGN KEY (ROOM_ID) REFERENCES ROOMS(ID),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();
            
            // Tabella INVENTORY - Inventario del giocatore
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS INVENTORY("
                    + "CHARACTER_ID INT,"
                    + "OBJECT_ID INT,"
                    + "PRIMARY KEY (CHARACTER_ID, OBJECT_ID),"
                    + "FOREIGN KEY (CHARACTER_ID) REFERENCES CHARACTERS(ID),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();

            // ==================
            // INSERIMENTO DATI
            // ==================
            
            // Verifica e inserimento ROOMS
            String checkDataSql = "SELECT COUNT(*) FROM ROOMS";
            stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(checkDataSql);
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO ROOMS VALUES "
                    + "(1, 'Entrance Hall', 'Una grande sala d''ingresso con alte colonne di pietra. La luce filtra attraverso vetrate colorate.', "
                    + "'Osservando con più attenzione noti antiche incisioni sulle colonne.', TRUE, 'entrance_hall'), "
                    + "(2, 'Armory', 'Un''armeria piena di armi e armature. Molte sono arrugginite dal tempo.', "
                    + "'Tra le armi noti una spada che sembra in buone condizioni.', TRUE, 'armory'), "
                    + "(3, 'Library', 'Una vasta biblioteca con scaffali che raggiungono il soffitto. L''aria sa di carta antica.', "
                    + "'Alcuni libri sono aperti su un tavolo, come se qualcuno li stesse leggendo di recente.', TRUE, 'library'), "
                    + "(4, 'Throne Room', 'Una maestosa sala del trono. Il trono è vuoto e coperto di polvere.', "
                    + "'Il trono sembra nascondere qualcosa dietro di sé.', TRUE, 'throne_room'), "
                    + "(5, 'Dungeon', 'Umide segrete del castello. L''aria è pesante e malsana.', "
                    + "'Senti strani rumori provenire dalle celle più buie.', TRUE, 'dungeon'), "
                    + "(6, 'Tower Top', 'La cima della torre più alta. Da qui si vede tutto il regno.', "
                    + "'Un telescopio è puntato verso l''orizzonte.', TRUE, 'tower_top');");
                stm.close();
            }

            // Verifica e inserimento ROOM_CONNECTIONS
            checkDataSql = "SELECT COUNT(*) FROM ROOM_CONNECTIONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO ROOM_CONNECTIONS VALUES "
                    + "(1, NULL, NULL, 2, 3), "  // Entrance: est=Armory, ovest=Library
                    + "(2, NULL, NULL, NULL, 1), " // Armory: ovest=Entrance
                    + "(3, 4, NULL, 1, NULL), "    // Library: nord=Throne Room, est=Entrance
                    + "(4, 6, 3, NULL, NULL), "    // Throne Room: nord=Tower, sud=Library
                    + "(5, 1, NULL, NULL, NULL), " // Dungeon: nord=Entrance
                    + "(6, NULL, 4, NULL, NULL);");  // Tower: sud=Throne Room
                stm.close();
            }

            // Verifica e inserimento OBJECTS
            checkDataSql = "SELECT COUNT(*) FROM OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO OBJECTS VALUES "
                    + "(1, 'Spada di Ferro', 'Una spada ben bilanciata con lama affilata.', 'spada,ferro,lama', FALSE, TRUE, FALSE, FALSE, FALSE, 'WEAPON'), "
                    + "(2, 'Pozione di Cura', 'Una fiala contenente un liquido rosso rigenerante.', 'pozione,cura,fiala', FALSE, TRUE, FALSE, FALSE, FALSE, 'CONSUMABLE'), "
                    + "(3, 'Chiave Dorata', 'Una chiave ornata d''oro con incisioni misteriose.', 'chiave,oro,dorata', FALSE, TRUE, FALSE, FALSE, FALSE, 'KEY'), "
                    + "(4, 'Libro Antico', 'Un tomo rilegato in pelle con pagine ingiallite.', 'libro,tomo,antico', TRUE, TRUE, FALSE, FALSE, FALSE, 'READABLE'), "
                    + "(5, 'Forziere', 'Un grande forziere di legno con serratura.', 'forziere,cassa,baule', TRUE, FALSE, FALSE, FALSE, FALSE, 'CONTAINER'), "
                    + "(6, 'Gemma Magica', 'Una gemma che pulsa di luce blu.', 'gemma,pietra,magica', FALSE, TRUE, FALSE, FALSE, FALSE, 'MAGICAL'), "
                    + "(7, 'Torcia', 'Una torcia che può essere accesa per illuminare.', 'torcia,luce,fiamma', FALSE, TRUE, FALSE, FALSE, FALSE, 'LIGHT');");
                stm.close();
            }

            // Verifica e inserimento WEAPONS
            checkDataSql = "SELECT COUNT(*) FROM WEAPONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO WEAPONS VALUES "
                    + "(1, 'SWORD', 15, 10.0, 2.0, FALSE, 0, NULL);"); // Spada di Ferro
                stm.close();
            }

            // Verifica e inserimento CHARACTERS
            checkDataSql = "SELECT COUNT(*) FROM CHARACTERS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO CHARACTERS VALUES "
                    + "(1, 'Avventuriero', 'Un coraggioso esploratore in cerca di avventure.', 'PLAYER', 100, 100, 15, 10, TRUE, 1), "
                    + "(2, 'Scheletro Guardiano', 'Un antico guardiano ora ridotto a scheletro.', 'ENEMY', 50, 50, 12, 5, TRUE, 5), "
                    + "(3, 'Mago Oscuro', 'Un potente mago dalle intenzioni malvagie.', 'ENEMY', 80, 80, 20, 8, TRUE, 6), "
                    + "(4, 'Bibliotecario', 'Un vecchio saggio custode della conoscenza.', 'NPC', 30, 30, 5, 2, TRUE, 3);");
                stm.close();
            }

            // Verifica e inserimento ROOM_OBJECTS
            checkDataSql = "SELECT COUNT(*) FROM ROOM_OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO ROOM_OBJECTS VALUES "
                    + "(1, 7), "  // Torcia nella Entrance Hall
                    + "(2, 1), "  // Spada di Ferro nell'Armory
                    + "(2, 2), "  // Pozione di Cura nell'Armory
                    + "(3, 4), "  // Libro Antico nella Library
                    + "(4, 5), "  // Forziere nella Throne Room
                    + "(4, 3), "  // Chiave Dorata nella Throne Room
                    + "(6, 6);"); // Gemma Magica nella Tower Top
                stm.close();
            }

            // Verifica e inserimento INVENTORY (inventario iniziale del giocatore)
            checkDataSql = "SELECT COUNT(*) FROM INVENTORY WHERE CHARACTER_ID = 1";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            
            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO INVENTORY VALUES "
                    + "(1, 2);"); // Giocatore inizia con una Pozione di Cura
                stm.close();
            }

            conn.close();
            System.out.println("✅ Database inizializzato con successo!");
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nell'inizializzazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Metodo di utilità per testare la connessione
     */
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Connessione fallita: " + e.getMessage());
            return false;
        }
    }

    /**
     * Metodo per resettare il database (utile per sviluppo)
     */
    public void resetDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stm = conn.createStatement()) {
            
            System.out.println("🔄 Reset database in corso...");
            
            // Disabilita vincoli temporaneamente
            stm.execute("SET REFERENTIAL_INTEGRITY FALSE");
            
            // Elimina tutti i dati
            stm.execute("DELETE FROM INVENTORY");
            stm.execute("DELETE FROM ROOM_OBJECTS");
            stm.execute("DELETE FROM WEAPONS");
            stm.execute("DELETE FROM CHARACTERS");
            stm.execute("DELETE FROM OBJECTS");
            stm.execute("DELETE FROM ROOM_CONNECTIONS");
            stm.execute("DELETE FROM ROOMS");
            
            // Riabilita vincoli
            stm.execute("SET REFERENTIAL_INTEGRITY TRUE");
            
            System.out.println("✅ Database resettato, reinizializzazione...");
            
            // Reinizializza con dati freschi
            initDatabase();
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel reset del database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}