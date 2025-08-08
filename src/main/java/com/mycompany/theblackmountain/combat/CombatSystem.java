/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.combat;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameUtils;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.CharacterType;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.Weapon;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema di combattimento del gioco
 * @author vince
 */
public class CombatSystem {
    
    private GameDescription gameDescription;
    private boolean inCombat;
    private GameCharacter player;
    private List<GameCharacter> enemies;
    private int currentTurn;
    
    public CombatSystem(GameDescription gameDescription) {
        this.gameDescription = gameDescription;
        this.inCombat = false;
        this.enemies = new ArrayList<>();
        this.currentTurn = 0;
        
        // Inizializza il giocatore
        this.player = new GameCharacter(0, "Giocatore", "Il coraggioso avventuriero", 100, 15, 5, CharacterType.PLAYER);
    }
    
    /**
     * Inizia un combattimento se ci sono nemici nella stanza corrente
     * @return messaggio di inizio combattimento
     */
    public String startCombat() {
        if (inCombat) {
            return "Sei già in combattimento!";
        }
        
        enemies.clear();
        int roomId = gameDescription.getCurrentRoom().getId();
        
        // Crea nemici basati sulla stanza
        switch (roomId) {
            case 0: // Ingresso - Goblin
                enemies.add(new GameCharacter(1, "Goblin", 
                    "Una creatura malvagia dalla pelle verde scuro, con occhi pieni d'odio e artigli affilati.", 
                    40, 12, 3, CharacterType.GOBLIN));
                break;
            case 1: // Stanza del Topo - Topo gigante
                enemies.add(new GameCharacter(2, "Topo Gigante", 
                    "Un enorme roditore con denti giallastri e occhi rossi.", 
                    25, 8, 2, CharacterType.GIANT_RAT));
                break;
            case 2: // Mensa - Due Goblin
                enemies.add(new GameCharacter(3, "Goblin Chiassoso", 
                    "Un goblin aggressivo che litiga per un osso.", 
                    35, 10, 3, CharacterType.GOBLIN));
                enemies.add(new GameCharacter(4, "Goblin Rissoso", 
                    "Un altro goblin altrettanto aggressivo.", 
                    30, 9, 2, CharacterType.GOBLIN));
                break;
            case 4: // Sala delle Guardie - Goblin Gigante + Goblin normale
                enemies.add(new GameCharacter(5, "Goblin Gigante", 
                    "Un goblin enorme che impugna una clava insanguinata.", 
                    60, 16, 5, CharacterType.GOBLIN));
                enemies.add(new GameCharacter(6, "Goblin Minuto", 
                    "Un goblin più piccolo ma altrettanto minaccioso.", 
                    25, 8, 2, CharacterType.GOBLIN));
                break;
            case 7: // Sala del Boss - Cane Demone
                enemies.add(new GameCharacter(7, "Cane Demone", 
                    "Una creatura infernale con zanne fumanti e occhi di fuoco.", 
                    120, 25, 8, CharacterType.DEMON_DOG));
                break;
            default:
                return ""; // Nessun nemico in questa stanza
        }
        
        if (enemies.isEmpty()) {
            return "";
        }
        
        inCombat = true;
        currentTurn = 0;
        
        StringBuilder msg = new StringBuilder();
        msg.append("\n=== COMBATTIMENTO ===\n");
        if (enemies.size() == 1) {
            msg.append("Un ").append(enemies.get(0).getName()).append(" ti blocca il passaggio!\n");
            msg.append(enemies.get(0).getDescription()).append("\n");
        } else {
            msg.append("Sei circondato da ").append(enemies.size()).append(" nemici!\n");
            for (GameCharacter enemy : enemies) {
                msg.append("- ").append(enemy.getName()).append("\n");
            }
        }
        msg.append("Scrivi 'attacca' per combattere, 'usa [arma]' per usare un'arma specifica, o 'usa cura' per curarti!\n");
        msg.append("===================");
        
        return msg.toString();
    }
    
    /**
     * Processa un'azione di combattimento
     * @param parserOutput comando del giocatore
     * @return risultato dell'azione
     */
    public String processCombatAction(ParserOutput parserOutput) {
        if (!inCombat) {
            return "Non sei in combattimento!";
        }
        
        StringBuilder result = new StringBuilder();
        
        // Azione del giocatore
        String playerAction = processPlayerAction(parserOutput);
        result.append(playerAction);
        
        // Rimuovi nemici morti
        enemies.removeIf(enemy -> !enemy.isAlive());
        
        // Controlla se il combattimento è finito
        if (enemies.isEmpty()) {
            inCombat = false;
            result.append("\n\n Hai vinto il combattimento! ");
            return result.toString();
        }
        
        // Turno dei nemici
        for (GameCharacter enemy : enemies) {
            if (enemy.isAlive()) {
                String enemyAction = processEnemyAction(enemy);
                result.append("\n").append(enemyAction);
            }
        }
        
        // Controlla se il giocatore è morto
        if (!player.isAlive()) {
            inCombat = false;
            result.append("\n\n Sei stato sconfitto! Game Over! ");
            // Qui potresti implementare un sistema di restart
        }
        
        currentTurn++;
        return result.toString();
    }
    
