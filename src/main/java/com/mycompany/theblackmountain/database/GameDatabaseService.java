package com.mycompany.theblackmountain.database;

import com.mycompany.theblackmountain.database.dao.*;
import com.mycompany.theblackmountain.database.entities.*;
import com.mycompany.theblackmountain.type.Character;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.Weapon;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servizio database migliorato con gestione errori robusta e supporto transazionale
 * @author vince
 */
public class GameDatabaseService {
    
    private final DatabaseManager databaseManager;
    private final RoomDAO roomDAO;
    private final ObjectDAO objectDAO;
    private final CharacterDAO characterDAO;
    private final WeaponDAO weaponDAO;
    
    public GameDatabaseService() {
        this.databaseManager = DatabaseManager.getInstance();
        this.roomDAO = new RoomDAO();
        this.objectDAO = new ObjectDAO();
        this.characterDAO = new CharacterDAO();
        this.weaponDAO = new WeaponDAO();
    }
    
    // ====== OPERAZIONI DI CARICAMENTO ======
    
    /**
     * Carica tutte le stanze dal database con gestione errori
     */
    public List<Room> loadAllRooms() throws DatabaseException {
        try {
            List<RoomEntity> entities = roomDAO.findAll();
            List<Room> rooms = DatabaseConverter.toRoomsWithConnections(entities);
            
            // Valida le connessioni delle stanze
            if (!DatabaseConverter.validateRoomConnections(entities)) {
                System.err.println("⚠️ Rilevate connessioni stanza non valide");
            }
            
            System.out.println("✅ Caricate " + rooms.size() + " stanze dal database");
            return rooms;
            
            throw new DatabaseException("Errore nello spostamento oggetto nella stanza", e);
        }
    }
    
    // ====== OPERAZIONI DI GESTIONE ARMI ======
    
    /**
     * Carica tutte le armi dal database
     */
    public List<Weapon> loadAllWeapons() throws DatabaseException {
        try {
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
            
            System.out.println("✅ Caricate " + weapons.size() + " armi dal database");
            return weapons;
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento delle armi", e);
        }
    }
    
    /**
     * Applica veleno a un'arma
     */
    public void applyPoisonToWeapon(int weaponId, int poisonDamage, String specialEffect) throws DatabaseException {
        try {
            weaponDAO.applyPoison(weaponId, poisonDamage, specialEffect);
            System.out.println("✅ Veleno applicato all'arma " + weaponId);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'applicazione del veleno all'arma", e);
        }
    }
    
    /**
     * Rimuove veleno da un'arma
     */
    public void removePoisonFromWeapon(int weaponId) throws DatabaseException {
        try {
            weaponDAO.removePoison(weaponId);
            System.out.println("✅ Veleno rimosso dall'arma " + weaponId);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nella rimozione del veleno dall'arma", e);
        }
    }
    
    // ====== OPERAZIONI DI RESET E MANUTENZIONE ======
    
    /**
     * Resetta tutti i nemici (li riporta in vita con HP pieni)
     */
    public void resetAllEnemies() throws DatabaseException {
        try {
            characterDAO.resetEnemies();
            System.out.println("✅ Tutti i nemici sono stati resettati");
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel reset dei nemici", e);
        }
    }
    
    /**
     * Conta i nemici vivi in una stanza
     */
    public int countAliveEnemiesInRoom(int roomId) throws DatabaseException {
        try {
            return characterDAO.countAliveEnemiesInRoom(roomId);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel conteggio nemici vivi per stanza: " + roomId, e);
        }
    }
    
    /**
     * Ottiene statistiche complete del database
     */
    public DatabaseStats getDatabaseStats() throws DatabaseException {
        try {
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
            
            // Statistiche aggiuntive
            stats.playerCount = characterDAO.findByType("PLAYER").size();
            stats.enemyCount = stats.totalCharacters - stats.playerCount;
            stats.deadEnemies = stats.enemyCount - (stats.aliveCharacters - stats.playerCount);
            
            return stats;
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'ottenimento delle statistiche", e);
        }
    }
    
    // ====== OPERAZIONI DI VALIDAZIONE E TEST ======
    
