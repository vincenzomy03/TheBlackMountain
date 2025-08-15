package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;

/**
 * Observer per gestire il sistema di combattimento
 */
public class CombatObserver extends GameObserver {
    
    private CombatSystem combatSystem;
    
    public void setCombatSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }
    
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        if (combatSystem == null) {
            combatSystem = new CombatSystem(description);
        }
        
        CommandType commandType = parserOutput.getCommand().getType();
        String commandName = parserOutput.getCommand().getName().toLowerCase();
        
        // DEBUG
        System.out.println("🔍 DEBUG CombatObserver:");
        System.out.println("  - Comando: " + commandName + " (Type: " + commandType + ")");
        System.out.println("  - In combattimento: " + combatSystem.isInCombat());
        
        StringBuilder result = new StringBuilder();
        
        // *** INIZIO COMBATTIMENTO ***
        if (commandType == CommandType.FIGHT || 
            commandName.equals("combatti") || 
            commandName.equals("combattimento") ||
            commandName.equals("battaglia")) {
            
            if (combatSystem.isInCombat()) {
                return "Sei già in combattimento!";
            }
            
            String startResult = combatSystem.startCombat();
            if (startResult.isEmpty()) {
                return "Non ci sono nemici da combattere in questa stanza.";
            }
            return startResult;
        }
        
        // *** AZIONI DURANTE IL COMBATTIMENTO ***
        if (combatSystem.isInCombat()) {
            // Attacchi diretti
            if (commandType == CommandType.ATTACK || 
                commandName.equals("attacca") || 
                commandName.equals("attacco") || 
                commandName.equals("colpisci")) {
                return combatSystem.processCombatAction(parserOutput);
            }
            
            // Uso di oggetti/armi durante il combattimento
            if (commandType == CommandType.USE) {
                // Se è un uso di armi o pozioni, gestiscilo nel combattimento
                if (commandName.contains("spada") || 
                    commandName.contains("arco") || 
                    commandName.contains("bastone") ||
                    commandName.contains("pozione") ||
                    commandName.contains("cura") ||
                    (parserOutput.getInvObject() != null && 
                     (isWeapon(parserOutput.getInvObject()) || isPotion(parserOutput.getInvObject())))) {
                    return combatSystem.processCombatAction(parserOutput);
                }
                // Altri usi vengono lasciati al normale Use observer
            }
            
            // Controlla se il combattimento è finito
            combatSystem.checkCombatEnd();
        }
        
        return result.toString();
    }
    
    /**
     * Controlla se un oggetto è un'arma
     */
    private boolean isWeapon(com.mycompany.theblackmountain.type.GameObjects obj) {
        return obj.getId() == 6 || obj.getId() == 7 || obj.getId() == 12; // bastone, arco, spada
    }
    
    /**
     * Controlla se un oggetto è una pozione
     */
    private boolean isPotion(com.mycompany.theblackmountain.type.GameObjects obj) {
        return obj.getId() == 2 || obj.getId() == 5; // pozioni di cura
    }
}