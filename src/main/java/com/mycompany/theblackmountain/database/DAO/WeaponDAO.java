package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.entities.WeaponEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object per le armi
 * @author vince
 */
public class WeaponDAO {
    
    /**
     * Trova un'arma per ID
     */
    public WeaponEntity findById(int id) throws SQLException {
        String sql = "SELECT * FROM weapons WHERE id = ?";
        
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
     * Trova un'arma per object ID
     */
    public WeaponEntity findByObjectId(int objectId) throws SQLException {
        String sql = "SELECT * FROM weapons WHERE object_id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, objectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Trova tutte le armi
     */
    public List<WeaponEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM weapons ORDER BY id";
        List<WeaponEntity> weapons = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                weapons.add(mapResultSetToEntity(rs));
            }
        }
        return weapons;
    }
    
    /**
     * Trova armi per tipo
     */
    public List<WeaponEntity> findByType(String weaponType) throws SQLException {
        String sql = "SELECT * FROM weapons WHERE weapon_type = ? ORDER BY id";
        List<WeaponEntity> weapons = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, weaponType);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    weapons.add(mapResultSetToEntity(rs));
                }
            }
        }
        return weapons;
    }
    
    /**
     * Trova armi avvelenate
     */
    public List<WeaponEntity> findPoisonedWeapons() throws SQLException {
        String sql = "SELECT * FROM weapons WHERE is_poisoned = true ORDER BY id";
        List<WeaponEntity> weapons = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                weapons.add(mapResultSetToEntity(rs));
            }
        }
        return weapons;
    }
    
    /**
     * Trova armi con bonus di attacco superiore a un valore
     */
    public List<WeaponEntity> findByMinAttackBonus(int minAttackBonus) throws SQLException {
        String sql = "SELECT * FROM weapons WHERE attack_bonus >= ? ORDER BY attack_bonus DESC";
        List<WeaponEntity> weapons = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, minAttackBonus);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    weapons.add(mapResultSetToEntity(rs));
                }
            }
        }
        return weapons;
    }
    
    /**
     * Salva una nuova arma
     */
    public void save(WeaponEntity weapon) throws SQLException {
        String sql = "INSERT INTO weapons (id, object_id, weapon_type, attack_bonus, " +
                    "critical_chance, critical_multiplier, is_poisoned, poison_damage, special_effect) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToStatement(stmt, weapon);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna un'arma esistente
     */
    public void update(WeaponEntity weapon) throws SQLException {
        String sql = "UPDATE weapons SET object_id = ?, weapon_type = ?, attack_bonus = ?, " +
                    "critical_chance = ?, critical_multiplier = ?, is_poisoned = ?, " +
                    "poison_damage = ?, special_effect = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, weapon.getObjectId());
            stmt.setString(2, weapon.getWeaponType());
            stmt.setInt(3, weapon.getAttackBonus());
            stmt.setInt(4, weapon.getCriticalChance());
            stmt.setInt(5, weapon.getCriticalMultiplier());
            stmt.setBoolean(6, weapon.isPoisoned());
            stmt.setInt(7, weapon.getPoisonDamage());
            stmt.setString(8, weapon.getSpecialEffect());
            stmt.setInt(9, weapon.getId());
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Elimina un'arma
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM weapons WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Avvelena un'arma
     */
    public void applyPoison(int id, int poisonDamage, String specialEffect) throws SQLException {
        String sql = "UPDATE weapons SET is_poisoned = true, poison_damage = ?, special_effect = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, poisonDamage);
            stmt.setString(2, specialEffect);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Rimuove il veleno da un'arma
     */
    public void removePoison(int id) throws SQLException {
        String sql = "UPDATE weapons SET is_poisoned = false, poison_damage = 0, special_effect = null WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna il bonus di attacco di un'arma
     */
    public void updateAttackBonus(int id, int attackBonus) throws SQLException {
        String sql = "UPDATE weapons SET attack_bonus = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, attackBonus);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Aggiorna la chance di critico di un'arma
     */
    public void updateCriticalChance(int id, int criticalChance) throws SQLException {
        String sql = "UPDATE weapons SET critical_chance = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, criticalChance);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Ottiene statistiche delle armi (query avanzata)
     */
    public List<String> getWeaponStats() throws SQLException {
        String sql = "SELECT weapon_type, COUNT(*) as count, AVG(attack_bonus) as avg_attack, " +
                    "MAX(attack_bonus) as max_attack, MIN(attack_bonus) as min_attack " +
                    "FROM weapons GROUP BY weapon_type ORDER BY avg_attack DESC";
        List<String> stats = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String stat = String.format("Tipo: %s, Quantità: %d, ATT Medio: %.1f, ATT Max: %d, ATT Min: %d",
                    rs.getString("weapon_type"),
                    rs.getInt("count"),
                    rs.getDouble("avg_attack"),
                    rs.getInt("max_attack"),
                    rs.getInt("min_attack")
                );
                stats.add(stat);
            }
        }
        return stats;
    }
    
    // Metodi di utilità
    private WeaponEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        WeaponEntity entity = new WeaponEntity();
        entity.setId(rs.getInt("id"));
        entity.setObjectId(rs.getInt("object_id"));
        entity.setWeaponType(rs.getString("weapon_type"));
        entity.setAttackBonus(rs.getInt("attack_bonus"));
        entity.setCriticalChance(rs.getInt("critical_chance"));
        entity.setCriticalMultiplier(rs.getInt("critical_multiplier"));
        entity.setPoisoned(rs.getBoolean("is_poisoned"));
        entity.setPoisonDamage(rs.getInt("poison_damage"));
        entity.setSpecialEffect(rs.getString("special_effect"));
        return entity;
    }
    
    private void mapEntityToStatement(PreparedStatement stmt, WeaponEntity weapon) throws SQLException {
        stmt.setInt(1, weapon.getId());
        stmt.setInt(2, weapon.getObjectId());
        stmt.setString(3, weapon.getWeaponType());
        stmt.setInt(4, weapon.getAttackBonus());
        stmt.setInt(5, weapon.getCriticalChance());
        stmt.setInt(6, weapon.getCriticalMultiplier());
        stmt.setBoolean(7, weapon.isPoisoned());
        stmt.setInt(8, weapon.getPoisonDamage());
        stmt.setString(9, weapon.getSpecialEffect());
    }
}