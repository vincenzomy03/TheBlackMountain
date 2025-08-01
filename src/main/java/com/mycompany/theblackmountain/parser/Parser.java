/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.parser;

import com.mycompany.theblackmountain.Utils;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Command;
import java.util.List;
import java.util.Set;

public class Parser {

    private final Set<String> stopwords;

    public Parser(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    private int checkForCommand(String token, List<Command> commands) {
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).getName().equals(token) || 
                (commands.get(i).getAlias() != null && commands.get(i).getAlias().contains(token))) {
                return i;
            }
        }
        return -1;
    }

    private int checkForObject(String token, List<Objects> objects) {
        for (int i = 0; i < objects.size(); i++) {
            // Controlla nome esatto
            if (objects.get(i).getName().equals(token)) {
                return i;
            }
            // Controlla alias se esistono
            if (objects.get(i).getAlias() != null && objects.get(i).getAlias().contains(token)) {
                return i;
            }
            // Controlla se il nome dell'oggetto contiene il token (per matching parziale)
            if (objects.get(i).getName().toLowerCase().contains(token.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    public ParserOutput parse(String command, List<Command> commands, List<Objects> objects, List<Objects> inventory) {
        List<String> tokens = Utils.parseString(command, stopwords);

        if (!tokens.isEmpty()) {
            int ic = checkForCommand(tokens.get(0), commands);

            if (ic > -1) {
                Command cmd = commands.get(ic);

                if (tokens.size() > 1) {
                    // Prima cerca negli oggetti della stanza
                    int io = -1;
                    int ioinv = -1;
                    
                    // Cerca negli oggetti della stanza e nell'inventario
                    for (int i = 1; i < tokens.size(); i++) {
                        if (io < 0) {
                            io = checkForObject(tokens.get(i), objects);
                        }
                        if (ioinv < 0) {
                            ioinv = checkForObject(tokens.get(i), inventory);
                        }
                    }

                    // Gestione speciale per comando "usa"
                    if (cmd.getName().equalsIgnoreCase("usa")) {
                        // Priorità all'inventario per il comando usa
                        if (ioinv > -1) {
                            return new ParserOutput(cmd, null, inventory.get(ioinv));
                        }
                        // Se non trovato nell'inventario, cerca nella stanza
                        if (io > -1) {
                            return new ParserOutput(cmd, objects.get(io), null);
                        }
                        
                        // Se non trovato da nessuna parte ma c'è un secondo token, 
                        // crea un comando con il nome del token per gestioni speciali
                        if (tokens.size() > 1) {
                            Command specialCmd = new Command(cmd.getType(), cmd.getName() + " " + tokens.get(1));
                            return new ParserOutput(specialCmd, null, null);
                        }
                        
                        return new ParserOutput(cmd, null, null);
                    }

                    // Per altri comandi, restituisci entrambi se trovati
                    if (io > -1 && ioinv > -1) {
                        return new ParserOutput(cmd, objects.get(io), inventory.get(ioinv));
                    } else if (io > -1) {
                        return new ParserOutput(cmd, objects.get(io), null);
                    } else if (ioinv > -1) {
                        return new ParserOutput(cmd, null, inventory.get(ioinv));
                    } else {
                        return new ParserOutput(cmd, null, null);
                    }
                } else {
                    return new ParserOutput(cmd, null, null);
                }
            } else {
                return new ParserOutput(null, null, null);
            }
        } else {
            return null;
        }
    }
}