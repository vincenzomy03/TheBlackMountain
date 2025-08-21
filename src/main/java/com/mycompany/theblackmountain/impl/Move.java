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
import com.mycompany.theblackmountain.gui.OutroScreen;

import javax.swing.*;

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

        // Se non è un comando di movimento, non gestire
        if (!isMovementCommand) {
            return "";
        }

        // NUOVO: Gestione principessa liberata
        if (description instanceof TBMGame) {
            TBMGame game = (TBMGame) description;
            
            // Se la principessa è liberata, permetti solo movimento verso EST dalla stanza 7
            if (game.isPrincessLiberated() && game.getCurrentRoom().getId() == 7) {
                if (commandType == CommandType.NORD || commandType == CommandType.SOUTH || 
                    commandType == CommandType.WEST) {
                    return "\"Non possiamo tornare indietro ora!\" dice la principessa. \"L'uscita e' a EST, dobbiamo scappare!\"";
                }
                
                // Se va a EST dalla stanza 7, attiva la sequenza finale
                if (commandType == CommandType.EAST) {
                    return handleFinalExit(game);
                }
            }
        }

        // Controllo più rigoroso per bloccare movimento con nemici vivi
        if (hasLivingEnemies(description)) {
            return "ATTENZIONE! Non puoi lasciare questa stanza! Ci sono ancora nemici da sconfiggere.\nUsa il comando 'combatti' per iniziare la battaglia.";
        }

        // Se siamo in combattimento e il comando è di movimento, blocca
        if (combatSystem.isInCombat() && isMovementCommand) {
            return "Non puoi muoverti durante un combattimento! Devi prima sconfiggere i nemici.";
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
                    return "Da quella parte non si puo' andare!";
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
                    return "Da quella parte non si puo' andare!";
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
                    return "Da quella parte non si puo' andare!";
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
                    return "Da quella parte non si puo' andare!";
                }
                break;
            default:
                return "";
        }

        if (!direction.isEmpty()) {
            // Descrivi la nuova stanza
            result += "Ti dirigi a " + getDirectionName(direction) + ".\n\n";
            result += description.getCurrentRoom().getName() + "\n";
            result += description.getCurrentRoom().getDescription();

            // Informa il giocatore se ci sono nemici nella nuova stanza
            if (hasLivingEnemies(description)) {
                result += "\n" + getEnemyWarning(description);
            }
        }

        return result;
    }

    /**
     * Gestisce l'uscita finale del gioco
     */
    private String handleFinalExit(TBMGame game) {
            
            // Programma l'outro dopo 2 secondi
            Timer outroTimer = new Timer(2000, e -> {
                SwingUtilities.invokeLater(() -> {
                    // Avvia l'outro
                    OutroScreen.showOutro(null, () -> {
                        // Dopo l'outro, aspetta 3 secondi e chiudi il gioco
                        Timer exitTimer = new Timer(3000, exitEvent -> {
                            System.out.println("Gioco completato - Uscita...");
                            System.exit(0);
                        });
                        exitTimer.setRepeats(false);
                        exitTimer.start();
                    });
                });
            });
            outroTimer.setRepeats(false);
            outroTimer.start();
        
        
        return "Tu e la principessa correte verso la liberta'!\n\n"
               + "Il sole del mattino illumina i vostri volti mentre lasciate per sempre\n"
               + "la Montagna Nera alle vostre spalle...\n\n"
               + "MISSIONE COMPLETATA";
    }

    /**
     * Converte la direzione in inglese al nome italiano
     */
    private String getDirectionName(String direction) {
        switch (direction.toLowerCase()) {
            case "north":
                return "nord";
            case "south":
                return "sud";
            case "east":
                return "est";
            case "west":
                return "ovest";
            default:
                return direction;
        }
    }

    /**
     * Reset del sistema porte per nuova partita
     */
    public void resetForNewGame() {
        if (doorSystem != null) {
            doorSystem.resetAllDoors();
            System.out.println("Move Observer: Sistema porte resettato");
        }
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
        warning.append("\nATTENZIONE! ");

        if (enemyCount == 1) {
            GameCharacter enemy = description.getCurrentRoom().getEnemies().stream()
                    .filter(GameCharacter::isAlive)
                    .findFirst().orElse(null);
            if (enemy != null) {
                warning.append("C'e' un ").append(enemy.getName()).append(" in questa stanza!");
            }
        } else {
            warning.append("Ci sono ").append(enemyCount).append(" nemici in questa stanza!");
        }

        warning.append("\nUsa il comando 'combatti' per iniziare la battaglia.");

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