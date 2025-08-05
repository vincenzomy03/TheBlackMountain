package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.entities.ObjectEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object per gli oggetti
 * @author vince
 */
public class ObjectDAO {
    
    /**
     * Trova un oggetto per ID
     */
    public ObjectEntity findById(int id) throws SQLException {
        String sql = "SELECT * FROM objects WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Trova tutti gli oggetti
     */
    public List<ObjectEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM objects ORDER BY id";
        List<ObjectEntity> objects = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                objects.add(mapResultSetToEntity(rs));
            }
        }
        return objects;
    }
    
    /**
     * Trova oggetti per nome (ricerca parziale)
     */
    public List<ObjectEntity> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM objects WHERE name LIKE ? OR aliases LIKE ? ORDER BY id";
        List<ObjectEntity> objects = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + name + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    objects.add(mapResultSetToEntity(rs));
                }
            }
        }
        return objects;
    }
    
    /**
     * Trova oggetti per tipo
     */
    public List<ObjectEntity> findByType(String objectType) throws SQLException {
        String sql = "SELECT * FROM objects WHERE object_type = ? ORDER BY id";
        List<ObjectEntity> objects = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, objectType);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    objects.add(mapResultSetToEntity(rs));
                }
            }
        }
        return objects;
    }
    
    /**
     * Trova oggetti in una stanza specifica
     */
    public List<ObjectEntity> findByRoomId(int roomId) throws SQLException {
        String sql = "SELECT o.* FROM objects o " +
                    "JOIN room_objects ro ON o.id = ro.object_id " +
                    "WHERE ro.room_id = ? AND ro.is_container_content = false " +
                    "ORDER BY o.id";
        List<ObjectEntity> objects = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    objects.add(mapResultSetToEntity(rs));
                }
            }
        }
        return objects;
    }
    
    /**
     * Trova oggetti contenuti in un container
     */
    public List<ObjectEntity> findByContainerId(int containerId) throws SQLException {
        String sql = "SELECT o.* FROM objects o " +
                    "JOIN room_objects ro ON o.id = ro.object_id " +
                    "WHERE ro.container_id = ? AND ro.is_container_content = true " +
                    "ORDER BY o.id";
        List<ObjectEntity> objects = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, containerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    objects.add(mapResultSetToEntity(rs));
                }
            }
        }
        return objects;
    }
    
    /**
     * Salva un nuovo oggetto
     */
    public void save(ObjectEntity object) throws SQLException {
        String sql = "INSERT INTO objects (id, name, description, aliases, is_openable, " +
                    "is_pickupable, is_pushable, is_open, is_pushed, object_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToStatement(stmt, object);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna un oggetto esistente
     */
    public void update(ObjectEntity object) throws SQLException {
        String sql = "UPDATE objects SET name = ?, description = ?, aliases = ?, " +
                    "is_openable = ?, is_pickupable = ?, is_pushable = ?, " +
                    "is_open = ?, is_pushed = ?, object_type = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Elimina un oggetto
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM objects WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna lo stato di apertura di un oggetto
     */
    public void updateOpenState(int id, boolean isOpen) throws SQLException {
        String sql = "UPDATE objects SET is_open = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, isOpen);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Sposta un oggetto da una stanza a un'altra
     */
    public void moveObjectToRoom(int objectId, int fromRoomId, int toRoomId) throws SQLException {
        String sql = "UPDATE room_objects SET room_id = ? WHERE object_id = ? AND room_id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, toRoomId);
            stmt.setInt(2, objectId);
            stmt.setInt(3, fromRoomId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Rimuove un oggetto da una stanza
     */
    public void removeFromRoom(int objectId, int roomId) throws SQLException {
        String sql = "DELETE FROM room_objects WHERE object_id = ? AND room_id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiunge un oggetto a una stanza
     */
    public void addToRoom(int objectId, int roomId) throws SQLException {
        String sql = "INSERT INTO room_objects (room_id, object_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            stmt.setInt(2, objectId);
            stmt.executeUpdate();
        }
    }
    
    // Metodi di utilità
    private ObjectEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
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
    
    private void mapEntityToStatement(PreparedStatement stmt, ObjectEntity object) throws SQLException {
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
}