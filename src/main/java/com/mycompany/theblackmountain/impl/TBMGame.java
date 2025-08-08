package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.GameObserver;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.TBMDatabase;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.GameObjects;
import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.CharacterType;
import com.mycompany.theblackmountain.factory.WeaponFactory;
import com.mycompany.theblackmountain.parser.ParserOutput;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TBMGame extends GameDescription implements GameObservable {

    private TBMDatabase databaseService;
    private CombatSystem combatSystem;
    private final List<GameObserver> observers = new ArrayList<>();

    @Override
    public void init() throws Exception {
        // Inizializza la lista messaggi se serve (da implementare in GameDescription)
        if (getMessages() == null) {
            initializeMessages();
        }

        // Inizializza il database (se fallisce, esce con eccezione)
        DatabaseManager.getInstance().initialize();
        databaseService = new TBMDatabase();

        if (!databaseService.testConnection()) {
            throw new Exception("Test di connessione al database fallito");
        }
        System.out.println("✅ Connessione al database riuscita");

        // Carica gioco da DB
        loadGameFromDatabase();

        // Inizializza combat system dopo caricamento giocatore
        combatSystem = new CombatSystem(this);

        // Inizializza observer
        initializeObservers();
    }

    private void initializeMessages() {
        // Implementa se serve
    }

    @Override
    public void nextMove(ParserOutput p, PrintStream out) {
        if (p == null || p.getCommand() == null) {
            out.println("Non capisco quello che mi vuoi dire.");
            return;
        }

        notifyObservers(p);

        saveGameState();
    }

    public CombatSystem getCombatSystem() {
        return combatSystem;
    }

    private void loadGameFromDatabase() throws Exception {
        System.out.println("📋 Caricamento dati dal database...");

        try {
            List<Room> rooms = databaseService.loadAllRooms();
            getRooms().clear();
            getRooms().addAll(rooms);

            for (Room room : rooms) {
                loadRoomData(room);
            }

            GameCharacter player = loadPlayer();
            setCurrentRoom(findPlayerRoom(player, rooms));
            initializePlayerInventoryFromDB(player);

            System.out.println("✅ Dati caricati dal database");
        } catch (DatabaseException e) {
            throw new Exception("Errore nel caricamento dal database", e);
        }
    }

    private void loadRoomData(Room room) throws DatabaseException {
        List<GameObjects> roomObjects = databaseService.loadRoomObjects(room.getId());
        room.getObjects().clear();
        room.getObjects().addAll(roomObjects);

        // Gestisci nemici solo se Room li supporta
        if (hasEnemiesSupport()) {
            List<GameCharacter> enemies = databaseService.loadRoomEnemies(room.getId());
            room.getEnemies().clear();
            room.getEnemies().addAll(enemies);
        }
    }

    private boolean hasEnemiesSupport() {
        // Modifica secondo implementazione Room
        return false;
    }

    private GameCharacter loadPlayer() throws DatabaseException {
        Optional<GameCharacter> playerOpt = databaseService.loadPlayer();
        if (!playerOpt.isPresent()) {
            GameCharacter defaultPlayer = createDefaultPlayer();
            databaseService.saveCharacter(defaultPlayer);
            return defaultPlayer;
        }
        return playerOpt.get();
    }

    private GameCharacter createDefaultPlayer() {
        return new GameCharacter(
                1,
                "Avventuriero",
                "Un coraggioso avventuriero",
                100,
                15,
                10,
                CharacterType.PLAYER
        );
    }

    private Room findPlayerRoom(GameCharacter player, List<Room> rooms) throws DatabaseException {
        try {
            int playerRoomId = databaseService.getPlayerRoomId(player.getId());
            return rooms.stream()
                    .filter(r -> r.getId() == playerRoomId)
                    .findFirst()
                    .orElse(rooms.get(0));
        } catch (DatabaseException e) {
            return rooms.isEmpty() ? null : rooms.get(0);
        }
    }

    private void initializePlayerInventoryFromDB(GameCharacter player) {
        getInventory().clear();
        // Per ora oggetti fissi (in futuro da DB)
        getInventory().add(new GameObjects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.", true));
        getInventory().add(WeaponFactory.createSword());
    }

    private void initializeObservers() {
        if (combatSystem == null) {
            System.err.println("⚠️ CombatSystem non inizializzato, skip inizializzazione observers");
            return;
        }

        try {
            CombatObserver combatObserver = new CombatObserver();
            combatObserver.setCombatSystem(combatSystem);
            this.attach(combatObserver);

            Move moveObserver = new Move();
            moveObserver.setCombatSystem(combatSystem);
            this.attach(moveObserver);

            this.attach(new OpenInventory());
            this.attach(new LookAt());
            this.attach(new PickUp());
            this.attach(new Open());
            this.attach(new Use());
        } catch (Exception e) {
            System.err.println("⚠️ Alcuni observer non sono disponibili: " + e.getMessage());
        }
    }

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
    public void notifyObservers(ParserOutput p) {
        for (GameObserver observer : observers) {
            observer.update(p);
        }
    }

    public void saveGameState() {
        try {
            System.out.println("💾 Salvataggio stato gioco nel database...");

            for (Room room : getRooms()) {
                databaseService.saveRoom(room);
                if (hasEnemiesSupport()) {
                    for (GameCharacter enemy : room.getEnemies()) {
                        databaseService.saveCharacter(enemy);
                    }
                }
            }

            if (combatSystem != null && combatSystem.getPlayer() != null) {
                GameCharacter player = combatSystem.getPlayer();
                databaseService.saveCharacter(player);
                if (getCurrentRoom() != null) {
                    databaseService.updatePlayerPosition(player.getId(), getCurrentRoom().getId());
                }
            }

            System.out.println("✅ Stato gioco salvato nel database");
        } catch (DatabaseException e) {
            System.err.println("❌ Errore nel salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateObjectState(GameObjects obj) {
        try {
            databaseService.updateObjectState(obj);
        } catch (DatabaseException e) {
            System.err.println("Errore nell'aggiornamento oggetto: " + e.getMessage());
        }
    }

    public void updateCharacterState(GameCharacter character) {
        try {
            databaseService.updateCharacterState(character);
        } catch (DatabaseException e) {
            System.err.println("Errore nell'aggiornamento personaggio: " + e.getMessage());
        }
    }

    public void moveObjectToCurrentRoom(GameObjects obj) {
        if (getCurrentRoom() == null) {
            return;
        }
        try {
            databaseService.moveObjectToRoom(obj.getId(), getCurrentRoom().getId());
        } catch (DatabaseException e) {
            System.err.println("Errore nello spostamento oggetto: " + e.getMessage());
        }
    }

    public void moveObjectToInventory(GameObjects obj) {
        try {
            // Assumendo giocatore con ID 1
            databaseService.moveObjectToInventory(obj.getId(), 1);
        } catch (DatabaseException e) {
            System.err.println("Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
        }
    }

    public void cleanup() {
        saveGameState();
        if (databaseService != null) {
            try {
                DatabaseManager.getInstance().shutdown();
            } catch (Exception e) {
                System.err.println("Errore nella chiusura del database: " + e.getMessage());
            }
        }
        System.out.println("🧹 Cleanup completato");
    }

    /**
     * Getter per i messaggi (implementa come serve)
     */
    public List<String> getMessages() {
        return null; // da implementare se serve
    }
}
