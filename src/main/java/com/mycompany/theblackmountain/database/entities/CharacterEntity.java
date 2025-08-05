package com.mycompany.theblackmountain.database.entities;

/**
 * Entity che rappresenta un personaggio nel database
 * @author vince
 */
public class CharacterEntity {
    
    private int id;
    private String name;
    private String description;
    private String characterType;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private boolean isAlive;
    private Integer roomId;
    
    // Costruttori
    public CharacterEntity() {}
    
    public CharacterEntity(int id, String name, String description, String characterType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.characterType = characterType;
        this.maxHp = 100;
        this.currentHp = 100;
        this.attack = 10;
        this.defense = 5;
        this.isAlive = true;
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
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
    
    public String getCharacterType() {
        return characterType;
    }
    
    public void setCharacterType(String characterType) {
        this.characterType = characterType;
    }
    
    public int getMaxHp() {
        return maxHp;
    }
    
    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }
    
    public int getCurrentHp() {
        return currentHp;
    }
    
    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
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
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setAlive(boolean alive) {
        isAlive = alive;
    }
    
    public Integer getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    
    @Override
    public String toString() {
        return "CharacterEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", characterType='" + characterType + '\'' +
                ", currentHp=" + currentHp +
                ", maxHp=" + maxHp +
                '}';
    }
}