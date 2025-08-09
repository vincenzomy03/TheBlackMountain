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

/**
 *
 * @author vince
 */
public class Use extends GameObserver {

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        
        // *** GESTIONE COMANDO CREATE ***
        if (parserOutput.getCommand().getType() == CommandType.CREATE) {
            String commandText = parserOutput.getCommand().getName().toLowerCase();
            
            // Se il comando contiene "arco"
            if (commandText.contains("arco")) {
                
                // Controlla se siamo nella Sala degli Incantesimi (ID = 5)
                if (description.getCurrentRoom().getId() != 5) {
                    msg.append("Puoi creare l'arco magico solo nella Sala degli Incantesimi.");
                    return msg.toString();
                }
                
                // Verifica se ha bastone (id 6) e stringhe ragnatela (id 4) nell'inventario
                GameObjects bastone = GameUtils.getObjectFromInventory(description.getInventory(), 6);
                GameObjects stringhe = GameUtils.getObjectFromInventory(description.getInventory(), 4);
                
                if (bastone != null && stringhe != null) {
                    // Controlla se il giocatore ha abbastanza HP
                    if (description instanceof TBMGame) {
                        TBMGame game = (TBMGame) description;
                        GameCharacter player = game.getPlayer();
                        
                        if (player != null && player.getCurrentHp() > 20) {
                            // Crea l'arco
                            msg.append("🏹 Hai combinato il bastone con le stringhe di ragnatela!");
                            msg.append("\n✨ L'altare magico infonde potere nella tua creazione...");
                            msg.append("\n💔 Il processo ti ha indebolito. (-20 HP)");
                            
                            // Rimuovi i componenti
                            description.getInventory().remove(bastone);
                            description.getInventory().remove(stringhe);
                            
                            // Sottrai HP al giocatore
                            player.setCurrentHp(player.getCurrentHp() - 20);
                            game.updateCharacterState(player);
                            
                            // Crea e aggiungi arco magico
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
                }
                
                return msg.toString();
            }
        }
        
        // *** GESTIONE COMANDO USE ***
        if (parserOutput.getCommand().getType() == CommandType.USE) {
            boolean interact = false;
            
            // Uso chiave ingresso (id 1)
            if (hasObject(parserOutput, 1) && description.getCurrentRoom().getId() == 0) {
                msg.append("Hai usato la chiave per aprire la porta principale della fortezza. Ora puoi procedere.");
                interact = true;
            }
            // Uso pozione di cura (id 2)
            else if (hasObject(parserOutput, 2)) {
                if (description instanceof TBMGame) {
                    TBMGame game = (TBMGame) description;
                    GameCharacter player = game.getPlayer();
                    if (player != null) {
                        int healing = Math.min(30, player.getMaxHp() - player.getCurrentHp());
                        player.setCurrentHp(player.getCurrentHp() + healing);
                        game.updateCharacterState(player);
                        msg.append("Hai bevuto la pozione di cura. Ti senti rinvigorito! (+" + healing + " HP)");
                        msg.append("\nHP attuali: " + player.getCurrentHp() + "/" + player.getMaxHp());
                    } else {
                        msg.append("Hai bevuto la pozione di cura. Ti senti rinvigorito! (+30 HP)");
                    }
                }
                removeFromInventory(description, 2);
                interact = true;
            }
            // Uso frecce (id 3) - richiedono un arco
            else if (hasObject(parserOutput, 3)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 7) != null) {
                    msg.append("Hai caricato le frecce nell'arco magico. Ora sei pronto per il combattimento!");
                } else {
                    msg.append("Hai bisogno di un arco per utilizzare le frecce.");
                }
                interact = true;
            }
            // Uso stringhe di ragnatela (id 4) + frecce (id 3) + bastone (id 6) = arco magico
            else if (hasObject(parserOutput, 4)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 3) != null
                        && GameUtils.getObjectFromInventory(description.getInventory(), 6) != null) {
                    msg.append("Hai combinato le stringhe di ragnatela con il bastone e le frecce per creare un arco magico potente!");

                    // Rimuovi i componenti
                    removeFromInventory(description, 4);
                    removeFromInventory(description, 3);
                    removeFromInventory(description, 6);

                    // Aggiungi arco magico
                    GameObjects magicBow = createMagicBow();
                    description.getInventory().add(magicBow);
                } else {
                    msg.append("Hai bisogno di frecce e un bastone per utilizzare le stringhe di ragnatela.");
                }
                interact = true;
            }
            // Uso pozione cura totale (id 5)
            else if (hasObject(parserOutput, 5)) {
                if (description instanceof TBMGame) {
                    TBMGame game = (TBMGame) description;
                    GameCharacter player = game.getPlayer();
                    if (player != null) {
                        player.setCurrentHp(player.getMaxHp());
                        game.updateCharacterState(player);
                        msg.append("Hai bevuto la pozione di cura totale. Sei completamente guarito!");
                        msg.append("\nHP: " + player.getCurrentHp() + "/" + player.getMaxHp());
                    } else {
                        msg.append("Hai bevuto la pozione di cura totale. Sei completamente guarito! (HP al massimo)");
                    }
                }
                removeFromInventory(description, 5);
                interact = true;
            }
            // Uso libro incantesimo del fuoco (id 8)
            else if (hasObject(parserOutput, 8)) {
                msg.append("Hai letto il libro degli incantesimi del fuoco. Ora conosci potenti magie offensive!");
                msg.append("\n🔥 Le tue armi infliggeranno danni da fuoco aggiuntivi!");
                interact = true;
            }
            // Uso veleno (id 9) - può essere applicato su armi
            else if (hasObject(parserOutput, 9)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 7) != null) {
                    msg.append("Hai applicato il veleno all'arco magico. I tuoi attacchi saranno più letali!");
                    msg.append("\n☠️ Le frecce ora sono avvelenate!");
                    removeFromInventory(description, 9);
                } else if (GameUtils.getObjectFromInventory(description.getInventory(), 6) != null) {
                    msg.append("Hai applicato il veleno al bastone. Non è l'arma ideale, ma farà più danni.");
                    msg.append("\n☠️ Il bastone ora è avvelenato!");
                    removeFromInventory(description, 9);
                } else if (GameUtils.getObjectFromInventory(description.getInventory(), 12) != null) {
                    msg.append("Hai applicato il veleno alla spada. La lama brilla di un luccichio sinistro.");
                    msg.append("\n☠️ La spada ora è avvelenata!");
                    removeFromInventory(description, 9);
                } else {
                    msg.append("Hai bisogno di un'arma per applicare il veleno.");
                }
                interact = true;
            }
            // Uso chiave cella principessa (id 10) - solo nella stanza del boss
            else if (hasObject(parserOutput, 10) && description.getCurrentRoom().getId() == 7) {
                msg.append("Hai usato la chiave per aprire la cella della principessa! La missione è quasi compiuta...");
                msg.append("\n👸 La principessa ti ringrazia e ti consegna la chiave finale per uscire dalla montagna.");

                // Aggiungi chiave del boss
                GameObjects bossKey = new GameObjects(11, "chiave del collo del boss",
                        "Una chiave pesante che apre l'uscita dalla Montagna Nera.");
                bossKey.setPickupable(true);
                description.getInventory().add(bossKey);
                removeFromInventory(description, 10);
                interact = true;
            }
            // Uso chiave del boss (id 11) - per uscire
            else if (hasObject(parserOutput, 11) && description.getCurrentRoom().getId() == 8) {
                msg.append("🗝️ Hai usato la chiave del boss per aprire l'uscita finale!");
                msg.append("\n🎉 Congratulazioni! Hai completato la tua avventura nella Montagna Nera!");
                msg.append("\n👸 La principessa è salva e il regno è libero dal male!");
                // Fine del gioco - potresti gestire questo diversamente
                interact = true;
            }
            // Uso altare nella sala degli incantesimi (stanza 5)
            else if (description.getCurrentRoom().getId() == 5 && (parserOutput.getObject() != null
                    || parserOutput.getCommand().getName().contains("altare"))) {
                
                if (description instanceof TBMGame) {
                    TBMGame game = (TBMGame) description;
                    GameCharacter player = game.getPlayer();
                    
                    if (player != null && player.getCurrentHp() > 20) {
                        msg.append("✨ Ti avvicini all'altare magico. Una voce sussurra:");
                        msg.append("\n'Sacrifica parte della tua essenza per ottenere potere.'");
                        msg.append("\n💔 (-20 HP) Hai ricevuto un arco magico dall'altare!");
                        
                        // Sottrai HP
                        player.setCurrentHp(player.getCurrentHp() - 20);
                        game.updateCharacterState(player);
                        
                        GameObjects magicBow = createMagicBow();
                        description.getInventory().add(magicBow);
                        
                        msg.append("\n❤️ HP rimanenti: ").append(player.getCurrentHp());
                    } else {
                        msg.append("💀 Non hai abbastanza energia vitale per usare l'altare! (Servono almeno 21 HP)");
                    }
                } else {
                    msg.append("✨ Ti avvicini all'altare magico...");
                    msg.append("\n💔 (-20 HP) Hai ricevuto un arco magico dall'altare!");
                    GameObjects magicBow = createMagicBow();
                    description.getInventory().add(magicBow);
                }
                interact = true;
            }

            if (!interact) {
                msg.append("Non puoi utilizzare questo oggetto qui o non hai gli oggetti necessari.");
            }
        }
        
        return msg.toString();
    }

    /**
     * Crea un arco magico con le proprietà corrette
     */
    private GameObjects createMagicBow() {
        // Crea l'arco magico come GameObjects normale
        GameObjects magicBow = new GameObjects(7, "arco magico", 
            "Un arco etereo creato combinando materiali magici nell'altare degli incantesimi.");
        magicBow.setPickupable(true);
        return magicBow;
    }

    /**
     * Verifica se il player ha un oggetto specifico (nell'inventario o selezionato)
     *
     * @param parserOutput
     * @param objectId
     * @return
     */
    private boolean hasObject(ParserOutput parserOutput, int objectId) {
        return (parserOutput.getInvObject() != null && parserOutput.getInvObject().getId() == objectId)
                || (parserOutput.getObject() != null && parserOutput.getObject().getId() == objectId);
    }

    /**
     * Rimuove un oggetto dall'inventario
     *
     * @param description
     * @param objectId
     */
    private void removeFromInventory(GameDescription description, int objectId) {
        GameObjects obj = GameUtils.getObjectFromInventory(description.getInventory(), objectId);
        if (obj != null) {
            description.getInventory().remove(obj);
        }
    }
}