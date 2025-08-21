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
import com.mycompany.theblackmountain.type.GameCharacter;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author vince
 */
public class Open extends GameObserver {

    private static boolean princessFreed = false;

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        StringBuilder msg = new StringBuilder();

        // CONTROLLO DEBUG INIZIALE
        System.out.println("DEBUG Open: Comando ricevuto - " + parserOutput.getCommand().getName());
        System.out.println("DEBUG Open: Tipo comando - " + parserOutput.getCommand().getType());
        System.out.println("DEBUG Open: Oggetto - " + (parserOutput.getObject() != null ? parserOutput.getObject().getName() : "null"));
        System.out.println("DEBUG Open: InvObject - " + (parserOutput.getInvObject() != null ? parserOutput.getInvObject().getName() : "null"));

        // COMANDO SPECIALE: libera principessa
        if (parserOutput.getCommand().getName().toLowerCase().contains("libera")) {
            System.out.println("DEBUG Open: Comando libera principessa ricevuto");

            // Controlla se siamo nella stanza del boss (ID 7)
            if (description.getCurrentRoom().getId() != 7) {
                return "Non c'e' nessuna principessa da liberare qui.";
            }

            // Finto controllo della chiave (sappiamo che ce l'ha)
            boolean hasKey = false;
            for (GameObjects obj : description.getInventory()) {
                if (obj.getId() == 10 || obj.getName().toLowerCase().contains("chiave")) {
                    hasKey = true;
                    break;
                }
            }

            if (!hasKey) {
                return "Ti serve una chiave per liberare la principessa.";
            }

            // Libera la principessa
            TBMGame game = (TBMGame) description;
            game.setPrincessFreed(true);
            game.onPrincessFreed();

            return "Usi la chiave per aprire la cella!\n\n"
                    + "\"Grazie, coraggioso eroe!\" esclama la principessa con voce tremula.\n"
                    + "\"Mi hai salvata da questo incubo! Ora dobbiamo fuggire insieme!\n"
                    + "L'uscita dovrebbe essere a EST da qui. Andiamo, presto!\"\n\n"
                    + "La principessa e' finalmente libera! L'uscita ti aspetta a EST.";
        }

