/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.GameObserver;
import java.util.Iterator;

/**
 *
 * @author vince
 */
public class Open extends GameObserver{
   
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
            /*ATTENZIONE: quando un oggetto contenitore viene aperto, tutti gli oggetti contenuti
                * vengongo inseriti nella stanza o nell'inventario a seconda di dove si trova l'oggetto contenitore.
                * Potrebbe non esssere la soluzione ottimale.
             */
            if (parserOutput.getObject() == null && parserOutput.getInvObject() == null) {
                msg.append("Non c'è niente da aprire qui.");
            } else {
                if (parserOutput.getObject() != null) {
                    if (parserOutput.getObject().isOpenable() && parserOutput.getObject().isOpen() == false) {
                        if (parserOutput.getObject() instanceof ContainerObj containerObj) {
                            msg.append("Hai aperto: ").append(parserOutput.getObject().getName());
                            ContainerObj c = containerObj;
                            if (!c.getList().isEmpty()) {
                                msg.append(c.getName()).append(" contiene:");
                                Iterator<Object> it = c.getList().iterator();
                                while (it.hasNext()) {
                                    Objects next = (Objects) it.next();
                                    description.getCurrentRoom().getObjects().add(next);
                                    msg.append(" ").append(next.getName());
                                    it.remove();
                                }
                                msg.append("\n");
                            }
                            parserOutput.getObject().setOpen(true);
                        } else {
                            msg.append("Hai aperto: ").append(parserOutput.getObject().getName());
                            parserOutput.getObject().setOpen(true);
                        }
                    } else {
                        msg.append("Non puoi aprire questo oggetto.");
                    }
                }
                if (parserOutput.getInvObject() != null) {
                    if (parserOutput.getInvObject().isOpenable() && parserOutput.getInvObject().isOpen() == false) {
                        if (parserOutput.getInvObject() instanceof ContainerObj c) {
                            if (!c.getList().isEmpty()) {
                                msg.append(c.getName()).append(" contiene:");
                                Iterator<Object> it = c.getList().iterator();
                                while (it.hasNext()) {
                                    Objects next = (Objects) it.next();
                                    description.getInventory().add(next);
                                    msg.append(" ").append(next.getName());
                                    it.remove();
                                }
                                msg.append("\n");
                            }
                            parserOutput.getInvObject().setOpen(true);
                        } else {
                            parserOutput.getInvObject().setOpen(true);
                        }
                        msg.append("Hai aperto nel tuo inventario: ").append(parserOutput.getInvObject().getName());
                    } else {
                        msg.append("Non puoi aprire questo oggetto.");
                    }
                }
            }
        }
        return msg.toString();
    }

}