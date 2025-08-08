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

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TBMGame extends GameDescription implements GameObservable {

    private TBMDatabase database;
    private GameLoader gameLoader;
    private CombatSystem combatSystem;
    private GameCharacter player;
    private final List<GameObserver> observers = new ArrayList<>();
    private boolean autoSaveEnabled = true;

    // Campo per memorizzare l'ultimo ParserOutput
    private ParserOutput lastParserOutput;

    @Override
    public void init() throws Exception {
        System.out.println("🚀 Inizializzazione The Black Mountain...");

        try {
            initializeDatabase();
            loadGameData();
            initializeCombatSystem();
            initializeObservers();
            verifyGameState();
            System.out.println("✅ The Black Mountain inizializzato con successo!");
        } catch (Exception e) {
            System.err.println("❌ Errore nell'inizializzazione del gioco: " + e.getMessage());
            throw e;
        }
    }

    private void initializeDatabase() throws SQLException {
        System.out.println("🔧 Inizializzazione database...");
        database = TBMDatabase.getInstance();
        database.initialize();
        if (!database.isHealthy()) {
            throw new SQLException("Database non funzionante dopo l'inizializzazione");
        }
        System.out.println("✅ Database inizializzato e testato");
    }

    private void loadGameData() throws SQLException {
        System.out.println("📋 Caricamento dati di gioco...");
        gameLoader = new GameLoader(this);
        gameLoader.loadGame();
        gameLoader.printDatabaseStats();
        gameLoader.verifyDatabaseIntegrity();
        System.out.println("✅ Dati di gioco caricati");
    }

    private void initializeCombatSystem() {
        if (player != null) {
            combatSystem = new CombatSystem(this);
            System.out.println("⚔️ Sistema di combattimento inizializzato");
        } else {
            System.err.println("⚠️ Impossibile inizializzare il combattimento: giocatore non trovato");
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

            System.out.println("👀 Observer inizializzati (" + observers.size() + " attivi)");
        } catch (Exception e) {
            System.err.println("⚠️ Alcuni observer non disponibili: " + e.getMessage());
        }
    }

    private void verifyGameState() throws Exception {
        List<String> errors = new ArrayList<>();
        if (getRooms().isEmpty()) errors.add("Nessuna stanza caricata");
        if (getCurrentRoom() == null) errors.add("Stanza corrente non impostata");
        if (player == null) errors.add("Giocatore non trovato");
        if (getInventory() == null) errors.add("Inventario non inizializzato");

        if (!errors.isEmpty()) {
            throw new Exception("Problemi nella verifica dello stato: " + String.join(", ", errors));
        }
        System.out.println("✅ Stato del gioco verificato");
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

            // Notifica observer
            notifyObservers();

            // Salvataggio automatico
            if (autoSaveEnabled) {
                saveGameState();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore nell'elaborazione comando: " + e.getMessage());
            out.println("Si è verificato un errore nell'elaborazione del comando.");
        }
    }

    public void saveGameState() {
        if (gameLoader == null) {
            System.err.println("⚠️ GameLoader non inizializzato, impossibile salvare");
            return;
        }
        try {
            System.out.println("💾 Salvataggio stato gioco...");
            gameLoader.savePlayerState();
            for (Room room : getRooms()) {
                for (GameObjects obj : room.getObjects()) {
                    gameLoader.updateObjectState(obj);
                }
            }
            for (GameObjects obj : getInventory()) {
                gameLoader.updateObjectState(obj);
            }
            if (player != null) {
                gameLoader.updateCharacterState(player);
            }
            System.out.println("✅ Stato gioco salvato");
        } catch (Exception e) {
            System.err.println("❌ Errore nel salvataggio: " + e.getMessage());
        }
    }

    public void dropObject(GameObjects obj) {
        if (getCurrentRoom() == null || gameLoader == null) return;
        getInventory().remove(obj);
        getCurrentRoom().getObjects().add(obj);
        gameLoader.moveObjectToRoom(obj, getCurrentRoom());
        System.out.println("📦 Oggetto " + obj.getName() + " lasciato in " + getCurrentRoom().getName());
    }

    public boolean pickupObject(GameObjects obj) {
        if (getCurrentRoom() == null || gameLoader == null) return false;
        if (!obj.isPickupable()) return false;
        getCurrentRoom().getObjects().remove(obj);
        getInventory().add(obj);
        gameLoader.moveObjectToInventory(obj, 1);
        System.out.println("🎒 Oggetto " + obj.getName() + " aggiunto all'inventario");
        return true;
    }

    public void destroyObject(GameObjects obj) {
        if (gameLoader == null) return;
        getInventory().remove(obj);
        for (Room room : getRooms()) {
            room.getObjects().remove(obj);
        }
        gameLoader.removeObject(obj);
        System.out.println("🗑️ Oggetto " + obj.getName() + " rimosso dal gioco");
    }

    public void updateObjectState(GameObjects obj) {
        if (gameLoader != null) gameLoader.updateObjectState(obj);
    }

    public void updateCharacterState(GameCharacter character) {
        if (gameLoader != null) gameLoader.updateCharacterState(character);
    }

    public void resetGame() throws Exception {
        System.out.println("🔄 Reset completo del gioco...");
        if (database != null) database.resetDatabase();
        loadGameData();
        System.out.println("✅ Gioco resettato");
    }

    public void cleanup() {
        try {
            System.out.println("🧹 Cleanup del gioco...");
            if (autoSaveEnabled) saveGameState();
            if (database != null) database.shutdown();
            System.out.println("✅ Cleanup completato");
        } catch (Exception e) {
            System.err.println("⚠️ Errore durante cleanup: " + e.getMessage());
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
                System.err.println("⚠️ Errore in observer " + observer.getClass().getSimpleName() + ": " + e.getMessage());
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

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
        System.out.println("💾 Auto-save " + (autoSaveEnabled ? "abilitato" : "disabilitato"));
    }

    public void printDatabaseInfo() {
        if (database != null) {
            System.out.println(database.getDatabaseInfo());
        }
    }
}
