package com.mycompany.theblackmountain.database;

import com.mycompany.theblackmountain.database.dao.*;
import com.mycompany.theblackmountain.database.entities.*;
import com.mycompany.theblackmountain.type.Character;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.Weapon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio di alto livello per l'interazione tra il gioco e il database
 * @author vince
 */
public class GameDatabaseService {
    
    private final RoomDAO roomDAO;
    private final ObjectDAO objectDAO;
    private final CharacterDAO characterDAO;
    private final WeaponDAO weaponDAO;
    
    public GameDatabaseService() {
        this.roomDAO = new RoomDAO();
        this.objectDAO = new ObjectDAO();
        this.characterDAO = new CharacterDAO();
        this.weaponDAO = new WeaponDAO();
    }
    
    /**
     * Carica tutte le stanze dal database
     */
    public List<Room> loadAllRooms() throws SQLException {
        List<RoomEntity> entities = roomDAO.findAll();
        return DatabaseConverter.toRoomsWithConnections(entities);
    }
    
    /**
     * Carica una stanza specifica dal database
     */
    public Room loadRoom(int roomId) throws SQLException {
        RoomEntity entity = roomDAO.findById(roomId);
        return DatabaseConverter.toRoom(entity);
    }
    
    /**
     * Carica tutti gli oggetti di una stanza dal database
     */
    public List<Objects> loadRoomObjects(int roomId) throws SQLException {
        List<ObjectEntity> entities = objectDAO.findByRoomId(roomId);
        List<Objects> objects = new ArrayList<>();
        
        for (ObjectEntity entity : entities) {
            Objects obj = DatabaseConverter.toObject(entity);
            
            // Se è un'arma, carica anche i dati dell'arma
            if ("WEAPON".equals(entity.getObjectType())) {
                WeaponEntity weaponEntity = weaponDAO.findByObjectId(entity.getId());
                if (weaponEntity != null) {
                    Weapon weapon = DatabaseConverter.createWeaponFromDatabase(weaponEntity, entity);
                    if (weapon != null) {
                        objects.add(weapon);
                        continue;
                    }
                }
            }
            
            // Se è un container, carica anche il contenuto
            if ("CONTAINER".equals(entity.getObjectType()) && obj instanceof ContainerObj) {
                ContainerObj container = (ContainerObj) obj;
                List<ObjectEntity> contents = objectDAO.findByContainerId(entity.getId());
                
                for (ObjectEntity contentEntity : contents) {
                    Objects contentObj = DatabaseConverter.toObject(contentEntity);
                    if (contentObj != null) {
                        container.add(contentObj);
                    }
                }
            }
            
            objects.add(obj);
        }
        
        return objects;
    }
    
    /**
     * Carica tutti i personaggi dal database
     */
    public List<Character> loadAllCharacters() throws SQLException {
        List<CharacterEntity> entities = characterDAO.findAll();
        List<Character> characters = new ArrayList<>();
        
        for (CharacterEntity entity : entities) {
            Character character = (Character) DatabaseConverter.toCharacter(entity);
            if (character != null) {
                characters.add(character);
            }
        }
        
        return characters;
    }
    
    /**
     * Carica i personaggi di una stanza specifica
     */
    public List<Character> loadRoomCharacters(int roomId) throws SQLException {
        List<CharacterEntity> entities = characterDAO.findByRoomId(roomId);
        List<Character> characters = new ArrayList<>();
        
        for (CharacterEntity entity : entities) {
            Character character = (Character) DatabaseConverter.toCharacter(entity);
            if (character != null) {
                characters.add(character);
            }
        }
        
        return characters;
    }
    
    /**
     * Carica il giocatore dal database
     */
    public Character loadPlayer() throws SQLException {
        CharacterEntity entity = characterDAO.findPlayer();
        return (Character) DatabaseConverter.toCharacter(entity);
    }
    
    /**
     * Salva lo stato di una stanza nel database
     */
    public void saveRoom(Room room) throws SQLException {
        RoomEntity entity = DatabaseConverter.toRoomEntity(room);
        
        // Verifica se la stanza esiste già
        RoomEntity existing = roomDAO.findById(room.getId());
        if (existing != null) {
            roomDAO.update(entity);
        } else {
            roomDAO.save(entity);
        }
    }
    
    /**
     * Salva lo stato di un personaggio nel database
     */
    public void saveCharacter(Character character) throws SQLException {
        CharacterEntity entity = DatabaseConverter.toCharacterEntity(character);
        
        // Verifica se il personaggio esiste già
        CharacterEntity existing = characterDAO.findById(character.getId());
        if (existing != null) {
            characterDAO.update(entity);
        } else {
            characterDAO.save(entity);
        }
    }
    
    /**
     * Aggiorna la posizione del giocatore
     */
    public void updatePlayerRoom(int playerId, int roomId) throws SQLException {
        characterDAO.moveToRoom(playerId, roomId);
    }
    
    /**
     * Aggiorna gli HP di un personaggio
     */
    public void updateCharacterHp(int characterId, int currentHp, int maxHp) throws SQLException {
        characterDAO.updateHp(characterId, currentHp, maxHp);
    }
    
