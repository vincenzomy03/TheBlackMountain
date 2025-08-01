/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.GameObserver;

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
            if (parserOutput.getObject() != null) {
                if (parserOutput.getObject().isPickupable()) {
                    description.getInventory().add(parserOutput.getObject());
                    description.getCurrentRoom().getObjects().remove(parserOutput.getObject());
                    msg.append("Hai raccolto: ").append(parserOutput.getObject().getName());
                    
                    // Aggiorna la descrizione della stanza dopo aver raccolto oggetti specifici
                    updateRoomDescription(description, parserOutput.getObject().getId());
                    
                } else {
                    msg.append("Non puoi raccogliere questo oggetto.");
                }
            } else {
                msg.append("Non c'è niente da raccogliere qui.");
            }
        }
        return msg.toString();
    }
    
    /**
     * Aggiorna la descrizione della stanza dopo aver raccolto oggetti specifici
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