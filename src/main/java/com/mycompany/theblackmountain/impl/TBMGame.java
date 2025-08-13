package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.database.GameLoader;
import com.mycompany.theblackmountain.database.TBMDatabase;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.Command;
import com.mycompany.theblackmountain.type.CommandType;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TBMGame extends GameDescription implements GameObservable {

    private TBMDatabase database;
    private GameLoader gameLoader;
    private CombatSystem combatSystem;
    private GameCharacter player;
    private final List<GameObserver> observers = new ArrayList<>();

    // Campo per memorizzare l'ultimo ParserOutput
    private ParserOutput lastParserOutput;

    @Override
    public void init() throws Exception {
        System.out.println("Inizializzazione The Black Mountain...");

        try {
            initializeDatabase();
            loadGameData();
            initializeCommands();
            initializeCombatSystem();
            initializeObservers();
            verifyGameState();
            System.out.println("The Black Mountain inizializzato con successo!");
        } catch (Exception e) {
            System.err.println("Errore nell'inizializzazione del gioco: " + e.getMessage());
            throw e;
        }
    }

    private void initializeDatabase() throws SQLException {
        System.out.println("Inizializzazione database...");
        database = TBMDatabase.getInstance();
        database.initialize();
        if (!database.isHealthy()) {
            throw new SQLException("Database non funzionante dopo l'inizializzazione");
        }
        System.out.println("Database inizializzato e testato");
    }

    private void loadGameData() throws SQLException {
        System.out.println("Caricamento dati di gioco...");
        gameLoader = new GameLoader(this);
        gameLoader.loadGame();
        resetForNewGame();
        gameLoader.printDatabaseStats();
        gameLoader.verifyDatabaseIntegrity();
        System.out.println("Dati di gioco caricati");
    }

    private void initializeCombatSystem() {
        if (player != null) {
            combatSystem = new CombatSystem(this);
            System.out.println("Sistema di combattimento inizializzato");
        } else {
            System.err.println("Impossibile inizializzare il combattimento: giocatore non trovato");
        }
    }

    private void initializeObservers() {
        try {
            if (combatSystem != null) {
                CombatObserver combatObserver = new CombatObserver();
                combatObserver.setCombatSystem(combatSystem);
                this.attach(combatObserver);

                Move moveObserver = new Move();
                moveObserver.setCombatSystem(combatSystem);
                this.attach(moveObserver);
            }

            this.attach(new OpenInventory());
            this.attach(new LookAt());
            this.attach(new PickUp());
            this.attach(new Open());
            this.attach(new Use());

            System.out.println("Observer inizializzati (" + observers.size() + " attivi)");
        } catch (Exception e) {
            System.err.println("Alcuni observer non disponibili: " + e.getMessage());
        }
    }

    private void verifyGameState() throws Exception {
        List<String> errors = new ArrayList<>();
        if (getRooms().isEmpty()) {
            errors.add("Nessuna stanza caricata");
        }
        if (getCurrentRoom() == null) {
            errors.add("Stanza corrente non impostata");
        }
        if (player == null) {
            errors.add("Giocatore non trovato");
        }
        if (getInventory() == null) {
            errors.add("Inventario non inizializzato");
        }

        if (!errors.isEmpty()) {
            throw new Exception("Problemi nella verifica dello stato: " + String.join(", ", errors));
        }
        System.out.println("Stato del gioco verificato");
    }

    @Override
    public void nextMove(ParserOutput p, PrintStream out) {
        if (p == null || p.getCommand() == null) {
            out.println("Non capisco quello che mi vuoi dire.");
            return;
        }

        try {
            // Salva l'ultimo comando per gli observer
            this.lastParserOutput = p;

            // Raccogli i risultati degli observer
            StringBuilder result = new StringBuilder();
            boolean commandHandled = false;

            for (GameObserver observer : observers) {
                try {
                    String observerResult = observer.update(this, lastParserOutput);
                    if (observerResult != null && !observerResult.trim().isEmpty()) {
                        result.append(observerResult);
                        if (!observerResult.endsWith("\n")) {
                            result.append("\n");
                        }
                        commandHandled = true;
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Errore in observer " + observer.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Se abbiamo un risultato viene stampato
            if (result.length() > 0) {
                String output = result.toString();
                out.print(output);
                out.flush();
            } else if (!commandHandled) {
                // Messaggio di fallback se nessun observer ha risposto
                out.println("Comando non riconosciuto o non applicabile qui.");
                out.flush();
            }

        } catch (Exception e) {
            System.err.println("⚠️ Errore nell'elaborazione comando: " + e.getMessage());
            e.printStackTrace();
            out.println("Si è verificato un errore nell'elaborazione del comando.");
            out.flush();
        }
    }

    private void initializeCommands() {
        System.out.println("Inizializzazione comandi...");

        // Commands
        Command nord = new Command(CommandType.NORD, "nord");
        nord.setAlias(new String[]{"n", "N", "Nord", "NORD"});
        getCommands().add(nord);

        Command inventory = new Command(CommandType.INVENTORY, "inventario");
        inventory.setAlias(new String[]{"inv"});
        getCommands().add(inventory);

        Command sud = new Command(CommandType.SOUTH, "sud");
        sud.setAlias(new String[]{"s", "S", "Sud", "SUD"});
        getCommands().add(sud);

        Command est = new Command(CommandType.EAST, "est");
        est.setAlias(new String[]{"e", "E", "Est", "EST"});
        getCommands().add(est);

        Command ovest = new Command(CommandType.WEST, "ovest");
        ovest.setAlias(new String[]{"o", "O", "Ovest", "OVEST"});
        getCommands().add(ovest);

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
        Command fight = new Command(CommandType.FIGHT, "combatti");
        fight.setAlias(new String[]{"combattimento", "inizia combattimento", "battaglia"});
        getCommands().add(fight);

        // Comando per attaccare
        Command attack = new Command(CommandType.ATTACK, "attacca");
        attack.setAlias(new String[]{"attacco", "colpisci", "fight"});
        getCommands().add(attack);

        // Comando per creare arco (CORREZIONE: era attack.setAlias invece di create.setAlias)
        Command create = new Command(CommandType.CREATE, "crea");
        create.setAlias(new String[]{"costruisci", "build", "create"});
        getCommands().add(create);

        System.out.println("Inizializzati " + getCommands().size() + " comandi");
    }

    /**
     * Reset del gioco per una nuova partita
     */
    public void resetForNewGame() {
        try {
            System.out.println("🔄 Resetting gioco per nuova partita...");

            // Reset posizione giocatore all'ingresso
            Room entrance = null;
            for (Room room : getRooms()) {
                if (room.getId() == 0) {
                    entrance = room;
                    break;
                }
            }

            if (entrance != null) {
                setCurrentRoom(entrance);
                System.out.println("✅ Giocatore riposizionato all'ingresso");
            }

            // Reset HP giocatore
            if (player != null) {
                player.setCurrentHp(player.getMaxHp());
                updateCharacterState(player);
                System.out.println("✅ HP giocatore ripristinati: " + player.getCurrentHp());
            }

            // Reset stato nemici - li rimette tutti vivi
            if (gameLoader != null && database != null) {
                try (Connection conn = database.getConnection()) {
                    String resetEnemiesSql = "UPDATE CHARACTERS SET CURRENT_HP = MAX_HP, IS_ALIVE = TRUE WHERE CHARACTER_TYPE != 'PLAYER'";
                    try (var stmt = conn.prepareStatement(resetEnemiesSql)) {
                        int updated = stmt.executeUpdate();
                        System.out.println(updated + " nemici ripristinati nel database");
                    }

                    // Ricarica i nemici nelle stanze
                    for (Room room : getRooms()) {
                        room.getEnemies().clear();
                    }

                    // Ricarica caratteri dal database (metodo privato chiamato attraverso GameLoader)
                    reloadCharactersFromDatabase();

                } catch (SQLException e) {
                    System.err.println("Errore nel reset nemici: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Termina eventuali combattimenti in corso
            if (combatSystem != null && combatSystem.isInCombat()) {
                combatSystem.endCombat();
                System.out.println("Combattimento in corso terminato");
            }

            System.out.println("Gioco pronto per una nuova avventura!");

        } catch (Exception e) {
            System.err.println("Errore nel reset del gioco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ricarica i personaggi dal database dopo il reset
     */
    private void reloadCharactersFromDatabase() {
        try {
            // Creare un nuovo GameLoader temporaneo per ricaricare solo i caratteri
            GameLoader tempLoader = new GameLoader(this);

            // Chiama il metodo pubblico loadGame che ricaricherà tutti i dati
            // Ma dato che abbiamo già le stanze, ricaricherà solo i personaggi
            try (Connection conn = database.getConnection()) {
                // Usa reflection per chiamare il metodo privato loadCharacters
                // In alternativa, potresti rendere loadCharacters pubblico in GameLoader
                java.lang.reflect.Method loadCharactersMethod
                        = GameLoader.class.getDeclaredMethod("loadCharacters", Connection.class);
                loadCharactersMethod.setAccessible(true);
                loadCharactersMethod.invoke(gameLoader, conn);

                System.out.println("✅ Personaggi ricaricati dalle stanze");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore nel ricaricamento personaggi: " + e.getMessage());
            // Fallback: ricrea manualmente i nemici nelle stanze
            createDefaultEnemiesInRooms();
        }
    }

    // Sostituisci i metodi game over in TBMGame.java con questi versioni con debug:

/**
 * Controlla se il giocatore è morto
 * @return true se il giocatore è morto (HP <= 0)
 */
public boolean isGameOver() {
    if (player == null) {
        System.out.println("🐛 DEBUG: Player è null!");
        return false;
    }
    
    int currentHp = player.getCurrentHp();
    int maxHp = player.getMaxHp();
    
    System.out.println("🐛 DEBUG Game Over Check:");
    System.out.println("  - Player: " + player.getName());
    System.out.println("  - Current HP: " + currentHp);
    System.out.println("  - Max HP: " + maxHp);
    System.out.println("  - Is Dead: " + (currentHp <= 0));
    
    return currentHp <= 0;
}

/**
 * Restituisce un messaggio di game over basato sullo stato del giocatore
 * @return messaggio di morte
 */
public String getGameOverMessage() {
    if (player == null) {
        return "Errore di sistema";
    }
    
    if (player.getCurrentHp() <= 0) {
        return "La tua forza vitale si è esaurita...";
    }
    
    return "Game Over";
}

/**
 * Debug: Stampa lo stato completo del giocatore
 */
public void debugPlayerState() {
    System.out.println(" === STATO GIOCATORE ===");
    if (player == null) {
        System.out.println("PLAYER È NULL!");
        return;
    }
    
    System.out.println("Nome: " + player.getName());
    System.out.println("HP Correnti: " + player.getCurrentHp());
    System.out.println("HP Massimi: " + player.getMaxHp());
    System.out.println("Attacco: " + player.getAttack());
    System.out.println("Difesa: " + player.getDefense());
    System.out.println("È vivo: " + player.isAlive());
    System.out.println("========================");
}
    /**
     * Crea manualmente i nemici nelle stanze come fallback
     */
    private void createDefaultEnemiesInRooms() {
        try {
            System.out.println("Creazione nemici di default...");

            for (Room room : getRooms()) {
                room.getEnemies().clear();

                switch (room.getId()) {
                    case 0: // Ingresso - Goblin
                        room.getEnemies().add(new GameCharacter(1, "Goblin",
                                "Una creatura malvagia dalla pelle verde scuro.", 40, 12, 3,
                                com.mycompany.theblackmountain.type.CharacterType.GOBLIN));
                        break;
                    case 1: // Stanza del Topo
                        room.getEnemies().add(new GameCharacter(2, "Topo Gigante",
                                "Un enorme roditore con denti giallastri.", 25, 8, 2,
                                com.mycompany.theblackmountain.type.CharacterType.GIANT_RAT));
                        break;
                    case 2: // Mensa - Due Goblin
                        room.getEnemies().add(new GameCharacter(3, "Goblin Chiassoso",
                                "Un goblin aggressivo.", 35, 10, 3,
                                com.mycompany.theblackmountain.type.CharacterType.GOBLIN));
                        room.getEnemies().add(new GameCharacter(4, "Goblin Rissoso",
                                "Un altro goblin aggressivo.", 30, 9, 2,
                                com.mycompany.theblackmountain.type.CharacterType.GOBLIN));
                        break;
                    case 4: // Sala delle Guardie
                        room.getEnemies().add(new GameCharacter(5, "Goblin Gigante",
                                "Un goblin enorme con clava.", 60, 16, 5,
                                com.mycompany.theblackmountain.type.CharacterType.GOBLIN));
                        room.getEnemies().add(new GameCharacter(6, "Goblin Minuto",
                                "Un goblin piccolo ma minaccioso.", 25, 8, 2,
                                com.mycompany.theblackmountain.type.CharacterType.GOBLIN));
                        break;
                    case 7: // Boss - Cane Demone
                        room.getEnemies().add(new GameCharacter(7, "Cane Demone",
                                "Una creatura infernale.", 120, 25, 8,
                                com.mycompany.theblackmountain.type.CharacterType.DEMON_DOG));
                        break;
                }
            }

            System.out.println("Nemici di default creati");
        } catch (Exception e) {
            System.err.println("Errore nella creazione nemici: " + e.getMessage());
        }
    }

    public void dropObject(GameObjects obj) {
        if (getCurrentRoom() == null || gameLoader == null) {
            return;
        }
        getInventory().remove(obj);
        getCurrentRoom().getObjects().add(obj);
        gameLoader.moveObjectToRoom(obj, getCurrentRoom());
        System.out.println("Oggetto " + obj.getName() + " lasciato in " + getCurrentRoom().getName());
    }

    public boolean pickupObject(GameObjects obj) {
        if (getCurrentRoom() == null || gameLoader == null) {
            return false;
        }
        if (!obj.isPickupable()) {
            return false;
        }
        getCurrentRoom().getObjects().remove(obj);
        getInventory().add(obj);
        gameLoader.moveObjectToInventory(obj, 0); // ID giocatore = 0
        System.out.println("Oggetto " + obj.getName() + " aggiunto all'inventario");
        return true;
    }

    public void destroyObject(GameObjects obj) {
        if (gameLoader == null) {
            return;
        }
        getInventory().remove(obj);
        for (Room room : getRooms()) {
            room.getObjects().remove(obj);
        }
        gameLoader.removeObject(obj);
        System.out.println("Oggetto " + obj.getName() + " rimosso dal gioco");
    }

    public void updateObjectState(GameObjects obj) {
        if (gameLoader != null) {
            gameLoader.updateObjectState(obj);
        }
    }

    public void updateCharacterState(GameCharacter character) {
        if (gameLoader != null) {
            gameLoader.updateCharacterState(character);
        }
    }

    public void cleanup() {
        try {
            System.out.println("Cleanup del gioco...");
            if (database != null) {
                database.shutdown();
            }
            System.out.println("Cleanup completato");
        } catch (Exception e) {
            System.err.println("Errore durante cleanup: " + e.getMessage());
        }
    }

    public void shutdown() {
        cleanup();
    }

    // =====================================
    //   GameObservable implementation
    // =====================================
    @Override
    public void attach(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(GameObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            try {
                observer.update(this, lastParserOutput);
            } catch (Exception e) {
                System.err.println("Errore in observer " + observer.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    // =====================================
    //   Getter / Setter
    // =====================================
    public CombatSystem getCombatSystem() {
        return combatSystem;
    }

    public GameCharacter getPlayer() {
        return player;
    }

    public void setPlayer(GameCharacter player) {
        this.player = player;
    }

    public TBMDatabase getDatabase() {
        return database;
    }

    public GameLoader getGameLoader() {
        return gameLoader;
    }

    public void printDatabaseInfo() {
        if (database != null) {
            System.out.println(database.getDatabaseInfo());
        }
    }
}
