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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.Weapon;
import com.mycompany.theblackmountain.type.WeaponType;
import com.mycompany.theblackmountain.type.CharacterType;
import com.mycompany.theblackmountain.impl.TBMGame;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe per caricare i dati dal database nel gioco.
 */
public class GameLoader {

    private static final String DB_URL = "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private TBMGame game;
    private Map<Integer, Room> roomMap;

    /**
     * Costruttore.
     *
     * @param game istanza del gioco da inizializzare.
     */
    public GameLoader(TBMGame game) {
        this.game = game;
        this.roomMap = new HashMap<>();
    }

    /**
     * Metodo principale per caricare tutti i dati di gioco.
     */
    public void loadGame() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Caricamento dati dal database...");

            // Carica tutte le stanze
            loadRooms(conn);

            // Imposta le connessioni tra stanze
            loadRoomConnections(conn);

            // Reset delle casse prima di caricare
            resetAllChests();

            // Carica gli oggetti nelle stanze
            loadRoomObjects(conn);

            // Carica i personaggi
            loadCharacters(conn);

            // Carica l'inventario del giocatore
            loadPlayerInventory(conn);

            // Imposta la stanza corrente del giocatore
            setPlayerCurrentRoom(conn);

            // Sincronizza stati oggetti 
            syncObjectStatesFromDatabase();

