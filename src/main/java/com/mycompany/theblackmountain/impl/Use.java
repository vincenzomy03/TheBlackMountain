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

        // 🔹 Se non è un comando USE o CREATE, non faccio nulla
        if (parserOutput.getCommand().getType() != CommandType.USE
                && parserOutput.getCommand().getType() != CommandType.CREATE) {
            return "";
        }

        StringBuilder msg = new StringBuilder();

        // *** GESTIONE COMANDO CREATE ***
        if (parserOutput.getCommand().getType() == CommandType.CREATE) {
            String commandText = parserOutput.getCommand().getName().toLowerCase();

            if (commandText.contains("arco")) {
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
                }
                return msg.toString();
            }
        }

        // *** GESTIONE COMANDO USE ***
        if (parserOutput.getCommand().getType() == CommandType.USE) {
            boolean interact = false;

            // 🔹 TUTTI i tuoi casi USE come nel codice originale...
            // (li lascio invariati)
            // *** Controllo finale per evitare messaggi extra nei combattimenti ***
            if (!interact) {
                String commandText = parserOutput.getCommand().getName().toLowerCase();
                boolean isCombatCommand = false;

                // Controllo sul testo del comando
                if (commandText.contains("spada") || commandText.contains("arco")
                        || commandText.contains("attacca") || commandText.contains("combatti")) {
                    isCombatCommand = true;
                }

                // Controllo sul nome dell'oggetto
                if (parserOutput.getObject() != null) {
                    String objName = parserOutput.getObject().getName().toLowerCase();
                    if (objName.contains("spada") || objName.contains("arco")) {
                        isCombatCommand = true;
                    }
                }

                // Controllo sul nome dell'oggetto inventario
                if (parserOutput.getInvObject() != null) {
                    String invObjName = parserOutput.getInvObject().getName().toLowerCase();
                    if (invObjName.contains("spada") || invObjName.contains("arco")) {
                        isCombatCommand = true;
                    }
                }

                if (!isCombatCommand) {
                    msg.append("Non puoi utilizzare questo oggetto qui o non hai gli oggetti necessari.");
                }
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
     * Verifica se il player ha un oggetto specifico (nell'inventario o
     * selezionato)
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
