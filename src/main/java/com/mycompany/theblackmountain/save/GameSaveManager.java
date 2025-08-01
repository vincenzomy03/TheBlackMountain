package com.mycompany.theblackmountain.save;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.Room;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manager per il salvataggio e caricamento dei dati di gioco
 * @author vince
 */
public class GameSaveManager {
    
    private static final String SAVE_DIRECTORY = "src/main/saves";
    private static final String SAVE_EXTENSION = ".dat";
    
    static {
        // Crea la directory dei salvataggi se non esiste
        File saveDir = new File(SAVE_DIRECTORY);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
    }
    
    /**
     * Salva lo stato corrente del gioco
     * @param gameDescription stato del gioco
     * @param combatSystem sistema di combattimento
     * @param saveName nome del salvataggio
     * @param playTimeMillis tempo di gioco in millisecondi
     * @return true se il salvataggio è riuscito
     */
    public static boolean saveGame(GameDescription gameDescription, CombatSystem combatSystem, 
                                 String saveName, long playTimeMillis) {
        try {
            GameSaveData saveData = createSaveData(gameDescription, combatSystem, saveName, playTimeMillis);
            
            File saveFile = new File(SAVE_DIRECTORY + sanitizeFilename(saveName) + SAVE_EXTENSION);
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
                oos.writeObject(saveData);
            }
            
            System.out.println("Gioco salvato in: " + saveFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("Errore nel salvataggio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Carica un gioco salvato
     * @param saveFile file di salvataggio
     * @return dati del gioco salvato
     * @throws Exception se il caricamento fallisce
     */
    public static GameSaveData loadGame(File saveFile) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            GameSaveData saveData = (GameSaveData) ois.readObject();
            System.out.println("Gioco caricato da: " + saveFile.getAbsolutePath());
            return saveData;
        }
    }
    
    /**
     * Applica i dati salvati al gioco
     * @param gameDescription gioco da modificare
     * @param saveData dati salvati
     * @param combatSystem sistema di combattimento
     */
    public static void applyLoadedData(TBMGame gameDescription, GameSaveData saveData, CombatSystem combatSystem) {
        try {
            System.out.println("Applicando dati salvati...");
            
            // Ripristina la stanza corrente
            Room currentRoom = findRoomById(gameDescription, saveData.getCurrentRoomId());
            if (currentRoom != null) {
                gameDescription.setCurrentRoom(currentRoom);
                System.out.println("Stanza corrente ripristinata: " + currentRoom.getName());
            }
            
            // Ripristina l'inventario
            gameDescription.getInventory().clear();
            gameDescription.getInventory().addAll(saveData.getInventory());
            System.out.println("Inventario ripristinato con " + saveData.getInventory().size() + " oggetti");
            
            // Ripristina lo stato delle stanze (casse aperte, oggetti rimossi)
            restoreRoomStates(gameDescription, saveData);
            
            // Ripristina il personaggio giocatore nel combat system
            if (saveData.getPlayerCharacter() != null && combatSystem != null) {
                combatSystem.getPlayer().setHp(saveData.getPlayerCharacter().getHp());
                combatSystem.getPlayer().setMaxHp(saveData.getPlayerCharacter().getMaxHp());
                System.out.println("Statistiche giocatore ripristinate: " + 
                    saveData.getPlayerCharacter().getHp() + "/" + saveData.getPlayerCharacter().getMaxHp() + " HP");
            }
            
            // Ripristina i nemici sconfitti
            restoreDefeatedEnemies(gameDescription, saveData);
            
            System.out.println("Dati salvati applicati con successo!");
            
        } catch (Exception e) {
            System.err.println("Errore nell'applicazione dei dati salvati: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore nell'applicazione dei dati salvati: " + e.getMessage());
        }
    }
    
    /**
     * Crea un oggetto GameSaveData dallo stato corrente del gioco
     */
    private static GameSaveData createSaveData(GameDescription gameDescription, CombatSystem combatSystem, 
                                             String saveName, long playTimeMillis) {
        GameSaveData saveData = new GameSaveData(saveName);
        
        // Informazioni base
        saveData.setSaveDate(new Date());
        saveData.setPlayTimeMillis(playTimeMillis);
        
        // Stato del giocatore
        if (gameDescription.getCurrentRoom() != null) {
            saveData.setCurrentRoomId(gameDescription.getCurrentRoom().getId());
        }
        
        // Copia dell'inventario - crea nuovi oggetti per evitare problemi di riferimento
        List<Objects> inventoryCopy = new ArrayList<>();
        for (Objects obj : gameDescription.getInventory()) {
            inventoryCopy.add(cloneObject(obj));
        }
        saveData.setInventory(inventoryCopy);
        
        // Stato del personaggio
        if (combatSystem != null && combatSystem.getPlayer() != null) {
            saveData.setPlayerCharacter(combatSystem.getPlayer());
        }
        
        // Stato del combattimento
        if (combatSystem != null) {
            saveData.setInCombat(combatSystem.isInCombat());
        }
        
        // Stato delle stanze
        saveRoomStates(gameDescription, saveData);
        
        // Flags di gioco (casse aperte, nemici sconfitti, ecc.)
        saveGameFlags(gameDescription, saveData, combatSystem);
        
        return saveData;
    }
    
    /**
     * Clona un oggetto per il salvataggio
     */
    private static Objects cloneObject(Objects original) {
        if (original instanceof ContainerObj) {
            ContainerObj container = (ContainerObj) original;
            ContainerObj clone = new ContainerObj(container.getId(), container.getName(), container.getDescription());
            clone.setAlias(container.getAlias());
            clone.setOpenable(container.isOpenable());
            clone.setPickupable(container.isPickupable());
            clone.setOpen(container.isOpen());
            // Clona anche il contenuto del container
            for (Object item : container.getList()) {
                clone.add(cloneObject((Objects) item));
            }
            return clone;
        } else {
            Objects clone = new Objects(original.getId(), original.getName(), original.getDescription());
            clone.setAlias(original.getAlias());
            clone.setPickupable(original.isPickupable());
            clone.setOpen(original.isOpen());
            return clone;
        }
    }
    
    /**
     * Salva lo stato delle stanze
     */
    private static void saveRoomStates(GameDescription gameDescription, GameSaveData saveData) {
        for (Room room : gameDescription.getRooms()) {
            GameSaveData.RoomState roomState = new GameSaveData.RoomState(room.getId());
            roomState.setDescription(room.getDescription());
            roomState.setLookDescription(room.getLook());
            roomState.setVisited(true);
            
            // Copia degli oggetti nella stanza (casse, oggetti a terra)
            List<Objects> roomObjectsCopy = new ArrayList<>();
            if (room.getObjects() != null) {
                for (Objects obj : room.getObjects()) {
                    roomObjectsCopy.add(cloneObject(obj));
                }
            }
            roomState.setObjectsInRoom(roomObjectsCopy);
            
            saveData.getRoomStates().add(roomState);
        }
    }
    
    /**
     * Ripristina lo stato delle stanze
     */
    private static void restoreRoomStates(GameDescription gameDescription, GameSaveData saveData) {
        for (GameSaveData.RoomState roomState : saveData.getRoomStates()) {
            Room room = findRoomById(gameDescription, roomState.getRoomId());
            if (room != null) {
                // Ripristina gli oggetti nella stanza
                room.getObjects().clear();
                room.getObjects().addAll(roomState.getObjectsInRoom());
                
                System.out.println("Stanza " + room.getName() + " ripristinata con " + 
                    roomState.getObjectsInRoom().size() + " oggetti");
            }
        }
    }
    
    /**
     * Salva i flag di gioco
     */
    private static void saveGameFlags(GameDescription gameDescription, GameSaveData saveData, CombatSystem combatSystem) {
        // Salva le casse aperte
        for (Room room : gameDescription.getRooms()) {
            for (Objects obj : room.getObjects()) {
                if (obj instanceof ContainerObj && obj.isOpen()) {
                    saveData.addOpenedChest(obj.getId());
                }
            }
        }
        
        // Salva i nemici sconfitti (se il combat system ha questa informazione)
        if (combatSystem != null) {
            // Assumiamo che il CombatSystem tenga traccia dei nemici sconfitti
            // Questo dipende dall'implementazione del CombatSystem
            // Per ora salviamo alcuni flag di esempio
            
            // Se tutte le stanze sono state visitate senza nemici, significa che sono stati sconfitti
            for (Room room : gameDescription.getRooms()) {
                if (room.getDescription() != null && 
                    !room.getDescription().contains("goblin") && 
                    !room.getDescription().contains("topo") && 
                    !room.getDescription().contains("demone")) {
                    // La stanza è stata "pulita" dai nemici
                    saveData.addGameFlag("room_" + room.getId() + "_cleared");
                }
            }
        }
    }
    
    /**
     * Ripristina i nemici sconfitti
     */
    private static void restoreDefeatedEnemies(GameDescription gameDescription, GameSaveData saveData) {
        // Ripristina lo stato delle stanze basandosi sui flag salvati
        for (String flag : saveData.getGameFlags()) {
            if (flag.startsWith("room_") && flag.endsWith("_cleared")) {
                // Estrai l'ID della stanza dal flag
                try {
                    String roomIdStr = flag.replace("room_", "").replace("_cleared", "");
                    int roomId = Integer.parseInt(roomIdStr);
                    Room room = findRoomById(gameDescription, roomId);
                    
                    if (room != null) {
                        // Aggiorna la descrizione della stanza per rimuovere i nemici
                        updateRoomDescriptionAfterCombat(room);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Flag non riconosciuto: " + flag);
                }
            }
        }
    }
    
    /**
     * Aggiorna la descrizione di una stanza dopo che i nemici sono stati sconfitti
     */
    private static void updateRoomDescriptionAfterCombat(Room room) {
        String currentDesc = room.getDescription();
        
        // Rimuovi le descrizioni dei nemici dalle stanze
        switch (room.getId()) {
            case 0: // Ingresso
                if (currentDesc.contains("goblin")) {
                    room.setDescription("Ti trovi all'ingresso della fortezza maledetta.\n" +
                        "L'aria è densa di umidità e il pavimento è cosparso di muschio.\n" +
                        "Il corpo del goblin giace immobile sul pavimento. Il passaggio è ora libero.");
                }
                break;
            case 1: // Stanza del Topo
                if (currentDesc.contains("topo")) {
                    room.setDescription("Le pareti sono coperte di ragnatele e muffa.\n" +
                        "Il grosso topo è stato sconfitto. La stanza ora è silenziosa.");
                }
                break;
            case 2: // Mensa
                if (currentDesc.contains("goblin")) {
                    room.setDescription("Le tavole di legno sono rovesciate, piatti infranti ovunque.\n" +
                        "I corpi dei due goblin giacciono tra i resti del loro ultimo pasto.");
                }
                break;
            case 4: // Sala delle Guardie  
                if (currentDesc.contains("goblin")) {
                    room.setDescription("I resti di un banchetto interrotto sono sparsi ovunque.\n" +
                        "I goblin sono stati sconfitti. La sala è ora pacifica.");
                }
                break;
            case 7: // Boss Room
                if (currentDesc.contains("demone")) {
                    room.setDescription("L'aria si sta lentamente purificando. Il fumo si dirada.\n" +
                        "Il cane demone è stato sconfitto! La principessa può essere liberata.");
                }
                break;
        }
    }
    
    /**
     * Trova una stanza per ID
     */
    private static Room findRoomById(GameDescription gameDescription, int roomId) {
        for (Room room : gameDescription.getRooms()) {
            if (room.getId() == roomId) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Ottiene la lista dei file di salvataggio disponibili
     * @return lista dei file di salvataggio
     */
    public static List<File> getAvailableSaves() {
        List<File> saveFiles = new ArrayList<>();
        File saveDir = new File(SAVE_DIRECTORY);
        
        if (saveDir.exists() && saveDir.isDirectory()) {
            File[] files = saveDir.listFiles((dir, name) -> name.endsWith(SAVE_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    saveFiles.add(file);
                }
            }
        }
        
        return saveFiles;
    }
    
    /**
     * Elimina un file di salvataggio
     * @param saveFile file da eliminare
     * @return true se l'eliminazione è riuscita
     */
    public static boolean deleteSave(File saveFile) {
        try {
            return saveFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Sanitizza il nome del file rimuovendo caratteri non validi
     * @param filename nome del file da sanitizzare
     * @return nome del file sanitizzato
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "save_" + System.currentTimeMillis();
        }
        
        // Rimuove caratteri non validi per i nomi dei file
        return filename.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_").trim();
    }
    
    /**
     * Verifica se un file di salvataggio è valido
     * @param saveFile file da verificare
     * @return true se il file è un salvataggio valido
     */
    public static boolean isValidSaveFile(File saveFile) {
        try {
            GameSaveData saveData = loadGame(saveFile);
            return saveData != null && saveData.getGameVersion() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Ottiene informazioni rapide su un salvataggio senza caricarlo completamente
     * @param saveFile file di salvataggio
     * @return informazioni base del salvataggio
     */
    public static SaveFileInfo getSaveFileInfo(File saveFile) {
        try {
            GameSaveData saveData = loadGame(saveFile);
            return new SaveFileInfo(
                saveData.getSaveName(),
                saveData.getSaveDate(),
                saveData.getFormattedPlayTime(),
                saveData.getCurrentRoomId(),
                saveFile.length()
            );
        } catch (Exception e) {
            return new SaveFileInfo(
                saveFile.getName().replace(SAVE_EXTENSION, ""),
                new Date(saveFile.lastModified()),
                "N/A",
                -1,
                saveFile.length()
            );
        }
    }
    
    /**
     * Classe per informazioni rapide sui file di salvataggio
     */
    public static class SaveFileInfo {
        private final String saveName;
        private final Date saveDate;
        private final String playTime;
        private final int currentRoomId;
        private final long fileSize;
        
        public SaveFileInfo(String saveName, Date saveDate, String playTime, int currentRoomId, long fileSize) {
            this.saveName = saveName;
            this.saveDate = saveDate;
            this.playTime = playTime;
            this.currentRoomId = currentRoomId;
            this.fileSize = fileSize;
        }
        
        public String getSaveName() { return saveName; }
        public Date getSaveDate() { return saveDate; }
        public String getPlayTime() { return playTime; }
        public int getCurrentRoomId() { return currentRoomId; }
        public long getFileSize() { return fileSize; }
        
        @Override
        public String toString() {
            return String.format("%s - %s (Tempo: %s)", saveName, saveDate, playTime);
        }
    }
}