            System.out.println("Tutti i dati caricati con successo!");
        }
    }

    public boolean isChestOpenInDatabase(int chestId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT IS_OPEN FROM OBJECTS WHERE ID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, chestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("IS_OPEN");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore controllo stato cassa: " + e.getMessage());
        }
        return false; // Default: assume chiusa
    }

    public void syncObjectStatesFromDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String sql = "SELECT ID, IS_OPEN, IS_PUSHED FROM OBJECTS WHERE ID >= 100 AND ID <= 103";

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int objectId = rs.getInt("ID");
                    boolean isOpenDB = rs.getBoolean("IS_OPEN");
                    boolean isPushedDB = rs.getBoolean("IS_PUSHED");

                    // Trova l'oggetto in memoria e sincronizza
                    for (Room room : roomMap.values()) {
                        for (GameObjects obj : room.getObjects()) {
                            if (obj.getId() == objectId) {
                                if (obj.isOpen() != isOpenDB) {
                                    System.out.println("🔄 Sync cassa " + objectId + ": " + obj.isOpen() + " -> " + isOpenDB);
                                    obj.setOpen(isOpenDB);
                                }
                                if (obj.isPush() != isPushedDB) {
                                    obj.setPush(isPushedDB);
                                }
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Errore sincronizzazione stati oggetti: " + e.getMessage());
        }
    }

    /**
     * Metodo per popolare inizialmente la tabella ROOM_OBJECTS con gli oggetti
     * fissi Questo metodo dovrebbe essere chiamato SOLO al primo avvio per
     * popolare il database
     */
    public void populateInitialRoomObjects() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // Controlla se ROOM_OBJECTS è vuota
            String checkSql = "SELECT COUNT(*) FROM ROOM_OBJECTS";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
                rs.next();
                int count = rs.getInt(1);

                if (count == 0) {
                    System.out.println("Popolamento iniziale ROOM_OBJECTS...");

                    // Inserisci gli oggetti fissi nelle stanze
                    String insertSql = """
                    INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES
                    (0, 100),   -- Cassa nell'Ingresso
                    (1, 4),     -- Stringhe di ragnatela nella Stanza del Topo
                    (3, 101),   -- Cassa nel Dormitorio
                    (4, 102),   -- Cassa nella Sala delle Guardie
                    (6, 103)    -- Cassa nella Stanza delle Torture
                """;

                    try (Statement insertStmt = conn.createStatement()) {
                        insertStmt.executeUpdate(insertSql);
                        System.out.println("ROOM_OBJECTS popolata con oggetti iniziali");
                    }
                } else {
                    System.out.println("ROOM_OBJECTS già popolata (" + count + " elementi)");
                }
            }
        }
    }

    /**
     * Carica gli oggetti presenti nelle stanze - VERSIONE CORRETTA FINALE
     * Risolve i problemi di casse mancanti e contenuti non caricati
     */
    private void loadRoomObjects(Connection conn) throws SQLException {
        String sql = """
    SELECT r.ROOM_ID, o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
           w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
    FROM ROOM_OBJECTS r
    JOIN OBJECTS o ON r.OBJECT_ID = o.ID
    LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
    ORDER BY r.ROOM_ID, o.ID
""";

        System.out.println("Caricamento oggetti dalle stanze...");

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            int objectCount = 0;
            while (rs.next()) {
                int roomId = rs.getInt("ROOM_ID");
                int objectId = rs.getInt("ID");
                String objectName = rs.getString("NAME");

                System.out.println("Caricamento oggetto ID " + objectId + " (" + objectName + ") nella stanza " + roomId);

                Room room = roomMap.get(roomId);

                if (room != null) {
                    GameObjects obj = createObjectFromResultSet(rs);
                    if (obj != null) {
                        room.getObjects().add(obj);
                        objectCount++;
                        System.out.println("Oggetto " + obj.getName() + " aggiunto alla stanza " + room.getName());
                    } else {
                        System.out.println("ERRORE: Impossibile creare oggetto ID " + objectId);
                    }
                } else {
                    System.out.println("ERRORE: Stanza " + roomId + " non trovata per oggetto " + objectId);
                }
            }

            System.out.println("Totale oggetti caricati: " + objectCount);

            // DEBUG: Verifica contenuto stanze PRIMA di assicurarsi che le casse ci siano
            System.out.println("\n=== STATO STANZE DOPO CARICAMENTO INIZIALE ===");
            for (Room room : roomMap.values()) {
                if (!room.getObjects().isEmpty()) {
                    System.out.println("Stanza " + room.getId() + " (" + room.getName() + ") contiene " + room.getObjects().size() + " oggetti:");
                    for (GameObjects obj : room.getObjects()) {
                        System.out.println("  - " + obj.getName() + " (ID: " + obj.getId() + ", Apribile: " + obj.isOpenable() + ")");
                    }
                } else {
                    System.out.println("Stanza " + room.getId() + " (" + room.getName() + ") è VUOTA");
                }
            }
            System.out.println("===============================================\n");

            // Assicura che le casse siano presenti dopo il caricamento iniziale
            ensureChestsInRooms(conn);

            // DEBUG: Verifica di nuovo dopo aver assicurato le casse
            System.out.println("\n=== STATO STANZE DOPO INSERIMENTO CASSE ===");
            for (Room room : roomMap.values()) {
                boolean hasChest = false;
                for (GameObjects obj : room.getObjects()) {
                    if (obj.getId() >= 100 && obj.getId() <= 103) {
                        if (!hasChest) {
                            System.out.println("Stanza " + room.getId() + " (" + room.getName() + ") - CASSE:");
                            hasChest = true;
                        }
                        System.out.println("  - CASSA " + obj.getName() + " (ID: " + obj.getId() + ", Apribile: " + obj.isOpenable() + ", Aperta: " + obj.isOpen() + ")");
                    }
                }
                if (!hasChest) {
                    // Controlla se questa stanza dovrebbe avere una cassa
                    int expectedChestId = getExpectedChestForRoom(room.getId());
                    if (expectedChestId != -1) {
                        System.out.println("PROBLEMA: Stanza " + room.getId() + " (" + room.getName() + ") dovrebbe avere la cassa " + expectedChestId + " ma non c'è!");
                    }
                }
            }
            System.out.println("============================================\n");
        }
    }

    /**
     * Carica tutte le stanze dal database.
     */
    private void loadRooms(Connection conn) throws SQLException {
        String sql = "SELECT * FROM ROOMS ORDER BY ID";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Room room = new Room(
                        rs.getInt("ID"),
                        rs.getString("NAME"),
                        rs.getString("DESCRIPTION")
                );

                room.setLook(rs.getString("LOOK_DESCRIPTION"));
                room.setVisible(rs.getBoolean("VISIBLE"));

                roomMap.put(room.getId(), room);
                game.getRooms().add(room);
            }
        }

        System.out.println("Caricate " + roomMap.size() + " stanze");
    }

    /**
     * Imposta le connessioni tra le stanze.
     */
    private void loadRoomConnections(Connection conn) throws SQLException {
        String sql = "SELECT * FROM ROOM_CONNECTIONS";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int roomId = rs.getInt("ROOM_ID");
                Room room = roomMap.get(roomId);

                if (room != null) {
                    // Imposta le connessioni (NULL safe)
                    Integer northId = (Integer) rs.getObject("NORTH_ID");
                    if (northId != null) {
                        room.setNorth(roomMap.get(northId));
                    }

                    Integer southId = (Integer) rs.getObject("SOUTH_ID");
                    if (southId != null) {
                        room.setSouth(roomMap.get(southId));
                    }

                    Integer eastId = (Integer) rs.getObject("EAST_ID");
                    if (eastId != null) {
                        room.setEast(roomMap.get(eastId));
                    }

                    Integer westId = (Integer) rs.getObject("WEST_ID");
                    if (westId != null) {
                        room.setWest(roomMap.get(westId));
                    }
                }
            }
        }

        System.out.println("🔗 Connessioni tra stanze impostate");
    }

    /**
     * Determina quale cassa dovrebbe essere in una stanza
     */
    private int getExpectedChestForRoom(int roomId) {
        return switch (roomId) {
            case 0 ->
                100; // Ingresso
            case 3 ->
                101; // Dormitorio 
            case 4 ->
                102; // Sala Guardie
            case 6 ->
                103; // Torture
            default ->
                -1; // Nessuna cassa prevista
        };
    }

    private void ensureChestsInRooms(Connection conn) throws SQLException {
        System.out.println("Verifica e correzione presenza casse nelle stanze...");

        // Array CORRETTO con le casse e le loro stanze
        int[][] chestRoomPairs = {
            {0, 100}, // Stanza 0 (Ingresso), Cassa 100
            {3, 101}, // Stanza 3 (Dormitorio), Cassa 101  
            {4, 102}, // Stanza 4 (Sala Guardie), Cassa 102
            {6, 103} // Stanza 6 (Torture), Cassa 103
        };

        String checkSql = "SELECT COUNT(*) FROM ROOM_OBJECTS WHERE ROOM_ID = ? AND OBJECT_ID = ?";
        String insertSql = "INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";
        String getChestSql = "SELECT * FROM OBJECTS WHERE ID = ?";

        int totalInserted = 0;

        for (int[] pair : chestRoomPairs) {
            int roomId = pair[0];
            int chestId = pair[1];

            System.out.println("Controllo cassa " + chestId + " in stanza " + roomId);

            Room room = roomMap.get(roomId);
            if (room == null) {
                System.out.println("ERRORE: Stanza " + roomId + " non trovata!");
                continue;
            }

            // 1. Controlla se è già presente nel database
            boolean inDatabase = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, roomId);
                checkStmt.setInt(2, chestId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        inDatabase = true;
                    }
                }
            }

            // 2. Se non è nel database, aggiungila
            if (!inDatabase) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, roomId);
                    insertStmt.setInt(2, chestId);
                    insertStmt.executeUpdate();
                    totalInserted++;
                    System.out.println("  Cassa " + chestId + " aggiunta al database per stanza " + roomId);
                }
            }

            // 3. CORREZIONE CRITICA: Controlla se è presente in memoria
            boolean inMemory = false;
            for (GameObjects obj : room.getObjects()) {
                if (obj.getId() == chestId) {
                    inMemory = true;
                    System.out.println("  Cassa " + chestId + " già presente in memoria nella stanza " + roomId);
                    break;
                }
            }

            // 4. Se non è in memoria, caricala dal database
            if (!inMemory) {
                try (PreparedStatement chestStmt = conn.prepareStatement(getChestSql)) {
                    chestStmt.setInt(1, chestId);

                    try (ResultSet rs = chestStmt.executeQuery()) {
                        if (rs.next()) {
                            GameObjects chestObj = createObjectFromResultSet(rs);
                            if (chestObj != null) {
                                room.getObjects().add(chestObj);
                                System.out.println("  Cassa " + chestId + " (" + chestObj.getName() + ") aggiunta in memoria alla stanza " + roomId);
                            } else {
                                System.out.println("  ERRORE: Impossibile creare oggetto cassa " + chestId);
                            }
                        } else {
                            System.out.println("  ERRORE: Cassa " + chestId + " non trovata nella tabella OBJECTS");
                        }
                    }
                }
            }
        }

        System.out.println("Totale casse inserite nel database: " + totalInserted);

        // DEBUG finale: Verifica tutte le casse
        debugChestsInDatabase(conn);
    }

    /**
     * METODO PUBBLICO AGGIORNATO per forzare refresh completo delle casse
     */
    public void forceRefreshChests() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            System.out.println("REFRESH FORZATO CASSE - Rimozione da memoria e ricaricamento...");

            // 1. Rimuovi tutte le casse dalla memoria
            for (Room room : roomMap.values()) {
                room.getObjects().removeIf(obj -> obj.getId() >= 100 && obj.getId() <= 103);
            }

            // 2. Assicura presenza nel database e ricarica in memoria
            ensureChestsInRooms(conn);

            // 3. Sincronizza stati
            syncObjectStatesFromDatabase();

            System.out.println("REFRESH COMPLETATO");

        } catch (SQLException e) {
            System.err.println("Errore nel refresh forzato casse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ripristina gli oggetti che dovrebbero sempre essere presenti nelle stanze
     * (non contenuti nelle casse)
     */
    private void restoreFixedRoomObjects(Connection conn) throws SQLException {
        System.out.println(" Ripristino oggetti fissi nelle stanze...");

        // Oggetti che dovrebbero sempre essere presenti in specifiche stanze
        int[][] fixedObjectPairs = {
            {1, 4} // Stanza 1 (Topo), Oggetto 4 (stringhe ragnatela)
        // Aggiungi altri oggetti fissi qui se necessario
        };

        String checkSql = "SELECT COUNT(*) FROM ROOM_OBJECTS WHERE ROOM_ID = ? AND OBJECT_ID = ?";
        String insertSql = "INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";

        int totalRestored = 0;

        for (int[] pair : fixedObjectPairs) {
            int roomId = pair[0];
            int objectId = pair[1];

            // Controlla se l'oggetto è già nella stanza
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, roomId);
                checkStmt.setInt(2, objectId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // L'oggetto non c'è, ripristinalo
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, roomId);
                            insertStmt.setInt(2, objectId);
                            insertStmt.executeUpdate();
                            totalRestored++;
                            System.out.println("  ✅ Oggetto " + objectId + " ripristinato nella stanza " + roomId);
                        }
                    } else {
                        System.out.println("  ℹ️ Oggetto " + objectId + " già presente nella stanza " + roomId);
                    }
                }
            }
        }

        System.out.println(" Totale oggetti fissi ripristinati: " + totalRestored);
    }

    /**
     * Metodo chiamato quando il boss viene sconfitto fa cadere la chiave
     * dell'uscita nella stanza del boss
     */
    public void onBossDefeated(int bossRoomId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            System.out.println("Il Cane Demone è stato sconfitto! La chiave dell'uscita cade a terra...");

            // Aggiungi la chiave dell'uscita (ID 11) alla stanza del boss
            String insertKeySQL = "INSERT OR IGNORE INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertKeySQL)) {
                stmt.setInt(1, bossRoomId);
                stmt.setInt(2, 11); // Chiave dell'uscita
                stmt.executeUpdate();
            }

            // Aggiungi anche alla memoria di gioco se il room è già caricato
            Room bossRoom = roomMap.get(bossRoomId);
            if (bossRoom != null) {
                // Verifica che non ci sia già
                boolean alreadyPresent = bossRoom.getObjects().stream()
                        .anyMatch(obj -> obj.getId() == 11);

                if (!alreadyPresent) {
                    // Carica l'oggetto dal database
                    String getObjectSQL = "SELECT * FROM OBJECTS WHERE ID = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(getObjectSQL)) {
                        stmt.setInt(1, 11);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                GameObjects exitKey = createObjectFromResultSet(rs);
                                if (exitKey != null) {
                                    bossRoom.getObjects().add(exitKey);
                                    System.out.println("Chiave dell'uscita aggiunta alla stanza del boss");
                                }
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore nel gestire la sconfitta del boss: " + e.getMessage());
        }
    }

    /**
     * Verifica se il giocatore ha la chiave corretta per aprire un oggetto
     */
    public boolean hasKeyForObject(int objectId, List<GameObjects> inventory) {
        // Mappatura oggetti -> chiavi richieste
        return switch (objectId) {
            case 13 -> // Cella principessa
                inventory.stream().anyMatch(obj -> obj.getId() == 10); // chiave cella principessa
            case 15 -> // Porta est
                inventory.stream().anyMatch(obj -> obj.getId() == 11); // chiave uscita
            default ->
                true; // Altri oggetti non richiedono chiavi specifiche
        };
    }

    /**
     * Apre la cella della principessa e libera la principessa
     */
    public boolean openPrincessCell(Room bossRoom, List<GameObjects> inventory) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // Verifica che il giocatore abbia la chiave cella principessa (ID 10)
            boolean hasKey = inventory.stream().anyMatch(obj -> obj.getId() == 10);
            if (!hasKey) {
                return false; // Non ha la chiave
            }

            // Trova la cella nella stanza
            GameObjects cell = bossRoom.getObjects().stream()
                    .filter(obj -> obj.getId() == 13)
                    .findFirst()
                    .orElse(null);

            if (cell != null) {
                // Apri la cella
                cell.setOpen(true);
                updateObjectState(cell);

                // Libera la principessa aggiungendola alla stanza
                String getPrincessSQL = "SELECT * FROM OBJECTS WHERE ID = 14";
                try (PreparedStatement stmt = conn.prepareStatement(getPrincessSQL)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            GameObjects princess = createObjectFromResultSet(rs);
                            if (princess != null) {
                                bossRoom.getObjects().add(princess);

                                // Aggiungi la principessa alla stanza nel database
                                String insertPrincessSQL = "INSERT OR IGNORE INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";
                                try (PreparedStatement insertStmt = conn.prepareStatement(insertPrincessSQL)) {
                                    insertStmt.setInt(1, bossRoom.getId());
                                    insertStmt.setInt(2, 14);
                                    insertStmt.executeUpdate();
                                }

                                System.out.println("La principessa è libera!");
                                return true;
                            }
                        }
                    }
                }
            }

            return false;

        } catch (SQLException e) {
            System.err.println("Errore nell'aprire la cella della principessa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Apre la porta dell'uscita verso est
     */
    public boolean openExitDoor(Room bossRoom, List<GameObjects> inventory) {
        // Verifica che il giocatore abbia la chiave dell'uscita (ID 11)
        boolean hasKey = inventory.stream().anyMatch(obj -> obj.getId() == 11);
        if (!hasKey) {
            return false; // Non ha la chiave
        }

        // Trova la porta nella stanza
        GameObjects door = bossRoom.getObjects().stream()
                .filter(obj -> obj.getId() == 15)
                .findFirst()
                .orElse(null);

        if (door != null) {
            // Apri la porta
            door.setOpen(true);
            updateObjectState(door);
            System.out.println("La porta dell'uscita si apre con un rumore metallico!");
            return true;
        }

        return false;
    }

    /**
     * Verifica se la principessa è stata liberata
     */
    public boolean isPrincessFree(Room bossRoom) {
        return bossRoom.getObjects().stream()
                .anyMatch(obj -> obj.getId() == 14); // Principessa presente nella stanza
    }

    /**
     * Verifica se la porta dell'uscita è aperta
     */
    public boolean isExitDoorOpen(Room bossRoom) {
        return bossRoom.getObjects().stream()
                .filter(obj -> obj.getId() == 15)
                .findFirst()
                .map(GameObjects::isOpen)
                .orElse(false);
    }

    /**
     * 
     */
    private String getChestContentQuery(int chestId) {
        switch (chestId) {
            case 100: // Cassa nell'Ingresso
                return """
        SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
               w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
        FROM OBJECTS o
        LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
        WHERE o.ID IN (1, 2)
        """; // chiave ingresso, pozione cura

            case 101: // Cassa nel Dormitorio
                return """
        SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
               w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
        FROM OBJECTS o
        LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
        WHERE o.ID IN (5, 6)
        """; // pozione cura totale, bastone

            case 102: // Cassa nella Sala Guardie
                return """
        SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
               w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
        FROM OBJECTS o
        LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
        WHERE o.ID IN (8, 9)
        """; // LIBRO INCANTESIMO FUOCO, VELENO

            case 103: // Cassa nella Stanza delle Torture - AGGIORNATA
                return """
        SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
               w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
        FROM OBJECTS o
        LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
        WHERE o.ID IN (10)
        """; // chiave cella principessa

            default:
                return null; // Cassa vuota o sconosciuta
        }
    }

    /**
     * Metodo per gestire il movimento del giocatore con la principessa Una
     * volta che la principessa è libera, limita i movimenti verso l'uscita
     */
    public boolean canMoveToRoom(Room currentRoom, Room targetRoom, boolean princessIsFree) {
        if (!princessIsFree) {
            return true; // Movimento normale se la principessa non è ancora libera
        }

        // Se la principessa è libera, può muoversi solo verso l'uscita (stanza 8)
        // o rimanere nella stanza del boss (stanza 7)
        int currentRoomId = currentRoom.getId();
        int targetRoomId = targetRoom.getId();

        // Dalla stanza del boss (7) può andare solo all'uscita (8)
        if (currentRoomId == 7) {
            return targetRoomId == 8;
        }

        // Dall'uscita (8) può tornare solo al boss (7)
        if (currentRoomId == 8) {
            return targetRoomId == 7;
        }

        // Da altre stanze non dovrebbe essere possibile se la principessa è libera
        return false;
    }

    /**
     * Reset completo di tutte le casse
     */
    public void resetAllChests() {
        System.out.println("Resetting tutte le casse del gioco...");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // 1. Verifica che le casse esistano nella tabella OBJECTS
            String checkChestsExistSql = "SELECT ID FROM OBJECTS WHERE ID IN (100, 101, 102, 103) ORDER BY ID";
            List<Integer> existingChests = new ArrayList<>();

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkChestsExistSql)) {
                while (rs.next()) {
                    existingChests.add(rs.getInt("ID"));
                }
            }

            System.out.println("Casse esistenti nel database: " + existingChests);

            if (existingChests.isEmpty()) {
                System.out.println("NESSUNA CASSA TROVATA NEL DATABASE! Creazione casse...");
                createMissingChests(conn);

                // Ricarica la lista dopo la creazione
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkChestsExistSql)) {
                    while (rs.next()) {
                        existingChests.add(rs.getInt("ID"));
                    }
                }
            }

            // 2. Chiudi tutte le casse esistenti
            if (!existingChests.isEmpty()) {
                String chestIds = existingChests.stream().map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","));
                String resetChestsSql = "UPDATE OBJECTS SET IS_OPEN = FALSE WHERE ID IN (" + chestIds + ")";
                try (Statement stmt = conn.createStatement()) {
                    int updated = stmt.executeUpdate(resetChestsSql);
                    System.out.println(updated + " casse chiuse");
                }
            }

            // 3. Rimuovi TUTTI i contenuti dalle stanze (tranne oggetti fissi)
            String removeContentSql = """
        DELETE FROM ROOM_OBJECTS 
        WHERE OBJECT_ID NOT IN (
            SELECT ID FROM OBJECTS WHERE OBJECT_TYPE IN ('DECORATION', 'FIXED')
        )
        AND OBJECT_ID NOT IN (100, 101, 102, 103, 4, 13, 15)
    """;
            try (Statement stmt = conn.createStatement()) {
                int removed = stmt.executeUpdate(removeContentSql);
                System.out.println(removed + " oggetti rimossi dalle stanze");
            }

            // 4. Posiziona le casse nelle stanze corrette
            int[][] chestRoomMapping = {
                {0, 100}, // Ingresso -> Cassa 100
                {3, 101}, // Dormitorio -> Cassa 101  
                {4, 102}, // Sala Guardie -> Cassa 102
                {6, 103} // Sala Torture -> Cassa 103
            };

            String insertChestSql = "MERGE INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) KEY(ROOM_ID, OBJECT_ID) VALUES (?, ?)";
            int chestsPlaced = 0;

            try (PreparedStatement stmt = conn.prepareStatement(insertChestSql)) {
                for (int[] mapping : chestRoomMapping) {
                    int roomId = mapping[0];
                    int chestId = mapping[1];

                    if (existingChests.contains(chestId)) {
                        stmt.setInt(1, roomId);
                        stmt.setInt(2, chestId);
                        stmt.addBatch();
                        chestsPlaced++;
                    } else {
                        System.out.println("Cassa " + chestId + " non esiste, salto...");
                    }
                }

                if (chestsPlaced > 0) {
                    stmt.executeBatch();
                    System.out.println(chestsPlaced + " casse posizionate nelle stanze");
                }
            }

            // 5. Reset stati di gioco importanti
            String resetGameStateSQL = """
        UPDATE OBJECTS SET IS_OPEN = FALSE 
        WHERE ID IN (13, 15); -- Cella principessa e porta uscita
        
        DELETE FROM ROOM_OBJECTS 
        WHERE OBJECT_ID IN (11, 14); -- Rimuovi chiave uscita e principessa libera
    """;

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(resetGameStateSQL);
                System.out.println("Stati del finale del gioco resettati");
            }

            System.out.println("Reset casse completato!");

        } catch (SQLException e) {
            System.err.println("Errore nel reset delle casse: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore nel reset delle casse", e);
        }
    }

    /**
     * Crea le casse mancanti nel database
     */
    private void createMissingChests(Connection conn) throws SQLException {
        System.out.println("Creando casse mancanti...");

        Object[][] chestDefinitions = {
            {100, "cassa di legno", "Una robusta cassa di legno con rinforzi metallici.", true, true, false},
            {101, "cassa del dormitorio", "Una cassa personale con incisioni decorative.", true, true, false},
            {102, "cassa delle guardie", "Una cassa militare con serratura robusta.", true, true, false},
            {103, "cassa della tortura", "Una cassa dall'aspetto sinistro, macchiata di scuro.", true, true, false}
        };

        String insertChestSql = """
    INSERT OR IGNORE INTO OBJECTS 
    (ID, NAME, DESCRIPTION, OBJECT_TYPE, PICKUPABLE, OPENABLE, IS_OPEN) 
    VALUES (?, ?, ?, 'CONTAINER', ?, ?, ?)
""";

        try (PreparedStatement stmt = conn.prepareStatement(insertChestSql)) {
            for (Object[] chest : chestDefinitions) {
                stmt.setInt(1, (Integer) chest[0]);      // ID
                stmt.setString(2, (String) chest[1]);    // NAME
                stmt.setString(3, (String) chest[2]);    // DESCRIPTION
                stmt.setBoolean(4, (Boolean) chest[3]);  // IS_PICKUPABLE
                stmt.setBoolean(5, (Boolean) chest[4]);  // IS_OPENABLE
                stmt.setBoolean(6, (Boolean) chest[5]);  // IS_OPEN
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            int created = java.util.Arrays.stream(results).sum();
            System.out.println(created + " casse create nel database");
        }
    }

    /**
     * Rimuove un oggetto specifico da una stanza specifica nel database
     *
     * @param objectId ID dell'oggetto da rimuovere
     * @param roomId ID della stanza da cui rimuovere
     */
    public void removeObjectFromRoom(int objectId, int roomId) {
        String sql = "DELETE FROM ROOM_OBJECTS WHERE OBJECT_ID = ? AND ROOM_ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, roomId);

            int removed = stmt.executeUpdate();
            if (removed > 0) {
                System.out.println("Rimosso oggetto " + objectId + " dalla stanza " + roomId + " nel database");
            }

        } catch (SQLException e) {
            System.err.println("Errore nella rimozione oggetto " + objectId + " dalla stanza " + roomId + ": " + e.getMessage());
        }
    }

    /**
     * Debug delle casse nel database
     */
    private void debugChestsInDatabase(Connection conn) throws SQLException {
        System.out.println("🔍 DEBUG: Stato attuale delle casse nel database:");

        String debugSql = """
        SELECT ro.ROOM_ID, ro.OBJECT_ID, o.NAME, o.IS_OPEN, r.NAME as ROOM_NAME
        FROM ROOM_OBJECTS ro
        JOIN OBJECTS o ON ro.OBJECT_ID = o.ID
        JOIN ROOMS r ON ro.ROOM_ID = r.ID
        WHERE ro.OBJECT_ID >= 100 AND ro.OBJECT_ID <= 103
        ORDER BY ro.ROOM_ID, ro.OBJECT_ID
    """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(debugSql)) {
            while (rs.next()) {
                int roomId = rs.getInt("ROOM_ID");
                int chestId = rs.getInt("OBJECT_ID");
                String chestName = rs.getString("NAME");
                boolean isOpen = rs.getBoolean("IS_OPEN");
                String roomName = rs.getString("ROOM_NAME");

                System.out.println("  🏠 Stanza " + roomId + " (" + roomName + "): "
                        + "Cassa " + chestId + " (" + chestName + ") - "
                        + (isOpen ? "APERTA" : "CHIUSA"));
            }
        }
    }

    // Aggiungi questo metodo pubblico a GameLoader
    public void ensureChestsInRooms() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            ensureChestsInRooms(conn);
        } catch (SQLException e) {
            System.err.println("Errore nell'assicurare le casse nelle stanze: " + e.getMessage());
        }
    }

    /**
     * Carica i personaggi dal database - MODIFICATA per non ricaricare nemici
     * morti
     */
    private void loadCharacters(Connection conn) throws SQLException {
        String sql = "SELECT * FROM CHARACTERS ORDER BY ID";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("🧑 Caricamento personaggi...");
            int characterCount = 0;

            while (rs.next()) {
                GameCharacter character = new GameCharacter(
                        rs.getInt("ID"),
                        rs.getString("NAME"),
                        rs.getString("DESCRIPTION"),
                        rs.getInt("MAX_HP"),
                        rs.getInt("ATTACK"),
                        rs.getInt("DEFENSE"),
                        CharacterType.valueOf(rs.getString("CHARACTER_TYPE"))
                );

                character.setCurrentHp(rs.getInt("CURRENT_HP"));

                // Se non è vivo, imposta HP a 0
                boolean isAlive = rs.getBoolean("IS_ALIVE");
                if (!isAlive) {
                    character.setCurrentHp(0);
                }

                // CORREZIONE CRITICA: Aggiungi alla lista appropriata
                CharacterType type = character.getType();

                if (type == CharacterType.PLAYER) {
                    game.setPlayer(character);
                    System.out.println(" Giocatore caricato: " + character.getName() + " (HP: " + character.getCurrentHp() + ")");
                    characterCount++;

                } else if (character.getType() == CharacterType.GOBLIN
                        || character.getType() == CharacterType.GIANT_RAT
                        || character.getType() == CharacterType.DEMON_DOG) {

                    // Solo aggiungi nemici se sono vivi
                    if (isAlive && character.getCurrentHp() > 0) {
                        int roomId = rs.getInt("ROOM_ID");
                        Room room = roomMap.get(roomId);
                        if (room != null) {
                            room.getEnemies().add(character);
                            System.out.println(" Nemico " + character.getName() + " caricato nella stanza " + roomId);
                            characterCount++;
                        } else {
                            System.out.println("ERRORE: Stanza " + roomId + " non trovata per nemico " + character.getName());
                        }
                    } else {
                        System.out.println(" Nemico " + character.getName() + " non caricato (morto)");
                    }
                }
            }

            System.out.println(" Caricati " + characterCount + " personaggi");

            // VERIFICA CRITICA: Assicurati che il giocatore sia stato caricato
            if (game.getPlayer() == null) {
                System.err.println(" ERRORE CRITICO: Giocatore non caricato dal database!");
                throw new SQLException("Impossibile caricare il giocatore dal database");
            } else {
                System.out.println(" Giocatore verificato: " + game.getPlayer().getName());
            }
        }
    }

    /**
     * Carica l'inventario del giocatore.
     */
    private void loadPlayerInventory(Connection conn) throws SQLException {
        String sql = """
            SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
                   w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
            FROM INVENTORY i
            JOIN OBJECTS o ON i.OBJECT_ID = o.ID
            LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
            WHERE i.CHARACTER_ID = 0
            ORDER BY o.ID
        """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            int inventoryCount = 0;
            while (rs.next()) {
                GameObjects obj = createObjectFromResultSet(rs);
                if (obj != null) {
                    game.getInventory().add(obj);
                    inventoryCount++;
                }
            }

            System.out.println(" Caricati " + inventoryCount + " oggetti nell'inventario");
        }
    }

    /**
     * Imposta la stanza corrente del giocatore.
     */
    private void setPlayerCurrentRoom(Connection conn) throws SQLException {
        String sql = "SELECT ROOM_ID FROM CHARACTERS WHERE CHARACTER_TYPE = 'PLAYER' AND ID = 0";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int roomId = rs.getInt("ROOM_ID");
                Room currentRoom = roomMap.get(roomId);

                if (currentRoom != null) {
                    game.setCurrentRoom(currentRoom);
                    System.out.println("Giocatore posizionato in: " + currentRoom.getName());
                } else {
                    // Fallback alla prima stanza
                    if (!game.getRooms().isEmpty()) {
                        game.setCurrentRoom(game.getRooms().get(0));
                        System.out.println("Stanza non trovata, posizionato nella prima stanza");
                    }
                }
            }
        }
    }

    /**
     * Crea un oggetto dal ResultSet, gestendo anche le armi.
     */
    private GameObjects createObjectFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        String name = rs.getString("NAME");
        String description = rs.getString("DESCRIPTION");
        String objectType = rs.getString("OBJECT_TYPE");

        GameObjects obj;

        // Se è un'arma, crea un oggetto Weapon
        if ("WEAPON".equals(objectType) && rs.getString("WEAPON_TYPE") != null) {
            WeaponType weaponType = WeaponType.valueOf(rs.getString("WEAPON_TYPE"));
            int attackBonus = rs.getInt("ATTACK_BONUS");
            int criticalChance = rs.getInt("CRITICAL_CHANCE");
            int criticalMultiplier = rs.getInt("CRITICAL_MULTIPLIER");

            obj = new Weapon(id, name, description, attackBonus, weaponType,
                    criticalChance, criticalMultiplier);

            // Gestisci veleno se presente
            if (rs.getBoolean("IS_POISONED")) {
                int poisonDamage = rs.getInt("POISON_DAMAGE");
                ((Weapon) obj).applyPoison(poisonDamage);

                String specialEffect = rs.getString("SPECIAL_EFFECT");
                if (specialEffect != null) {
                    ((Weapon) obj).setSpecialEffect(specialEffect);
                }
            }
        } else {
            // Oggetto normale
            obj = new GameObjects(id, name, description);
        }

        // Imposta le proprietà base
        obj.setOpenable(rs.getBoolean("OPENABLE"));
        obj.setPickupable(rs.getBoolean("PICKUPABLE"));
        obj.setPushable(rs.getBoolean("PUSHABLE"));
        obj.setOpen(rs.getBoolean("IS_OPEN"));
        obj.setPush(rs.getBoolean("IS_PUSHED"));

        // Gestisci gli alias
        String aliases = rs.getString("ALIASES");
        if (aliases != null && !aliases.trim().isEmpty()) {
            String[] aliasArray = aliases.split(",");
            HashSet<String> aliasSet = new HashSet<>();
            for (String alias : aliasArray) {
                aliasSet.add(alias.trim());
            }
            obj.setAlias(aliasSet);
        }

        return obj;
    }

    /**
     * Salva lo stato del giocatore nel database.
     */
    public void savePlayerState() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // Aggiorna posizione del giocatore
            if (game.getCurrentRoom() != null) {
                String sql = "UPDATE CHARACTERS SET ROOM_ID = ? WHERE ID = 0 AND CHARACTER_TYPE = 'PLAYER'";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, game.getCurrentRoom().getId());
                    stmt.executeUpdate();
                }
            }

            // Aggiorna HP del giocatore se disponibile
            if (game.getPlayer() != null) {
                String sql = "UPDATE CHARACTERS SET CURRENT_HP = ?, IS_ALIVE = ? WHERE ID = 0 AND CHARACTER_TYPE = 'PLAYER'";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, game.getPlayer().getCurrentHp());
                    stmt.setBoolean(2, game.getPlayer().isAlive());
                    stmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            System.err.println("Errore nel salvataggio dello stato del giocatore: " + e.getMessage());
        }
    }

    /**
     * Aggiorna lo stato di un oggetto nel database.
     */
    public void updateObjectState(GameObjects obj) {
        String sql = "UPDATE OBJECTS SET IS_OPEN = ?, IS_PUSHED = ? WHERE ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, obj.isOpen());
            stmt.setBoolean(2, obj.isPush());
            stmt.setInt(3, obj.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento oggetto " + obj.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Apre una cassa e sposta i suoi oggetti dal database alla stanza VERSIONE
     * CORRETTA CHE RISOLVE I PROBLEMI DELLE CASSE
     *
     * @param chestId ID della cassa
     * @param room stanza dove si trova la cassa
     * @return lista degli oggetti trovati
     */
    public List<GameObjects> openChest(int chestId, Room room) {
        List<GameObjects> foundObjects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // Query per ottenere gli oggetti che dovrebbero essere nella cassa
            String sql = getChestContentQuery(chestId);
            if (sql == null) {
                System.out.println(" Nessuna query definita per cassa " + chestId);
                return foundObjects; // Cassa vuota
            }

            System.out.println(" Apertura cassa " + chestId + " nella stanza " + room.getId());
            System.out.println(" Query SQL: " + sql);

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                int objectCount = 0;
                while (rs.next()) {
                    int objectId = rs.getInt("ID");
                    String objectName = rs.getString("NAME");
                    System.out.println(" Trovato oggetto: " + objectId + " - " + objectName);

                    GameObjects obj = createObjectFromResultSet(rs);
                    if (obj != null) {
                        foundObjects.add(obj);
                        room.getObjects().add(obj);

                        // Aggiungi l'oggetto alla tabella ROOM_OBJECTS se non c'è già
                        addObjectToRoom(conn, room.getId(), obj.getId());
                        objectCount++;
                        System.out.println(" Oggetto " + obj.getName() + " aggiunto alla stanza " + room.getName());
                    } else {
                        System.out.println(" Impossibile creare oggetto ID " + objectId);
                    }
                }

                System.out.println(" Totale oggetti trovati nella cassa " + chestId + ": " + objectCount);

            }

        } catch (SQLException e) {
            System.err.println(" Errore nell'apertura cassa " + chestId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return foundObjects;
    }

    /**
     * Aggiunge un oggetto a una stanza nel database (se non c'è già)
     *
     * @param conn connessione al database
     * @param roomId ID della stanza
     * @param objectId ID dell'oggetto
     */
    private void addObjectToRoom(Connection conn, int roomId, int objectId) {
        try {
            // Controlla se l'oggetto è già nella stanza
            String checkSql = "SELECT COUNT(*) FROM ROOM_OBJECTS WHERE ROOM_ID = ? AND OBJECT_ID = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, roomId);
                checkStmt.setInt(2, objectId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // L'oggetto non c'è, aggiungilo
                        String insertSql = "INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, roomId);
                            insertStmt.setInt(2, objectId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("️ Errore nell'aggiunta oggetto alla stanza: " + e.getMessage());
        }
    }

    /**
     * Segna una cassa come aperta nel database
     *
     * @param chestId ID della cassa
     */
    public void markChestAsOpened(int chestId) {
        String sql = "UPDATE OBJECTS SET IS_OPEN = TRUE WHERE ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chestId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nel segnare cassa come aperta: " + e.getMessage());
        }
    }

    /**
     * Sposta un oggetto dall'inventario a una stanza.
     */
    public void moveObjectToRoom(GameObjects obj, Room room) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            conn.setAutoCommit(false);

            try {
                // Rimuovi dall'inventario
                String deleteSql = "DELETE FROM INVENTORY WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Rimuovi da eventuali altre stanze
                String deleteRoomSql = "DELETE FROM ROOM_OBJECTS WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteRoomSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Aggiungi alla nuova stanza
                String insertSql = "INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setInt(1, room.getId());
                    stmt.setInt(2, obj.getId());
                    stmt.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto in stanza: " + e.getMessage());
        }
    }

    /**
     * Sposta un oggetto da una stanza all'inventario.
     */
    public void moveObjectToInventory(GameObjects obj, int playerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            conn.setAutoCommit(false);

            try {
                // Rimuovi da tutte le stanze
                String deleteRoomSql = "DELETE FROM ROOM_OBJECTS WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteRoomSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Verifica che non sia già nell'inventario
                String checkSql = "SELECT COUNT(*) FROM INVENTORY WHERE CHARACTER_ID = ? AND OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, obj.getId());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            // Aggiungi all'inventario solo se non è già presente
                            String insertSql = "INSERT INTO INVENTORY (CHARACTER_ID, OBJECT_ID) VALUES (?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                insertStmt.setInt(1, playerId);
                                insertStmt.setInt(2, obj.getId());
                                insertStmt.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
        }
    }

    /**
     * Rimuove un oggetto dal gioco (distrugge).
     */
    public void removeObject(GameObjects obj) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            conn.setAutoCommit(false);

            try {
                // Rimuovi da inventario
                String deleteInvSql = "DELETE FROM INVENTORY WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteInvSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Rimuovi da stanze
                String deleteRoomSql = "DELETE FROM ROOM_OBJECTS WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteRoomSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Se è un'arma, rimuovi dai dati arma
                String deleteWeaponSql = "DELETE FROM WEAPONS WHERE OBJECT_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteWeaponSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                // Infine rimuovi l'oggetto stesso
                String deleteObjSql = "DELETE FROM OBJECTS WHERE ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteObjSql)) {
                    stmt.setInt(1, obj.getId());
                    stmt.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Errore nella rimozione dell'oggetto: " + e.getMessage());
        }
    }

    /**
     * Aggiorna lo stato di un personaggio.
     */
    public void updateCharacterState(GameCharacter character) {
        String sql = "UPDATE CHARACTERS SET CURRENT_HP = ?, IS_ALIVE = ? WHERE ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, character.getCurrentHp());
            stmt.setBoolean(2, character.isAlive());
            stmt.setInt(3, character.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento personaggio " + character.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Ottiene statistiche del database per debug.
     */
    public void printDatabaseStats() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            System.out.println("\n=== STATISTICHE DATABASE ===");

            String[] tables = {"ROOMS", "OBJECTS", "CHARACTERS", "WEAPONS", "ROOM_OBJECTS", "INVENTORY"};

            for (String table : tables) {
                String sql = "SELECT COUNT(*) FROM " + table;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                    if (rs.next()) {
                        System.out.printf("%-15s: %d record%n", table, rs.getInt(1));
                    }
                } catch (SQLException e) {
                    System.out.printf("%-15s: ERRORE - %s%n", table, e.getMessage());
                }
            }

            System.out.println("============================\n");

        } catch (SQLException e) {
            System.err.println("Errore nel recupero statistiche: " + e.getMessage());
        }
    }

    /**
     * Verifica l'integrità del database.
     */
    public boolean verifyDatabaseIntegrity() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            boolean allGood = true;

            // Verifica che ogni stanza nelle connessioni esista
            String sql = """
                SELECT DISTINCT c.NORTH_ID, c.SOUTH_ID, c.EAST_ID, c.WEST_ID
                FROM ROOM_CONNECTIONS c
                WHERE c.NORTH_ID IS NOT NULL 
                   OR c.SOUTH_ID IS NOT NULL 
                   OR c.EAST_ID IS NOT NULL 
                   OR c.WEST_ID IS NOT NULL
            """;

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    for (int i = 1; i <= 4; i++) {
                        Integer roomId = (Integer) rs.getObject(i);
                        if (roomId != null && !roomMap.containsKey(roomId)) {
                            System.err.println("⚠️ Connessione a stanza inesistente: " + roomId);
                            allGood = false;
                        }
                    }
                }
            }

            // Verifica oggetti orfani
            String orphanSql = """
                SELECT o.ID, o.NAME FROM OBJECTS o
                WHERE o.ID NOT IN (SELECT OBJECT_ID FROM ROOM_OBJECTS)
                  AND o.ID NOT IN (SELECT OBJECT_ID FROM INVENTORY)
            """;

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(orphanSql)) {

                while (rs.next()) {
                    System.err.println("Oggetto orfano trovato: " + rs.getInt(1) + " - " + rs.getString(2));
                    allGood = false;
                }
            }

            if (allGood) {
                System.out.println("Integrità database verificata");
            }

            return allGood;

        } catch (SQLException e) {
            System.err.println("Errore nella verifica integrità: " + e.getMessage());
            return false;
        }
    }
}