    /**
     * Processa l'azione del giocatore
     * @param parserOutput comando del giocatore
     * @return risultato dell'azione
     */
    private String processPlayerAction(ParserOutput parserOutput) {
        String commandName = parserOutput.getCommand().getName().toLowerCase();
        
        // Attacco normale
        if (commandName.equals("attacca") || commandName.equals("attack") || 
            commandName.equals("attacco") || commandName.equals("colpisci")) {
            return attackEnemy(null);
        }
        
        // Uso di oggetti/armi
        if (commandName.equals("usa") || parserOutput.getCommand().getName().equals("usa")) {
            
            // Controlla se ha specificato un oggetto dall'inventario
            if (parserOutput.getInvObject() != null) {
                GameObjects obj = parserOutput.getInvObject();
                
                // Pozioni di cura
                if (obj.getId() == 2 || obj.getId() == 5) {
                    return useHealingPotion(obj);
                }
                
                // Armi
                if (obj instanceof Weapon || obj.getId() == 12 || obj.getId() == 7 || obj.getId() == 6) {
                    return attackEnemy((Weapon) obj);
                }
            }
            
            // Parsing testuale per comandi come "usa cura", "usa spada"
            if (commandName.contains("cura") || commandName.equals("usa cura")) {
                // Cerca pozione di cura nell'inventario
                GameObjects healPotion = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 2);
                if (healPotion == null) {
                    healPotion = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 5);
                }
                
                if (healPotion != null) {
                    return useHealingPotion(healPotion);
                } else {
                    return "Non hai pozioni di cura nell'inventario!";
                }
            }
            
            if (commandName.contains("spada") || commandName.equals("usa spada")) {
                GameObjects sword = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 12);
                if (sword != null) {
                    return attackEnemy((Weapon) sword);
                } else {
                    return "Non hai una spada nell'inventario!";
                }
            }
            
            if (commandName.contains("arco") || commandName.equals("usa arco")) {
                GameObjects bow = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 7);
                if (bow != null) {
                    return attackEnemy((Weapon) bow);
                } else {
                    return "Non hai un arco nell'inventario!";
                }
            }
        }
        
        return "Azione non riconosciuta in combattimento!";
    }
    
    /**
     * Attacca un nemico
     * @param weapon arma da usare (null per attacco a mani nude)
     * @return risultato dell'attacco
     */
    private String attackEnemy(Weapon weapon) {
        if (enemies.isEmpty()) {
            return "Non ci sono nemici da attaccare!";
        }
        
        // Attacca il primo nemico vivo
        GameCharacter target = enemies.get(0);
        
        int damage = player.getBaseDamage();
        String weaponInfo = "";
        
        if (weapon != null) {
            damage = weapon.calculateDamage(damage);
            weaponInfo = " con " + weapon.getName();
            
            if (weapon.isCriticalHit()) {
                weaponInfo += " (COLPO CRITICO!)";
            }
        }
        
        String damageResult = target.takeDamage(damage);
        
        return "Attacchi " + target.getName() + weaponInfo + "!\n" + damageResult;
    }
    
    /**
     * Usa una pozione di cura
     * @param potion pozione da usare
     * @return risultato della cura
     */
    private String useHealingPotion(GameObjects potion) {
        int healAmount;
        
        if (potion.getId() == 2) { // Pozione normale
            healAmount = 30;
        } else if (potion.getId() == 5) { // Pozione totale
            return player.fullHeal() + "\nHai usato " + potion.getName() + "!";
        } else {
            return "Questa non è una pozione di cura!";
        }
        
        // Rimuovi la pozione dall'inventario
        gameDescription.getInventory().remove(potion);
        
        return player.heal(healAmount) + "\nHai usato " + potion.getName() + "!";
    }
    
    /**
     * Processa l'azione di un nemico
     * @param enemy nemico che agisce
     * @return risultato dell'azione
     */
    private String processEnemyAction(GameCharacter enemy) {
        // I nemici attaccano sempre il giocatore
        int damage = enemy.getBaseDamage() + GameUtils.randomBetween(-2, 2);
        String attackResult = player.takeDamage(damage);
        
        return enemy.getName() + " ti attacca!\n" + attackResult;
    }
    
    /**
     * Verifica se è in corso un combattimento
     * @return true se in combattimento
     */
    public boolean isInCombat() {
        return inCombat;
    }
    
    /**
     * Termina forzatamente il combattimento
     */
    public void endCombat() {
        inCombat = false;
        enemies.clear();
    }
    
    /**
     * Restituisce il giocatore
     * @return personaggio giocatore
     */
    public GameCharacter getPlayer() {
        return player;
    }
    
    /**
     * Restituisce la lista dei nemici
     * @return lista nemici
     */
    public List<GameCharacter> getEnemies() {
        return enemies;
    }
}