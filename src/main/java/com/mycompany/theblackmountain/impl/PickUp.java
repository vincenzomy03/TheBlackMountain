/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.database.GameLoader;

/**
 *
 * @author vince
 */
public class PickUp extends GameObserver {

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();
        if (parserOutput.getCommand().getType() == CommandType.PICK_UP) {

            // DEBUG: Stampa oggetti disponibili nella stanza
            System.out.println("🔍 DEBUG PickUp - Oggetti nella stanza " + description.getCurrentRoom().getId() + ":");
            for (var obj : description.getCurrentRoom().getObjects()) {
                System.out.println("  - " + obj.getName() + " (ID: " + obj.getId() + ", Raccoglibile: " + obj.isPickupable() + ")");
            }

            if (parserOutput.getObject() != null) {
                if (parserOutput.getObject().isPickupable()) {

                    // *** PARTE CORRETTA: Aggiorna sia memoria che database ***
                    description.getInventory().add(parserOutput.getObject());
                    description.getCurrentRoom().getObjects().remove(parserOutput.getObject());

                    // *** NUOVO: Sincronizza con il database ***
                    GameLoader gameLoader = getGameLoader(description);
                    if (gameLoader != null) {
                        gameLoader.moveObjectToInventory(parserOutput.getObject(), 0); // 0 = ID giocatore
                        System.out.println("✅ DEBUG: Oggetto " + parserOutput.getObject().getName() + " spostato nel database");
                    } else {
                        System.out.println("⚠️ DEBUG: GameLoader non disponibile, sincronizzazione database saltata");
                    }

                    msg.append("Hai raccolto: ").append(parserOutput.getObject().getName());

                    // Aggiorna la descrizione della stanza dopo aver raccolto oggetti specifici
                    updateRoomDescription(description, parserOutput.getObject().getId());

                } else {
                    msg.append("Non puoi raccogliere questo oggetto.");
                }
            } else {
                // *** NUOVO: Gestione comando "raccogli stringhe" senza oggetto specifico ***
                String commandText = parserOutput.getCommand().getName().toLowerCase();
                if (commandText.contains("stringhe") || commandText.contains("ragnatela")) {
                    return handlePickupStrings(description);
                }

                msg.append("Non c'è niente da raccogliere qui.");
            }
        }
        return msg.toString();
    }

    /**
     * Gestisce il comando "raccogli stringhe" quando non viene trovato un
     * oggetto specifico
     */
    private String handlePickupStrings(GameDescription description) {
        System.out.println("🔍 DEBUG: Ricerca stringhe nella stanza " + description.getCurrentRoom().getId());

        // Cerca le stringhe di ragnatela (ID 4) nella stanza
        for (var obj : description.getCurrentRoom().getObjects()) {
            if (obj.getId() == 4 || obj.getName().toLowerCase().contains("stringhe")
                    || obj.getName().toLowerCase().contains("ragnatela")) {

                System.out.println("✅ DEBUG: Trovate stringhe: " + obj.getName());

                if (obj.isPickupable()) {
                    // Sposta l'oggetto
                    description.getInventory().add(obj);
                    description.getCurrentRoom().getObjects().remove(obj);

                    // Sincronizza database
                    GameLoader gameLoader = getGameLoader(description);
                    if (gameLoader != null) {
                        gameLoader.moveObjectToInventory(obj, 0);
                        System.out.println("✅ DEBUG: Stringhe spostate nel database");
                    }

                    updateRoomDescription(description, obj.getId());
                    return "Hai raccolto: " + obj.getName();
                } else {
                    return "Non puoi raccogliere le stringhe di ragnatela.";
                }
            }
        }

        System.out.println("❌ DEBUG: Stringhe non trovate nella stanza corrente");
        return "Non ci sono stringhe di ragnatela da raccogliere qui.";
    }

    /**
     * Ottiene il GameLoader dall'istanza del gioco
     */
    private GameLoader getGameLoader(GameDescription description) {
        if (description instanceof TBMGame) {
            TBMGame game = (TBMGame) description;
            return game.getGameLoader();
        }
        return null;
    }

    /**
     * Aggiorna la descrizione della stanza dopo aver raccolto oggetti specifici
     *
     * @param description
     * @param objectId
     */
    private void updateRoomDescription(GameDescription description, int objectId) {
        int roomId = description.getCurrentRoom().getId();

        // Aggiorna la descrizione look delle stanze quando oggetti vengono raccolti
        switch (roomId) {
            case 0: // Ingresso della Fortezza
                if (objectId == 1 || objectId == 2) { // chiave ingresso o pozione cura
                    description.getCurrentRoom().setLook("La cassa è ora vuota. Non c'è altro di interessante qui.");
                }
                break;
            case 1: // Stanza del Topo
                if (objectId == 4) { // stringhe di ragnatela
                    description.getCurrentRoom().setLook("Le ragnatele sono state saccheggiate. Ora la stanza sembra ancora più desolata.");
                }
                break;
            case 2: // Mensa Abbandonata
                description.getCurrentRoom().setLook("La mensa rimane un luogo di caos e distruzione.");
                break;
            case 3: // Dormitorio delle Guardie
                if (objectId == 5 || objectId == 6) { // pozione cura totale o bastone
                    description.getCurrentRoom().setLook("Il dormitorio è ora completamente vuoto di oggetti utili.");
                }
                break;
            case 4: // Sala delle Guardie
                if (objectId == 8 || objectId == 9) { // libro incantesimo fuoco o veleno
                    description.getCurrentRoom().setLook("I resti della barricata non nascondono più nulla di valore.");
                }
                break;
            case 6: // Stanza delle Torture
                description.getCurrentRoom().setLook("La stanza delle torture mantiene la sua atmosfera sinistra.");
                break;
        }
    }
}
