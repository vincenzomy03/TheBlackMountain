/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.systems.DoorSystem;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.type.GameCharacter;

/**
 *
 * @author vince
 */
public class Move extends GameObserver {

    private CombatSystem combatSystem;
    private DoorSystem doorSystem;

    public Move() {
        this.doorSystem = new DoorSystem();
    }

    public void setCombatSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        if (combatSystem == null) {
            combatSystem = new CombatSystem(description);
        }

        // Controlla se il comando è di movimento
        CommandType commandType = parserOutput.getCommand().getType();
        boolean isMovementCommand = (commandType == CommandType.NORD
                || commandType == CommandType.SOUTH
                || commandType == CommandType.EAST
                || commandType == CommandType.WEST);

        // Se siamo in combattimento e il comando è di movimento, blocca
        if (combatSystem.isInCombat() && isMovementCommand) {
            return "Non puoi muoverti durante un combattimento! Devi prima sconfiggere i nemici.";
        }

        // Se non è un comando di movimento, non gestire
        if (!isMovementCommand) {
            return "";
        }

        String direction = "";
        String result = "";

        switch (commandType) {
            case NORD:
                direction = "north";
                if (description.getCurrentRoom().getNorth() != null) {
                    // Controlla se la porta è bloccata
                    String doorResult = doorSystem.attemptDoorPassage(description, description.getCurrentRoom().getId(), "north");
                    if (doorResult.contains("chiave")) {
                        return doorResult;
                    } else if (!doorResult.isEmpty()) {
                        result = doorResult + "\n";
                    }

                    description.setCurrentRoom(description.getCurrentRoom().getNorth());
                } else {
                    return "Da quella parte non si può andare!";
                }
                break;
            case SOUTH:
                direction = "south";
                if (description.getCurrentRoom().getSouth() != null) {
                    // Controlla se la porta è bloccata
                    String doorResult = doorSystem.attemptDoorPassage(description, description.getCurrentRoom().getId(), "south");
                    if (doorResult.contains("chiave")) {
                        return doorResult;
                    } else if (!doorResult.isEmpty()) {
                        result = doorResult + "\n";
                    }

                    description.setCurrentRoom(description.getCurrentRoom().getSouth());
                } else {
                    return "Da quella parte non si può andare!";
                }
                break;
            case EAST:
                direction = "east";
                if (description.getCurrentRoom().getEast() != null) {
                    // Controlla se la porta è bloccata
                    String doorResult = doorSystem.attemptDoorPassage(description, description.getCurrentRoom().getId(), "east");
                    if (doorResult.contains("chiave")) {
                        return doorResult;
                    } else if (!doorResult.isEmpty()) {
                        result = doorResult + "\n";
                    }

                    description.setCurrentRoom(description.getCurrentRoom().getEast());
                } else {
                    return "Da quella parte non si può andare!";
                }
                break;
            case WEST:
                direction = "west";
                if (description.getCurrentRoom().getWest() != null) {
                    // Controlla se la porta è bloccata
                    String doorResult = doorSystem.attemptDoorPassage(description, description.getCurrentRoom().getId(), "west");
                    if (doorResult.contains("chiave")) {
                        return doorResult;
                    } else if (!doorResult.isEmpty()) {
                        result = doorResult + "\n";
                    }

                    description.setCurrentRoom(description.getCurrentRoom().getWest());
                } else {
                    return "Da quella parte non si può andare!";
                }
                break;
            default:
                return "";
        }

        // Informa il giocatore se ci sono nemici nella stanza
        if (!direction.isEmpty() && hasLivingEnemies(description)) {
            result += getEnemyWarning(description);
        }

        return result;
    }

    /**
     * Controlla se ci sono nemici vivi nella stanza corrente
     *
     * @param description stato del gioco
     * @return true se ci sono nemici vivi
     */
    private boolean hasLivingEnemies(GameDescription description) {
        return description.getCurrentRoom().getEnemies().stream()
                .anyMatch(GameCharacter::isAlive);
    }

    /**
     * Restituisce un messaggio di avviso sui nemici presenti
     *
     * @param description stato del gioco
     * @return messaggio di avviso
     */
    private String getEnemyWarning(GameDescription description) {
        long enemyCount = description.getCurrentRoom().getEnemies().stream()
                .filter(GameCharacter::isAlive)
                .count();

        if (enemyCount == 0) {
            return "";
        }

        StringBuilder warning = new StringBuilder();
        warning.append("\n⚠️ ATTENZIONE! ");

        if (enemyCount == 1) {
            GameCharacter enemy = description.getCurrentRoom().getEnemies().stream()
                    .filter(GameCharacter::isAlive)
                    .findFirst().orElse(null);
            if (enemy != null) {
                warning.append("C'è un ").append(enemy.getName()).append(" in questa stanza!");
            }
        } else {
            warning.append("Ci sono ").append(enemyCount).append(" nemici in questa stanza!");
        }

        warning.append("\n💡 Usa il comando 'combatti' per iniziare la battaglia.");

        return warning.toString();
    }

    /**
     * Restituisce il sistema delle porte per accesso esterno
     *
     * @return DoorSystem
     */
    public DoorSystem getDoorSystem() {
        return doorSystem;
    }
}
