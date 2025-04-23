/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.ContainerObj;
import com.mycompany.theblackmountain.type.Command;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.GameObserver;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vince
 */
public class TBMGame extends GameDescription implements GameObservable{

    private final List<GameObserver> observer = new ArrayList<>();

    private ParserOutput parserOutput;

    private final List<String> messages = new ArrayList<>();

    /**
     *
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        messages.clear();
        
        //Commands
        Command nord = new Command(CommandType.NORD, "nord");
        nord.setAlias(new String[]{"n", "N", "Nord", "NORD"});
        getCommands().add(nord);
        Command iventory = new Command(CommandType.INVENTORY, "inventario");
        iventory.setAlias(new String[]{"inv"});
        getCommands().add(iventory);
        Command sud = new Command(CommandType.SOUTH, "sud");
        sud.setAlias(new String[]{"s", "S", "Sud", "SUD"});
        getCommands().add(sud);
        Command est = new Command(CommandType.EAST, "est");
        est.setAlias(new String[]{"e", "E", "Est", "EST"});
        getCommands().add(est);
        Command ovest = new Command(CommandType.WEST, "ovest");
        ovest.setAlias(new String[]{"o", "O", "Ovest", "OVEST"});
        getCommands().add(ovest);
        Command end = new Command(CommandType.END, "end");
        end.setAlias(new String[]{"end", "fine", "esci", "muori", "ammazzati", "ucciditi", "suicidati", "exit", "basta"});
        getCommands().add(end);
        Command look = new Command(CommandType.LOOK_AT, "osserva");
        look.setAlias(new String[]{"guarda", "vedi", "trova", "cerca", "descrivi"});
        getCommands().add(look);
        Command pickup = new Command(CommandType.PICK_UP, "raccogli");
        pickup.setAlias(new String[]{"prendi"});
        getCommands().add(pickup);
        Command open = new Command(CommandType.OPEN, "apri");
        open.setAlias(new String[]{});
        getCommands().add(open);
        Command push = new Command(CommandType.PUSH, "premi");
        push.setAlias(new String[]{"spingi", "attiva"});
        getCommands().add(push);
        Command use = new Command(CommandType.USE, "usa");
        use.setAlias(new String[]{"utilizza", "combina"});
        getCommands().add(use);
        
        //Rooms
        Room hall = new Room(0, "Corridoio", "Sei nel corridoio della vecchia casa.\nOrmai non abiti più qui da anni!\nTi ricorderai come raggiungere le innumerevoli stanze?");
        hall.setLook("Sei nel corridoio, a nord vedi il bagno, a sud il soggiorno e ad ovest la tua cameretta.\nForse il gioco sarà lì?");
        Room livingRoom = new Room(1, "Soggiorno", "Ti trovi nel soggiorno.\nCi sono quei mobili marrone scuro che hai sempre odiato e delle orribili sedie.");
        livingRoom.setLook("Non c'è nulla di interessante qui.");
        Room kitchen = new Room(2, "Cucina", "Ti trovi nella solita cucina.\nMobili bianchi, maniglie azzurre, quello strano lampadario che adoravi tanto quando eri piccolo.\n"
                + "C'è un tavolo con un bel portafrutta e una finestra.");
        kitchen.setLook("La solita cucina, ma noti una chiave vicino al portafrutta.");
        Room bathroom = new Room(3, "Bagno", "Sei nel bagno.\nQuanto tempo passato qui dentro...meglio non pensarci...");
        bathroom.setLook("Vedo delle batterie sul mobile alla destra del lavandino.");
        Room yourRoom = new Room(4, "La tua cameratta", "Finalmente la tua cameretta!\nQuesto luogo ti è così famigliare...ma non ricordi dove hai messo il nuovo regalo di zia Lina.");
        yourRoom.setLook("C'è un armadio bianco, di solito ci conservi i tuoi giochi.");
        
        //map
        kitchen.setEast(livingRoom);
        livingRoom.setNorth(hall);
        livingRoom.setWest(kitchen);
        hall.setSouth(livingRoom);
        hall.setWest(yourRoom);
        hall.setNorth(bathroom);
        bathroom.setSouth(hall);
        yourRoom.setEast(hall);
        getRooms().add(kitchen);
        getRooms().add(livingRoom);
        getRooms().add(hall);
        getRooms().add(bathroom);
        getRooms().add(yourRoom);
        
        //obejcts
        Objects battery = new Objects(1, "batteria", "Un pacco di batterie, chissà se sono cariche.");
        battery.setAlias(new String[]{"batterie", "pile", "pila"});
        bathroom.getObjects().add(battery);
        ContainerObj wardrobe = new ContainerObj(2, "armadio", "Un semplice armadio.");
        wardrobe.setAlias(new String[]{"guardaroba", "vestiario"});
        wardrobe.setOpenable(false);
        wardrobe.setPickupable(false);
        wardrobe.setOpen(false);
        yourRoom.getObjects().add(wardrobe);
        
        
        //Observer
        GameObserver Move = new Move();
        this.attach(Move);
        GameObserver OpenInventory = new OpenInventory();
        this.attach(OpenInventory);
        GameObserver LookAt = new LookAt();
        this.attach(LookAt);
        GameObserver PickUp = new PickUp();
        this.attach(PickUp);
        GameObserver Open = new Open();
        this.attach(Open);
        GameObserver Use = new Use();
        this.attach(Use);
        
        //set starting room
        setCurrentRoom(hall);
    }

    /**
     *
     * @param p
     * @param out
     */
    @Override
    public void nextMove(ParserOutput p, PrintStream out) {
        parserOutput = p;
        messages.clear();
        if (p.getCommand() == null) {
            out.println("Non ho capito cosa devo fare! Prova con un altro comando.");
        } else {
            Room cr = getCurrentRoom();
            notifyObservers();
            boolean move = !cr.equals(getCurrentRoom()) && getCurrentRoom() != null;
            if (!messages.isEmpty()) {
                for (String m : messages) {
                    if (m.length() > 0) {
                        out.println(m);
                    }
                }
            }
            if (move) {
                out.println(getCurrentRoom().getName());
                out.println("================================================");
                out.println(getCurrentRoom().getDescription());
            }
        }
    }

    /**
     *
     * @param o
     */
    @Override
    public void attach(GameObserver o) {
        if (!observer.contains(o)) {
            observer.add(o);
        }
    }

    /**
     *
     * @param o
     */
    @Override
    public void detach(GameObserver o) {
        observer.remove(o);
    }

    /**
     *
     */
    @Override
    public void notifyObservers() {
        for (GameObserver o : observer) {
            messages.add(o.update(this, parserOutput));
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String getWelcomeMsg() {
        return """
               Sei appena tornato a casa e non sai cosa fare.
               Ti ricordi che non hai ancora utilizzato quel fantastico regalo di tua zia Lina.
               Sar\u00e0 il caso di cercarlo e di giocarci!""";
    }
    
    public void cleanup() {
    }
}