/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.database.GameLoader;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author vince
 */
public class Open extends GameObserver {

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        if (parserOutput.getCommand().getType() == CommandType.OPEN) {

            // Controlla se il comando contiene "cassa" anche senza oggetto specifico
            String commandText = parserOutput.getCommand().getName().toLowerCase();
            boolean lookingForChest = commandText.contains("cassa")
                    || (parserOutput.getObject() == null && parserOutput.getInvObject() == null);

            if (parserOutput.getObject() == null && parserOutput.getInvObject() == null && lookingForChest) {
                // DEBUG: Stampa tutti gli oggetti nella stanza corrente
                System.out.println("🔍 DEBUG: Ricerca cassa nella stanza " + description.getCurrentRoom().getId());
                System.out.println("🔍 DEBUG: Oggetti nella stanza:");
                for (GameObjects obj : description.getCurrentRoom().getObjects()) {
                    System.out.println("  - ID: " + obj.getId() + ", Nome: '" + obj.getName() + 
                                     "', Openable: " + obj.isOpenable() + ", Opened: " + obj.isOpen());
                }

                // Cerca una cassa nella stanza corrente - METODO MIGLIORATO
                GameObjects cassa = findChestInRoom(description.getCurrentRoom().getObjects());

                if (cassa != null) {
                    System.out.println("✅ DEBUG: Trovata cassa con ID " + cassa.getId());
                    return openChestFromDB(description, cassa);
                } else {
                    System.out.println("❌ DEBUG: Nessuna cassa trovata nella stanza " + description.getCurrentRoom().getId());
                    
                    // DEBUG aggiuntivo: controlla se ci sono casse che dovrebbero essere qui secondo il database
                    GameLoader gameLoader = getGameLoader(description);
                    if (gameLoader != null) {
                        System.out.println("🔍 DEBUG: Verifica presenza casse nel database per stanza " + description.getCurrentRoom().getId());
                        // Potresti aggiungere qui un metodo per verificare il database se necessario
                    }
                    
                    msg.append("Non c'è nessuna cassa da aprire qui.");
                }
            } else {
                // Gestione oggetti nella stanza
                if (parserOutput.getObject() != null) {
                    GameObjects obj = parserOutput.getObject();

                    // Gestione casse (id >= 100)
                    if (obj.getId() >= 100) {
                        return openChestFromDB(description, obj);
                    } else if (obj.isOpenable() && !obj.isOpen()) {
                        if (obj instanceof ContainerObj containerObj) {
                            msg.append("Hai aperto: ").append(obj.getName());
                            if (!containerObj.getList().isEmpty()) {
                                msg.append("\n").append(containerObj.getName()).append(" contiene:");
                                Iterator<Object> it = containerObj.getList().iterator();
                                while (it.hasNext()) {
                                    GameObjects next = (GameObjects) it.next();
                                    description.getCurrentRoom().getObjects().add(next);
                                    msg.append(" ").append(next.getName());
                                    it.remove();
                                }
                                msg.append(".");
                            } else {
                                msg.append(" ma è vuota.");
                            }
                            obj.setOpen(true);
                        } else {
                            msg.append("Hai aperto: ").append(obj.getName());
                            obj.setOpen(true);
                        }
                    } else if (obj.isOpen()) {
                        msg.append("È già aperto.");
                    } else {
                        msg.append("Non puoi aprire questo oggetto.");
                    }
                }

                // Gestione oggetti nell'inventario
                if (parserOutput.getInvObject() != null) {
                    GameObjects invObj = parserOutput.getInvObject();

                    if (invObj.isOpenable() && !invObj.isOpen()) {
                        if (invObj instanceof ContainerObj containerObj) {
                            msg.append("Hai aperto nel tuo inventario: ").append(invObj.getName());
                            if (!containerObj.getList().isEmpty()) {
                                msg.append("\n").append(containerObj.getName()).append(" contiene:");
                                Iterator<Object> it = containerObj.getList().iterator();
                                while (it.hasNext()) {
                                    GameObjects next = (GameObjects) it.next();
                                    description.getInventory().add(next);
                                    msg.append(" ").append(next.getName());
                                    it.remove();
                                }
                                msg.append(".");
                            }
                            invObj.setOpen(true);
                        } else {
                            msg.append("Hai aperto nel tuo inventario: ").append(invObj.getName());
                            invObj.setOpen(true);
                        }
                    } else if (invObj.isOpen()) {
                        msg.append("È già aperto.");
                    } else {
                        msg.append("Non puoi aprire questo oggetto.");
                    }
                }
            }
        }
        return msg.toString();
    }

    /**
     * Cerca casse in base a ID e nome
     */
    private GameObjects findChestInRoom(List<GameObjects> roomObjects) {
        for (GameObjects obj : roomObjects) {
            // Cerca per ID (casse hanno ID >= 100 e <= 103)
            if (obj.getId() >= 100 && obj.getId() <= 103) {
                System.out.println("✅ DEBUG: Trovata cassa per ID: " + obj.getId());
                return obj;
            }
            
            // Cerca per nome (come fallback)
            if (obj.getName().toLowerCase().contains("cassa") && obj.isOpenable()) {
                System.out.println("✅ DEBUG: Trovata cassa per nome: " + obj.getName());
                return obj;
            }
        }
        return null;
    }

    /**
     * Apre una cassa usando il GameLoader per caricare il contenuto dal DB
     *
     * @param description
     * @param cassa
     * @return messaggio di apertura
     */
    private String openChestFromDB(GameDescription description, GameObjects cassa) {
        StringBuilder msg = new StringBuilder();

        System.out.println("🔍 Tentativo apertura cassa " + cassa.getId() + " nella stanza " + description.getCurrentRoom().getId());

        if (!cassa.isOpenable()) {
            msg.append("Questa cassa non può essere aperta.");
            return msg.toString();
        }

        GameLoader gameLoader = getGameLoader(description);
        if (gameLoader == null) {
            msg.append("Errore nel sistema del gioco.");
            return msg.toString();
        }

        // *** USA IL NUOVO METODO PER CONTROLLARE LO STATO NEL DATABASE ***
        if (gameLoader.isChestOpenInDatabase(cassa.getId())) {
            msg.append("La cassa è già stata aperta.");
            return msg.toString();
        }

        msg.append("Hai aperto la cassa!\n");

        // Usa il GameLoader per aprire la cassa
        List<GameObjects> foundObjects = gameLoader.openChest(cassa.getId(), description.getCurrentRoom());

        if (foundObjects.isEmpty()) {
            msg.append("La cassa è vuota.");
        } else {
            msg.append("Dentro trovi:");
            for (GameObjects obj : foundObjects) {
                msg.append(" ").append(obj.getName());
            }
            msg.append("!");
        }

        // Aggiorna lo stato della cassa
        cassa.setOpen(true);
        gameLoader.markChestAsOpened(cassa.getId());

        return msg.toString();
    }

    /**
     * Ottiene il GameLoader dall'istanza del gioco
     *
     * @param description
     * @return GameLoader o null se non trovato
     */
    private GameLoader getGameLoader(GameDescription description) {
        // Se la GameDescription è un'istanza di TBMGame, ottieni il GameLoader
        if (description instanceof com.mycompany.theblackmountain.impl.TBMGame) {
            com.mycompany.theblackmountain.impl.TBMGame game
                    = (com.mycompany.theblackmountain.impl.TBMGame) description;
            return game.getGameLoader();
        }
        return null;
    }
}