    /**
     * Test completo di connessione e integrità del database
     */
    public DatabaseHealthCheck performHealthCheck() {
        DatabaseHealthCheck check = new DatabaseHealthCheck();
        
        try {
            // Test connessione base
            check.connectionOk = databaseManager.isHealthy();
            
            if (!check.connectionOk) {
                check.errors.add("Connessione database non disponibile");
                return check;
            }
            
            // Test tabelle principali
            try {
                roomDAO.count();
                check.tablesOk = true;
            } catch (Exception e) {
                check.tablesOk = false;
                check.errors.add("Errore accesso tabelle: " + e.getMessage());
            }
            
            // Test integrità dati
            try {
                List<RoomEntity> rooms = roomDAO.findAll();
                check.dataIntegrityOk = DatabaseConverter.validateRoomConnections(rooms);
                if (!check.dataIntegrityOk) {
                    check.warnings.add("Rilevate connessioni stanza non valide");
                }
            } catch (Exception e) {
                check.dataIntegrityOk = false;
                check.errors.add("Errore validazione integrità: " + e.getMessage());
            }
            
            // Test giocatore
            try {
                CharacterEntity player = characterDAO.findPlayer();
                check.playerExists = (player != null);
                if (!check.playerExists) {
                    check.warnings.add("Giocatore non trovato nel database");
                }
            } catch (Exception e) {
                check.playerExists = false;
                check.errors.add("Errore ricerca giocatore: " + e.getMessage());
            }
            
            // Calcola punteggio salute generale
            int score = 0;
            if (check.connectionOk) score += 25;
            if (check.tablesOk) score += 25;
            if (check.dataIntegrityOk) score += 25;
            if (check.playerExists) score += 25;
            check.healthScore = score;
            
            check.overall = (score >= 75) ? "HEALTHY" : (score >= 50) ? "WARNING" : "CRITICAL";
            
        } catch (Exception e) {
            check.errors.add("Errore critico durante health check: " + e.getMessage());
            check.overall = "CRITICAL";
        }
        
        return check;
    }
    
    /**
     * Esegue backup dei dati principali
     */
    public BackupData createBackup() throws DatabaseException {
        try {
            BackupData backup = new BackupData();
            backup.timestamp = System.currentTimeMillis();
            
            // Backup stanze
            backup.rooms = roomDAO.findAll();
            
            // Backup oggetti
            backup.objects = objectDAO.findAll();
            
            // Backup personaggi
            backup.characters = characterDAO.findAll();
            
            // Backup armi
            backup.weapons = weaponDAO.findAll();
            
            System.out.println("✅ Backup creato con " + backup.rooms.size() + " stanze, " + 
                             backup.objects.size() + " oggetti, " + backup.characters.size() + " personaggi");
            
            return backup;
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nella creazione del backup", e);
        }
    }
    
    // ====== CLASSI DI SUPPORTO ======
    
    /**
     * Statistiche del database migliorate
     */
    public static class DatabaseStats {
        public int totalRooms;
        public int totalObjects;
        public int totalCharacters;
        public int totalWeapons;
        public int aliveCharacters;
        public int openableObjects;
        public int playerCount;
        public int enemyCount;
        public int deadEnemies;
        public List<String> weaponStats;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== STATISTICHE DATABASE ===\n");
            sb.append("Stanze totali: ").append(totalRooms).append("\n");
            sb.append("Oggetti totali: ").append(totalObjects).append("\n");
            sb.append("  - Apribili: ").append(openableObjects).append("\n");
            sb.append("Personaggi totali: ").append(totalCharacters).append("\n");
            sb.append("  - Giocatori: ").append(playerCount).append("\n");
            sb.append("  - Nemici: ").append(enemyCount).append("\n");
            sb.append("  - Nemici vivi: ").append(aliveCharacters - playerCount).append("\n");
            sb.append("  - Nemici morti: ").append(deadEnemies).append("\n");
            sb.append("Armi totali: ").append(totalWeapons).append("\n");
            
            if (weaponStats != null && !weaponStats.isEmpty()) {
                sb.append("\n--- Statistiche Armi ---\n");
                weaponStats.forEach(stat -> sb.append(stat).append("\n"));
            }
            