        if (parserOutput.getCommand().getType() == CommandType.OPEN) {
            System.out.println("DEBUG Open: Comando OPEN confermato, procedo...");

            // MODIFICA CRITICA: Controlla sempre se si tratta di una cassa
            String commandText = parserOutput.getCommand().getName().toLowerCase();
            boolean isChestCommand = commandText.contains("cassa") || commandText.contains("cella") || commandText.equals("apri");

            System.out.println("DEBUG Open: Comando text: '" + commandText + "', isChestCommand: " + isChestCommand);

            // CASO 1: Oggetto specifico trovato dal parser
            if (parserOutput.getObject() != null) {
                GameObjects obj = parserOutput.getObject();
                System.out.println("DEBUG Open: Oggetto specifico trovato - " + obj.getName() + " (ID: " + obj.getId() + ")");

                // Gestione cella della principessa (ID 13)
                if (obj.getId() == 13) {
                    TBMGame game = (TBMGame) description;

                    // 1. Controlla se il boss (id 7) è ancora vivo nella stanza 7
                    boolean bossDefeated = true;
                    if (game.getCurrentRoom() != null && game.getCurrentRoom().getId() == 7) {
                        for (GameCharacter enemy : game.getCurrentRoom().getEnemies()) {
                            if (enemy.getId() == 7 && enemy.isAlive()) {
                                bossDefeated = false;
                                break;
                            }
                        }
                    }

                    if (!bossDefeated) {
                        return "La cella è sigillata magicamente. Devi prima sconfiggere il guardiano.";
                    }

                    // 2. Controlla se il giocatore ha la chiave della principessa (id 10)
                    GameObjects princessKey = null;
                    for (GameObjects invObj : game.getInventory()) {
                        if (invObj.getId() == 10) {
                            princessKey = invObj;
                            break;
                        }
                    }

                    if (princessKey == null) {
                        return "La cella è chiusa a chiave. Ti serve la chiave della principessa.";
                    }

                    // 3. Apri la cella
                    if (!obj.isOpen()) {
                        obj.setOpen(true);
                        game.updateObjectState(obj);

                        // Rimuovi la chiave dall'inventario
                        game.getInventory().remove(princessKey);
                        game.destroyObject(princessKey);

                        // Imposta flag
                        princessFreed = true;

                        // Notifica il gioco
                        game.onPrincessFreed();

                        return "Apri la cella della principessa!\n\n"
                                + "\"Grazie, coraggioso eroe! Mi hai salvata dalle grinfie di questi mostri!\n"
                                + "Ora dobbiamo fuggire insieme da questo luogo maledetto!\n"
                                + "L'uscita dovrebbe essere a EST da qui. Andiamo, presto!\"\n\n"
                                + "La principessa è libera! L'uscita finale ti aspetta a EST.";
                    } else {
                        return "La cella è già aperta e la principessa è libera.";
                    }
                } // Gestione porta est (ID 15)
                else if (obj.getId() == 15) {
                    TBMGame game = (TBMGame) description;

                    // Cerca la chiave del boss (ID 11) nell'inventario
                    GameObjects bossKey = null;
                    for (GameObjects invObj : game.getInventory()) {
                        if (invObj.getId() == 11) {
                            bossKey = invObj;
                            break;
                        }
                    }

                    if (bossKey == null) {
                        return "La porta e' sigillata. Ti serve la chiave del guardiano.";
                    }

                    if (!princessFreed) {
                        return "Non puoi andartene senza salvare la principessa!";
                    }

                    if (!obj.isOpen()) {
                        obj.setOpen(true);
                        game.updateObjectState(obj);

                        // Rimuovi la chiave
                        game.getInventory().remove(bossKey);
                        game.destroyObject(bossKey);

                        return "Usi la chiave del demone!\n\n"
                                + "La porta si apre! L'uscita e' libera!\n\n"
                                + "Ora puoi andare a EST per uscire dalla montagna!";
                    } else {
                        return "La porta e' gia' aperta. Vai a EST per uscire!";
                    }
                } // Gestione casse (id 100-103)
                else if (obj.getId() >= 100 && obj.getId() <= 103) {
                    System.out.println("DEBUG Open: È una cassa, procedo con apertura DB");
                    return openChestFromDB(description, obj);
                } // Gestione oggetti normali apribili
                else if (obj.isOpenable() && !obj.isOpen()) {
                    return handleNormalObject(description, obj, msg);
                } else if (obj.isOpen()) {
                    msg.append("E' gia' aperto.");
                } else {
                    msg.append("Non puoi aprire questo oggetto.");
                }
            } // CASO 2: Oggetto nell'inventario - MA solo se non è una chiave per aprire qualcos'altro
            else if (parserOutput.getInvObject() != null) {
                GameObjects invObj = parserOutput.getInvObject();
                System.out.println("DEBUG Open: Oggetto inventario trovato - " + invObj.getName());

                // Se è una chiave, cerca cosa aprire nella stanza invece di aprire la chiave stessa
                if (invObj.getName().toLowerCase().contains("chiave")) {
                    System.out.println("DEBUG Open: È una chiave, cerco cosa aprire nella stanza");
                    return handleChestSearch(description);
                }

                return handleInventoryObject(description, invObj, msg);
            } // CASO 3: Nessun oggetto specifico - cerca cassa nella stanza
            else if (isChestCommand) {
                System.out.println("DEBUG Open: Nessun oggetto specifico, cerco cassa nella stanza");
                return handleChestSearch(description);
            } // CASO 4: Comando non riconosciuto
            else {
                System.out.println("DEBUG Open: Comando non gestibile");
                msg.append("Cosa vuoi aprire?");
            }
        } else {
            System.out.println("DEBUG Open: Non è un comando OPEN, ignoro");
            return ""; // Non è un comando OPEN, non fare nulla
        }

