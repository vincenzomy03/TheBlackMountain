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
 * Observer che gestisce tutti i comandi di combattimento
 *
 * @author vince
 */
public class CombatObserver extends GameObserver {

    private CombatSystem combatSystem;

    public CombatObserver() {
        // Costruttore vuoto, il combatSystem viene impostato dopo
    }

    /**
     * Imposta il sistema di combattimento
     *
     * @param combatSystem
     */
    public void setCombatSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }

    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        if (combatSystem == null) {
            return ""; // Non possiamo gestire comandi di combattimento senza il sistema
        }

        StringBuilder msg = new StringBuilder();
        CommandType commandType = parserOutput.getCommand().getType();
        String commandName = parserOutput.getCommand().getName().toLowerCase();

        if (commandType == CommandType.CREATE) {
            return ""; // Lascia passare a Use.java
        }

        // Gestione comando COMBATTI/FIGHT
        if (commandType == CommandType.FIGHT
                || commandName.contains("combatti")
                || commandName.contains("battaglia")
                || commandName.contains("combattimento")) {

            // Verifica se ci sono nemici vivi nella stanza
            boolean hasEnemies = description.getCurrentRoom().getEnemies().stream()
                    .anyMatch(enemy -> enemy.isAlive());

            if (!hasEnemies) {
                msg.append("Non ci sono nemici da combattere in questa stanza.");
            } else if (combatSystem.isInCombat()) {
                msg.append("Sei già in combattimento! Usa 'attacca' per combattere.");
            } else {
                // Inizia il combattimento
                String combatStart = combatSystem.startCombat();
                if (!combatStart.isEmpty()) {
                    msg.append(combatStart);
                } else {
                    msg.append("Il combattimento è iniziato!");
                }
            }
        } // Gestione comando ATTACCA/ATTACK
        else if (commandType == CommandType.ATTACK
                || commandName.contains("attacca")
                || commandName.contains("attacco")
                || commandName.contains("colpisci")) {

            if (!combatSystem.isInCombat()) {
                // Se non siamo in combattimento, il giocatore deve prima iniziarlo
                msg.append("Non sei in combattimento! Usa prima 'combatti' per iniziare la battaglia.");
            } else {
                // Siamo già in combattimento, processa l'attacco
                String combatResult = combatSystem.processCombatAction(parserOutput);
                msg.append(combatResult);

                // *** NUOVO: Controlla se il combattimento è finito ***
                if (!combatSystem.isInCombat()) {
                    msg.append("\nCombattimento terminato!");
                }
            }
        } // Gestione altri comandi durante il combattimento
        else if (combatSystem.isInCombat()) {
            // Durante il combattimento, alcuni comandi sono permessi, altri no
            if (commandType == CommandType.USE) {
                // Durante il combattimento, l'uso di oggetti viene gestito dal CombatSystem
                msg.append(combatSystem.processCombatAction(parserOutput));
            } else if (commandType == CommandType.INVENTORY) {
                // Permetti di controllare l'inventario durante il combattimento
                // Questo viene gestito dall'observer OpenInventory.java
                return "";
            } else if (commandType == CommandType.LOOK_AT) {
                // Permetti di guardare durante il combattimento
                // Questo viene gestito dall'observer LookAt.java
                return "";
            } else if (isMovementCommand(commandType)) {
                // Il movimento è bloccato durante il combattimento (gestito in Move.java)
                return "";
            } else if (commandType == CommandType.OPEN || commandType == CommandType.PICK_UP) {
                // Blocca apertura oggetti e raccolta durante il combattimento
                msg.append("Non puoi fare questo durante il combattimento! Concentrati sulla battaglia!");
            }
        }

        // Se non è un comando di combattimento e non siamo in combattimento, lascia passare
        return msg.toString();
    }

    /**
     * Verifica se un comando è di movimento
     *
     * @param commandType
     * @return true se è un comando di movimento
     */
    private boolean isMovementCommand(CommandType commandType) {
        return commandType == CommandType.NORD
                || commandType == CommandType.SOUTH
                || commandType == CommandType.EAST
                || commandType == CommandType.WEST;
    }

    /**
     * Restituisce il sistema di combattimento per accesso esterno
     *
     * @return CombatSystem
     */
    public CombatSystem getCombatSystem() {
        return combatSystem;
    }
}
