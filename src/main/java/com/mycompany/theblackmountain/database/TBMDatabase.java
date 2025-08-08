package com.mycompany.theblackmountain.database;

import com.mycompany.theblackmountain.database.entities.*;
import com.mycompany.theblackmountain.type.*;
import java.sql.*;
import java.util.*;

/**
 * Servizio database per The Black Mountain
 */
public class TBMDatabase {
    
    private final DatabaseManager dbManager;
    
    public TBMDatabase() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Test di connessione al database
     */
    public boolean testConnection() {
        return dbManager.isHealthy();
    }
    
    // =====================
    // OPERAZIONI ROOM
    // =====================
    
    /**
     * Carica tutte le stanze dal database
     */
    public List<Room> loadAllRooms() throws DatabaseException {
        String sql = "SELECT * FROM rooms ORDER BY id";
        List<RoomEntity> entities = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                entities.add(mapRoomEntity(rs));
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento delle stanze", e);
        }
        
        return DatabaseConverter.toRoomsWithConnections(entities);
    }
    
    /**
     * Salva una stanza nel database
     */
    public void saveRoom(Room room) throws DatabaseException {
        String sql = """
            MERGE INTO rooms (id, name, description, look_description, visible, 
                             north_room_id, south_room_id, east_room_id, west_room_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, room.getId());
            stmt.setString(2, room.getName());
            stmt.setString(3, room.getDescription());
            stmt.setString(4, room.getLook());
            stmt.setBoolean(5, room.isVisible());
            
            // Connessioni alle altre stanze
            stmt.setObject(6, room.getNorth() != null ? room.getNorth().getId() : null);
            stmt.setObject(7, room.getSouth() != null ? room.getSouth().getId() : null);
            stmt.setObject(8, room.getEast() != null ? room.getEast().getId() : null);
            stmt.setObject(9, room.getWest() != null ? room.getWest().getId() : null);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel salvataggio della stanza: " + room.getName(), e);
        }
    }
    
    // =====================
    // OPERAZIONI OGGETTI
    // =====================
    
    /**
     * Carica gli oggetti di una stanza
     */
    public List<Objects> loadRoomObjects(int roomId) throws DatabaseException {
        String sql = """
            SELECT o.* FROM objects o 
            JOIN room_objects ro ON o.id = ro.object_id 
            WHERE ro.room_id = ?
        """;
        
        List<Objects> objects = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ObjectEntity entity = mapObjectEntity(rs);
                    Objects obj = DatabaseConverter.toObject(entity);
                    
                    // Se è un'arma, carica anche i dati specifici
                    if ("WEAPON".equals(entity.getObjectType())) {
                        WeaponEntity weaponEntity = loadWeaponEntity(conn, entity.getId());
                        if (weaponEntity != null) {
                            Weapon weapon = DatabaseConverter.toWeapon(weaponEntity, entity);
                            if (weapon != null) {
                                objects.add(weapon);
                                continue;
                            }
                        }
                    }
                    
                    if (obj != null) {
                        objects.add(obj);
                    }
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento degli oggetti della stanza " + roomId, e);
        }
        
        return objects;
    }
    
    /**
     * Carica i nemici di una stanza
     */
    public List<Character> loadRoomEnemies(int roomId) throws DatabaseException {
        String sql = """
            SELECT c.* FROM characters c 
            WHERE c.room_id = ? AND c.character_type = 'ENEMY'
        """;
        
        List<Character> enemies = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CharacterEntity entity = mapCharacterEntity(rs);
                    Character enemy = DatabaseConverter.toCharacter(entity);
                    if (enemy != null) {
                        enemies.add(enemy);
                    }
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento dei nemici della stanza " + roomId, e);
        }
        
        return enemies;
    }
    
    // =====================
    // OPERAZIONI PLAYER
    // =====================
    
    /**
     * Carica il giocatore dal database
     */
    public Optional<Character> loadPlayer() throws DatabaseException {
        String sql = "SELECT * FROM characters WHERE character_type = 'PLAYER' LIMIT 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                CharacterEntity entity = mapCharacterEntity(rs);
                Character player = DatabaseConverter.toCharacter(entity);
                return Optional.ofNullable(player);
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento del giocatore", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Ottiene l'ID della stanza in cui si trova il giocatore
     */
    public int getPlayerRoomId(int playerId) throws DatabaseException {
        String sql = "SELECT room_id FROM characters WHERE id = ? AND character_type = 'PLAYER'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("room_id");
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel recupero della posizione del giocatore", e);
        }
        
        return 1; // Stanza di default se non trovata
    }
    
    /**
     * Aggiorna la posizione del giocatore
     */
    public void updatePlayerPosition(int playerId, int roomId) throws DatabaseException {
        String sql = "UPDATE characters SET room_id = ? WHERE id = ? AND character_type = 'PLAYER'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            stmt.setInt(2, playerId);
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Giocatore non trovato per l'aggiornamento della posizione");
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'aggiornamento della posizione del giocatore", e);
        }
    }
    
    // =====================
    // OPERAZIONI CHARACTER
    // =====================
    
    /**
     * Salva un personaggio nel database
     */
    public void saveCharacter(Character character) throws DatabaseException {
        String sql = """
            MERGE INTO characters (id, name, description, character_type, max_hp, 
                                  current_hp, attack, defense, alive, room_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, character.getId());
            stmt.setString(2, character.getName());
            stmt.setString(3, character.getDescription());
            stmt.setString(4, character.getType().name());
            stmt.setInt(5, character.getMaxHp());
            stmt.setInt(6, character.getCurrentHp());
            stmt.setInt(7, character.getAttack());
            stmt.setInt(8, character.getDefense());
            stmt.setBoolean(9, character.isAlive());
            stmt.setInt(10, 1); // Room ID di default - potrebbe essere migliorato
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel salvataggio del personaggio: " + character.getName(), e);
        }
    }
    
    /**
     * Aggiorna lo stato di un personaggio
     */
    public void updateCharacterState(Character character) throws DatabaseException {
        String sql = """
            UPDATE characters SET current_hp = ?, alive = ? 
            WHERE id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, character.getCurrentHp());
            stmt.setBoolean(2, character.isAlive());
            stmt.setInt(3, character.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'aggiornamento dello stato del personaggio", e);
        }
    }
    
    // =====================
    // OPERAZIONI OGGETTI - STATO
    // =====================
    
    /**
     * Aggiorna lo stato di un oggetto
     */
    public void updateObjectState(Objects obj) throws DatabaseException {
        String sql = """
            UPDATE objects SET is_open = ?, is_pushed = ?, openable = ?, 
                              pickupable = ?, pushable = ? 
            WHERE id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, obj.isOpen());
            stmt.setBoolean(2, obj.isPush());
            stmt.setBoolean(3, obj.isOpenable());
            stmt.setBoolean(4, obj.isPickupable());
            stmt.setBoolean(5, obj.isPushable());
            stmt.setInt(6, obj.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'aggiornamento dello stato dell'oggetto", e);
        }
    }
    
    /**
     * Sposta un oggetto in una stanza
     */
    public void moveObjectToRoom(int objectId, int roomId) throws DatabaseException {
        dbManager.executeInTransaction(conn -> {
            // Rimuovi l'oggetto da tutte le posizioni
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM room_objects WHERE object_id = ?")) {
                deleteStmt.setInt(1, objectId);
                deleteStmt.executeUpdate();
            }
            
            try (PreparedStatement deleteInvStmt = conn.prepareStatement(
                    "DELETE FROM inventory WHERE object_id = ?")) {
                deleteInvStmt.setInt(1, objectId);
                deleteInvStmt.executeUpdate();
            }
            
            // Aggiungi alla nuova stanza
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO room_objects (room_id, object_id) VALUES (?, ?)")) {
                insertStmt.setInt(1, roomId);
                insertStmt.setInt(2, objectId);
                insertStmt.executeUpdate();
            }
        });
    }
    
    /**
     * Sposta un oggetto nell'inventario
     */
    public void moveObjectToInventory(int objectId, int playerId) throws DatabaseException {
        dbManager.executeInTransaction(conn -> {
            // Rimuovi l'oggetto da tutte le stanze
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM room_objects WHERE object_id = ?")) {
                deleteStmt.setInt(1, objectId);
                deleteStmt.executeUpdate();
            }
            
            // Aggiungi all'inventario se non è già presente
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM inventory WHERE character_id = ? AND object_id = ?")) {
                checkStmt.setInt(1, playerId);
                checkStmt.setInt(2, objectId);
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO inventory (character_id, object_id) VALUES (?, ?)")) {
                            insertStmt.setInt(1, playerId);
                            insertStmt.setInt(2, objectId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        });
    }
    
    // =====================
    // METODI DI MAPPING
    // =====================
    
    private RoomEntity mapRoomEntity(ResultSet rs) throws SQLException {
        RoomEntity entity = new RoomEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setLookDescription(rs.getString("look_description"));
        entity.setVisible(rs.getBoolean("visible"));
        
        // Gestione corretta dei NULL per le foreign key
        Object northId = rs.getObject("north_room_id");
        entity.setNorthRoomId(northId != null ? (Integer) northId : null);
        
        Object southId = rs.getObject("south_room_id");
        entity.setSouthRoomId(southId != null ? (Integer) southId : null);
        
        Object eastId = rs.getObject("east_room_id");
        entity.setEastRoomId(eastId != null ? (Integer) eastId : null);
        
        Object westId = rs.getObject("west_room_id");
        entity.setWestRoomId(westId != null ? (Integer) westId : null);
        
        return entity;
    }
    
    private ObjectEntity mapObjectEntity(ResultSet rs) throws SQLException {
        ObjectEntity entity = new ObjectEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setObjectType(rs.getString("object_type"));
        entity.setOpenable(rs.getBoolean("openable"));
        entity.setPickupable(rs.getBoolean("pickupable"));
        entity.setPushable(rs.getBoolean("pushable"));
        entity.setOpen(rs.getBoolean("is_open"));
        entity.setPushed(rs.getBoolean("is_pushed"));
        entity.setAliases(rs.getString("aliases"));
        return entity;
    }
    
    private CharacterEntity mapCharacterEntity(ResultSet rs) throws SQLException {
        CharacterEntity entity = new CharacterEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setCharacterType(rs.getString("character_type"));
        entity.setMaxHp(rs.getInt("max_hp"));
        entity.setCurrentHp(rs.getInt("current_hp"));
        entity.setAttack(rs.getInt("attack"));
        entity.setDefense(rs.getInt("defense"));
        entity.setAlive(rs.getBoolean("alive"));
        return entity;
    }
    
    private WeaponEntity loadWeaponEntity(Connection conn, int weaponId) throws SQLException {
        String sql = "SELECT * FROM weapons WHERE object_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, weaponId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WeaponEntity entity = new WeaponEntity();
                    entity.setId(rs.getInt("id"));
                    entity.setObjectId(rs.getInt("object_id"));
                    entity.setWeaponType(rs.getString("weapon_type"));
                    entity.setAttackBonus(rs.getInt("attack_bonus"));
                    entity.setCriticalChance(rs.getDouble("critical_chance"));
                    entity.setCriticalMultiplier(rs.getDouble("critical_multiplier"));
                    entity.setPoisoned(rs.getBoolean("is_poisoned"));
                    entity.setPoisonDamage(rs.getInt("poison_damage"));
                    entity.setSpecialEffect(rs.getString("special_effect"));
                    return entity;
                }
            }
        }
        
        return null;
    }
    
    // =====================
    // UTILITÀ
    // =====================
    
    /**
     * Ottiene statistiche del database
     */
    public Map<String, Integer> getDatabaseStats() {
        Map<String, Integer> stats = new HashMap<>();
        String[] tables = {"rooms", "objects", "characters", "weapons"};
        
        try (Connection conn = dbManager.getConnection()) {
            for (String table : tables) {
                String sql = "SELECT COUNT(*) FROM " + table;
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        stats.put(table, rs.getInt(1));
                    }
                } catch (SQLException e) {
                    stats.put(table, -1); // Indica errore
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero delle statistiche: " + e.getMessage());
        }
        
        return stats;
    }
}