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
import com.mycompany.theblackmountain.type.Weapon;
import com.mycompany.theblackmountain.factory.WeaponFactory;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vince
 */
public class TBMGame extends GameDescription implements GameObservable {

    private final List<GameObserver> observer = new ArrayList<>();
    private ParserOutput parserOutput;
    private final List<String> messages = new ArrayList<>();
    private CombatSystem combatSystem;

    /**
     *
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        messages.clear();

        // Inizializza il combat system
        combatSystem = new CombatSystem(this);

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
        
        // Comando per iniziare il combattimento
        Command fight = new Command(CommandType.USE, "combatti");
        fight.setAlias(new String[]{"combattimento", "inizia combattimento", "battaglia"});
        getCommands().add(fight);
        
        // Comando per attaccare
        Command attack = new Command(CommandType.USE, "attacca");
        attack.setAlias(new String[]{"attacco", "colpisci", "fight"});
        getCommands().add(attack);

        //Rooms
        Room entrance = new Room(0, "Ingresso della Fortezza",
                "Ti trovi all'ingresso della fortezza maledetta.\nL'aria è densa di umidità e il pavimento è cosparso di muschio.\nUn enorme goblin dalla pelle verde scuro ti osserva con occhi colmi d'odio. Blocca il passaggio, pronto a colpirti.");
        entrance.setLook("Scorgi una vecchia cassa, probabilmente abbandonata dai precedenti avventurieri.\nDentro potrebbe esserci qualcosa di utile per iniziare il tuo cammino.");

        Room ratRoom = new Room(1, "Stanza del Topo",
                "Le pareti sono coperte di ragnatele e muffa.\nUn enorme topo, con denti giallastri e occhi rossi, grugnisce in un angolo buio.");
        ratRoom.setLook("Tra le ragnatele, potresti trovare qualcosa di utile o… disgustoso.");

        Room diningHall = new Room(2, "Mensa Abbandonata",
                "Le tavole di legno sono rovesciate, piatti infranti ovunque.\nDue goblin chiassosi stanno litigando per un osso ancora sanguinante. Si accorgono della tua presenza.");
        diningHall.setLook("Non sembra esserci nulla di valore qui, a parte i goblin pronti a combattere.");

        Room bedroom = new Room(3, "Dormitorio delle Guardie",
                "Letti rotti e coperte lacerate giacciono sparsi ovunque.\nIl silenzio regna sovrano: nessun nemico in vista, solo l'eco dei tuoi passi.");
        bedroom.setLook("Una cassa giace accanto a un letto distrutto. Dentro, potresti trovare qualcosa di prezioso.");

        Room guardRoom = new Room(4, "Sala delle Guardie",
                "I resti di un banchetto interrotto sono sparsi ovunque.\nUn goblin gigante impugna una clava insanguinata, affiancato da un goblin più minuto ma altrettanto minaccioso.");
        guardRoom.setLook("Tra i resti di una barricata, spunta una cassa semiaperta.");

        Room spellRoom = new Room(5, "Sala degli Incantesimi",
                "Le pareti sono incise con simboli magici che pulsano di luce blu.\nNel centro, un altare emette un suono basso e costante. Qui, puoi sacrificare parte della tua vita per ottenere un potente arco.");
        spellRoom.setLook("Un'iscrizione recita: 'Offri parte della tua essenza, e il tuo braccio sarà guidato da un arco etereo.'");

        Room tortureRoom = new Room(6, "Stanza delle Torture",
                "Catene arrugginite pendono dal soffitto. Il pavimento è macchiato di vecchio sangue secco.\nNon ci sono nemici… ma il luogo mette i brividi.");
        tortureRoom.setLook("Una cassa chiusa giace in un angolo, apparentemente dimenticata.");

        Room bossRoom = new Room(7, "Sala del Signore dei Goblin",
                "L'aria è irrespirabile. Un fumo denso copre il volto del cane demone, una creatura infernale con zanne fumanti e occhi di fuoco.\nProtegge la cella della principessa con feroce determinazione.");
        bossRoom.setLook("Sul piedistallo alle spalle del demone c'è una chiave che potrebbe aprire la cella.");

        Room exitRoom = new Room(8, "Uscita");

        // Mappa aggiornata con i nuovi nomi fantasy
        entrance.setEast(ratRoom);         // Ingresso → Stanza del Topo
        ratRoom.setWest(entrance);

        ratRoom.setEast(diningHall);       // Stanza del Topo → Mensa
        diningHall.setWest(ratRoom);

        diningHall.setEast(bedroom);       // Mensa → Dormitorio
        bedroom.setWest(diningHall);

        bedroom.setSouth(guardRoom);       // Dormitorio → Sala delle Guardie
        guardRoom.setNorth(bedroom);

        guardRoom.setWest(spellRoom);      // Sala delle Guardie → Sala degli Incantesimi
        spellRoom.setEast(guardRoom);

        ratRoom.setSouth(tortureRoom);     // Stanza del Topo → Stanza delle Torture
        tortureRoom.setNorth(ratRoom);

        tortureRoom.setSouth(bossRoom);    // Stanza delle Torture → Sala del Boss
        bossRoom.setNorth(tortureRoom);

        exitRoom.setEast(bossRoom);       // Uscita → Sala del Boss
        bossRoom.setWest(exitRoom);

        //creazione stanze
        getRooms().add(entrance);
        getRooms().add(ratRoom);
        getRooms().add(diningHall);
        getRooms().add(bedroom);
        getRooms().add(guardRoom);
        getRooms().add(spellRoom);
        getRooms().add(tortureRoom);
        getRooms().add(bossRoom);
        getRooms().add(exitRoom);

        //objects
        Objects keyEntrance = new Objects(1, "chiave ingresso", "Una chiave d'ottone annerita dal tempo. Potrebbe aprire la porta principale della fortezza.");
        keyEntrance.setPickupable(true);

        Objects healPotionChest = new Objects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.");
        healPotionChest.setPickupable(true);

        Objects arrows = new Objects(3, "frecce", "Un piccolo fascio di frecce con punte d'acciaio. Sembrano leggere ma letali.");
        arrows.setPickupable(true);

        Objects webString = new Objects(4, "stringhe di ragnatela", "Filamenti spessi e resistenti. Potrebbero servire per creare qualcosa di utile.");
        webString.setPickupable(true);

        Objects fullHealPotion = new Objects(5, "pozione cura totale", "Una pozione brillante di colore dorato. Ti riempie di energia solo a guardarla.");
        fullHealPotion.setPickupable(true);

        // Crea armi usando WeaponFactory
        Weapon staff = WeaponFactory.createStaff();

        Weapon magicBow = WeaponFactory.createMagicBow();

        Objects fireBook = new Objects(8, "libro incantesimo del fuoco", "Un grimorio antico, le sue pagine brillano di energia arcana. Contiene l'Incantesimo del Fuoco.");
        fireBook.setPickupable(true);

        Objects poison = new Objects(9, "veleno", "Una boccetta scura. Può essere applicata su armi per aumentare il danno.");
        poison.setPickupable(true);

        Objects princessKey = new Objects(10, "chiave cella principessa",
                "Una chiave dorata e decorata, diversa da tutte le altre. Probabilmente apre la cella della principessa.");
        princessKey.setPickupable(true);

        Objects bossKey = new Objects(11, "chiave del collo del boss",
                "Una chiave pesante, con pendaglio di ferro annerito. Cade dal collo del demone canino quando lo sconfiggi: apre l'uscita dalla Montagna dei Goblin.");
        bossKey.setPickupable(true);

        // Casse specifiche per stanze
        ContainerObj entranceChest = new ContainerObj(100, "cassa", "Una vecchia cassa di legno, chiusa ma non bloccata.");
        entranceChest.setAlias(new String[]{"baule", "contenitore", "scrigno"});
        entranceChest.setOpenable(true);
        entranceChest.setPickupable(false);
        entranceChest.setOpen(false);
        entranceChest.add(keyEntrance);
        entranceChest.add(healPotionChest);
        entrance.getObjects().add(entranceChest);

        ContainerObj bedroomChest = new ContainerObj(101, "cassa", "Una cassa giace accanto a un letto distrutto.");
        bedroomChest.setAlias(new String[]{"baule", "contenitore", "scrigno"});
        bedroomChest.setOpenable(true);
        bedroomChest.setPickupable(false);
        bedroomChest.setOpen(false);
        bedroomChest.add(fullHealPotion);
        bedroomChest.add(staff);
        bedroom.getObjects().add(bedroomChest);

        ContainerObj guardChest = new ContainerObj(102, "cassa", "Una cassa semiaperta tra i resti di una barricata.");
        guardChest.setAlias(new String[]{"baule", "contenitore", "scrigno"});
        guardChest.setOpenable(true);
        guardChest.setPickupable(false);
        guardChest.setOpen(false);
        guardChest.add(fireBook);
        guardChest.add(poison);
        guardRoom.getObjects().add(guardChest);

        ContainerObj tortureChest = new ContainerObj(103, "cassa", "Una cassa chiusa giace in un angolo, apparentemente dimenticata.");
        tortureChest.setAlias(new String[]{"baule", "contenitore", "scrigno"});
        tortureChest.setOpenable(true);
        tortureChest.setPickupable(false);
        tortureChest.setOpen(false);
        tortureChest.add(princessKey);
        tortureRoom.getObjects().add(tortureChest);

        // Oggetti sparsi nelle stanze
        ratRoom.getObjects().add(webString);

        // Observer - IMPORTANTE: L'ordine è importante!
        // Prima il combattimento (priorità massima in combattimento)
        CombatObserver combatObserver = new CombatObserver();
        combatObserver.setCombatSystem(combatSystem);
        this.attach(combatObserver);

        // Poi i movimenti (che possono iniziare combattimenti)
        Move moveObserver = new Move();
        moveObserver.setCombatSystem(combatSystem);
        this.attach(moveObserver);

        // Poi gli altri observer
        GameObserver openInventory = new OpenInventory();
        this.attach(openInventory);

        GameObserver lookAtObserver = new LookAt();
        this.attach(lookAtObserver);

        GameObserver pickUpObserver = new PickUp();
        this.attach(pickUpObserver);

        GameObserver openObserver = new Open();
        this.attach(openObserver);

        GameObserver useObserver = new Use();
        this.attach(useObserver);

        // Inventario iniziale del giocatore
        Objects startingHealPotion = new Objects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.");
        startingHealPotion.setPickupable(true);
        getInventory().add(startingHealPotion);

        // Aggiungi spada iniziale
        Weapon startingSword = WeaponFactory.createSword();
        getInventory().add(startingSword);

        //set starting room
        setCurrentRoom(entrance);
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
            return;
        } 

        // Gestione speciale per iniziare il combattimento
        if (p.getCommand().getName().equals("combatti") || 
            p.getCommand().getName().equals("combattimento") ||
            p.getCommand().getName().equals("inizia combattimento") ||
            p.getCommand().getName().equals("battaglia")) {
            
            if (!combatSystem.isInCombat()) {
                String combatStart = combatSystem.startCombat();
                if (!combatStart.isEmpty()) {
                    out.println(combatStart);
                } else {
                    out.println("Non ci sono nemici da combattere qui.");
                }
            } else {
                out.println("Sei già in combattimento!");
            }
            return;
        }

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

    /**
     * Restituisce il sistema di combattimento
     * @return CombatSystem
     */
    public CombatSystem getCombatSystem() {
        return combatSystem;
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
     * Versione modificata che ferma l'elaborazione quando un observer gestisce il comando
     */
    @Override
    public void notifyObservers() {
        // Se siamo in combattimento, dai priorità al CombatObserver
        if (combatSystem.isInCombat()) {
            for (GameObserver o : observer) {
                if (o instanceof CombatObserver) {
                    String message = o.update(this, parserOutput);
                    if (message != null && !message.isEmpty()) {
                        messages.add(message);
                        return; // Ferma qui se il combattimento ha gestito il comando
                    }
                }
            }
        }
        
        // Per tutti gli altri casi, esegui gli observer normalmente ma fermati se uno gestisce il comando
        for (GameObserver o : observer) {
            // Salta il CombatObserver se già processato sopra
            if (combatSystem.isInCombat() && o instanceof CombatObserver) {
                continue;
            }
            
            String message = o.update(this, parserOutput);
            if (message != null && !message.isEmpty()) {
                messages.add(message);
                
                // Se il messaggio non è un errore generico, ferma l'elaborazione
                if (!isGenericErrorMessage(message)) {
                    return;
                }
            }
        }
    }
    
    /**
     * Verifica se un messaggio è un errore generico che non dovrebbe fermare l'elaborazione
     * @param message messaggio da verificare
     * @return true se è un errore generico
     */
    private boolean isGenericErrorMessage(String message) {
        return message.contains("Non puoi muoverti durante un combattimento") ||
               message.contains("Non puoi utilizzare questo oggetto") ||
               message.contains("Non c'è niente da") ||
               message.isEmpty();
    }

    public void cleanup() {
        // Cleanup resources if needed
    }
}