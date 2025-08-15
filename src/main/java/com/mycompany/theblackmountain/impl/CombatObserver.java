/*
 * Observer che gestisce tutti i comandi di combattimento
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;

/**
 * Observer che gestisce tutti i comandi di combattimento
 * @author vince
 */
public class CombatObserver extends GameObserver {

    private CombatSystem combatSystem;

    public CombatObserver() {
        // Costruttore vuoto, il combatSystem viene impostato dopo
    }

    /**
     * Imposta il sistema di combattimento
     */
    public void setCombatSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }

    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        if (combatSystem == null) {
            return ""; // Non possiamo gestire comandi senza il sistema
        }

        StringBuilder msg = new StringBuilder();
        CommandType commandType = parserOutput.getCommand().getType();
        String commandName = parserOutput.getCommand().getName().toLowerCase();

        // *** GESTIONE COMANDO COMBATTI/FIGHT ***
        if (commandType == CommandType.FIGHT
                || commandName.contains("combatti")
                || commandName.contains("battaglia")
                || commandName.contains("combattimento")) {

            return handleFightCommand(description);
        } 
        
        // *** GESTIONE COMANDO ATTACCA/ATTACK ***
        else if (commandType == CommandType.ATTACK
                || commandName.contains("attacca")
                || commandName.contains("attacco")
                || commandName.contains("colpisci")) {

            return handleAttackCommand(parserOutput);
        } 
        
        // *** GESTIONE USO OGGETTI/ARMI DURANTE IL COMBATTIMENTO ***
        else if (commandType == CommandType.USE && combatSystem.isInCombat()) {
            
            // Verifica se è uso di armi/oggetti da combattimento
            if (isCombatUseCommand(commandName, parserOutput)) {
                String combatResult = combatSystem.processCombatAction(parserOutput);
                msg.append(combatResult);

                // Controlla se il combattimento è finito dopo l'azione
                if (!combatSystem.isInCombat()) {
                    msg.append("\n\n🏆 Combattimento terminato! Tutti i nemici sono stati sconfitti.");
                    msg.append("\n📍 Puoi ora muoverti liberamente o esplorare la stanza.");

                    if (hasOtherRoomsWithEnemies(description)) {
                        msg.append("\n💡 Ricorda: dovrai usare 'combatti' se incontri altri nemici.");
                    }
                }
                return msg.toString();
            }
        }
        
        // *** GESTIONE ALTRI COMANDI DURANTE IL COMBATTIMENTO ***
        else if (combatSystem.isInCombat()) {
            return handleCommandsDuringCombat(commandType, commandName);
        }

        // Se non è un comando di combattimento e non siamo in combattimento, lascia passare
        return "";
    }

    /**
     * Gestisce il comando "combatti"
     */
    private String handleFightCommand(GameDescription description) {
        StringBuilder msg = new StringBuilder();
        
        // Verifica se ci sono nemici vivi nella stanza
        boolean hasEnemies = description.getCurrentRoom().getEnemies().stream()
                .anyMatch(enemy -> enemy.isAlive());

        if (!hasEnemies) {
            msg.append("🏴 Non ci sono nemici da combattere in questa stanza.");
        } else if (combatSystem.isInCombat()) {
            msg.append("⚔️ Sei già in combattimento! Usa 'attacca' per combattere.");
        } else {
            // Inizia il combattimento
            String combatStart = combatSystem.startCombat();
            if (!combatStart.isEmpty()) {
                msg.append(combatStart);
            } else {
                msg.append("⚔️ Il combattimento è iniziato!");
            }
        }
        
        return msg.toString();
    }

    /**
     * Gestisce il comando "attacca"
     */
    private String handleAttackCommand(ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        
        if (!combatSystem.isInCombat()) {
            msg.append("🛡️ Non sei in combattimento! Usa prima 'combatti' per iniziare la battaglia.");
        } else {
            // Siamo già in combattimento, processa l'attacco
            String combatResult = combatSystem.processCombatAction(parserOutput);
            msg.append(combatResult);

            // Controlla esplicitamente se il combattimento è finito
            if (!combatSystem.isInCombat()) {
                msg.append("\n\n🏆 Combattimento terminato! Tutti i nemici sono stati sconfitti.");
                msg.append("\n📍 Puoi ora muoverti liberamente o esplorare la stanza.");
            }
        }
        
        return msg.toString();
    }

    /**
     * Gestisce i comandi durante il combattimento
     */
    private String handleCommandsDuringCombat(CommandType commandType, String commandName) {
        StringBuilder msg = new StringBuilder();
        
        if (commandType == CommandType.INVENTORY) {
            // Permetti di controllare l'inventario durante il combattimento
            return ""; // Lascia gestire all'OpenInventory observer
        } 
        else if (commandType == CommandType.LOOK_AT) {
            // Permetti di guardare durante il combattimento
            return ""; // Lascia gestire al LookAt observer
        } 
        else if (isMovementCommand(commandType)) {
            // Il movimento è bloccato durante il combattimento
            msg.append("🚫 Non puoi muoverti durante un combattimento! Devi prima sconfiggere i nemici.");
        } 
        else if (commandType == CommandType.OPEN || commandType == CommandType.PICK_UP) {
            // Blocca apertura oggetti e raccolta durante il combattimento
            msg.append("⚔️ Non puoi fare questo durante il combattimento! Concentrati sulla battaglia!");
        }
        else if (commandType == CommandType.USE) {
            // Gli altri usi sono gestiti sopra o lasciati passare al Use observer
            return "";
        }
        
        return msg.toString();
    }

    /**
     * Verifica se è un comando USE relativo al combattimento
     */
    private boolean isCombatUseCommand(String commandName, ParserOutput parserOutput) {
        // Controllo sul testo del comando
        if (commandName.contains("spada") || commandName.contains("arco") || 
            commandName.contains("bastone") || commandName.contains("cura") || 
            commandName.contains("pozione")) {
            return true;
        }

        // Controllo sull'oggetto dall'inventario
        if (parserOutput.getInvObject() != null) {
            int objId = parserOutput.getInvObject().getId();
            // Armi: spada(12), arco(7), bastone(6) + pozioni: normale(2), totale(5)
            if (objId == 12 || objId == 7 || objId == 6 || objId == 2 || objId == 5) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se un comando è di movimento
     */
    private boolean isMovementCommand(CommandType commandType) {
        return commandType == CommandType.NORD
                || commandType == CommandType.SOUTH
                || commandType == CommandType.EAST
                || commandType == CommandType.WEST;
    }

    /**
     * Controlla se ci sono altre stanze con nemici vivi
     */
    private boolean hasOtherRoomsWithEnemies(GameDescription description) {
        return description.getRooms().stream()
                .filter(room -> room.getId() != description.getCurrentRoom().getId())
                .anyMatch(room -> room.getEnemies().stream().anyMatch(enemy -> enemy.isAlive()));
    }

    /**
     * Restituisce il sistema di combattimento per accesso esterno
     */
    public CombatSystem getCombatSystem() {
        return combatSystem;
    }
}