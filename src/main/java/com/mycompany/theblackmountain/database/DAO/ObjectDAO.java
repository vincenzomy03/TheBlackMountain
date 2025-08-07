package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.entities.ObjectEntity;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * DAO migliorato per gli oggetti
 */
public class ObjectDAO extends BaseDAO<ObjectEntity, Integer> {
    
    @Override
    protected String getTableName() {
        return "objects";
    }
    
    @Override
    protected String getIdColumn() {
        return "id";
    }
    
    @Override
    protected Integer getEntityId(ObjectEntity entity) {
        return entity.getId();
    }
    
    @Override
    protected String buildInsertSql() {
        return "INSERT INTO objects (id, name, description, aliases, is_openable, " +
               "is_pickupable, is_pushable, is_open, is_pushed, object_type) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    protected String buildUpdateSql() {
        return "UPDATE objects SET name = ?, description = ?, aliases = ?, " +
               "is_openable = ?, is_pickupable = ?, is_pushable = ?, " +
               "is_open = ?, is_pushed = ?, object_type = ? WHERE id = ?";
    }
    
    @Override
    protected ObjectEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        ObjectEntity entity = new ObjectEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setAliases(rs.getString("aliases"));
        entity.setOpenable(rs.getBoolean("is_openable"));
        entity.setPickupable(rs.getBoolean("is_pickupable"));
        entity.setPushable(rs.getBoolean("is_pushable"));
        entity.setOpen(rs.getBoolean("is_open"));
        entity.setPushed(rs.getBoolean("is_pushed"));
        entity.setObjectType(rs.getString("object_type"));
        return entity;
    }
    
    @Override
    protected void mapEntityToInsertStatement(PreparedStatement stmt, ObjectEntity object) throws SQLException {
        stmt.setInt(1, object.getId());
        stmt.setString(2, object.getName());
        stmt.setString(3, object.getDescription());
        stmt.setString(4, object.getAliases());
        stmt.setBoolean(5, object.isOpenable());
        stmt.setBoolean(6, object.isPickupable());
        stmt.setBoolean(7, object.isPushable());
        stmt.setBoolean(8, object.isOpen());
        stmt.setBoolean(9, object.isPushed());
        stmt.setString(10, object.getObjectType());
    }
    
    @Override
    protected void mapEntityToUpdateStatement(PreparedStatement stmt, ObjectEntity object) throws SQLException {
        stmt.setString(1, object.getName());
        stmt.setString(2, object.getDescription());
        stmt.setString(3, object.getAliases());
        stmt.setBoolean(4, object.isOpenable());
        stmt.setBoolean(5, object.isPickupable());
        stmt.setBoolean(6, object.isPushable());
        stmt.setBoolean(7, object.isOpen());
        stmt.setBoolean(8, object.isPushed());
        stmt.setString(9, object.getObjectType());
        stmt.setInt(10, object.getId());
    }
    
    // Metodi specifici per Object
    
    /**
     * Trova oggetti per nome o alias (ricerca parziale)
     */
    public List<ObjectEntity> findByNameOrAlias(String searchTerm) throws SQLException {
        String pattern = "%" + searchTerm + "%";
        return executeQuery("SELECT * FROM objects WHERE name LIKE ? OR aliases LIKE ? ORDER BY id", pattern, pattern);
    }
    
    /**
     * Trova oggetti per tipo
     */
    public List<ObjectEntity> findByType(String objectType) throws SQLException {
        return executeQuery("SELECT * FROM objects WHERE object_type = ? ORDER BY id", objectType);
    }
    
    /**
     * Trova oggetti in una stanza specifica
     */
    public List<ObjectEntity> findByRoomId(int roomId) throws SQLException {
        String sql = "SELECT o.* FROM objects o " +
                    "JOIN room_objects ro ON o.id = ro.object_id " +
                    "WHERE ro.room_id = ? AND ro.is_container_content = false " +
                    "ORDER BY o.id";
        return executeQuery(sql, roomId);
    }
    
    /**
     * Trova oggetti contenuti in un container
     */
    public List<ObjectEntity> findByContainerId(int containerId) throws SQLException {
        String sql = "SELECT o.* FROM objects o " +
                    "JOIN room_objects ro ON o.id = ro.object_id " +
                    "WHERE ro.container_id = ? AND ro.is_container_content = true " +
                    "ORDER BY o.id";
        return executeQuery(sql, containerId);
    }
    
    /**
     * Trova oggetti raccoglibili
     */
    public List<ObjectEntity> findPickupable() throws SQLException {
        return executeQuery("SELECT * FROM objects WHERE is_pickupable = true ORDER BY id");
    }
    
    /**
     * Trova oggetti apribili
     */
    public List<ObjectEntity> findOpenable() throws SQLException {
        return executeQuery("SELECT * FROM objects WHERE is_openable = true ORDER BY id");
    }
    
    /**
     * Aggiorna lo stato di apertura di un oggetto
     */
    public void updateOpenState(int id, boolean isOpen) throws SQLException {
        executeUpdate("UPDATE objects SET is_open = ? WHERE id = ?", isOpen, id);
    }
    
    /**
     * Sposta un oggetto da una stanza a un'altra
     */
    public void moveObjectToRoom(int objectId, int fromRoomId, int toRoomId) throws SQLException {
        executeUpdate("UPDATE room_objects SET room_id = ? WHERE object_id = ? AND room_id = ?", 
                     toRoomId, objectId, fromRoomId);
    }
    
    /**
     * Rimuove un oggetto da una stanza
     */
    public void removeFromRoom(int objectId, int roomId) throws SQLException {
        executeUpdate("DELETE FROM room_objects WHERE object_id = ? AND room_id = ?", objectId, roomId);
    }
    
    /**
     * Aggiunge un oggetto a una stanza
     */
    public void addToRoom(int objectId, int roomId) throws SQLException {
        // Verifica che l'associazione non esista già
        Optional<Integer> existing = executeScalarQuery(
            "SELECT 1 FROM room_objects WHERE object_id = ? AND room_id = ?",
            Integer.class, objectId, roomId
        );
        
        if (!existing.isPresent()) {
            executeUpdate("INSERT INTO room_objects (room_id, object_id) VALUES (?, ?)", roomId, objectId);
        }
    }
    
    /**
     * Ottiene statistiche degli oggetti per tipo
     */
    public List<ObjectTypeStats> getObjectStatsByType() throws SQLException {
        String sql = "SELECT object_type, COUNT(*) as count, " +
                    "SUM(CASE WHEN is_pickupable = true THEN 1 ELSE 0 END) as pickupable_count, " +
                    "SUM(CASE WHEN is_openable = true THEN 1 ELSE 0 END) as openable_count, " +
                    "SUM(CASE WHEN is_open = true THEN 1 ELSE 0 END) as open_count " +
                    "FROM objects GROUP BY object_type";
        
        List<ObjectTypeStats> stats = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ObjectTypeStats stat = new ObjectTypeStats();
                stat.objectType = rs.getString("object_type");
                stat.totalCount = rs.getInt("count");
                stat.pickupableCount = rs.getInt("pickupable_count");
                stat.openableCount = rs.getInt("openable_count");
                stat.openCount = rs.getInt("open_count");
                stats.add(stat);
            }
        }
        
        return stats;
    }
    
    @Override
    protected void validateBeforeSave(ObjectEntity entity) throws SQLException {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new SQLException("Il nome dell'oggetto non può essere vuoto");
        }
        if (entity.getObjectType() == null || entity.getObjectType().trim().isEmpty()) {
            throw new SQLException("Il tipo dell'oggetto non può essere vuoto");
        }
    }
    
    /**
     * Classe per statistiche degli oggetti per tipo
     */
    public static class ObjectTypeStats {
        public String objectType;
        public int totalCount;
        public int pickupableCount;
        public int openableCount;
        public int openCount;
        
        @Override
        public String toString() {
            return String.format("Tipo: %s, Totali: %d, Raccoglibili: %d, Apribili: %d, Aperti: %d",
                objectType, totalCount, pickupableCount, openableCount, openCount);
        }
    }
}