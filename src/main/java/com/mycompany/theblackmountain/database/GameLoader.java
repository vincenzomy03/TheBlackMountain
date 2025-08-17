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

    // Aggiungi questo metodo al GameLoader.java dopo resetAllChests()
    /**
     * Ripristina gli oggetti che dovrebbero sempre essere presenti nelle stanze
     * (non contenuti nelle casse)
     */
    private void restoreFixedRoomObjects(Connection conn) throws SQLException {
        System.out.println("🔧 Ripristino oggetti fissi nelle stanze...");

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

        System.out.println("🔧 Totale oggetti fissi ripristinati: " + totalRestored);
    }

    /**
     * reset completo delle casse
     */
    public void resetAllChests() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            System.out.println("🔄 Reset di tutte le casse...");

            // 1. Chiudi tutte le casse
            String resetChestsSql = "UPDATE OBJECTS SET IS_OPEN = FALSE WHERE ID >= 100 AND ID <= 103";
            try (PreparedStatement stmt = conn.prepareStatement(resetChestsSql)) {
                int updated = stmt.executeUpdate();
                System.out.println("🔒 " + updated + " casse chiuse");
            }

            // 2. CORREZIONE: Rimuovi SOLO il contenuto delle casse dalle stanze
            // Mantieni sempre l'oggetto 4 (stringhe ragnatela) nella stanza 1
            String removeContentSql = """
        DELETE FROM ROOM_OBJECTS 
        WHERE OBJECT_ID IN (1, 2, 5, 6, 8, 9, 10)
        AND NOT (ROOM_ID = 1 AND OBJECT_ID = 4)
        """;

            try (PreparedStatement stmt = conn.prepareStatement(removeContentSql)) {
                int removed = stmt.executeUpdate();
                System.out.println("📤 Rimossi " + removed + " oggetti contenuti nelle casse dalle stanze");
            }

            // 3. ASSICURATI CHE LE CASSE SIANO NELLE STANZE CORRETTE
            ensureChestsInRooms(conn);

            // 4. Ripristina oggetti fissi che dovrebbero sempre essere presenti
            restoreFixedRoomObjects(conn);

            System.out.println(" Reset casse completato!");

        } catch (SQLException e) {
            System.err.println(" Errore nel reset delle casse: " + e.getMessage());
            e.printStackTrace();
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
     * NUOVO METODO: Debug delle casse nel database
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

                // Aggiungi alla lista appropriata
                CharacterType type = character.getType();
                if (type == CharacterType.PLAYER) {
                    game.setPlayer(character);
                } else if (character.getType() == CharacterType.GOBLIN
                        || character.getType() == CharacterType.GIANT_RAT
                        || character.getType() == CharacterType.DEMON_DOG) {

                    // *** NUOVO: Solo aggiungi nemici se sono vivi ***
                    if (isAlive && character.getCurrentHp() > 0) {
                        int roomId = rs.getInt("ROOM_ID");
                        Room room = roomMap.get(roomId);
                        if (room != null) {
                            room.getEnemies().add(character);
                        }
                    }
                }
            }
        }

        System.out.println("Personaggi caricati");
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

            System.out.println("🎒 Caricati " + inventoryCount + " oggetti nell'inventario");
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
                System.out.println("❌ Nessuna query definita per cassa " + chestId);
                return foundObjects; // Cassa vuota
            }

            System.out.println("🔍 Apertura cassa " + chestId + " nella stanza " + room.getId());
            System.out.println("📋 Query SQL: " + sql);

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                int objectCount = 0;
                while (rs.next()) {
                    int objectId = rs.getInt("ID");
                    String objectName = rs.getString("NAME");
                    System.out.println("🎁 Trovato oggetto: " + objectId + " - " + objectName);

                    GameObjects obj = createObjectFromResultSet(rs);
                    if (obj != null) {
                        foundObjects.add(obj);
                        room.getObjects().add(obj);

                        // Aggiungi l'oggetto alla tabella ROOM_OBJECTS se non c'è già
                        addObjectToRoom(conn, room.getId(), obj.getId());
                        objectCount++;
                        System.out.println("✅ Oggetto " + obj.getName() + " aggiunto alla stanza " + room.getName());
                    } else {
                        System.out.println("❌ Impossibile creare oggetto ID " + objectId);
                    }
                }

                System.out.println("📊 Totale oggetti trovati nella cassa " + chestId + ": " + objectCount);

            }

        } catch (SQLException e) {
            System.err.println("❌ Errore nell'apertura cassa " + chestId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return foundObjects;
    }

    /**
     * Restituisce la query SQL per il contenuto di una cassa specifica
     *
     * @param chestId ID della cassa
     * @return query SQL o null se la cassa è vuota
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

            case 103: // Cassa nella Stanza delle Torture
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
            System.err.println("⚠️ Errore nell'aggiunta oggetto alla stanza: " + e.getMessage());
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
