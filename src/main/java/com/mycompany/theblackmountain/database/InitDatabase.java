/*
 * Package database
 */
package com.mycompany.theblackmountain.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe di inizializzazione del database per The Black Mountain.
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
            createTables(conn);

            // INSERIMENTO DATI IN ORDINE CORRETTO
            // 1. PRIMA le stanze
            insertRoomsIfEmpty(conn);

            // 2. POI le connessioni tra stanze
            insertRoomConnectionsIfEmpty(conn);

            // 3. POI gli oggetti
            insertObjectsIfEmpty(conn);

            // 4. POI le armi
            insertWeaponsIfEmpty(conn);

            // 5. POI i personaggi 
            insertCharactersIfEmpty(conn);

            // 6. POI l'inventario
            insertInventoryIfEmpty(conn);

            // 7. POI gli oggetti nelle stanze (SOLO oggetti fissi e casse)
            insertRoomObjectsIfEmpty(conn);

            conn.close();
            System.out.println("✅ Database inizializzato con successo!");

        } catch (SQLException e) {
            System.err.println("❌ Errore nell'inizializzazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
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
                + "ROOM_ID INT DEFAULT 0);");
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
    }

    private void insertRoomsIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM ROOMS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            // USARE PREPARED STATEMENTS per gestire caratteri speciali
            String sql = "INSERT INTO ROOMS (ID, NAME, DESCRIPTION, LOOK_DESCRIPTION, VISIBLE, IMAGE_PATH) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Stanza 0 - Ingresso
                stmt.setInt(1, 0);
                stmt.setString(2, "Ingresso della Fortezza");
                stmt.setString(3, "Ti trovi all'ingresso della fortezza maledetta. L'aria è densa di umidità e il pavimento è cosparso di muschio. Un enorme goblin dalla pelle verde scuro ti osserva con occhi colmi d'odio.");
                stmt.setString(4, "Scorgi una vecchia cassa, probabilmente abbandonata dai precedenti avventurieri. Dentro potrebbe esserci qualcosa di utile per iniziare il tuo cammino.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "entrance");
                stmt.addBatch();

                // Stanza 1 - Stanza del Topo
                stmt.setInt(1, 1);
                stmt.setString(2, "Stanza del Topo");
                stmt.setString(3, "Le pareti sono coperte di ragnatele e muffa. Un enorme topo, con denti giallastri e occhi rossi, grugnisce in un angolo buio.");
                stmt.setString(4, "Tra le ragnatele, potresti trovare qualcosa di utile o... disgustoso.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "rat_room");
                stmt.addBatch();

                // Stanza 2 - Mensa Abbandonata
                stmt.setInt(1, 2);
                stmt.setString(2, "Mensa Abbandonata");
                stmt.setString(3, "Le tavole di legno sono rovesciate, piatti infranti ovunque.");
                stmt.setString(4, "Non sembra esserci nulla di valore qui, a parte due goblin chiassosi pronti a combattere.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "mess_hall");
                stmt.addBatch();

                // Stanza 3 - Dormitorio delle Guardie
                stmt.setInt(1, 3);
                stmt.setString(2, "Dormitorio delle Guardie");
                stmt.setString(3, "Letti rotti e coperte lacerate giacciono sparsi ovunque. Il silenzio regna sovrano: nessun nemico in vista.");
                stmt.setString(4, "Una cassa giace accanto a un letto distrutto. Dentro, potresti trovare qualcosa di prezioso.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "dormitory");
                stmt.addBatch();

                // Stanza 4 - Sala delle Guardie
                stmt.setInt(1, 4);
                stmt.setString(2, "Sala delle Guardie");
                stmt.setString(3, "La sala è colma di resti di armature e ossa spezzate. Un enorme goblin con una clava insanguinata ti fronteggia, affiancato da un compagno più piccolo e agile.");
                stmt.setString(4, "Tra assi rotte e macerie intravedi una cassa di ferro semiaperta.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "guard_hall");
                stmt.addBatch();

                // Stanza 5 - Sala degli Incantesimi
                stmt.setInt(1, 5);
                stmt.setString(2, "Sala degli Incantesimi");
                stmt.setString(3, "Le pareti sono incise con simboli magici che pulsano di luce blu. Nel centro, un altare emette un suono basso e costante.");
                stmt.setString(4, "Un'iscrizione recita: Offri parte della tua essenza, e il tuo braccio sarà guidato da un arco etereo.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "magic_room");
                stmt.addBatch();

                // Stanza 6 - Stanza delle Torture
                stmt.setInt(1, 6);
                stmt.setString(2, "Stanza delle Torture");
                stmt.setString(3, "Catene arrugginite pendono dal soffitto. Il pavimento è macchiato di vecchio sangue secco. Non ci sono nemici.");
                stmt.setString(4, "Una cassa chiusa giace in un angolo, apparentemente dimenticata.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "torture_room");
                stmt.addBatch();

                // Stanza 7 - Sala del Signore dei Goblin
                stmt.setInt(1, 7);
                stmt.setString(2, "Sala del Signore dei Goblin");
                stmt.setString(3, "L'aria è irrespirabile. Un fumo denso copre il volto del cane demone, una creatura infernale con zanne fumanti.");
                stmt.setString(4, "Sul piedistallo alle spalle del demone c'è una chiave che potrebbe aprire la cella.");
                stmt.setBoolean(5, true);
                stmt.setString(6, "boss_room");
                stmt.addBatch();

                // Stanza 8 - Uscita
                stmt.setInt(1, 8);
                stmt.setString(2, "Uscita");
                stmt.setString(3, "L'uscita dalla Montagna Nera. La libertà ti aspetta.");
                stmt.setString(4, "Finalmente libero!");
                stmt.setBoolean(5, true);
                stmt.setString(6, "exit");
                stmt.addBatch();

                stmt.executeBatch();
            }

            System.out.println(" Stanze inserite");
        }
    }

    private void insertRoomConnectionsIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM ROOM_CONNECTIONS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            stm = conn.createStatement();
            stm.execute("INSERT INTO ROOM_CONNECTIONS VALUES "
                    + "(0, NULL, NULL, 1, NULL), "
                    + "(1, NULL, 6, 2, 0), "
                    + "(2, NULL, NULL, 3, 1), "
                    + "(3, NULL, 4, NULL, 2), "
                    + "(4, 3, NULL, NULL, 5), "
                    + "(5, NULL, NULL, 4, NULL), "
                    + "(6, 1, 7, NULL, NULL), "
                    + "(7, 6, NULL, NULL, 8), "
                    + "(8, NULL, NULL, 7, NULL);");
            stm.close();
            System.out.println("✅ Connessioni stanze inserite");
        }
    }

    private void insertCharactersIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM CHARACTERS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            // USARE PREPARED STATEMENTS anche per i personaggi
            String sql = "INSERT INTO CHARACTERS (ID, NAME, DESCRIPTION, CHARACTER_TYPE, MAX_HP, CURRENT_HP, ATTACK, DEFENSE, IS_ALIVE, ROOM_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Giocatore (ID = 0)
                stmt.setInt(1, 0);
                stmt.setString(2, "Giocatore");
                stmt.setString(3, "Il coraggioso avventuriero che si addentra nella Montagna Nera per salvare la principessa.");
                stmt.setString(4, "PLAYER");
                stmt.setInt(5, 100);
                stmt.setInt(6, 100);
                stmt.setInt(7, 15);
                stmt.setInt(8, 5);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 0);
                stmt.addBatch();

                // Goblin nell'ingresso (ID = 1)
                stmt.setInt(1, 1);
                stmt.setString(2, "Goblin");
                stmt.setString(3, "Una creatura malvagia dalla pelle verde scuro, con occhi pieni d'odio e artigli affilati.");
                stmt.setString(4, "GOBLIN");
                stmt.setInt(5, 40);
                stmt.setInt(6, 40);
                stmt.setInt(7, 12);
                stmt.setInt(8, 3);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 0);
                stmt.addBatch();

                // Topo Gigante nella stanza del topo (ID = 2)
                stmt.setInt(1, 2);
                stmt.setString(2, "Topo Gigante");
                stmt.setString(3, "Un enorme roditore con denti giallastri e occhi rossi.");
                stmt.setString(4, "GIANT_RAT");
                stmt.setInt(5, 25);
                stmt.setInt(6, 25);
                stmt.setInt(7, 8);
                stmt.setInt(8, 2);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 1);
                stmt.addBatch();

                // Goblin Chiassoso nella mensa (ID = 3)
                stmt.setInt(1, 3);
                stmt.setString(2, "Goblin Chiassoso");
                stmt.setString(3, "Un goblin aggressivo che litiga per un osso.");
                stmt.setString(4, "GOBLIN");
                stmt.setInt(5, 35);
                stmt.setInt(6, 35);
                stmt.setInt(7, 10);
                stmt.setInt(8, 3);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 2);
                stmt.addBatch();

                // Goblin Rissoso nella mensa (ID = 4)
                stmt.setInt(1, 4);
                stmt.setString(2, "Goblin Rissoso");
                stmt.setString(3, "Un altro goblin altrettanto aggressivo.");
                stmt.setString(4, "GOBLIN");
                stmt.setInt(5, 30);
                stmt.setInt(6, 30);
                stmt.setInt(7, 9);
                stmt.setInt(8, 2);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 2);
                stmt.addBatch();

                // Goblin Gigante nella sala delle guardie (ID = 5)
                stmt.setInt(1, 5);
                stmt.setString(2, "Goblin Gigante");
                stmt.setString(3, "Un goblin enorme che impugna una clava insanguinata.");
                stmt.setString(4, "GOBLIN");
                stmt.setInt(5, 60);
                stmt.setInt(6, 60);
                stmt.setInt(7, 16);
                stmt.setInt(8, 5);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 4);
                stmt.addBatch();

                // Goblin Minuto nella sala delle guardie (ID = 6)
                stmt.setInt(1, 6);
                stmt.setString(2, "Goblin Minuto");
                stmt.setString(3, "Un goblin più piccolo ma altrettanto minaccioso.");
                stmt.setString(4, "GOBLIN");
                stmt.setInt(5, 25);
                stmt.setInt(6, 25);
                stmt.setInt(7, 8);
                stmt.setInt(8, 2);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 4);
                stmt.addBatch();

                // Cane Demone nella sala del boss (ID = 7)
                stmt.setInt(1, 7);
                stmt.setString(2, "Cane Demone");
                stmt.setString(3, "Una creatura infernale con zanne fumanti e occhi di fuoco.");
                stmt.setString(4, "DEMON_DOG");
                stmt.setInt(5, 150);
                stmt.setInt(6, 150);
                stmt.setInt(7, 25);
                stmt.setInt(8, 8);
                stmt.setBoolean(9, true);
                stmt.setInt(10, 7);
                stmt.addBatch();

                stmt.executeBatch();
            }

            System.out.println(" Personaggi inseriti (giocatore in stanza 0)");
        }
    }

    private void insertObjectsIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM OBJECTS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            // USARE PREPARED STATEMENTS per gestire caratteri speciali
            String sql = "INSERT INTO OBJECTS (ID, NAME, DESCRIPTION, ALIASES, OPENABLE, PICKUPABLE, PUSHABLE, IS_OPEN, IS_PUSHED, OBJECT_TYPE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Oggetto 1 - Chiave ingresso
                stmt.setInt(1, 1);
                stmt.setString(2, "chiave ingresso");
                stmt.setString(3, "Una chiave d'ottone annerita dal tempo. Potrebbe aprire la porta principale della fortezza.");
                stmt.setString(4, "chiave,key");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 2 - Pozione di cura
                stmt.setInt(1, 2);
                stmt.setString(2, "pozione di cura");
                stmt.setString(3, "Una fiala dal liquido rosso, emana un lieve calore.");
                stmt.setString(4, "pozione,cura,healing");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 4 - Stringhe di ragnatela
                stmt.setInt(1, 4);
                stmt.setString(2, "stringhe di ragnatela");
                stmt.setString(3, "Filamenti spessi e resistenti. Potrebbero servire per creare qualcosa di utile.");
                stmt.setString(4, "ragnatela,filo,stringhe,stringa,ragnatele");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 5 - Pozione cura totale
                stmt.setInt(1, 5);
                stmt.setString(2, "pozione cura totale");
                stmt.setString(3, "Una pozione brillante di colore dorato. Ti riempie di energia solo a guardarla.");
                stmt.setString(4, "pozione totale,cura totale");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 6 - Bastone
                stmt.setInt(1, 6);
                stmt.setString(2, "bastone");
                stmt.setString(3, "Un robusto bastone di legno. Può essere combinato con altri oggetti.");
                stmt.setString(4, "staff,stick");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "WEAPON");
                stmt.addBatch();

                // Oggetto 7 - Arco magico
                stmt.setInt(1, 7);
                stmt.setString(2, "arco magico");
                stmt.setString(3, "Un arco leggero ma potente, creato con energia arcana e materiali raccolti nella fortezza.");
                stmt.setString(4, "arco,bow");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "WEAPON");
                stmt.addBatch();

                // Oggetto 8 - Libro incantesimo del fuoco
                stmt.setInt(1, 8);
                stmt.setString(2, "libro incantesimo del fuoco");
                stmt.setString(3, "Un grimorio antico, le sue pagine brillano di energia arcana. Contiene l'Incantesimo del Fuoco.");
                stmt.setString(4, "libro,grimorio,fuoco");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 9 - Veleno
                stmt.setInt(1, 9);
                stmt.setString(2, "veleno");
                stmt.setString(3, "Una boccetta scura. Può essere applicata su armi per aumentare il danno.");
                stmt.setString(4, "poison,boccetta");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 10 - Chiave cella principessa
                stmt.setInt(1, 10);
                stmt.setString(2, "chiave cella principessa");
                stmt.setString(3, "Una chiave dorata e decorata, diversa da tutte le altre. Probabilmente apre la cella della principessa.");
                stmt.setString(4, "chiave principessa,chiave dorata");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 11 - Chiave del collo del boss
                stmt.setInt(1, 11);
                stmt.setString(2, "chiave del collo del boss");
                stmt.setString(3, "Una chiave pesante, con pendaglio di ferro annerito. Cade dal collo del demone canino quando lo sconfiggi.");
                stmt.setString(4, "chiave boss,chiave finale");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "NORMAL");
                stmt.addBatch();

                // Oggetto 12 - Spada
                stmt.setInt(1, 12);
                stmt.setString(2, "spada");
                stmt.setString(3, "Una spada d'acciaio ben bilanciata. Arma affidabile per il combattimento.");
                stmt.setString(4, "sword,lama");
                stmt.setBoolean(5, false);
                stmt.setBoolean(6, true);
                stmt.setBoolean(7, false);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, false);
                stmt.setString(10, "WEAPON");
                stmt.addBatch();

                // Casse (100-103)
                for (int chestId = 100; chestId <= 103; chestId++) {
                    stmt.setInt(1, chestId);
                    stmt.setString(2, "cassa");
                    stmt.setString(3, getChestDescription(chestId));
                    stmt.setString(4, "baule,contenitore,scrigno");
                    stmt.setBoolean(5, true); // openable
                    stmt.setBoolean(6, false); // not pickupable
                    stmt.setBoolean(7, false);
                    stmt.setBoolean(8, false); // starts closed
                    stmt.setBoolean(9, false);
                    stmt.setString(10, "CONTAINER");
                    stmt.addBatch();
                }

                stmt.executeBatch();
            }

            System.out.println("✅ Oggetti inseriti");
        }
    }

    private String getChestDescription(int chestId) {
        return switch (chestId) {
            case 100 ->
                "Una vecchia cassa di legno, chiusa ma non bloccata.";
            case 101 ->
                "Una cassa giace accanto a un letto distrutto.";
            case 102 ->
                "Una cassa semiaperta tra i resti di una barricata.";
            case 103 ->
                "Una cassa chiusa giace in un angolo, apparentemente dimenticata.";
            default ->
                "Una cassa misteriosa.";
        };
    }

    private void insertWeaponsIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM WEAPONS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            stm = conn.createStatement();
            stm.execute("INSERT INTO WEAPONS VALUES "
                    + "(6, 'STAFF', 5, 5.0, 2.0, FALSE, 0, NULL), "
                    + "(7, 'MAGIC', 12, 15.0, 2.0, FALSE, 0, NULL), "
                    + "(12, 'SWORD', 8, 10.0, 2.0, FALSE, 0, NULL);");
            stm.close();
            System.out.println(" Armi inserite");
        }
    }

    private void insertInventoryIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM INVENTORY WHERE CHARACTER_ID = 0";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            stm = conn.createStatement();
            stm.execute("INSERT INTO INVENTORY VALUES "
                    + "(0, 2), " // Pozione di cura
                    + "(0, 12);"); // Spada
            stm.close();
            System.out.println("✅ Inventario iniziale del giocatore impostato");
        }
    }

    private void insertRoomObjectsIfEmpty(Connection conn) throws SQLException {
        String checkDataSql = "SELECT COUNT(*) FROM ROOM_OBJECTS";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(checkDataSql);
        rs.next();
        int count = rs.getInt(1);
        rs.close();

        if (count == 0) {
            stm = conn.createStatement();
            // Solo oggetti fissi e casse, NON il contenuto delle casse
            stm.execute("INSERT INTO ROOM_OBJECTS VALUES "
                    + "(0, 100), " // Ingresso: cassa (chiusa)
                    + "(1, 4), " // Stanza del Topo: ragnatela (oggetto fisso)
                    + "(3, 101), " // Dormitorio: cassa (chiusa)
                    + "(4, 102), " // Sala Guardie: cassa (chiusa) ← qui libro e veleno sono DENTRO
                    + "(6, 103);"); // Torture: cassa (chiusa)
            stm.close();
            System.out.println(" Oggetti fissi e casse inseriti nelle stanze");
            System.out.println("ℹ️ Gli oggetti delle casse verranno aggiunti alle stanze quando le casse vengono aperte");
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
}
