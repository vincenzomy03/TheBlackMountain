/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain;

import com.mycompany.theblackmountain.type.GameObjects;
import java.util.List;

/**
 * Classe di utilità per operazioni comuni nel gioco
 * @author vince
 */
public class GameUtils {
    
    /**
     * Cerca un oggetto nell'inventario per ID
     * @param inventory lista degli oggetti nell'inventario
     * @param objectId ID dell'oggetto da cercare
     * @return l'oggetto se trovato, null altrimenti
     */
    public static GameObjects getObjectFromInventory(List<GameObjects> inventory, int objectId) {
        for (GameObjects obj : inventory) {
            if (obj.getId() == objectId) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Cerca un oggetto nell'inventario per nome
     * @param inventory lista degli oggetti nell'inventario
     * @param objectName nome dell'oggetto da cercare
     * @return l'oggetto se trovato, null altrimenti
     */
    public static GameObjects getObjectFromInventoryByName(List<GameObjects> inventory, String objectName) {
        for (GameObjects obj : inventory) {
            if (obj.getName().equalsIgnoreCase(objectName)) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Verifica se l'inventario contiene un oggetto con l'ID specificato
     * @param inventory lista degli oggetti nell'inventario
     * @param objectId ID dell'oggetto da verificare
     * @return true se l'oggetto è presente, false altrimenti
     */
    public static boolean inventoryContains(List<GameObjects> inventory, int objectId) {
        return getObjectFromInventory(inventory, objectId) != null;
    }
    
    /**
     * Conta il numero di oggetti con un determinato ID nell'inventario
     * @param inventory lista degli oggetti nell'inventario
     * @param objectId ID dell'oggetto da contare
     * @return numero di oggetti con quell'ID
     */
    public static int countObjectsInInventory(List<GameObjects> inventory, int objectId) {
        int count = 0;
        for (GameObjects obj : inventory) {
            if (obj.getId() == objectId) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Rimuove la prima occorrenza di un oggetto con l'ID specificato dall'inventario
     * @param inventory lista degli oggetti nell'inventario
     * @param objectId ID dell'oggetto da rimuovere
     * @return true se l'oggetto è stato rimosso, false se non trovato
     */
    public static boolean removeObjectFromInventory(List<GameObjects> inventory, int objectId) {
        GameObjects obj = getObjectFromInventory(inventory, objectId);
        if (obj != null) {
            inventory.remove(obj);
            return true;
        }
        return false;
    }
    
    /**
     * Cerca un oggetto in una stanza per ID
     * @param roomObjects lista degli oggetti nella stanza
     * @param objectId ID dell'oggetto da cercare
     * @return l'oggetto se trovato, null altrimenti
     */
    public static GameObjects getObjectFromRoom(List<GameObjects> roomObjects, int objectId) {
        for (GameObjects obj : roomObjects) {
            if (obj.getId() == objectId) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Formatta un messaggio di salute per un personaggio
     * @param currentHp HP attuali
     * @param maxHp HP massimi
     * @return stringa formattata della salute
     */
    public static String formatHealth(int currentHp, int maxHp) {
        return currentHp + "/" + maxHp + " HP";
    }
    
    /**
     * Calcola la percentuale di salute
     * @param currentHp HP attuali
     * @param maxHp HP massimi
     * @return percentuale di salute (0-100)
     */
    public static int getHealthPercentage(int currentHp, int maxHp) {
        if (maxHp <= 0) return 0;
        return (currentHp * 100) / maxHp;
    }
    
    /**
     * Restituisce una descrizione dello stato di salute
     * @param currentHp HP attuali
     * @param maxHp HP massimi
     * @return descrizione dello stato
     */
    public static String getHealthStatus(int currentHp, int maxHp) {
        int percentage = getHealthPercentage(currentHp, maxHp);
        
        if (percentage == 0) {
            return "Morto";
        } else if (percentage <= 25) {
            return "Gravemente ferito";
        } else if (percentage <= 50) {
            return "Ferito";
        } else if (percentage <= 75) {
            return "Leggermente ferito";
        } else if (percentage < 100) {
            return "In buona salute";
        } else {
            return "In perfetta salute";
        }
    }
    
    /**
     * Genera un numero casuale tra min e max (inclusi)
     * @param min valore minimo
     * @param max valore massimo
     * @return numero casuale
     */
    public static int randomBetween(int min, int max) {
        return min + (int)(Math.random() * (max - min + 1));
    }
    
    /**
     * Verifica se un attacco va a segno basato sulla precisione
     * @param accuracy precisione (0-100)
     * @return true se l'attacco va a segno
     */
    public static boolean isHit(int accuracy) {
        return Math.random() * 100 < accuracy;
    }
}