package com.mycompany.theblackmountain.save;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.Room;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Sistema di salvataggio semplificato che usa Properties
 * @author vince
 */
public class SaveManager {
    
    public static final String SAVE_DIRECTORY = "src/main/saves";
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
     */
    public static boolean saveGame(GameDescription gameDescription, CombatSystem combatSystem,
                               String saveName, long playTimeMillis) {
    try {
        // Percorso completo per salvataggi
        String saveDirPath = "src/main/saves";
        File saveDir = new File(saveDirPath);
        if (!saveDir.exists()) {
            boolean created = saveDir.mkdirs();
            System.out.println("Directory 'saves' creata? " + created);
        } else {
            System.out.println("Directory 'saves' già esistente");
        }

        Properties saveData = new Properties();

        // Dati generali
        saveData.setProperty("save.name", saveName != null ? saveName : "Salvataggio");
        saveData.setProperty("save.date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        saveData.setProperty("save.version", "1.0");
        saveData.setProperty("play.time", String.valueOf(playTimeMillis));

        // Stanza attuale
        if (gameDescription.getCurrentRoom() != null) {
            saveData.setProperty("player.room", String.valueOf(gameDescription.getCurrentRoom().getId()));
        }

        // Inventario
        StringBuilder inventoryStr = new StringBuilder();
        for (GameObjects obj : gameDescription.getInventory()) {
            if (obj != null && obj.getName() != null) {
                if (inventoryStr.length() > 0) inventoryStr.append(";");
                inventoryStr.append(obj.getId()).append(",").append(obj.getName());
            }
        }
        saveData.setProperty("player.inventory", inventoryStr.toString());

        // HP giocatore
        if (combatSystem != null && combatSystem.getPlayer() != null) {
            saveData.setProperty("player.hp", String.valueOf(combatSystem.getPlayer().getHp()));
            saveData.setProperty("player.maxhp", String.valueOf(combatSystem.getPlayer().getMaxHp()));
        }

        // Stato combattimento
        if (combatSystem != null) {
            saveData.setProperty("combat.active", String.valueOf(combatSystem.isInCombat()));
        }

        // Stato casse
        StringBuilder chestsStr = new StringBuilder();
        for (Room room : gameDescription.getRooms()) {
            if (room.getObjects() == null) continue;
            for (GameObjects obj : room.getObjects()) {
                if (obj instanceof ContainerObj) {
                    if (chestsStr.length() > 0) chestsStr.append(";");
                    chestsStr.append(obj.getId()).append(",").append(obj.isOpen());
                }
            }
        }
        saveData.setProperty("world.chests", chestsStr.toString());

        // Descrizioni stanze
        for (Room room : gameDescription.getRooms()) {
            if (room.getDescription() != null) {
                saveData.setProperty("room." + room.getId() + ".description", room.getDescription());
            }
            if (room.getLook() != null) {
                saveData.setProperty("room." + room.getId() + ".look", room.getLook());
            }
        }

        // Salvataggio su file
        String filename = sanitizeFilename(saveName) + ".dat";
        File saveFile = new File(saveDir, filename);
        System.out.println("Salvataggio in corso in: " + saveFile.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            saveData.store(fos, "The Black Mountain - Save Game");
        }

        System.out.println("Gioco salvato con successo.");
        return true;

    } catch (Exception e) {
        System.err.println("Errore nel salvataggio: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

    
    /**
     * Carica un gioco salvato
     */
    public static String loadGame(File saveFile) throws Exception {
        Properties saveData = new Properties();
        
        try (FileInputStream fis = new FileInputStream(saveFile)) {
            saveData.load(fis);
        }
        
        System.out.println("Gioco caricato da: " + saveFile.getAbsolutePath());
        
        // Converte Properties in stringa per la compatibilità
        StringBuilder result = new StringBuilder();
        for (String key : saveData.stringPropertyNames()) {
            result.append(key).append("=").append(saveData.getProperty(key)).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Applica i dati salvati al gioco
     */
    public static void applyLoadedData(TBMGame gameDescription, String saveDataStr, CombatSystem combatSystem) {
        try {
            System.out.println("Applicando dati salvati...");
            
            // Parsing dei dati salvati
            Properties saveData = new Properties();
            try (StringReader reader = new StringReader(saveDataStr)) {
                saveData.load(reader);
            }
            
            // Ripristina la stanza corrente
            String roomIdStr = saveData.getProperty("player.room");
            if (roomIdStr != null) {
                int roomId = Integer.parseInt(roomIdStr);
                Room currentRoom = findRoomById(gameDescription, roomId);
                if (currentRoom != null) {
                    gameDescription.setCurrentRoom(currentRoom);
                    System.out.println("Stanza corrente ripristinata: " + currentRoom.getName());
                }
            }
            
            // Ripristina l'inventario
            String inventoryStr = saveData.getProperty("player.inventory");
            if (inventoryStr != null && !inventoryStr.isEmpty()) {
                gameDescription.getInventory().clear();
                String[] items = inventoryStr.split(";");
                for (String item : items) {
                    if (!item.trim().isEmpty()) {
                        String[] parts = item.split(",");
                        if (parts.length >= 2) {
                            int objId = Integer.parseInt(parts[0]);
                            String objName = parts[1];
                            
                            // Trova l'oggetto originale nel gioco e aggiungilo all'inventario
                            GameObjects obj = findObjectInGame(gameDescription, objId, objName);
                            if (obj != null) {
                                gameDescription.getInventory().add(obj);
                            }
                        }
                    }
                }
                System.out.println("Inventario ripristinato con " + gameDescription.getInventory().size() + " oggetti");
            }
            
            // Ripristina HP del giocatore
            String hpStr = saveData.getProperty("player.hp");
            String maxHpStr = saveData.getProperty("player.maxhp");
            if (hpStr != null && maxHpStr != null && combatSystem != null && combatSystem.getPlayer() != null) {
                combatSystem.getPlayer().setHp(Integer.parseInt(hpStr));
                combatSystem.getPlayer().setMaxHp(Integer.parseInt(maxHpStr));
                System.out.println("HP giocatore ripristinato: " + hpStr + "/" + maxHpStr);
            }
            
            // Ripristina stato delle casse
            String chestsStr = saveData.getProperty("world.chests");
            if (chestsStr != null && !chestsStr.isEmpty()) {
                String[] chests = chestsStr.split(";");
                for (String chest : chests) {
                    if (!chest.trim().isEmpty()) {
                        String[] parts = chest.split(",");
                        if (parts.length >= 2) {
                            int chestId = Integer.parseInt(parts[0]);
                            boolean isOpen = Boolean.parseBoolean(parts[1]);
                            
                            // Trova e aggiorna la cassa
                            ContainerObj container = findChestById(gameDescription, chestId);
                            if (container != null) {
                                container.setOpen(isOpen);
                            }
                        }
                    }
                }
                System.out.println("Stato delle casse ripristinato");
            }
            
            // Ripristina descrizioni delle stanze
            for (Room room : gameDescription.getRooms()) {
                String savedDesc = saveData.getProperty("room." + room.getId() + ".description");
                if (savedDesc != null) {
                    room.setDescription(savedDesc);
                }
                String savedLook = saveData.getProperty("room." + room.getId() + ".look");
                if (savedLook != null) {
                    room.setLook(savedLook);
                }
            }
            
            System.out.println("Dati salvati applicati con successo!");
            
        } catch (Exception e) {
            System.err.println("Errore nell'applicazione dei dati salvati: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore nell'applicazione dei dati salvati: " + e.getMessage());
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
     * Trova una cassa per ID
     */
    private static ContainerObj findChestById(GameDescription gameDescription, int chestId) {
        for (Room room : gameDescription.getRooms()) {
            for (GameObjects obj : room.getObjects()) {
                if (obj instanceof ContainerObj && obj.getId() == chestId) {
                    return (ContainerObj) obj;
                }
            }
        }
        return null;
    }
    
    /**
     * Trova un oggetto nel gioco (per ricostruire l'inventario)
     */
    private static GameObjects findObjectInGame(GameDescription gameDescription, int objId, String objName) {
        // Cerca in tutte le stanze
        for (Room room : gameDescription.getRooms()) {
            for (GameObjects obj : room.getObjects()) {
                if (obj.getId() == objId || obj.getName().equals(objName)) {
                    // Crea una copia dell'oggetto
                    GameObjects copy = new GameObjects(obj.getId(), obj.getName(), obj.getDescription());
                    copy.setAlias(obj.getAlias());
                    copy.setPickupable(obj.isPickupable());
                    return copy;
                }
                
                // Cerca anche dentro i container
                if (obj instanceof ContainerObj) {
                    ContainerObj container = (ContainerObj) obj;
                    for (Object item : container.getList()) {
                        if (item instanceof GameObjects) {
                            GameObjects containerObj = (GameObjects) item;
                            if (containerObj.getId() == objId || containerObj.getName().equals(objName)) {
                                GameObjects copy = new GameObjects(containerObj.getId(), containerObj.getName(), containerObj.getDescription());
                                copy.setAlias(containerObj.getAlias());
                                copy.setPickupable(containerObj.isPickupable());
                                return copy;
                            }
                        }
                    }
                }
            }
        }
        
        // Se non trovato, crea un oggetto base
        return new GameObjects(objId, objName, "Oggetto caricato dal salvataggio");
    }
    
    /**
     * Sanitizza il nome del file
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "save_" + System.currentTimeMillis();
        }
        return filename.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_").trim();
    }
    
    /**
     * Ottiene informazioni su un file di salvataggio
     */
    public static String getSaveInfo(File saveFile) {
        try {
            Properties saveData = new Properties();
            try (FileInputStream fis = new FileInputStream(saveFile)) {
                saveData.load(fis);
            }
            
            String saveName = saveData.getProperty("save.name", "Salvataggio");
            String saveDate = saveData.getProperty("save.date", "Data sconosciuta");
            String playTime = saveData.getProperty("play.time", "0");
            
            // Converti millisecondi in formato leggibile
            long millis = Long.parseLong(playTime);
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            String formattedTime = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
            
            return String.format("%s - %s (Tempo: %s)", saveName, saveDate, formattedTime);
            
        } catch (Exception e) {
            return saveFile.getName().replace(SAVE_EXTENSION, "") + " - Errore nel caricamento info";
        }
    }
}