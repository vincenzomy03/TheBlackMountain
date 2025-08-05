package com.mycompany.theblackmountain.database.entities;

/**
 * Entity che rappresenta un'arma nel database
 * @author vince
 */
public class WeaponEntity {
    
    private int id;
    private int objectId;
    private String weaponType;
    private int attackBonus;
    private int criticalChance;
    private int criticalMultiplier;
    private boolean isPoisoned;
    private int poisonDamage;
    private String specialEffect;
    
    // Costruttori
    public WeaponEntity() {}
    
    public WeaponEntity(int id, int objectId, String weaponType, int attackBonus) {
        this.id = id;
        this.objectId = objectId;
        this.weaponType = weaponType;
        this.attackBonus = attackBonus;
        this.criticalChance = 5;
        this.criticalMultiplier = 2;
        this.isPoisoned = false;
        this.poisonDamage = 0;
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getObjectId() {
        return objectId;
    }
    
    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }
    
    public String getWeaponType() {
        return weaponType;
    }
    
    public void setWeaponType(String weaponType) {
        this.weaponType = weaponType;
    }
    
    public int getAttackBonus() {
        return attackBonus;
    }
    
    public void setAttackBonus(int attackBonus) {
        this.attackBonus = attackBonus;
    }
    
    public int getCriticalChance() {
        return criticalChance;
    }
    
    public void setCriticalChance(int criticalChance) {
        this.criticalChance = criticalChance;
    }
    
    public int getCriticalMultiplier() {
        return criticalMultiplier;
    }
    
    public void setCriticalMultiplier(int criticalMultiplier) {
        this.criticalMultiplier = criticalMultiplier;
    }
    
    public boolean isPoisoned() {
        return isPoisoned;
    }
    
    public void setPoisoned(boolean poisoned) {
        isPoisoned = poisoned;
    }
    
    public int getPoisonDamage() {
        return poisonDamage;
    }
    
    public void setPoisonDamage(int poisonDamage) {
        this.poisonDamage = poisonDamage;
    }
    
    public String getSpecialEffect() {
        return specialEffect;
    }
    
    public void setSpecialEffect(String specialEffect) {
        this.specialEffect = specialEffect;
    }
    
    @Override
    public String toString() {
        return "WeaponEntity{" +
                "id=" + id +
                ", objectId=" + objectId +
                ", weaponType='" + weaponType + '\'' +
                ", attackBonus=" + attackBonus +
                '}';
    }
}