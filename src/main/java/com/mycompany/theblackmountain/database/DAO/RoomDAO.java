package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.entities.RoomEntity;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * DAO migliorato per le stanze
 */
public class RoomDAO extends BaseDAO<RoomEntity, Integer> {
    
    @Override
    protected String getTableName() {
        return "rooms";
    }
    
    @Override
    protected String getIdColumn() {
        return "id";
    }
    
    @Override
    protected Integer getEntityId(RoomEntity entity) {
        return entity.getId();
    }
    
    @Override
    protected String buildInsertSql() {
        return "INSERT INTO rooms (id, name, description, look_description, " +
               "north_room_id, south_room_id, east_room_id, west_room_id, is_visible) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    protected String buildUpdateSql() {
        return "UPDATE rooms SET name = ?, description = ?, look_description = ?, " +
               "north_room_id = ?, south_room_id = ?, east_room_id = ?, west_room_id = ?, " +
               "is_visible = ? WHERE id = ?";
    }
    
    @Override
    protected RoomEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
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
    
    @Override
    protected void mapEntityToInsertStatement(PreparedStatement stmt, RoomEntity room) throws SQLException {
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
    
    @Override
    protected void mapEntityToUpdateStatement(PreparedStatement stmt, RoomEntity room) throws SQLException {
        stmt.setString(1, room.getName());
        stmt.setString(2, room.getDescription());
        stmt.setString(3, room.getLookDescription());
        setIntegerOrNull(stmt, 4, room.getNorthRoomId());
        setIntegerOrNull(stmt, 5, room.getSouthRoomId());
        setIntegerOrNull(stmt, 6, room.getEastRoomId());
        setIntegerOrNull(stmt, 7, room.getWestRoomId());
        stmt.setBoolean(8, room.isVisible());
        stmt.setInt(9, room.getId());
    }
    
    // Metodi specifici per Room
    
    /**
     * Trova stanze per nome (ricerca parziale)
     */
    public List<RoomEntity> findByNameContaining(String name) throws SQLException {
        return executeQuery("SELECT * FROM rooms WHERE name LIKE ? ORDER BY id", "%" + name + "%");
    }
    
    /**
     * Trova stanze visibili
     */
    public List<RoomEntity> findVisible() throws SQLException {
        return executeQuery("SELECT * FROM rooms WHERE is_visible = true ORDER BY id");
    }
    
    /**
     * Aggiorna solo la descrizione di una stanza
     */
    public void updateDescription(int id, String description) throws SQLException {
        executeUpdate("UPDATE rooms SET description = ? WHERE id = ?", description, id);
    }
    
    /**
     * Aggiorna solo la descrizione look di una stanza
     */
    public void updateLookDescription(int id, String lookDescription) throws SQLException {
        executeUpdate("UPDATE rooms SET look_description = ? WHERE id = ?", lookDescription, id);
    }
    
    /**
     * Trova stanze connesse a una stanza specifica
     */
    public List<RoomEntity> findConnectedRooms(int roomId) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE " +
                    "north_room_id = ? OR south_room_id = ? OR east_room_id = ? OR west_room_id = ?";
        return executeQuery(sql, roomId, roomId, roomId, roomId);
    }
    
    @Override
    protected void validateBeforeSave(RoomEntity entity) throws SQLException {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new SQLException("Il nome della stanza non può essere vuoto");
        }
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            throw new SQLException("La descrizione della stanza non può essere vuota");
        }
    }
}