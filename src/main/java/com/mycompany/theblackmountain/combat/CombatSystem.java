/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.combat;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameUtils;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.CharacterType;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.Weapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sistema di combattimento del gioco
 * @author vince
 */
public class CombatSystem {
    
    private GameDescription gameDescription;
    private boolean inCombat;
    private GameCharacter player;
    private List<GameCharacter> currentEnemies;
    private int currentTurn;
    private Random random;
    
    public CombatSystem(GameDescription gameDescription) {
        this.gameDescription = gameDescription;
        this.inCombat = false;
        this.currentEnemies = new ArrayList<>();
        this.currentTurn = 0;
        this.random = new Random();
        
        // Ottieni il giocatore dalla GameDescription se possibile
        if (gameDescription instanceof TBMGame) {
            TBMGame game = (TBMGame) gameDescription;
            this.player = game.getPlayer();
        }
        
        // Fallback se il giocatore non è disponibile
        if (this.player == null) {
            this.player = new GameCharacter(0, "Giocatore", "Il coraggioso avventuriero", 100, 15, 5, CharacterType.PLAYER);
        }
    }
    
    /**
     * Inizia un combattimento se ci sono nemici nella stanza corrente
     * @return messaggio di inizio combattimento
     */
    public String startCombat() {
        if (inCombat) {
            return ""; // Già in combattimento
        }
        
        // Controlla se ci sono nemici vivi nella stanza
        List<GameCharacter> roomEnemies = gameDescription.getCurrentRoom().getEnemies();
        currentEnemies.clear();
        
        // Aggiungi solo nemici vivi
        for (GameCharacter enemy : roomEnemies) {
            if (enemy.isAlive()) {
                currentEnemies.add(enemy);
            }
        }
        
        if (currentEnemies.isEmpty()) {
            return ""; // Nessun nemico da combattere
        }
        
        inCombat = true;
        currentTurn = 0;
        
        StringBuilder msg = new StringBuilder();
        msg.append("\n⚔️ === COMBATTIMENTO ===\n");
        
        if (currentEnemies.size() == 1) {
            GameCharacter enemy = currentEnemies.get(0);
            msg.append(" ").append(enemy.getName()).append(" ti blocca il passaggio!\n");
            msg.append(" ").append(enemy.getDescription()).append("\n");
            msg.append("️ HP Nemico: ").append(enemy.getCurrentHp()).append("/").append(enemy.getMaxHp()).append("\n");
        } else {
            msg.append("😱 Sei circondato da ").append(currentEnemies.size()).append(" nemici!\n");
            for (int i = 0; i < currentEnemies.size(); i++) {
                GameCharacter enemy = currentEnemies.get(i);
                msg.append("🎯 ").append(i + 1).append(". ").append(enemy.getName())
                   .append(" (HP: ").append(enemy.getCurrentHp()).append("/").append(enemy.getMaxHp()).append(")\n");
            }
        }
        
        msg.append(" I tuoi HP: ").append(player.getCurrentHp()).append("/").append(player.getMaxHp()).append("\n");
        msg.append("🗡️ Scrivi 'attacca' per combattere, 'usa [oggetto]' per usare un oggetto!\n");
        msg.append("=========================");
        
        return msg.toString();
    }
    
    /**
     * Termina il combattimento e rimuove i nemici morti
     */
    public void endCombat() {
        if (!inCombat) {
            return;
        }
        
        inCombat = false;
        
        // Rimuovi nemici morti dalla stanza
        if (gameDescription.getCurrentRoom() != null) {
            gameDescription.getCurrentRoom().getEnemies().removeIf(enemy -> !enemy.isAlive());
            
            // Aggiorna lo stato dei nemici morti nel database se disponibile
            if (gameDescription instanceof TBMGame) {
                TBMGame game = (TBMGame) gameDescription;
                for (GameCharacter deadEnemy : currentEnemies) {
                    if (!deadEnemy.isAlive()) {
                        game.updateCharacterState(deadEnemy);
                    }
                }
            }
        }
        
        currentEnemies.clear();
        System.out.println(" Combattimento terminato - nemici morti rimossi dalla stanza");
    }
    
