/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.systems;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameUtils;
import com.mycompany.theblackmountain.type.Room;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema per gestire porte chiuse che richiedono chiavi
 * @author vince
 */
public class DoorSystem {
    
    private Map<String, Integer> lockedDoors; // "roomId-direction" -> keyId
    private Map<String, Boolean> unlockedDoors; // "roomId-direction" -> unlocked
    
    public DoorSystem() {
        this.lockedDoors = new HashMap<>();
        this.unlockedDoors = new HashMap<>();
        initializeLockedDoors();
    }
    
    /**
     * Inizializza le porte chiuse del gioco
     */
    private void initializeLockedDoors() {
        // Ingresso -> Stanza del Topo (richiede chiave ingresso)
        lockedDoors.put("0-east", 1);
        
        // Stanza delle Torture -> Sala del Boss (richiede chiave speciale o progressione)
        lockedDoors.put("6-south", 10);
        
        // Sala del Boss -> Uscita (richiede chiave del boss)
        lockedDoors.put("7-west", 11);
    }
    
    /**
     * Controlla se una porta è bloccata
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     * @return true se la porta è bloccata
     */
    public boolean isDoorLocked(int fromRoomId, String direction) {
        String doorKey = fromRoomId + "-" + direction;
        return lockedDoors.containsKey(doorKey) && 
               !unlockedDoors.getOrDefault(doorKey, false);
    }
    
    /**
     * Tenta di attraversare una porta
     * @param gameDescription stato del gioco
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     * @return messaggio risultato
     */
    public String attemptDoorPassage(GameDescription gameDescription, int fromRoomId, String direction) {
        String doorKey = fromRoomId + "-" + direction;
        
        if (!isDoorLocked(fromRoomId, direction)) {
            return ""; // Porta non bloccata
        }
        
        int requiredKeyId = lockedDoors.get(doorKey);
        
        // Controlla se il giocatore ha la chiave
        if (GameUtils.getObjectFromInventory(gameDescription.getInventory(), requiredKeyId) != null) {
            // Sblocca la porta
            unlockedDoors.put(doorKey, true);
            
            String keyName = getKeyName(requiredKeyId);
            return "Hai usato " + keyName + " per aprire la porta!";
        } else {
            String keyName = getKeyName(requiredKeyId);
            return "La porta è chiusa a chiave. Ti serve: " + keyName + ".";
        }
    }
    
    /**
     * Forza l'apertura di una porta (amministratore/debug)
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     */
    public void unlockDoor(int fromRoomId, String direction) {
        String doorKey = fromRoomId + "-" + direction;
        unlockedDoors.put(doorKey, true);
    }
    
    /**
     * Restituisce il nome della chiave richiesta
     * @param keyId ID della chiave
     * @return nome della chiave
     */
    private String getKeyName(int keyId) {
        switch (keyId) {
            case 1:
                return "chiave ingresso";
            case 10:
                return "chiave cella principessa";
            case 11:
                return "chiave del collo del boss";
            default:
                return "chiave sconosciuta";
        }
    }
    
    /**
     * Restituisce lo stato di tutte le porte
     * @return mappa con stato porte
     */
    public Map<String, Boolean> getAllDoorsStatus() {
        Map<String, Boolean> status = new HashMap<>();
        for (String doorKey : lockedDoors.keySet()) {
            status.put(doorKey, !isDoorLocked(Integer.parseInt(doorKey.split("-")[0]), doorKey.split("-")[1]));
        }
        return status;
    }
    
    /**
     * Controlla se una porta esiste (è stata definita come bloccabile)
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     * @return true se la porta è definita nel sistema
     */
    public boolean doorExists(int fromRoomId, String direction) {
        String doorKey = fromRoomId + "-" + direction;
        return lockedDoors.containsKey(doorKey);
    }
    
    /**
     * Aggiunge una nuova porta bloccata
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     * @param requiredKeyId ID della chiave richiesta
     */
    public void addLockedDoor(int fromRoomId, String direction, int requiredKeyId) {
        String doorKey = fromRoomId + "-" + direction;
        lockedDoors.put(doorKey, requiredKeyId);
    }
    
    /**
     * Rimuove una porta dal sistema
     * @param fromRoomId stanza di partenza
     * @param direction direzione
     */
    public void removeDoor(int fromRoomId, String direction) {
        String doorKey = fromRoomId + "-" + direction;
        lockedDoors.remove(doorKey);
        unlockedDoors.remove(doorKey);
    }
}