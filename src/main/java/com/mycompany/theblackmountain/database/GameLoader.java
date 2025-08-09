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
            System.out.println("📋 Caricamento dati dal database...");

            // 1. Carica tutte le stanze
            loadRooms(conn);

            // 2. Imposta le connessioni tra stanze
            loadRoomConnections(conn);

            // 3. Carica gli oggetti nelle stanze
            loadRoomObjects(conn);

            // 4. Carica i personaggi
            loadCharacters(conn);

            // 5. Carica l'inventario del giocatore
            loadPlayerInventory(conn);

            // 6. Imposta la stanza corrente del giocatore
            setPlayerCurrentRoom(conn);

            System.out.println("✅ Tutti i dati caricati con successo!");
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

        System.out.println("🏰 Caricate " + roomMap.size() + " stanze");
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
     * Carica gli oggetti presenti nelle stanze.
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

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            int objectCount = 0;
            while (rs.next()) {
                int roomId = rs.getInt("ROOM_ID");
                Room room = roomMap.get(roomId);

                if (room != null) {
                    GameObjects obj = createObjectFromResultSet(rs);
                    if (obj != null) {
                        room.getObjects().add(obj);
                        objectCount++;
                    }
                }
            }

            System.out.println("📦 Caricati " + objectCount + " oggetti nelle stanze");
        }
    }

    /**
     * Carica i personaggi dal database.
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
                if (!rs.getBoolean("IS_ALIVE")) {
                    character.setCurrentHp(0);
                }

                // Aggiungi alla lista appropriata
                CharacterType type = character.getType();
                if (type == CharacterType.PLAYER) {
                    // Il giocatore viene gestito separatamente
                    game.setPlayer(character);
                } else if (character.getType() == CharacterType.GOBLIN
                        || character.getType() == CharacterType.GIANT_RAT
                        || character.getType() == CharacterType.DEMON_DOG) {
                    // Aggiungi nemico alla stanza appropriata
                    int roomId = rs.getInt("ROOM_ID");
                    Room room = roomMap.get(roomId);
                    if (room != null) {
                        room.getEnemies().add(character);
                    }
                }
            }
        }

        System.out.println("👤 Personaggi caricati");
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
                    System.out.println("📍 Giocatore posizionato in: " + currentRoom.getName());
                } else {
                    // Fallback alla prima stanza
                    if (!game.getRooms().isEmpty()) {
                        game.setCurrentRoom(game.getRooms().get(0));
                        System.out.println("⚠️ Stanza non trovata, posizionato nella prima stanza");
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
            System.err.println("❌ Errore nel salvataggio dello stato del giocatore: " + e.getMessage());
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
            System.err.println("❌ Errore nell'aggiornamento oggetto " + obj.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Apre una cassa e sposta i suoi oggetti dal database alla stanza
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
                return foundObjects; // Cassa vuota
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    GameObjects obj = createObjectFromResultSet(rs);
                    if (obj != null) {
                        foundObjects.add(obj);
                        room.getObjects().add(obj);

                        // Aggiungi l'oggetto alla tabella ROOM_OBJECTS se non c'è già
                        addObjectToRoom(conn, room.getId(), obj.getId());
                    }
                }

            }

        } catch (SQLException e) {
            System.err.println("❌ Errore nell'apertura cassa " + chestId + ": " + e.getMessage());
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
                """; // libro incantesimo fuoco, veleno

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
            System.err.println("❌ Errore nel segnare cassa come aperta: " + e.getMessage());
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
            System.err.println("❌ Errore nello spostamento oggetto in stanza: " + e.getMessage());
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
            System.err.println("❌ Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
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
            System.err.println("❌ Errore nella rimozione dell'oggetto: " + e.getMessage());
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
            System.err.println("❌ Errore nell'aggiornamento personaggio " + character.getName() + ": " + e.getMessage());
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
            System.err.println("❌ Errore nel recupero statistiche: " + e.getMessage());
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
                    System.err.println("⚠️ Oggetto orfano trovato: " + rs.getInt(1) + " - " + rs.getString(2));
                    allGood = false;
                }
            }

            if (allGood) {
                System.out.println("✅ Integrità database verificata");
            }

            return allGood;

        } catch (SQLException e) {
            System.err.println("❌ Errore nella verifica integrità: " + e.getMessage());
            return false;
        }
    }
}