    /**
     * Controlla se tutti i nemici sono morti e termina il combattimento
     */
    public boolean checkCombatEnd() {
        if (!inCombat) {
            return false;
        }
        
        boolean allEnemiesDead = currentEnemies.stream().noneMatch(GameCharacter::isAlive);
        
        if (allEnemiesDead) {
            endCombat();
            return true;
        }
        
        return false;
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
        
        // Rimuovi nemici morti dalla lista corrente
        currentEnemies.removeIf(enemy -> !enemy.isAlive());
        
        // Controlla se il combattimento è finito (tutti i nemici morti)
        if (currentEnemies.isEmpty()) {
            endCombat();
            result.append("\n\n Vittoria! Hai sconfitto tutti i nemici! 🎉");
            return result.toString();
        }
        
        // Turno dei nemici
        result.append("\n\n--- Turno dei nemici ---");
        for (GameCharacter enemy : currentEnemies) {
            if (enemy.isAlive()) {
                String enemyAction = processEnemyAction(enemy);
                result.append("\n").append(enemyAction);
            }
        }
        
        // Controlla se il giocatore è morto
        if (!player.isAlive()) {
            endCombat();
            result.append("\n\n Sei stato sconfitto! Game Over!");
            // Reset HP giocatore per permettere di riprovare
            if (gameDescription instanceof TBMGame) {
                TBMGame game = (TBMGame) gameDescription;
                player.setCurrentHp(player.getMaxHp());
                game.updateCharacterState(player);
                result.append("\n✨ Ma la magia della montagna ti riporta in vita...");
            }
        }
        
        // Mostra stato attuale se il combattimento continua
        if (inCombat) {
            result.append("\n\n Stato attuale:");
            result.append("\n️ I tuoi HP: ").append(player.getCurrentHp()).append("/").append(player.getMaxHp());
            for (GameCharacter enemy : currentEnemies) {
                if (enemy.isAlive()) {
                    result.append("\n ").append(enemy.getName()).append(": ")
                           .append(enemy.getCurrentHp()).append("/").append(enemy.getMaxHp()).append(" HP");
                }
            }
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
        if (commandName.equals("usa") || commandName.startsWith("usa ")) {
            
            // Controlla se ha specificato un oggetto dall'inventario
            if (parserOutput.getInvObject() != null) {
                GameObjects obj = parserOutput.getInvObject();
                
                // Pozioni di cura
                if (obj.getId() == 2 || obj.getId() == 5) {
                    return useHealingPotion(obj);
                }
                
                // Armi
                if (obj instanceof Weapon || obj.getId() == 12 || obj.getId() == 7 || obj.getId() == 6) {
                    return attackEnemy(obj);
                }
            }
            
            // Parsing testuale per comandi come "usa cura", "usa spada"
            if (commandName.contains("cura") || commandName.contains("pozione")) {
                return useBestHealingPotion();
            }
            
            if (commandName.contains("spada")) {
                GameObjects sword = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 12);
                if (sword != null) {
                    return attackEnemy(sword);
                } else {
                    return "Non hai una spada nell'inventario!";
                }
            }
            
            if (commandName.contains("arco")) {
                GameObjects bow = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 7);
                if (bow != null) {
                    return attackEnemy(bow);
                } else {
                    return "Non hai un arco nell'inventario!";
                }
            }
            
            if (commandName.contains("bastone")) {
                GameObjects staff = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 6);
                if (staff != null) {
                    return attackEnemy(staff);
                } else {
                    return "Non hai un bastone nell'inventario!";
                }
            }
        }
        
        return "Azione non riconosciuta in combattimento! Prova 'attacca' o 'usa [oggetto]'";
    }
    
    /**
     * Attacca un nemico
     * @param weapon arma da usare (null per attacco a mani nude)
     * @return risultato dell'attacco
     */
    private String attackEnemy(GameObjects weapon) {
        if (currentEnemies.isEmpty()) {
            return "Non ci sono nemici da attaccare!";
        }
        
        // Attacca il primo nemico vivo
        GameCharacter target = currentEnemies.get(0);
        
        int baseDamage = player.getAttack();
        int weaponDamage = 0;
        String weaponInfo = "a mani nude";
        boolean isCritical = false;
        
        if (weapon != null) {
            weaponInfo = "con " + weapon.getName();
            
            // Calcola danni arma in base all'ID
            switch (weapon.getId()) {
                case 12: // Spada
                    weaponDamage = 8 + random.nextInt(4); // 8-11 danni
                    isCritical = random.nextInt(100) < 10; // 10% critico
                    break;
                case 7: // Arco magico
                    weaponDamage = 12 + random.nextInt(6); // 12-17 danni
                    isCritical = random.nextInt(100) < 15; // 15% critico
                    break;
                case 6: // Bastone
                    weaponDamage = 5 + random.nextInt(3); // 5-7 danni
                    isCritical = random.nextInt(100) < 5; // 5% critico
                    break;
                default:
                    weaponDamage = 2 + random.nextInt(3); // 2-4 danni
                    break;
            }
        }
        
        int totalDamage = baseDamage + weaponDamage + random.nextInt(3); // Variazione casuale 0-2
        
        if (isCritical) {
            totalDamage *= 2;
            weaponInfo += " (COLPO CRITICO!)";
        }
        
        // Applica danni considerando la difesa del nemico
        int actualDamage = Math.max(1, totalDamage - target.getDefense());
        target.setCurrentHp(Math.max(0, target.getCurrentHp() - actualDamage));
        
        StringBuilder result = new StringBuilder();
        result.append(" Attacchi ").append(target.getName()).append(" ").append(weaponInfo).append("!");
        result.append("\nInflitti ").append(actualDamage).append(" danni!");
        
        if (target.getCurrentHp() <= 0) {
            target.setCurrentHp(0);
            result.append("\n ").append(target.getName()).append(" è stato sconfitto!");
        } else {
            result.append("\n ").append(target.getName()).append(" HP: ")
                   .append(target.getCurrentHp()).append("/").append(target.getMaxHp());
        }
        
        return result.toString();
    }
    
    /**
     * Usa la migliore pozione di cura disponibile
     * @return risultato della cura
     */
    private String useBestHealingPotion() {
        // Prima cerca pozione cura totale
        GameObjects totalPotion = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 5);
        if (totalPotion != null) {
            return useHealingPotion(totalPotion);
        }
        
        // Poi cerca pozione normale
        GameObjects normalPotion = GameUtils.getObjectFromInventory(gameDescription.getInventory(), 2);
        if (normalPotion != null) {
            return useHealingPotion(normalPotion);
        }
        
        return "Non hai pozioni di cura nell'inventario!";
    }
    
    /**
     * Usa una pozione di cura
     * @param potion pozione da usare
     * @return risultato della cura
     */
    private String useHealingPotion(GameObjects potion) {
        int healAmount;
        String potionName = potion.getName();
        
        if (potion.getId() == 2) { // Pozione normale
            healAmount = 30;
        } else if (potion.getId() == 5) { // Pozione totale
            healAmount = player.getMaxHp() - player.getCurrentHp();
            player.setCurrentHp(player.getMaxHp());
        } else {
            return "Questa non è una pozione di cura!";
        }
        
        if (potion.getId() == 2) {
            int oldHp = player.getCurrentHp();
            player.setCurrentHp(Math.min(player.getMaxHp(), player.getCurrentHp() + healAmount));
            healAmount = player.getCurrentHp() - oldHp;
        }
        
        // Rimuovi la pozione dall'inventario
        gameDescription.getInventory().remove(potion);
        
        // Aggiorna database se disponibile
        if (gameDescription instanceof TBMGame) {
            TBMGame game = (TBMGame) gameDescription;
            game.updateCharacterState(player);
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Hai usato ").append(potionName).append("!");
        if (healAmount > 0) {
            result.append("\nRecuperati ").append(healAmount).append(" HP!");
            result.append("\nHP attuali: ").append(player.getCurrentHp()).append("/").append(player.getMaxHp());
        } else {
            result.append("\nSei già al massimo della salute!");
        }
        
        return result.toString();
    }
    
    /**
     * Processa l'azione di un nemico
     * @param enemy nemico che agisce
     * @return risultato dell'azione
     */
    private String processEnemyAction(GameCharacter enemy) {
        // I nemici attaccano sempre il giocatore
        int damage = enemy.getAttack() + random.nextInt(3) - 1; // Variazione -1 a +1
        int actualDamage = Math.max(1, damage - player.getDefense());
        
        int oldHp = player.getCurrentHp();
        player.setCurrentHp(Math.max(0, player.getCurrentHp() - actualDamage));
        
        // Aggiorna database se disponibile
        if (gameDescription instanceof TBMGame) {
            TBMGame game = (TBMGame) gameDescription;
            game.updateCharacterState(player);
        }
        
        StringBuilder result = new StringBuilder();
        result.append("").append(enemy.getName()).append(" ti attacca!");
        result.append("\nSubisci ").append(actualDamage).append(" danni!");
        
        if (player.getCurrentHp() <= 0) {
            result.append("\nSei stato gravemente ferito!");
        }
        
        return result.toString();
    }
    
    /**
     * Verifica se è in corso un combattimento
     * @return true se in combattimento
     */
    public boolean isInCombat() {
        return inCombat;
    }
    
    /**
     * Restituisce il giocatore
     * @return personaggio giocatore
     */
    public GameCharacter getPlayer() {
        return player;
    }
    
    /**
     * Restituisce la lista dei nemici attuali
     * @return lista nemici
     */
    public List<GameCharacter> getCurrentEnemies() {
        return currentEnemies;
    }
}