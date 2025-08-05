package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.entities.CharacterEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object per i personaggi
 * @author vince
 */
public class CharacterDAO {
    
    /**
     * Trova un personaggio per ID
     */
    public CharacterEntity findById(int id) throws SQLException {
        String sql = "SELECT * FROM characters WHERE id = ?";
        
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
     * Trova tutti i personaggi
     */
    public List<CharacterEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM characters ORDER BY id";
        List<CharacterEntity> characters = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                characters.add(mapResultSetToEntity(rs));
            }
        }
        return characters;
    }
    
    /**
     * Trova personaggi per tipo
     */
    public List<CharacterEntity> findByType(String characterType) throws SQLException {
        String sql = "SELECT * FROM characters WHERE character_type = ? ORDER BY id";
        List<CharacterEntity> characters = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, characterType);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapResultSetToEntity(rs));
                }
            }
        }
        return characters;
    }
    
    /**
     * Trova personaggi in una stanza specifica
     */
    public List<CharacterEntity> findByRoomId(int roomId) throws SQLException {
        String sql = "SELECT * FROM characters WHERE room_id = ? ORDER BY id";
        List<CharacterEntity> characters = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapResultSetToEntity(rs));
                }
            }
        }
        return characters;
    }
    
    /**
     * Trova personaggi vivi in una stanza
     */
    public List<CharacterEntity> findAliveByRoomId(int roomId) throws SQLException {
        String sql = "SELECT * FROM characters WHERE room_id = ? AND is_alive = true ORDER BY id";
        List<CharacterEntity> characters = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapResultSetToEntity(rs));
                }
            }
        }
        return characters;
    }
    
    /**
     * Trova il giocatore
     */
    public CharacterEntity findPlayer() throws SQLException {
        String sql = "SELECT * FROM characters WHERE character_type = 'PLAYER'";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        }
        return null;
    }
    
    /**
     * Salva un nuovo personaggio
     */
    public void save(CharacterEntity character) throws SQLException {
        String sql = "INSERT INTO characters (id, name, description, character_type, " +
                    "max_hp, current_hp, attack, defense, is_alive, room_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToStatement(stmt, character);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna un personaggio esistente
     */
    public void update(CharacterEntity character) throws SQLException {
        String sql = "UPDATE characters SET name = ?, description = ?, character_type = ?, " +
                    "max_hp = ?, current_hp = ?, attack = ?, defense = ?, " +
                    "is_alive = ?, room_id = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, character.getName());
            stmt.setString(2, character.getDescription());
            stmt.setString(3, character.getCharacterType());
            stmt.setInt(4, character.getMaxHp());
            stmt.setInt(5, character.getCurrentHp());
            stmt.setInt(6, character.getAttack());
            stmt.setInt(7, character.getDefense());
            stmt.setBoolean(8, character.isAlive());
            
            if (character.getRoomId() != null) {
                stmt.setInt(9, character.getRoomId());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }
            
            stmt.setInt(10, character.getId());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Elimina un personaggio
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM characters WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna gli HP di un personaggio
     */
    public void updateHp(int id, int currentHp, int maxHp) throws SQLException {
        String sql = "UPDATE characters SET current_hp = ?, max_hp = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentHp);
            stmt.setInt(2, maxHp);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna lo stato di vita di un personaggio
     */
    public void updateAliveStatus(int id, boolean isAlive) throws SQLException {
        String sql = "UPDATE characters SET is_alive = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, isAlive);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Sposta un personaggio in una stanza diversa
     */
    public void moveToRoom(int characterId, int roomId) throws SQLException {
        String sql = "UPDATE characters SET room_id = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            stmt.setInt(2, characterId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Resetta tutti i nemici (li riporta in vita con HP pieni)
     */
    public void resetEnemies() throws SQLException {
        String sql = "UPDATE characters SET current_hp = max_hp, is_alive = true " +
                    "WHERE character_type != 'PLAYER'";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Conta i nemici vivi in una stanza
     */
    public int countAliveEnemiesInRoom(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM characters " +
                    "WHERE room_id = ? AND is_alive = true AND character_type != 'PLAYER'";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roomId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    // Metodi di utilità
    private CharacterEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        CharacterEntity entity = new CharacterEntity();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setCharacterType(rs.getString("character_type"));
        entity.setMaxHp(rs.getInt("max_hp"));
        entity.setCurrentHp(rs.getInt("current_hp"));
        entity.setAttack(rs.getInt("attack"));
        entity.setDefense(rs.getInt("defense"));
        entity.setAlive(rs.getBoolean("is_alive"));
        
        int roomId = rs.getInt("room_id");
        entity.setRoomId(rs.wasNull() ? null : roomId);
        
        return entity;
    }
    
    private void mapEntityToStatement(PreparedStatement stmt, CharacterEntity character) throws SQLException {
        stmt.setInt(1, character.getId());
        stmt.setString(2, character.getName());
        stmt.setString(3, character.getDescription());
        stmt.setString(4, character.getCharacterType());
        stmt.setInt(5, character.getMaxHp());
        stmt.setInt(6, character.getCurrentHp());
        stmt.setInt(7, character.getAttack());
        stmt.setInt(8, character.getDefense());
        stmt.setBoolean(9, character.isAlive());
        
        if (character.getRoomId() != null) {
            stmt.setInt(10, character.getRoomId());
        } else {
            stmt.setNull(10, Types.INTEGER);
        }
    }
}