        System.out.println("DEBUG Open: Messaggio finale: '" + msg.toString() + "'");
        return msg.toString();
    }

    /**
     * Gestisce l'apertura di oggetti normali
     */
    private String handleNormalObject(GameDescription description, GameObjects obj, StringBuilder msg) {
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
                msg.append(" ma e' vuota.");
            }
            obj.setOpen(true);
        } else {
            msg.append("Hai aperto: ").append(obj.getName());
            obj.setOpen(true);
        }
        return msg.toString();
    }

    /**
     * Gestisce l'apertura di oggetti nell'inventario
     */
    private String handleInventoryObject(GameDescription description, GameObjects invObj, StringBuilder msg) {
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
            msg.append("E' gia' aperto.");
        } else {
            msg.append("Non puoi aprire questo oggetto.");
        }
        return msg.toString();
    }

    /**
     * Gestisce la ricerca e apertura di casse nella stanza
     */
    private String handleChestSearch(GameDescription description) {
        System.out.println("DEBUG Open: Ricerca oggetto apribile nella stanza " + description.getCurrentRoom().getId());

        // Cerca casse, celle, porte nella stanza corrente
        GameObjects openableObj = findOpenableObjectInRoom(description.getCurrentRoom().getObjects());

        if (openableObj != null) {
            System.out.println("DEBUG Open: Trovato oggetto apribile: " + openableObj.getName());


            // Gestione speciale per porta est
            if (openableObj.getName().toLowerCase().contains("porta est")) {
                return openExitDoor(description, openableObj);
            }

            // Gestione casse normali
            if (openableObj.getId() >= 100 && openableObj.getId() <= 103) {
                return openChestFromDB(description, openableObj);
            }
        }

        return "Non c'e' niente da aprire qui.";
    }

    // Nuovo metodo per trovare oggetti apribili generici
    private GameObjects findOpenableObjectInRoom(List<GameObjects> roomObjects) {
        for (GameObjects obj : roomObjects) {
            if (obj.isOpenable()) {
                return obj;
            }
        }
        return null;
    }

    // Nuovo metodo per aprire la porta finale
    private String openExitDoor(GameDescription description, GameObjects porta) {
        System.out.println("DEBUG Open: Tentativo apertura porta finale");

        if (porta.isOpen()) {
            return "La porta e' gia' aperta. La via d'uscita ti aspetta!";
        }

        // Controlla se il boss è stato sconfitto e se c'è la chiave del boss
        boolean hasBossKey = false;
        for (GameObjects obj : description.getInventory()) {
            if (obj.getName().toLowerCase().contains("chiave del collo del boss")
                    || obj.getName().toLowerCase().contains("chiave boss")) {
                hasBossKey = true;
                break;
            }
        }

        if (!hasBossKey) {
            return "La porta e' sigillata. Devi sconfiggere il guardiano di questa sala per ottenere la chiave.";
        }

        // Controlla se la principessa è stata liberata
        boolean princessFreedLocal = false;
        for (GameObjects obj : description.getCurrentRoom().getObjects()) {
            if (obj.getName().toLowerCase().contains("principessa")) {
                princessFreedLocal = true;
                break;
            }
        }

        if (!princessFreedLocal) {
            return "Non puoi andartene senza aver prima liberato la principessa!";
        }

        // Apri la porta finale
        porta.setOpen(true);

        // Aggiorna il database
        GameLoader gameLoader = getGameLoader(description);
        if (gameLoader != null) {
            gameLoader.updateObjectState(porta);
        }

        return "Hai aperto la porta finale! Tu e la principessa potete finalmente fuggire dalla Montagna Nera!\n"
                + "Vai ad EST per uscire e completare la tua missione!";
    }

    /**
     * Cerca casse in base a ID e nome - VERSIONE MIGLIORATA
     */
    private GameObjects findChestInRoom(List<GameObjects> roomObjects) {
        System.out.println("DEBUG Open: Ricerca cassa tra " + roomObjects.size() + " oggetti");

        for (GameObjects obj : roomObjects) {
            System.out.println("DEBUG Open: Controllo oggetto ID " + obj.getId() + " (" + obj.getName() + ")");

            // Cerca per ID (casse hanno ID >= 100 e <= 103)
            if (obj.getId() >= 100 && obj.getId() <= 103) {
                System.out.println("DEBUG Open: Trovata cassa per ID: " + obj.getId() + " (" + obj.getName() + ")");
                return obj;
            }

            // Cerca per nome (come fallback) - controllo più permissivo
            String objName = obj.getName().toLowerCase();
            if ((objName.contains("cassa") || objName.equals("chest")) && obj.isOpenable()) {
                System.out.println("DEBUG Open: Trovata cassa per nome: " + obj.getName() + " (ID: " + obj.getId() + ")");
                return obj;
            }
        }

        System.out.println("DEBUG Open: Nessuna cassa trovata tra gli oggetti della stanza");
        return null;
    }

    /**
     * Apre una cassa usando il GameLoader - VERSIONE CORRETTA
     */
    private String openChestFromDB(GameDescription description, GameObjects cassa) {
        StringBuilder msg = new StringBuilder();

        System.out.println("DEBUG Open: Tentativo apertura cassa " + cassa.getId() + " (" + cassa.getName() + ") nella stanza " + description.getCurrentRoom().getId());

        if (!cassa.isOpenable()) {
            msg.append("Questa cassa non puo' essere aperta.");
            System.out.println("DEBUG Open: Cassa " + cassa.getId() + " non è apribile");
            return msg.toString();
        }

        GameLoader gameLoader = getGameLoader(description);
        if (gameLoader == null) {
            msg.append("Errore nel sistema del gioco.");
            System.out.println("DEBUG Open: GameLoader non disponibile");
            return msg.toString();
        }

        // CORREZIONE CRITICA: Rimuovi prima eventuali oggetti che dovrebbero essere DENTRO la cassa
        List<Integer> expectedContents = getExpectedChestContents(cassa.getId());
        if (expectedContents != null && !expectedContents.isEmpty()) {
            System.out.println("DEBUG Open: Controllo contenuti che dovrebbero essere nella cassa " + cassa.getId());

            // Rimuovi dalla stanza gli oggetti che dovrebbero essere nella cassa
            Iterator<GameObjects> roomObjIterator = description.getCurrentRoom().getObjects().iterator();
            while (roomObjIterator.hasNext()) {
                GameObjects roomObj = roomObjIterator.next();
                if (expectedContents.contains(roomObj.getId())) {
                    System.out.println("DEBUG Open: Rimuovo " + roomObj.getName() + " dalla stanza (dovrebbe essere nella cassa)");
                    roomObjIterator.remove();
                }
            }

            // IMPORTANTE: Rimuovi anche dal database ROOM_OBJECTS
            for (Integer contentId : expectedContents) {
                gameLoader.removeObjectFromRoom(contentId, description.getCurrentRoom().getId());
            }
        }

        // Controlla se la cassa è già stata aperta
        if (gameLoader.isChestOpenInDatabase(cassa.getId())) {
            msg.append("La cassa e' gia' stata aperta.");
            System.out.println("DEBUG Open: Cassa " + cassa.getId() + " già aperta nel database");
            return msg.toString();
        }

        System.out.println("DEBUG Open: Procedo con apertura cassa " + cassa.getId());
        msg.append("Hai aperto la cassa!\n");

        // Usa il GameLoader per aprire la cassa
        List<GameObjects> foundObjects = gameLoader.openChest(cassa.getId(), description.getCurrentRoom());

        if (foundObjects.isEmpty()) {
            msg.append("La cassa e' vuota.");
            System.out.println("DEBUG Open: Cassa " + cassa.getId() + " risulta vuota dopo apertura");
        } else {
            msg.append("Dentro trovi:");
            System.out.println("DEBUG Open: Trovati " + foundObjects.size() + " oggetti nella cassa " + cassa.getId() + ":");

            for (GameObjects obj : foundObjects) {
                msg.append(" ").append(obj.getName());
                System.out.println("  - " + obj.getName() + " (ID: " + obj.getId() + ")");
            }
            msg.append("!");
        }

        // Aggiorna lo stato della cassa
        cassa.setOpen(true);
        gameLoader.markChestAsOpened(cassa.getId());
        gameLoader.updateObjectState(cassa);

        System.out.println("DEBUG Open: Apertura cassa " + cassa.getId() + " completata con successo");
        return msg.toString();
    }

    /**
     * Restituisce i contenuti attesi di una cassa
     */
    private List<Integer> getExpectedChestContents(int chestId) {
        return switch (chestId) {
            case 100 ->
                List.of(1, 2); // chiave ingresso, pozione cura
            case 101 ->
                List.of(5, 6); // pozione cura totale, bastone
            case 102 ->
                List.of(8, 9); // libro incantesimo fuoco, veleno
            case 103 ->
                List.of(10);   // chiave cella principessa
            default ->
                null;
        };
    }

    public void resetForNewGame() {
        princessFreed = false;
        System.out.println("DEBUG: Stati Open observer resettati");
    }

    /**
     * Ottiene il GameLoader dall'istanza del gioco
     */
    private GameLoader getGameLoader(GameDescription description) {
        if (description instanceof com.mycompany.theblackmountain.impl.TBMGame) {
            com.mycompany.theblackmountain.impl.TBMGame game
                    = (com.mycompany.theblackmountain.impl.TBMGame) description;
            return game.getGameLoader();
        }
        return null;
    }
}
