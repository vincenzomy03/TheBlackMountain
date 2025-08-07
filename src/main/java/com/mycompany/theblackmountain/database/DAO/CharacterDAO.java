package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.entities.CharacterEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO migliorato per i personaggi
 */
public class CharacterDAO extends BaseDAO<CharacterEntity, Integer> {
    
    @Override
    protected String getTableName() {
        return "characters";
    }
    
    @Override
    protected String getIdColumn() {
        return "id";
    }
    
    @Override
    protected Integer getEntityId(CharacterEntity entity) {
        return entity.getId();
    }
    
    @Override
    protected String buildInsertSql() {
        return "INSERT INTO characters (id, name, description, character_type, " +
               "max_hp, current_hp, attack, defense, is_alive, room_id) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    protected String buildUpdateSql() {
        return "UPDATE characters SET name = ?, description = ?, character_type = ?, " +
               "max_hp = ?, current_hp = ?, attack = ?, defense = ?, " +
               "is_alive = ?, room_id = ? WHERE id = ?";
    }
    
    @Override
    protected CharacterEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
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
        entity.setRoomId(getIntegerOrNull(rs, "room_id"));
        return entity;
    }
    
    @Override
    protected void mapEntityToInsertStatement(PreparedStatement stmt, CharacterEntity character) throws SQLException {
        stmt.setInt(1, character.getId());
        stmt.setString(2, character.getName());
        stmt.setString(3, character.getDescription());
        stmt.setString(4, character.getCharacterType());
        stmt.setInt(5, character.getMaxHp());
        stmt.setInt(6, character.getCurrentHp());
        stmt.setInt(7, character.getAttack());
        stmt.setInt(8, character.getDefense());
        stmt.setBoolean(9, character.isAlive());
        setIntegerOrNull(stmt, 10, character.getRoomId());
    }
    
    @Override
    protected void mapEntityToUpdateStatement(PreparedStatement stmt, CharacterEntity character) throws SQLException {
        stmt.setString(1, character.getName());
        stmt.setString(2, character.getDescription());
        stmt.setString(3, character.getCharacterType());
        stmt.setInt(4, character.getMaxHp());
        stmt.setInt(5, character.getCurrentHp());
        stmt.setInt(6, character.getAttack());
        stmt.setInt(7, character.getDefense());
        stmt.setBoolean(8, character.isAlive());
        setIntegerOrNull(stmt, 9, character.getRoomId());
        stmt.setInt(10, character.getId());
    }
    
    // Metodi specifici per Character
    
    /**
     * Trova personaggi per tipo
     */
    public List<CharacterEntity> findByType(String characterType) throws SQLException {
        return executeQuery("SELECT * FROM characters WHERE character_type = ? ORDER BY id", characterType);
    }
    
    /**
     * Trova personaggi in una stanza specifica
     */
    public List<CharacterEntity> findByRoomId(int roomId) throws SQLException {
        return executeQuery("SELECT * FROM characters WHERE room_id = ? ORDER BY id", roomId);
    }
    
    /**
     * Trova personaggi vivi in una stanza
     */
    public List<CharacterEntity> findAliveByRoomId(int roomId) throws SQLException {
        return executeQuery("SELECT * FROM characters WHERE room_id = ? AND is_alive = true ORDER BY id", roomId);
    }
    
    /**
     * Trova il giocatore
     */
    public CharacterEntity findPlayer() throws SQLException {
        Optional<CharacterEntity> player = executeQuerySingle("SELECT * FROM characters WHERE character_type = 'PLAYER'");
        return player.orElse(null);
    }
    
    /**
     * Aggiorna gli HP di un personaggio
     */
    public void updateHp(int id, int currentHp, int maxHp) throws SQLException {
        executeUpdate("UPDATE characters SET current_hp = ?, max_hp = ? WHERE id = ?", currentHp, maxHp, id);
    }
    
    /**
     * Aggiorna lo stato di vita di un personaggio
     */
    public void updateAliveStatus(int id, boolean isAlive) throws SQLException {
        executeUpdate("UPDATE characters SET is_alive = ? WHERE id = ?", isAlive, id);
    }
    
    /**
     * Sposta un personaggio in una stanza diversa
     */
    public void moveToRoom(int characterId, int roomId) throws SQLException {
        executeUpdate("UPDATE characters SET room_id = ? WHERE id = ?", roomId, characterId);
    }
    
    /**
     * Resetta tutti i nemici (li riporta in vita con HP pieni)
     */
    public void resetEnemies() throws SQLException {
        executeUpdate("UPDATE characters SET current_hp = max_hp, is_alive = true WHERE character_type != 'PLAYER'");
    }
    
    /**
     * Conta i nemici vivi in una stanza
     */
    public int countAliveEnemiesInRoom(int roomId) throws SQLException {
        Optional<Integer> count = executeScalarQuery(
            "SELECT COUNT(*) FROM characters WHERE room_id = ? AND is_alive = true AND character_type != 'PLAYER'",
            Integer.class, roomId
        );
        return count.orElse(0);
    }
    
    /**
     * Trova personaggi con HP sotto una soglia
     */
    public List<CharacterEntity> findLowHealthCharacters(int healthThreshold) throws SQLException {
        return executeQuery("SELECT * FROM characters WHERE current_hp <= ? AND is_alive = true ORDER BY current_hp", healthThreshold);
    }
    
    /**
     * Ottiene statistiche dei personaggi per tipo
     */
    public List<CharacterStats> getCharacterStatsByType() throws SQLException {
        String sql = "SELECT character_type, COUNT(*) as count, " +
                    "AVG(CAST(current_hp as FLOAT)) as avg_hp, " +
                    "AVG(CAST(attack as FLOAT)) as avg_attack, " +
                    "SUM(CASE WHEN is_alive = true THEN 1 ELSE 0 END) as alive_count " +
                    "FROM characters GROUP BY character_type";
        
        List<CharacterStats> stats = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                CharacterStats stat = new CharacterStats();
                stat.characterType = rs.getString("character_type");
                stat.totalCount = rs.getInt("count");
                stat.averageHp = rs.getDouble("avg_hp");
                stat.averageAttack = rs.getDouble("avg_attack");
                stat.aliveCount = rs.getInt("alive_count");
                stats.add(stat);
            }
        }
        
        return stats;
    }
    
    @Override
    protected void validateBeforeSave(CharacterEntity entity) throws SQLException {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new SQLException("Il nome del personaggio non può essere vuoto");
        }
        if (entity.getCurrentHp() < 0) {
            throw new SQLException("Gli HP correnti non possono essere negativi");
        }
        if (entity.getMaxHp() <= 0) {
            throw new SQLException("Gli HP massimi devono essere positivi");
        }
        if (entity.getCurrentHp() > entity.getMaxHp()) {
            throw new SQLException("Gli HP correnti non possono superare quelli massimi");
        }
    }
    
    /**
     * Classe per statistiche dei personaggi
     */
    public static class CharacterStats {
        public String characterType;
        public int totalCount;
        public int aliveCount;
        public double averageHp;
        public double averageAttack;
        
        @Override
        public String toString() {
            return String.format("Tipo: %s, Totali: %d, Vivi: %d, HP Medio: %.1f, ATT Medio: %.1f",
                characterType, totalCount, aliveCount, averageHp, averageAttack);
        }
    }
}