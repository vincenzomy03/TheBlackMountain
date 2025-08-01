/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.save;

import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Character;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Classe per memorizzare i dati di salvataggio del gioco
 * @author vince
 */
public class GameSaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Informazioni di base del salvataggio
    private String saveName;
    private Date saveDate;
    private String gameVersion;
    
    // Stato del giocatore
    private int currentRoomId;
    private List<Objects> inventory;
    private Character playerCharacter;
    
    // Stato del gioco
    private List<Integer> openedChests;
    private List<Integer> defeatedEnemies;
    private List<String> gameFlags;
    private boolean inCombat;
    
    // Stato delle stanze
    private List<RoomState> roomStates;
    
    // Statistiche di gioco
    private long playTimeMillis;
    private int totalEnemiesDefeated;
    private int totalItemsCollected;
    
    public GameSaveData() {
        this.saveDate = new Date();
        this.gameVersion = "1.0";
        this.inventory = new ArrayList<>();
        this.openedChests = new ArrayList<>();
        this.defeatedEnemies = new ArrayList<>();
        this.gameFlags = new ArrayList<>();
        this.roomStates = new ArrayList<>();
        this.playTimeMillis = 0;
        this.totalEnemiesDefeated = 0;
        this.totalItemsCollected = 0;
        this.inCombat = false;
    }
    
    public GameSaveData(String saveName) {
        this();
        this.saveName = saveName;
    }
    
    // Getters e Setters
    public String getSaveName() { return saveName; }
    public void setSaveName(String saveName) { this.saveName = saveName; }
    
    public Date getSaveDate() { return saveDate; }
    public void setSaveDate(Date saveDate) { this.saveDate = saveDate; }
    
    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }
    
    public int getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(int currentRoomId) { this.currentRoomId = currentRoomId; }
    
    public List<Objects> getInventory() { return inventory; }
    public void setInventory(List<Objects> inventory) { this.inventory = inventory; }
    
    public Character getPlayerCharacter() { return playerCharacter; }
    public void setPlayerCharacter(Character playerCharacter) { this.playerCharacter = playerCharacter; }
    
    public List<Integer> getOpenedChests() { return openedChests; }
    public void setOpenedChests(List<Integer> openedChests) { this.openedChests = openedChests; }
    
    public List<Integer> getDefeatedEnemies() { return defeatedEnemies; }
    public void setDefeatedEnemies(List<Integer> defeatedEnemies) { this.defeatedEnemies = defeatedEnemies; }
    
    public List<String> getGameFlags() { return gameFlags; }
    public void setGameFlags(List<String> gameFlags) { this.gameFlags = gameFlags; }
    
    public boolean isInCombat() { return inCombat; }
    public void setInCombat(boolean inCombat) { this.inCombat = inCombat; }
    
    public List<RoomState> getRoomStates() { return roomStates; }
    public void setRoomStates(List<RoomState> roomStates) { this.roomStates = roomStates; }
    
    public long getPlayTimeMillis() { return playTimeMillis; }
    public void setPlayTimeMillis(long playTimeMillis) { this.playTimeMillis = playTimeMillis; }
    
    public int getTotalEnemiesDefeated() { return totalEnemiesDefeated; }
    public void setTotalEnemiesDefeated(int totalEnemiesDefeated) { this.totalEnemiesDefeated = totalEnemiesDefeated; }
    
    public int getTotalItemsCollected() { return totalItemsCollected; }
    public void setTotalItemsCollected(int totalItemsCollected) { this.totalItemsCollected = totalItemsCollected; }
    
    // Metodi di utilità
    public void addGameFlag(String flag) {
        if (!gameFlags.contains(flag)) {
            gameFlags.add(flag);
        }
    }
    
    public boolean hasGameFlag(String flag) {
        return gameFlags.contains(flag);
    }
    
    public void addOpenedChest(int chestId) {
        if (!openedChests.contains(chestId)) {
            openedChests.add(chestId);
        }
    }
    
    public boolean isChestOpened(int chestId) {
        return openedChests.contains(chestId);
    }
    
    public void addDefeatedEnemy(int enemyId) {
        if (!defeatedEnemies.contains(enemyId)) {
            defeatedEnemies.add(enemyId);
            totalEnemiesDefeated++;
        }
    }
    
    public boolean isEnemyDefeated(int enemyId) {
        return defeatedEnemies.contains(enemyId);
    }
    
    public String getFormattedSaveDate() {
        return saveDate.toString();
    }
    
    public String getFormattedPlayTime() {
        long seconds = playTimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s (Tempo: %s)", 
                saveName != null ? saveName : "Salvataggio", 
                getFormattedSaveDate(), 
                getFormattedPlayTime());
    }
    
    // Classe interna per lo stato delle stanze
    public static class RoomState implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int roomId;
        private String description;
        private String lookDescription;
        private List<Objects> objectsInRoom;
        private boolean visited;
        
        public RoomState() {
            this.objectsInRoom = new ArrayList<>();
            this.visited = false;
        }
        
        public RoomState(int roomId) {
            this();
            this.roomId = roomId;
        }
        
        // Getters e Setters
        public int getRoomId() { return roomId; }
        public void setRoomId(int roomId) { this.roomId = roomId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getLookDescription() { return lookDescription; }
        public void setLookDescription(String lookDescription) { this.lookDescription = lookDescription; }
        
        public List<Objects> getObjectsInRoom() { return objectsInRoom; }
        public void setObjectsInRoom(List<Objects> objectsInRoom) { this.objectsInRoom = objectsInRoom; }
        
        public boolean isVisited() { return visited; }
        public void setVisited(boolean visited) { this.visited = visited; }
    }
}