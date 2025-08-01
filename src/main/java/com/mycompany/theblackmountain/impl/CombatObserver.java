/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;

/**
 * Observer che gestisce i comandi di combattimento
 * @author vince
 */
public class CombatObserver extends GameObserver {
    
    private CombatSystem combatSystem;
    
    public CombatObserver() {
        // Il combat system verrà inizializzato quando necessario
    }
    
    public void setCombatSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }

    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        // Inizializza il combat system se non è stato fatto
        if (combatSystem == null) {
            combatSystem = new CombatSystem(description);
        }
        
        // Se non siamo in combattimento, non fare nulla
        if (!combatSystem.isInCombat()) {
            return "";
        }
        
        // Gestisci comandi di combattimento
        if (parserOutput.getCommand() != null) {
            String commandName = parserOutput.getCommand().getName().toLowerCase();
            CommandType commandType = parserOutput.getCommand().getType();
            
            // Comando "attacca"
            if (commandName.equals("attacca") || commandName.equals("attack") || 
                commandName.equals("attacco") || commandName.equals("colpisci")) {
                return combatSystem.processCombatAction(parserOutput);
            }
            
            // Comandi "usa [oggetto]"
            if (commandType == CommandType.USE) {
                // Controlla se sta usando una pozione di cura
                if (parserOutput.getInvObject() != null) {
                    int objId = parserOutput.getInvObject().getId();
                    
                    // Pozioni di cura
                    if (objId == 2 || objId == 5) { 
                        return combatSystem.processCombatAction(parserOutput);
                    }
                    
                    // Armi (spada, arco magico, bastone)
                    if (objId == 12 || objId == 7 || objId == 6) { 
                        return combatSystem.processCombatAction(parserOutput);
                    }
                }
                
                // Gestione comandi testuali per cura
                if (commandName.contains("cura") || commandName.contains("heal") ||
                    commandName.equals("usa cura")) {
                    return combatSystem.processCombatAction(parserOutput);
                }
                
                // Gestione comandi testuali per armi
                if (commandName.contains("spada") || commandName.contains("arco") || 
                    commandName.contains("arma") || commandName.contains("bastone")) {
                    return combatSystem.processCombatAction(parserOutput);
                }
                
                // Se è un comando USE ma non riconosciuto, prova comunque il combattimento
                return combatSystem.processCombatAction(parserOutput);
            }
            
            // Se siamo in combattimento ma il comando non è valido per il combattimento
            // Lascia che altri observer gestiscano comandi come "inventario", "osserva", etc.
            if (commandType == CommandType.INVENTORY || commandType == CommandType.LOOK_AT) {
                return ""; // Permetti questi comandi anche in combattimento
            }
            
            // Blocca movimenti in combattimento
            if (commandType == CommandType.NORD || commandType == CommandType.SOUTH || 
                commandType == CommandType.EAST || commandType == CommandType.WEST) {
                return "Non puoi muoverti durante un combattimento! Devi prima sconfiggere i nemici.";
            }
            
            // Per altri comandi durante il combattimento, non gestire (lascia agli altri observer)
            return "";
        }
        
        return "";
    }
    
    /**
     * Restituisce il combat system per accesso esterno
     * @return CombatSystem
     */
    public CombatSystem getCombatSystem() {
        return combatSystem;
    }
}