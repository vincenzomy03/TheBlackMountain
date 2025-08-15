/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.GameUtils;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.Weapon;
import com.mycompany.theblackmountain.type.WeaponType;

/**
 * Observer per gestire i comandi USE e CREATE
 * @author vince
 */
public class Use extends GameObserver {

    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        
        // Se non è un comando USE o CREATE, non faccio nulla
        if (parserOutput.getCommand().getType() != CommandType.USE
                && parserOutput.getCommand().getType() != CommandType.CREATE) {
            return "";
        }

        StringBuilder msg = new StringBuilder();

        // *** GESTIONE COMANDO CREATE ***
        if (parserOutput.getCommand().getType() == CommandType.CREATE) {
            return handleCreateCommand(description, parserOutput);
        }

        // *** GESTIONE COMANDO USE ***
        if (parserOutput.getCommand().getType() == CommandType.USE) {
            return handleUseCommand(description, parserOutput);
        }

        return msg.toString();
    }

    /**
     * Gestisce i comandi CREATE
     */
    private String handleCreateCommand(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        String commandText = parserOutput.getCommand().getName().toLowerCase();

        System.out.println("🔍 DEBUG CREATE: comando = '" + commandText + "'");

        if (commandText.contains("arco") || commandText.equals("crea")) {
            if (description.getCurrentRoom().getId() != 5) {
                msg.append("Puoi creare l'arco magico solo nella Sala degli Incantesimi.");
                return msg.toString();
            }

            GameObjects bastone = GameUtils.getObjectFromInventory(description.getInventory(), 6);
            GameObjects stringhe = GameUtils.getObjectFromInventory(description.getInventory(), 4);

            if (bastone != null && stringhe != null) {
                if (description instanceof TBMGame) {
                    TBMGame game = (TBMGame) description;
                    GameCharacter player = game.getPlayer();

                    if (player != null && player.getCurrentHp() > 20) {
                        msg.append("🏹 Hai combinato il bastone con le stringhe di ragnatela!");
                        msg.append("\n✨ L'altare magico infonde potere nella tua creazione...");
                        msg.append("\n💔 Il processo ti ha indebolito. (-20 HP)");

                        description.getInventory().remove(bastone);
                        description.getInventory().remove(stringhe);

                        player.setCurrentHp(player.getCurrentHp() - 20);
                        game.updateCharacterState(player);

                        GameObjects magicBow = createMagicBow();
                        description.getInventory().add(magicBow);

                        msg.append("\n🎯 Hai ottenuto: ").append(magicBow.getName());
                        msg.append("\n❤️ HP rimanenti: ").append(player.getCurrentHp());

                    } else {
                        msg.append("💀 Non hai abbastanza energia vitale per creare l'arco! (Servono almeno 21 HP)");
                    }
                } else {
                    msg.append("❌ Errore nel sistema del gioco.");
                }
            } else {
                msg.append("📦 Per creare un arco magico ti servono:");
                msg.append("\n  • Bastone").append(bastone == null ? " ❌" : " ✅");
                msg.append("\n  • Stringhe di ragnatela").append(stringhe == null ? " ❌" : " ✅");
                msg.append("\n💡 Vai nella Sala degli Incantesimi per combinare i materiali!");
            }
        } else {
            msg.append("Non capisco cosa vuoi creare. Prova 'crea arco' se hai i materiali giusti.");
        }

        return msg.toString();
    }

    /**
     * Gestisce i comandi USE
     */
    private String handleUseCommand(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        String commandText = parserOutput.getCommand().getName().toLowerCase();
        boolean commandHandled = false;

        System.out.println("🔍 DEBUG USE: comando = '" + commandText + "'");

        // *** GESTIONE SPECIALE PER IL VELENO ***
        if (commandText.contains("veleno")) {
            return handlePoisonUsage(description);
        }

        // *** POZIONI DI CURA ***
        if (commandText.contains("pozione") || commandText.contains("cura")) {
            String result = handleHealingPotions(description, parserOutput);
            if (!result.isEmpty()) {
                return result;
            }
        }

        // *** USO OGGETTO SPECIFICO DALL'INVENTARIO ***
        if (parserOutput.getInvObject() != null) {
            GameObjects invObj = parserOutput.getInvObject();
            
            switch (invObj.getId()) {
                case 2: // Pozione normale
                    return useHealingPotion(description, invObj, 30);
                    
                case 5: // Pozione totale
                    return useHealingPotion(description, invObj, -1); // -1 = cura completa
                    
                case 9: // Veleno
                    return handlePoisonUsage(description);
                    
                case 1: // Chiave ingresso
                    if (description.getCurrentRoom().getId() == 0) {
                        msg.append("Hai già usato la chiave per entrare nella fortezza!");
                        commandHandled = true;
                    }
                    break;
                    
                case 3: // Chiave delle celle
                    if (description.getCurrentRoom().getId() == 6) {
                        msg.append("La chiave apre le celle delle prigioni, ma sono tutte vuote.");
                        commandHandled = true;
                    }
                    break;
                    
                case 10: // Chiave del tesoro
                    if (description.getCurrentRoom().getId() == 7) {
                        msg.append("La chiave del tesoro potrebbe essere utile qui...");
                        commandHandled = true;
                    }
                    break;
                    
                case 8: // Libro incantesimo fuoco
                    return useFireSpellBook(description);
                    
                default:
                    // Per armi e oggetti speciali, lasciali passare al CombatSystem se siamo in combattimento
                    if (isWeapon(invObj)) {
                        // Lascia che il CombatObserver gestisca le armi
                        return "";
                    }
                    break;
            }
        }

        // *** USO OGGETTO NELLA STANZA ***
        if (parserOutput.getObject() != null) {
            GameObjects roomObj = parserOutput.getObject();
            
            switch (roomObj.getId()) {
                case 11: // Altare magico nella Sala degli Incantesimi
                    if (description.getCurrentRoom().getId() == 5) {
                        msg.append("L'altare magico emana un'energia arcana. Potresti usarlo per creare qualcosa di speciale...");
                        msg.append("\n💡 Prova a scrivere 'crea arco' se hai i materiali giusti!");
                        commandHandled = true;
                    }
                    break;
                    
                default:
                    msg.append("Non puoi usare questo oggetto.");
                    commandHandled = true;
                    break;
            }
        }

        // *** PARSING TESTUALE PER COMANDI SPECIFICI ***
        if (!commandHandled) {
            
            // Comandi di cura
            if (commandText.contains("cura")) {
                return useBestHealingPotion(description);
            }
            
            // Uso chiavi specifiche
            if (commandText.contains("chiave")) {
                return handleKeyUsage(description, commandText);
            }
            
            // Uso libri/incantesimi
            if (commandText.contains("libro") || commandText.contains("incantesimo") || commandText.contains("fuoco")) {
                GameObjects fireBook = GameUtils.getObjectFromInventory(description.getInventory(), 8);
                if (fireBook != null) {
                    return useFireSpellBook(description);
                } else {
                    msg.append("Non hai nessun libro di incantesimi.");
                    commandHandled = true;
                }
            }
            
            // Se non è stato gestito e non è un comando di combattimento
            if (!commandHandled && !isCombatRelatedCommand(commandText, parserOutput)) {
                msg.append("Non puoi utilizzare questo oggetto qui o non hai gli oggetti necessari.");
            }
        }

        return msg.toString();
    }

    /**
     * Gestisce l'uso del veleno sulle armi
     */
    private String handlePoisonUsage(GameDescription description) {
        StringBuilder msg = new StringBuilder();
        
        GameObjects poison = GameUtils.getObjectFromInventory(description.getInventory(), 9);
        if (poison == null) {
            return "Non hai veleno nell'inventario!";
        }

        // Cerca armi nell'inventario (priorità: spada > arco > bastone)
        Weapon weaponToPoison = null;
        String weaponName = "";
        
        // Controlla spada
        GameObjects sword = GameUtils.getObjectFromInventory(description.getInventory(), 12);
        if (sword instanceof Weapon) {
            weaponToPoison = (Weapon) sword;
            weaponName = "spada";
        }
        
        // Se non c'è spada, controlla arco magico
        if (weaponToPoison == null) {
            GameObjects bow = GameUtils.getObjectFromInventory(description.getInventory(), 7);
            if (bow instanceof Weapon) {
                weaponToPoison = (Weapon) bow;
                weaponName = "arco magico";
            }
        }
        
        // Se non c'è arco, controlla bastone
        if (weaponToPoison == null) {
            GameObjects staff = GameUtils.getObjectFromInventory(description.getInventory(), 6);
            if (staff instanceof Weapon) {
                weaponToPoison = (Weapon) staff;
                weaponName = "bastone";
            }
        }
        
        if (weaponToPoison == null) {
            msg.append("Non hai armi nell'inventario da avvelenare!");
            msg.append("\n💡 Il veleno può essere applicato su: spada, arco magico, o bastone.");
            return msg.toString();
        }
        
        // Controlla se l'arma è già avvelenata
        if (weaponToPoison.isPoisoned()) {
            msg.append("La tua ").append(weaponName).append(" è già avvelenata!");
            return msg.toString();
        }
        
        // Applica il veleno usando il metodo della classe Weapon
        weaponToPoison.applyPoison(5); // 5 danni veleno aggiuntivi
        
        msg.append("🧪 Applichi il veleno sulla tua ").append(weaponName).append("!");
        msg.append("\n💀 L'arma ora infliggerà +5 danni da veleno!");
        msg.append("\n⚔️ Statistiche aggiornate: ").append(weaponToPoison.getWeaponStats());
        
        // Rimuovi il veleno dall'inventario
        description.getInventory().remove(poison);
        
        // Aggiorna il database se disponibile
        if (description instanceof TBMGame) {
            TBMGame game = (TBMGame) description;
            if (game.getGameLoader() != null) {
                game.updateObjectState(weaponToPoison);
                game.getGameLoader().removeObject(poison);
            }
        }
        
        msg.append("\n✅ Veleno applicato! La fiala è ora vuota.");
        
        return msg.toString();
    }

    /**
     * Gestisce le pozioni di cura
     */
    private String handleHealingPotions(GameDescription description, ParserOutput parserOutput) {
        // Prima prova oggetti specifici selezionati
        if (parserOutput.getInvObject() != null && 
            (parserOutput.getInvObject().getId() == 2 || parserOutput.getInvObject().getId() == 5)) {
            int healAmount = (parserOutput.getInvObject().getId() == 2) ? 30 : -1;
            return useHealingPotion(description, parserOutput.getInvObject(), healAmount);
        }
        
        // Altrimenti usa la migliore disponibile
        return useBestHealingPotion(description);
    }

    /**
     * Usa una pozione di cura
     */
    private String useHealingPotion(GameDescription description, GameObjects potion, int healAmount) {
        if (!(description instanceof TBMGame)) {
            return "Errore nel sistema del gioco.";
        }
        
        TBMGame game = (TBMGame) description;
        GameCharacter player = game.getPlayer();
        
        if (player == null) {
            return "Errore: giocatore non trovato.";
        }
        
        StringBuilder msg = new StringBuilder();
        
        // Calcola la cura effettiva
        int oldHp = player.getCurrentHp();
        int newHp;
        int actualHeal;
        
        if (healAmount == -1) { // Cura totale
            newHp = player.getMaxHp();
            actualHeal = newHp - oldHp;
        } else {
            newHp = Math.min(player.getMaxHp(), oldHp + healAmount);
            actualHeal = newHp - oldHp;
        }
        
        if (actualHeal <= 0) {
            msg.append("Sei già al massimo della salute!");
            return msg.toString();
        }
        
        // Applica la cura
        player.setCurrentHp(newHp);
        description.getInventory().remove(potion);
        
        // Aggiorna il database
        game.updateCharacterState(player);
        
        msg.append("Hai bevuto la ").append(potion.getName()).append("!");
        msg.append("\n💚 Recuperati ").append(actualHeal).append(" HP!");
        msg.append("\n❤️ HP attuali: ").append(player.getCurrentHp()).append("/").append(player.getMaxHp());
        
        return msg.toString();
    }

    /**
     * Usa la migliore pozione di cura disponibile
     */
    private String useBestHealingPotion(GameDescription description) {
        // Prima cerca pozione cura totale
        GameObjects totalPotion = GameUtils.getObjectFromInventory(description.getInventory(), 5);
        if (totalPotion != null) {
            return useHealingPotion(description, totalPotion, -1);
        }

        // Poi cerca pozione normale
        GameObjects normalPotion = GameUtils.getObjectFromInventory(description.getInventory(), 2);
        if (normalPotion != null) {
            return useHealingPotion(description, normalPotion, 30);
        }

        return "Non hai pozioni di cura nell'inventario!";
    }

    /**
     * Usa il libro degli incantesimi del fuoco
     */
    private String useFireSpellBook(GameDescription description) {
        StringBuilder msg = new StringBuilder();
        
        if (description.getCurrentRoom().getId() != 5) {
            msg.append("Puoi usare il libro degli incantesimi solo nella Sala degli Incantesimi, vicino all'altare magico.");
        } else {
            msg.append("🔥 Reciti l'incantesimo del fuoco dal libro antico!");
            msg.append("\n✨ L'altare magico si illumina di una luce rossastra...");
            msg.append("\n📚 Il libro potrebbe essere utile per potenziare armi o altri incantesimi.");
            msg.append("\n💡 Potresti combinarlo con altri oggetti magici!");
        }
        
        return msg.toString();
    }

    /**
     * Gestisce l'uso delle chiavi
     */
    private String handleKeyUsage(GameDescription description, String commandText) {
        StringBuilder msg = new StringBuilder();
        int roomId = description.getCurrentRoom().getId();
        
        if (commandText.contains("ingresso")) {
            GameObjects key = GameUtils.getObjectFromInventory(description.getInventory(), 1);
            if (key != null) {
                if (roomId == 0) {
                    msg.append("Hai già usato questa chiave per entrare nella fortezza!");
                } else {
                    msg.append("Questa chiave apriva l'ingresso della fortezza.");
                }
            } else {
                msg.append("Non hai la chiave dell'ingresso.");
            }
        } else if (commandText.contains("celle")) {
            GameObjects key = GameUtils.getObjectFromInventory(description.getInventory(), 3);
            if (key != null) {
                if (roomId == 6) {
                    msg.append("Usi la chiave sulle celle della prigione. Sono tutte vuote.");
                } else {
                    msg.append("Questa chiave serve per le celle della prigione.");
                }
            } else {
                msg.append("Non hai la chiave delle celle.");
            }
        } else if (commandText.contains("tesoro")) {
            GameObjects key = GameUtils.getObjectFromInventory(description.getInventory(), 10);
            if (key != null) {
                if (roomId == 7) {
                    msg.append("La chiave del tesoro brilla... potrebbe aprire qualcosa di importante qui!");
                } else {
                    msg.append("Questa è la chiave del tesoro. Potrebbe essere utile nella stanza finale.");
                }
            } else {
                msg.append("Non hai la chiave del tesoro.");
            }
        } else {
            msg.append("Specifica quale chiave vuoi usare: ingresso, celle, o tesoro.");
        }
        
        return msg.toString();
    }

    /**
     * Controlla se un oggetto è un'arma
     */
    private boolean isWeapon(GameObjects obj) {
        return obj.getId() == 6 || obj.getId() == 7 || obj.getId() == 12; // bastone, arco, spada
    }

    /**
     * Controlla se è un comando relativo al combattimento
     */
    private boolean isCombatRelatedCommand(String commandText, ParserOutput parserOutput) {
        // Controllo sul testo del comando
        if (commandText.contains("spada") || commandText.contains("arco") || 
            commandText.contains("bastone") || commandText.contains("attacca") || 
            commandText.contains("combatti")) {
            return true;
        }

        // Controllo sull'oggetto selezionato
        if (parserOutput.getObject() != null || parserOutput.getInvObject() != null) {
            GameObjects obj = (parserOutput.getInvObject() != null) ? 
                             parserOutput.getInvObject() : parserOutput.getObject();
            if (isWeapon(obj)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Crea un arco magico con le proprietà corrette usando la classe Weapon
     */
    private GameObjects createMagicBow() {
        Weapon magicBow = new Weapon(7, "arco magico",
                "Un arco etereo creato combinando materiali magici nell'altare degli incantesimi. " +
                "Emana una leggera aura bluastra e vibra di potere arcano.",
                12, // Attack bonus
                WeaponType.BOW, 
                15, // Critical chance (15%)
                2   // Critical multiplier (x2)
        );
        magicBow.setPickupable(true);
        return magicBow;
    }
}