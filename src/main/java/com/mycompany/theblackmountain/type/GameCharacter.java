/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.type;

/**
 * Classe che rappresenta un personaggio nel gioco
 *
 * @author vince
 */
public class GameCharacter {

    private final int id;
    private String name;
    private String description;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private boolean isAlive;
    private CharacterType type;

    /**
     * Costruttore per Character
     *
     * @param id
     * @param name
     * @param description
     * @param maxHp
     * @param attack
     * @param defense
     * @param type
     */
    public GameCharacter(int id, String name, String description, int maxHp, int attack, int defense, CharacterType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.isAlive = true;
        this.type = type;
    }

    /**
     * Infligge danno al personaggio
     *
     * @param damage
     * @return messaggio del danno inflitto
     */
    public String takeDamage(int damage) {
        int actualDamage = Math.max(1, damage - defense);
        currentHp = Math.max(0, currentHp - actualDamage);

        if (currentHp <= 0) {
            isAlive = false;
            return name + " ha subito " + actualDamage + " danni ed è stato sconfitto!";
        } else {
            return name + " ha subito " + actualDamage + " danni. HP rimanenti: " + currentHp + "/" + maxHp;
        }
    }

    /**
     * Cura il personaggio
     *
     * @param healAmount
     * @return messaggio della cura
     */
    public String heal(int healAmount) {
        int oldHp = currentHp;
        currentHp = Math.min(maxHp, currentHp + healAmount);
        int actualHeal = currentHp - oldHp;

        if (actualHeal > 0) {
            return name + " ha recuperato " + actualHeal + " HP. HP attuali: " + currentHp + "/" + maxHp;
        } else {
            return name + " è già in piena salute!";
        }
    }

    /**
     * Cura completamente il personaggio
     *
     * @return messaggio della cura completa
     */
    public String fullHeal() {
        currentHp = maxHp;
        return name + " è stato completamente curato! HP: " + currentHp + "/" + maxHp;
    }

    /**
     * Calcola il danno base che il personaggio può infliggere
     *
     * @return danno base
     */
    public int getBaseDamage() {
        return attack;
    }

    /**
     * Verifica se il personaggio è vivo
     *
     * @return true se è vivo
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Resuscita il personaggio (se necessario)
     */
    public void revive() {
        isAlive = true;
        currentHp = maxHp;
    }

    // Getter e Setter
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
        if (currentHp > maxHp) {
            currentHp = maxHp;
        }
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = Math.max(0, Math.min(maxHp, currentHp));
        if (this.currentHp <= 0) {
            isAlive = false;
        }
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public CharacterType getType() {
        return type;
    }

    public void setType(CharacterType type) {
        this.type = type;
    }

    /**
     * Restituisce lo stato del personaggio come stringa
     *
     * @return stato del personaggio
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append(name).append(" - ");
        status.append("HP: ").append(currentHp).append("/").append(maxHp).append(" - ");
        status.append("ATT: ").append(attack).append(" - ");
        status.append("DEF: ").append(defense).append(" - ");
        status.append("Stato: ").append(isAlive ? "Vivo" : "Morto");
        return status.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GameCharacter character = (GameCharacter) obj;
        return id == character.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    public int getHp() {
        return getCurrentHp();
    }

    public void setHp(int hp) {
        setCurrentHp(hp);
    }

}
