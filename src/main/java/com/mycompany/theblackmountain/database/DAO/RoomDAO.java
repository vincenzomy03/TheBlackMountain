package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.entities.RoomEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object per le stanze
 * @author vince
 */
public class RoomDAO {
    
    /**
     * Trova una stanza per ID
     */
    public RoomEntity findById(int id) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        
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
     * Trova tutte le stanze
     */
    public List<RoomEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM rooms ORDER BY id";
        List<RoomEntity> rooms = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                rooms.add(mapResultSetToEntity(rs));
            }
        }
        return rooms;
    }
    
    /**
     * Trova stanze per nome (ricerca parziale)
     */
    public List<RoomEntity> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE name LIKE ? ORDER BY id";
        List<RoomEntity> rooms = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + name + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapResultSetToEntity(rs));
                }
            }
        }
        return rooms;
    }
    
    /**
     * Salva una nuova stanza
     */
    public void save(RoomEntity room) throws SQLException {
        String sql = "INSERT INTO rooms (id, name, description, look_description, " +
                    "north_room_id, south_room_id, east_room_id, west_room_id, is_visible) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToStatement(stmt, room);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna una stanza esistente
     */
    public void update(RoomEntity room) throws SQLException {
        String sql = "UPDATE rooms SET name = ?, description = ?, look_description = ?, " +
                    "north_room_id = ?, south_room_id = ?, east_room_id = ?, west_room_id = ?, " +
                    "is_visible = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, room.getName());
            stmt.setString(2, room.getDescription());
            stmt.setString(3, room.getLookDescription());
            
            setIntegerOrNull(stmt, 4, room.getNorthRoomId());
            setIntegerOrNull(stmt, 5, room.getSouthRoomId());
            setIntegerOrNull(stmt, 6, room.getEastRoomId());
            setIntegerOrNull(stmt, 7, room.getWestRoomId());
            
            stmt.setBoolean(8, room.isVisible());
            stmt.setInt(9, room.getId());
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Elimina una stanza
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM rooms WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna la descrizione di una stanza
     */
    public void updateDescription(int id, String description) throws SQLException {
        String sql = "UPDATE rooms SET description = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, description);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna la descrizione look di una stanza
     */
    public void updateLookDescription(int id, String lookDescription) throws SQLException {
        String sql = "UPDATE rooms SET look_description = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, lookDescription);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Conta il numero totale di stanze
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    // Metodi di utilità
    private RoomEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        RoomEntity entity = new RoomEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setLookDescription(rs.getString("look_description"));
        entity.setNorthRoomId(getIntegerOrNull(rs, "north_room_id"));
        entity.setSouthRoomId(getIntegerOrNull(rs, "south_room_id"));
        entity.setEastRoomId(getIntegerOrNull(rs, "east_room_id"));
        entity.setWestRoomId(getIntegerOrNull(rs, "west_room_id"));
        entity.setVisible(rs.getBoolean("is_visible"));
        return entity;
    }
    
    private void mapEntityToStatement(PreparedStatement stmt, RoomEntity room) throws SQLException {
        stmt.setInt(1, room.getId());
        stmt.setString(2, room.getName());
        stmt.setString(3, room.getDescription());
        stmt.setString(4, room.getLookDescription());
        
        setIntegerOrNull(stmt, 5, room.getNorthRoomId());
        setIntegerOrNull(stmt, 6, room.getSouthRoomId());
        setIntegerOrNull(stmt, 7, room.getEastRoomId());
        setIntegerOrNull(stmt, 8, room.getWestRoomId());
        
        stmt.setBoolean(9, room.isVisible());
    }
    
    private Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
    
    private void setIntegerOrNull(PreparedStatement stmt, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(parameterIndex, Types.INTEGER);
        } else {
            stmt.setInt(parameterIndex, value);
        }
    }
}