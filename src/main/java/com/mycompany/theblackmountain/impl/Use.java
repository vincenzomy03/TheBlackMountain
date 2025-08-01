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
import com.mycompany.theblackmountain.type.Objects;

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
        if (parserOutput.getCommand().getType() == CommandType.USE) {
            boolean interact = false;
            
            // Uso chiave ingresso (id 1)
            if (hasObject(parserOutput, 1) && description.getCurrentRoom().getId() == 0) {
                msg.append("Hai usato la chiave per aprire la porta principale della fortezza. Ora puoi procedere.");
                interact = true;
            }
            
            // Uso pozione di cura (id 2)
            else if (hasObject(parserOutput, 2)) {
                msg.append("Hai bevuto la pozione di cura. Ti senti rinvigorito! (+30 HP)");
                removeFromInventory(description, 2);
                interact = true;
            }
            
            // Uso frecce (id 3) - richiedono un arco
            else if (hasObject(parserOutput, 3)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 7) != null) {
                    msg.append("Hai caricato le frecce nell'arco.");
                } else {
                    msg.append("Hai bisogno di un arco per utilizzare le frecce.");
                }
                interact = true;
            }
            
            // Uso stringhe di ragnatela (id 4) + frecce (id 3) + bastone (id 6) = arco magico
            else if (hasObject(parserOutput, 4)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 3) != null && 
                    GameUtils.getObjectFromInventory(description.getInventory(), 6) != null) {
                    msg.append("Hai combinato le stringhe di ragnatela con il bastone e le frecce per creare un arco magico potente!");
                    
                    // Rimuovi i componenti
                    removeFromInventory(description, 4);
                    removeFromInventory(description, 3);
                    removeFromInventory(description, 6);
                    
                    // Aggiungi arco magico
                    Objects magicBow = new Objects(7, "arco magico", 
                        "Un arco leggero ma potente, creato con energia arcana e materiali raccolti nella fortezza.");
                    magicBow.setPickupable(true);
                    description.getInventory().add(magicBow);
                } else {
                    msg.append("Hai bisogno di frecce e un bastone per utilizzare le stringhe di ragnatela.");
                }
                interact = true;
            }
            
            // Uso pozione cura totale (id 5)
            else if (hasObject(parserOutput, 5)) {
                msg.append("Hai bevuto la pozione di cura totale. Sei completamente guarito! (HP al massimo)");
                removeFromInventory(description, 5);
                interact = true;
            }
            
            // Uso libro incantesimo del fuoco (id 8)
            else if (hasObject(parserOutput, 8)) {
                msg.append("Hai letto il libro degli incantesimi del fuoco. Ora conosci potenti magie offensive!");
                interact = true;
            }
            
            // Uso veleno (id 9) - può essere applicato su armi
            else if (hasObject(parserOutput, 9)) {
                if (GameUtils.getObjectFromInventory(description.getInventory(), 7) != null) {
                    msg.append("Hai applicato il veleno all'arco magico. I tuoi attacchi saranno più letali!");
                    removeFromInventory(description, 9);
                } else if (GameUtils.getObjectFromInventory(description.getInventory(), 6) != null) {
                    msg.append("Hai applicato il veleno al bastone. Non è l'arma ideale, ma farà più danni.");
                    removeFromInventory(description, 9);
                } else {
                    msg.append("Hai bisogno di un'arma per applicare il veleno.");
                }
                interact = true;
            }
            
            // Uso chiave cella principessa (id 10) - solo nella stanza del boss
            else if (hasObject(parserOutput, 10) && description.getCurrentRoom().getId() == 7) {
                msg.append("Hai usato la chiave per aprire la cella della principessa! La missione è quasi compiuta...");
                msg.append("\nLa principessa ti ringrazia e ti consegna la chiave finale per uscire dalla montagna.");
                
                // Aggiungi chiave del boss
                Objects bossKey = new Objects(11, "chiave del collo del boss",
                    "Una chiave pesante che apre l'uscita dalla Montagna Nera.");
                bossKey.setPickupable(true);
                description.getInventory().add(bossKey);
                interact = true;
            }
            
            // Uso chiave del boss (id 11) - per uscire
            else if (hasObject(parserOutput, 11) && description.getCurrentRoom().getId() == 8) {
                msg.append("Hai usato la chiave del boss per aprire l'uscita finale!");
                msg.append("\nCongratulazioni! Hai completato la tua avventura nella Montagna Nera!");
                description.setCurrentRoom(null); // Fine del gioco
                interact = true;
            }
            
            // Uso altare nella sala degli incantesimi (stanza 5)
            else if (description.getCurrentRoom().getId() == 5 && (parserOutput.getObject() != null || 
                     parserOutput.getCommand().getName().contains("altare"))) {
                msg.append("Ti avvicini all'altare magico. Una voce sussurra: 'Sacrifica parte della tua essenza per ottenere potere.'");
                msg.append("\n(-20 HP) Hai ricevuto un arco magico dall'altare!");
                
                Objects magicBow = new Objects(7, "arco magico", 
                    "Un arco etereo ottenuto attraverso un sacrificio magico.");
                magicBow.setPickupable(true);
                description.getInventory().add(magicBow);
                interact = true;
            }
            
            if (!interact) {
                msg.append("Non puoi utilizzare questo oggetto qui o non hai gli oggetti necessari.");
            }
        }
        return msg.toString();
    }
    
    /**
     * Verifica se il player ha un oggetto specifico (nell'inventario o selezionato)
     * @param parserOutput
     * @param objectId
     * @return
     */
    private boolean hasObject(ParserOutput parserOutput, int objectId) {
        return (parserOutput.getInvObject() != null && parserOutput.getInvObject().getId() == objectId) ||
               (parserOutput.getObject() != null && parserOutput.getObject().getId() == objectId);
    }
    
    /**
     * Rimuove un oggetto dall'inventario
     * @param description
     * @param objectId
     */
    private void removeFromInventory(GameDescription description, int objectId) {
        Objects obj = GameUtils.getObjectFromInventory(description.getInventory(), objectId);
        if (obj != null) {
            description.getInventory().remove(obj);
        }
    }
}