            sb.append("============================");
            return sb.toString();
        }
    }
    
    /**
     * Risultato del controllo di salute del database
     */
    public static class DatabaseHealthCheck {
        public boolean connectionOk;
        public boolean tablesOk;
        public boolean dataIntegrityOk;
        public boolean playerExists;
        public int healthScore;
        public String overall;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DATABASE HEALTH CHECK ===\n");
            sb.append("Stato generale: ").append(overall).append(" (").append(healthScore).append("/100)\n");
            sb.append("Connessione: ").append(connectionOk ? "✅" : "❌").append("\n");
            sb.append("Tabelle: ").append(tablesOk ? "✅" : "❌").append("\n");
            sb.append("Integrità dati: ").append(dataIntegrityOk ? "✅" : "❌").append("\n");
            sb.append("Giocatore presente: ").append(playerExists ? "✅" : "❌").append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("\n🚫 ERRORI:\n");
                errors.forEach(error -> sb.append("  - ").append(error).append("\n"));
            }
            
            if (!warnings.isEmpty()) {
                sb.append("\n⚠️ AVVERTIMENTI:\n");
                warnings.forEach(warning -> sb.append("  - ").append(warning).append("\n"));
            }
            
            sb.append("=============================");
            return sb.toString();
        }
    }
    
    /**
     * Dati di backup
     */
    public static class BackupData {
        public long timestamp;
        public List<RoomEntity> rooms;
        public List<ObjectEntity> objects;
        public List<CharacterEntity> characters;
        public List<WeaponEntity> weapons;
        
        public String getFormattedTimestamp() {
            return new java.util.Date(timestamp).toString();
        }
    }
            throw new DatabaseException("Errore nel caricamento delle stanze", e);
        }
    }
    
    /**
     * Carica una stanza specifica con validazione
     */
    public Optional<Room> loadRoom(int roomId) throws DatabaseException {
        try {
            RoomEntity entity = roomDAO.findById(roomId);
            if (entity == null) {
                System.out.println("⚠️ Stanza con ID " + roomId + " non trovata");
                return Optional.empty();
            }
            
            Room room = DatabaseConverter.toRoom(entity);
            return Optional.ofNullable(room);
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento della stanza ID: " + roomId, e);
        }
    }
    
    /**
     * Carica oggetti di una stanza con supporto per armi e container
     */
    public List<Objects> loadRoomObjects(int roomId) throws DatabaseException {
        try {
            List<ObjectEntity> entities = objectDAO.findByRoomId(roomId);
            List<Objects> objects = new ArrayList<>();
            
            for (ObjectEntity entity : entities) {
                Objects obj = processObjectEntity(entity);
                if (obj != null) {
                    objects.add(obj);
                }
            }
            
            System.out.println("✅ Caricati " + objects.size() + " oggetti per stanza " + roomId);
            return objects;
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento oggetti per stanza: " + roomId, e);
        }
    }
    
    /**
     * Processa un ObjectEntity creando l'oggetto appropriato (normale, arma, container)
     */
    private Objects processObjectEntity(ObjectEntity entity) throws SQLException {
        Objects obj = DatabaseConverter.toObject(entity);
        if (obj == null) return null;
        
        // Se è un'arma, carica i dati specifici
        if ("WEAPON".equals(entity.getObjectType())) {
            WeaponEntity weaponEntity = weaponDAO.findByObjectId(entity.getId());
            if (weaponEntity != null) {
                Weapon weapon = DatabaseConverter.createWeaponFromDatabase(weaponEntity, entity);
                if (weapon != null) {
                    return weapon;
                }
            }
        }
        
        // Se è un container, carica il contenuto
        if ("CONTAINER".equals(entity.getObjectType()) && obj instanceof ContainerObj) {
            ContainerObj container = (ContainerObj) obj;
            loadContainerContents(container, entity.getId());
        }
        
        return obj;
    }
    
    /**
     * Carica il contenuto di un container
     */
    private void loadContainerContents(ContainerObj container, int containerId) throws SQLException {
        List<ObjectEntity> contents = objectDAO.findByContainerId(containerId);
        
        for (ObjectEntity contentEntity : contents) {
            Objects contentObj = processObjectEntity(contentEntity);
            if (contentObj != null) {
                container.add(contentObj);
            }
        }
    }
    
    /**
     * Carica i personaggi di una stanza (escluso il giocatore)
     */
    public List<Character> loadRoomEnemies(int roomId) throws DatabaseException {
        try {
            List<CharacterEntity> entities = characterDAO.findByRoomId(roomId);
            List<Character> enemies = new ArrayList<>();
            
            for (CharacterEntity entity : entities) {
                // Esclude il giocatore
                if (!"PLAYER".equals(entity.getCharacterType())) {
                    Character enemy = DatabaseConverter.toCharacter(entity);
                    if (enemy != null) {
                        enemies.add(enemy);
                    }
                }
            }
            
            System.out.println("✅ Caricati " + enemies.size() + " nemici per stanza " + roomId);
            return enemies;
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento nemici per stanza: " + roomId, e);
        }
    }
    
    /**
     * Carica il giocatore dal database
     */
    public Optional<Character> loadPlayer() throws DatabaseException {
        try {
            CharacterEntity entity = characterDAO.findPlayer();
            if (entity == null) {
                System.err.println("❌ Giocatore non trovato nel database!");
                return Optional.empty();
            }
            
            Character player = DatabaseConverter.toCharacter(entity);
            System.out.println("✅ Giocatore caricato: " + player.getName() + " (HP: " + player.getCurrentHp() + "/" + player.getMaxHp() + ")");
            return Optional.ofNullable(player);
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel caricamento del giocatore", e);
        }
    }
    
    /**
     * Ottiene l'ID della stanza del giocatore
     */
    public int getPlayerRoomId(int playerId) throws DatabaseException {
        try {
            CharacterEntity player = characterDAO.findById(playerId);
            if (player != null && player.getRoomId() != null) {
                return player.getRoomId();
            }
            
            System.out.println("⚠️ Posizione giocatore non trovata, uso stanza iniziale");
            return 0; // ID stanza iniziale
            
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel recupero posizione giocatore: " + playerId, e);
        }
    }
    
    // ====== OPERAZIONI DI SALVATAGGIO ======
    
    /**
     * Salva lo stato completo del gioco in una transazione
     */
    public void saveCompleteGameState(List<Room> rooms, Character player) throws DatabaseException {
        databaseManager.executeInTransaction(conn -> {
            System.out.println("💾 Salvataggio stato completo del gioco...");
            
            // 1. Salva tutte le stanze
            for (Room room : rooms) {
                saveRoom(room, conn);
                
                // Salva i nemici nella stanza
                for (Character enemy : room.getEnemies()) {
                    saveCharacter(enemy, conn);
                }
            }
            
            // 2. Salva il giocatore
            if (player != null) {
                saveCharacter(player, conn);
            }
            
            System.out.println("✅ Stato gioco salvato completamente");
        });
    }
    
    /**
     * Salva una stanza in una connessione specifica
     */
    private void saveRoom(Room room, Connection conn) throws SQLException {
        RoomEntity entity = DatabaseConverter.toRoomEntity(room);
        
        // Verifica se esiste già
        RoomEntity existing = roomDAO.findById(room.getId());
        if (existing != null) {
            roomDAO.update(entity);
        } else {
            roomDAO.save(entity);
        }
    }
    
    /**
     * Salva un personaggio in una connessione specifica
     */
    private void saveCharacter(Character character, Connection conn) throws SQLException {
        CharacterEntity entity = DatabaseConverter.toCharacterEntity(character);
        
        // Aggiorna la stanza se disponibile
        if (character.getCurrentRoom() != null) {
            entity.setRoomId(character.getCurrentRoom().getId());
        }
        
        // Verifica se esiste già
        CharacterEntity existing = characterDAO.findById(character.getId());
        if (existing != null) {
            characterDAO.update(entity);
        } else {
            characterDAO.save(entity);
        }
    }
    
    /**
     * Salva una stanza (operazione singola)
     */
    public void saveRoom(Room room) throws DatabaseException {
        try {
            saveRoom(room, null);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel salvataggio stanza: " + room.getName(), e);
        }
    }
    
    /**
     * Salva un personaggio (operazione singola)
     */
    public void saveCharacter(Character character) throws DatabaseException {
        try {
            saveCharacter(character, null);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel salvataggio personaggio: " + character.getName(), e);
        }
    }
    
    // ====== OPERAZIONI DI AGGIORNAMENTO ======
    
    /**
     * Aggiorna la posizione del giocatore
     */
    public void updatePlayerPosition(int playerId, int roomId) throws DatabaseException {
        try {
            characterDAO.moveToRoom(playerId, roomId);
            System.out.println("✅ Posizione giocatore aggiornata: stanza " + roomId);
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'aggiornamento posizione giocatore", e);
        }
    }
    
    /**
     * Aggiorna lo stato di un personaggio (HP, stato vita)
     */
    public void updateCharacterState(Character character) throws DatabaseException {
        databaseManager.executeInTransaction(conn -> {
            characterDAO.updateHp(character.getId(), character.getCurrentHp(), character.getMaxHp());
            characterDAO.updateAliveStatus(character.getId(), character.isAlive());
        });
    }
    
    /**
     * Aggiorna lo stato di un oggetto
     */
    public void updateObjectState(Objects obj) throws DatabaseException {
        try {
            if (obj.isOpenable()) {
                objectDAO.updateOpenState(obj.getId(), obj.isOpen());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Errore nell'aggiornamento oggetto: " + obj.getName(), e);
        }
    }
    
    /**
     * Gestisce movimento oggetti con transazione
     */
    public void moveObjectToInventory(int objectId, int fromRoomId) throws DatabaseException {
        try {
            objectDAO.removeFromRoom(objectId, fromRoomId);
            System.out.println("✅ Oggetto " + objectId + " spostato nell'inventario");
        } catch (SQLException e) {
            throw new DatabaseException("Errore nello spostamento oggetto nell'inventario", e);
        }
    }
    
    /**
     * Sposta oggetto dall'inventario a una stanza
     */
    public void moveObjectToRoom(int objectId, int toRoomId) throws DatabaseException {
        try {
            objectDAO.addToRoom(objectId, toRoomId);
            System.out.println("✅ Oggetto " + objectId + " spostato nella stanza " + toRoomId);
        } catch (SQLException e) {