    /**
     * Aggiorna lo stato di vita di un personaggio
     */
    public void updateCharacterAliveStatus(int characterId, boolean isAlive) throws SQLException {
        characterDAO.updateAliveStatus(characterId, isAlive);
    }
    
    /**
     * Aggiorna lo stato di apertura di un oggetto
     */
    public void updateObjectOpenState(int objectId, boolean isOpen) throws SQLException {
        objectDAO.updateOpenState(objectId, isOpen);
    }
    
    /**
     * Sposta un oggetto da una stanza all'inventario (rimuove da room_objects)
     */
    public void moveObjectToInventory(int objectId, int roomId) throws SQLException {
        objectDAO.removeFromRoom(objectId, roomId);
    }
    
    /**
     * Sposta un oggetto dall'inventario a una stanza
     */
    public void moveObjectToRoom(int objectId, int roomId) throws SQLException {
        objectDAO.addToRoom(objectId, roomId);
    }
    
    /**
     * Carica tutti gli oggetti di un determinato tipo
     */
    public List<Objects> loadObjectsByType(String objectType) throws SQLException {
        List<ObjectEntity> entities = objectDAO.findByType(objectType);
        List<Objects> objects = new ArrayList<>();
        
        for (ObjectEntity entity : entities) {
            Objects obj = DatabaseConverter.toObject(entity);
            if (obj != null) {
                objects.add(obj);
            }
        }
        
        return objects;
    }
    
    /**
     * Carica tutte le armi dal database
     */
    public List<Weapon> loadAllWeapons() throws SQLException {
        List<WeaponEntity> weaponEntities = weaponDAO.findAll();
        List<Weapon> weapons = new ArrayList<>();
        
        for (WeaponEntity weaponEntity : weaponEntities) {
            ObjectEntity objectEntity = objectDAO.findById(weaponEntity.getObjectId());
            if (objectEntity != null) {
                Weapon weapon = DatabaseConverter.createWeaponFromDatabase(weaponEntity, objectEntity);
                if (weapon != null) {
                    weapons.add(weapon);
                }
            }
        }
        
        return weapons;
    }
    
    /**
     * Applica veleno a un'arma nel database
     */
    public void applyPoisonToWeapon(int weaponId, int poisonDamage, String specialEffect) throws SQLException {
        weaponDAO.applyPoison(weaponId, poisonDamage, specialEffect);
    }
    
    /**
     * Rimuove veleno da un'arma nel database
     */
    public void removePoisonFromWeapon(int weaponId) throws SQLException {
        weaponDAO.removePoison(weaponId);
    }
    
    /**
     * Resetta tutti i nemici (li riporta in vita)
     */
    public void resetAllEnemies() throws SQLException {
        characterDAO.resetEnemies();
    }
    
    /**
     * Conta i nemici vivi in una stanza
     */
    public int countAliveEnemiesInRoom(int roomId) throws SQLException {
        return characterDAO.countAliveEnemiesInRoom(roomId);
    }
    
    /**
     * Ottiene statistiche del database
     */
    public DatabaseStats getDatabaseStats() throws SQLException {
        DatabaseStats stats = new DatabaseStats();
        
        stats.totalRooms = roomDAO.count();
        stats.totalObjects = objectDAO.findAll().size();
        stats.totalCharacters = characterDAO.findAll().size();
        stats.totalWeapons = weaponDAO.findAll().size();
        
        stats.aliveCharacters = characterDAO.findAll().stream()
            .mapToInt(c -> c.isAlive() ? 1 : 0)
            .sum();
        
        stats.openableObjects = objectDAO.findAll().stream()
            .mapToInt(o -> o.isOpenable() ? 1 : 0)
            .sum();
        
        stats.weaponStats = weaponDAO.getWeaponStats();
        
        return stats;
    }
    
    /**
     * Classe per contenere statistiche del database
     */
    public static class DatabaseStats {
        public int totalRooms;
        public int totalObjects;
        public int totalCharacters;
        public int totalWeapons;
        public int aliveCharacters;
        public int openableObjects;
        public List<String> weaponStats;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== STATISTICHE DATABASE ===\n");
            sb.append("Stanze totali: ").append(totalRooms).append("\n");
            sb.append("Oggetti totali: ").append(totalObjects).append("\n");
            sb.append("Personaggi totali: ").append(totalCharacters).append("\n");
            sb.append("Armi totali: ").append(totalWeapons).append("\n");
            sb.append("Personaggi vivi: ").append(aliveCharacters).append("\n");
            sb.append("Oggetti apribili: ").append(openableObjects).append("\n");
            
            if (weaponStats != null && !weaponStats.isEmpty()) {
                sb.append("\n--- Statistiche Armi ---\n");
                for (String stat : weaponStats) {
                    sb.append(stat).append("\n");
                }
            }
            
            sb.append("============================");
            return sb.toString();
        }
    }
    
    /**
     * Test di connessione al database
     */
    public boolean testConnection() {
        try {
            roomDAO.count();
            return true;
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database: " + e.getMessage());
            return false;
        }
    }
}