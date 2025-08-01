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
            if (parserOutput.getObject() == null && parserOutput.getInvObject() == null) {
                msg.append("Non c'è niente da aprire qui.");
            } else {
                // Gestione oggetti nella stanza
                if (parserOutput.getObject() != null) {
                    Objects obj = parserOutput.getObject();
                    
                    // Gestione casse (id 100)
                    if (obj.getId() == 100) {
                        if (obj.isOpenable() && !obj.isOpen()) {
                            if (obj instanceof ContainerObj containerObj) {
                                msg.append("Hai aperto: ").append(obj.getName());
                                if (!containerObj.getList().isEmpty()) {
                                    msg.append("\n").append(containerObj.getName()).append(" contiene:");
                                    Iterator<Object> it = containerObj.getList().iterator();
                                    while (it.hasNext()) {
                                        Objects next = (Objects) it.next();
                                        description.getCurrentRoom().getObjects().add(next);
                                        msg.append(" ").append(next.getName());
                                        it.remove();
                                    }
                                    msg.append(".");
                                } else {
                                    msg.append(" ma è vuota.");
                                }
                                obj.setOpen(true);
                            }
                        } else if (obj.isOpen()) {
                            msg.append("La cassa è già aperta.");
                        } else {
                            msg.append("Questa cassa non può essere aperta.");
                        }
                    } else if (obj.isOpenable() && !obj.isOpen()) {
                        msg.append("Hai aperto: ").append(obj.getName());
                        obj.setOpen(true);
                    } else if (obj.isOpen()) {
                        msg.append("È già aperto.");
                    } else {
                        msg.append("Non puoi aprire questo oggetto.");
                    }
                }
                
                // Gestione oggetti nell'inventario
                if (parserOutput.getInvObject() != null) {
                    Objects invObj = parserOutput.getInvObject();
                    
                    if (invObj.isOpenable() && !invObj.isOpen()) {
                        if (invObj instanceof ContainerObj containerObj) {
                            msg.append("Hai aperto nel tuo inventario: ").append(invObj.getName());
                            if (!containerObj.getList().isEmpty()) {
                                msg.append("\n").append(containerObj.getName()).append(" contiene:");
                                Iterator<Object> it = containerObj.getList().iterator();
                                while (it.hasNext()) {
                                    Objects next = (Objects) it.next();
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
}