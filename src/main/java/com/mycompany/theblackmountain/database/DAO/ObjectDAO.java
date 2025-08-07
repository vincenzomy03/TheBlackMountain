package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.entities.ObjectEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per gli oggetti
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
                    "WHERE ro.room_id = ? AND (ro.is_container_content = false OR ro.is_container_content IS NULL) " +
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
     * Aggiorna lo stato di apertura di un oggetto
     */
    public void updateOpenState(int id, boolean isOpen) throws SQLException {
        executeUpdate("UPDATE objects SET is_open = ? WHERE id = ?", isOpen, id);
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
        Optional<Integer> existing = executeScalarQuery(
            "SELECT 1 FROM room_objects WHERE object_id = ? AND room_id = ?",
            Integer.class, objectId, roomId
        );
        
        if (!existing.isPresent()) {
            executeUpdate("INSERT INTO room_objects (room_id, object_id) VALUES (?, ?)", roomId, objectId);
        }
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
}