/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.type;

/**
 * Classe che rappresenta un'arma nel gioco
 * @author vince  
 */
public class Weapon extends GameObjects {
    
    private int attackBonus;
    private int criticalChance; // Percentuale (0-100)
    private int criticalMultiplier; // Moltiplicatore del danno critico
    private WeaponType weaponType;
    private boolean isPoisoned;
    private int poisonDamage;
    private String specialEffect;
    
    /**
     * Costruttore per Weapon
     * @param id
     * @param name
     * @param description
     * @param attackBonus
     * @param weaponType
     */
    public Weapon(int id, String name, String description, int attackBonus, WeaponType weaponType) {
        super(id, name, description);
        this.attackBonus = attackBonus;
        this.weaponType = weaponType;
        this.criticalChance = 5; // Default 5%
        this.criticalMultiplier = 2; // Default x2
        this.isPoisoned = false;
        this.poisonDamage = 0;
        this.specialEffect = "";
        setPickupable(true);
    }
    
    /**
     * Costruttore completo per Weapon
     * @param id
     * @param name
     * @param description
     * @param attackBonus
     * @param weaponType
     * @param criticalChance
     * @param criticalMultiplier
     */
    public Weapon(int id, String name, String description, int attackBonus, WeaponType weaponType, 
                  int criticalChance, int criticalMultiplier) {
        super(id, name, description);
        this.attackBonus = attackBonus;
        this.weaponType = weaponType;
        this.criticalChance = criticalChance;
        this.criticalMultiplier = criticalMultiplier;
        this.isPoisoned = false;
        this.poisonDamage = 0;
        this.specialEffect = "";
        setPickupable(true);
    }
    
    /**
     * Calcola il danno totale dell'arma
     * @param baseDamage danno base del personaggio
     * @return danno totale
     */
    public int calculateDamage(int baseDamage) {
        int totalDamage = baseDamage + attackBonus;
        
        // Controllo critico
        if (Math.random() * 100 < criticalChance) {
            totalDamage *= criticalMultiplier;
            return totalDamage; // Il danno critico include già il veleno se presente
        }
        
        // Aggiunge danno da veleno se l'arma è avvelenata
        if (isPoisoned) {
            totalDamage += poisonDamage;
        }
        
        return totalDamage;
    }
    
    /**
     * Avvelena l'arma
     * @param poisonDamage danno aggiuntivo del veleno
     */
    public void applyPoison(int poisonDamage) {
        this.isPoisoned = true;
        this.poisonDamage = poisonDamage;
        this.specialEffect = "Avvelenata (+" + poisonDamage + " danno veleno)";
        setDescription(getDescription() + " [" + specialEffect + "]");
    }
    
    /**
     * Verifica se l'attacco è critico
     * @return true se l'attacco è critico
     */
    public boolean isCriticalHit() {
        return Math.random() * 100 < criticalChance;
    }
    
    /**
     * Restituisce informazioni dettagliate sull'arma
     * @return stringa con le statistiche dell'arma
     */
    public String getWeaponStats() {
        StringBuilder stats = new StringBuilder();
        stats.append(getName()).append(" - ");
        stats.append("ATT: +").append(attackBonus).append(" - ");
        stats.append("Crit: ").append(criticalChance).append("% - ");
        stats.append("Tipo: ").append(weaponType.getDisplayName());
        
        if (isPoisoned) {
            stats.append(" - ").append(specialEffect);
        }
        
        return stats.toString();
    }
    
    // Getter e Setter
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
        this.criticalChance = Math.max(0, Math.min(100, criticalChance));
    }
    
    public int getCriticalMultiplier() {
        return criticalMultiplier;
    }
    
    public void setCriticalMultiplier(int criticalMultiplier) {
        this.criticalMultiplier = Math.max(1, criticalMultiplier);
    }
    
    public WeaponType getWeaponType() {
        return weaponType;
    }
    
    public void setWeaponType(WeaponType weaponType) {
        this.weaponType = weaponType;
    }
    
    public boolean isPoisoned() {
        return isPoisoned;
    }
    
    public int getPoisonDamage() {
        return poisonDamage;
    }
    
    public String getSpecialEffect() {
        return specialEffect;
    }
    
    public void setSpecialEffect(String specialEffect) {
        this.specialEffect = specialEffect;
    }
}