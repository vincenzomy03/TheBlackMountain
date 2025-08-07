package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.entities.WeaponEntity;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * DAO per le armi (estende BaseDAO)
 */
public class WeaponDAO extends BaseDAO<WeaponEntity, Integer> {
    
    @Override
    protected String getTableName() {
        return "weapons";
    }
    
    @Override
    protected String getIdColumn() {
        return "id";
    }
    
    @Override
    protected Integer getEntityId(WeaponEntity entity) {
        return entity.getId();
    }
    
    @Override
    protected String buildInsertSql() {
        return "INSERT INTO weapons (id, object_id, weapon_type, attack_bonus, " +
               "critical_chance, critical_multiplier, is_poisoned, poison_damage, special_effect) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    protected String buildUpdateSql() {
        return "UPDATE weapons SET object_id = ?, weapon_type = ?, attack_bonus = ?, " +
               "critical_chance = ?, critical_multiplier = ?, is_poisoned = ?, " +
               "poison_damage = ?, special_effect = ? WHERE id = ?";
    }
    
    @Override
    protected WeaponEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
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
    
    @Override
    protected void mapEntityToInsertStatement(PreparedStatement stmt, WeaponEntity weapon) throws SQLException {
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
    
    @Override
    protected void mapEntityToUpdateStatement(PreparedStatement stmt, WeaponEntity weapon) throws SQLException {
        stmt.setInt(1, weapon.getObjectId());
        stmt.setString(2, weapon.getWeaponType());
        stmt.setInt(3, weapon.getAttackBonus());
        stmt.setInt(4, weapon.getCriticalChance());
        stmt.setInt(5, weapon.getCriticalMultiplier());
        stmt.setBoolean(6, weapon.isPoisoned());
        stmt.setInt(7, weapon.getPoisonDamage());
        stmt.setString(8, weapon.getSpecialEffect());
        stmt.setInt(9, weapon.getId());
    }
    
    /**
     * Trova un'arma per object ID
     */
    public WeaponEntity findByObjectId(int objectId) throws SQLException {
        Optional<WeaponEntity> weapon = executeQuerySingle("SELECT * FROM weapons WHERE object_id = ?", objectId);
        return weapon.orElse(null);
    }
    
    /**
     * Trova armi per tipo
     */
    public List<WeaponEntity> findByType(String weaponType) throws SQLException {
        return executeQuery("SELECT * FROM weapons WHERE weapon_type = ? ORDER BY id", weaponType);
    }
    
    /**
     * Applica veleno a un'arma
     */
    public void applyPoison(int id, int poisonDamage, String specialEffect) throws SQLException {
        executeUpdate("UPDATE weapons SET is_poisoned = true, poison_damage = ?, special_effect = ? WHERE id = ?", 
                     poisonDamage, specialEffect, id);
    }
    
    /**
     * Rimuove il veleno da un'arma
     */
    public void removePoison(int id) throws SQLException {
        executeUpdate("UPDATE weapons SET is_poisoned = false, poison_damage = 0, special_effect = null WHERE id = ?", id);
    }
}