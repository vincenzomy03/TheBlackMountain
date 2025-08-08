package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.combat.CombatSystem;
import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.TBMDatabase;
import com.mycompany.theblackmountain.database.DatabaseException;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Character;
import com.mycompany.theblackmountain.factory.WeaponFactory;

import java.util.List;
import java.util.Optional;

public class TBMGame extends GameDescription implements GameObservable {

    private TBMDatabase databaseService;
    private boolean useDatabaseMode = true;

    @Override
    public void init() throws Exception {
        messages.clear();

        if (useDatabaseMode) {
            if (!tryInitializeDatabase()) {
                System.out.println("🔄 Fallback alla modalità hardcoded...");
                useDatabaseMode = false;
                initializeHardcodedGame();
            } else {
                loadGameFromDatabase();
            }
        } else {
            initializeHardcodedGame();
        }

        // Inizializza il sistema di combattimento dopo aver caricato il giocatore
        combatSystem = new CombatSystem(this);

        initializeObservers();
    }

    /**
     * Prova a inizializzare la connessione al DB
     */
    private boolean tryInitializeDatabase() {
        try {
            DatabaseManager.getInstance().initialize();
            databaseService = new TBMDatabase();

            if (!databaseService.testConnection()) {
                throw new Exception("Test di connessione al database fallito");
            }
            System.out.println("✅ Connessione al database riuscita");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Errore di connessione al database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Carica l'intero gioco dal DB
     */
    private void loadGameFromDatabase() throws Exception {
        System.out.println("📋 Caricamento dati dal database...");

        try {
            List<Room> rooms = databaseService.loadAllRooms();
            getRooms().clear();
            getRooms().addAll(rooms);

            for (Room room : rooms) {
                loadRoomData(room);
            }

            Character player = loadPlayer();
            setCurrentRoom(findPlayerRoom(player, rooms));

            initializePlayerInventoryFromDB(player);

            System.out.println("✅ Dati caricati dal database");
        } catch (DatabaseException e) {
            throw new Exception("Errore nel caricamento dal database", e);
        }
    }

    private void loadRoomData(Room room) throws DatabaseException {
        List<Objects> roomObjects = databaseService.loadRoomObjects(room.getId());
        room.getObjects().clear();
        room.getObjects().addAll(roomObjects);

        List<Character> enemies = databaseService.loadRoomEnemies(room.getId());
        room.getEnemies().clear();
        room.getEnemies().addAll(enemies);
    }

    private Character loadPlayer() throws DatabaseException {
        Optional<Character> playerOpt = databaseService.loadPlayer();
        if (!playerOpt.isPresent()) {
            throw new DatabaseException("Giocatore non trovato nel database");
        }
        return playerOpt.get();
    }

    private Room findPlayerRoom(Character player, List<Room> rooms) throws DatabaseException {
        int playerRoomId = databaseService.getPlayerRoomId(player.getId());
        return rooms.stream()
                .filter(r -> r.getId() == playerRoomId)
                .findFirst()
                .orElse(rooms.get(0));
    }

    private void initializePlayerInventoryFromDB(Character player) {
        getInventory().clear();
        // Qui in futuro potresti caricare l'inventario reale dal DB
        // Per ora mettiamo gli oggetti iniziali
        getInventory().add(new Objects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.", true));
        getInventory().add(WeaponFactory.createSword());
    }

    /**
     * Modalità hardcoded
     */
    private void initializeHardcodedGame() throws Exception {
        System.out.println("🔧 Inizializzazione gioco con dati hardcoded...");
        initializeRoomsHardcoded();
        initializePlayerInventoryFromDB(null);
        if (!getRooms().isEmpty()) {
            setCurrentRoom(getRooms().get(0));
        }
        System.out.println("✅ Gioco inizializzato in modalità hardcoded");
    }

    private void initializeRoomsHardcoded() {
        Room startRoom = new Room(0, "Ingresso", "Ti trovi all'ingresso della fortezza maledetta.");
        getRooms().add(startRoom);
        // Aggiungi qui altre stanze hardcoded...
    }

    private void initializeObservers() {
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
    }

    public void saveGameState() {
        if (!useDatabaseMode || databaseService == null) {
            System.out.println("⚠️ Database non disponibile per il salvataggio");
            return;
        }

        try {
            System.out.println("💾 Salvataggio stato gioco nel database...");

            for (Room room : getRooms()) {
                databaseService.saveRoom(room);
                for (Character enemy : room.getEnemies()) {
                    databaseService.saveCharacter(enemy);
                }
            }

            if (combatSystem != null && combatSystem.getPlayer() != null) {
                Character player = combatSystem.getPlayer();
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

    public void updateObjectState(Objects obj) {
        if (!useDatabaseMode || databaseService == null) return;
        try {
            databaseService.updateObjectState(obj);
        } catch (DatabaseException e) {
            System.err.println("Errore nell'aggiornamento oggetto: " + e.getMessage());
        }
    }

    public void updateCharacterState(Character character) {
        if (!useDatabaseMode || databaseService == null) return;
        try {
            databaseService.updateCharacterState(character);
        } catch (DatabaseException e) {
            System.err.println("Errore nell'aggiornamento personaggio: " + e.getMessage());
        }
    }

    public void moveObjectToCurrentRoom(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;
        try {
            databaseService.moveObjectToRoom(obj.getId(), getCurrentRoom().getId());
        } catch (DatabaseException e) {
            System.err.println("Errore nello spostamento oggetto: " + e.getMessage());
        }
    }

    public void moveObjectToInventory(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;
        try {
            databaseService.moveObjectToInventory(obj.getId(), getCurrentRoom().getId());
        } catch (DatabaseException e) {
            System.err.println("Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
        }
    }

    public boolean isDatabaseModeEnabled() {
        return useDatabaseMode;
    }

    public TBMDatabase getDatabaseService() {
        return databaseService;
    }

    public void cleanup() {
        if (useDatabaseMode) {
            saveGameState();
        }
        if (databaseService != null) {
            try {
                DatabaseManager.getInstance().shutdown();
            } catch (Exception e) {
                System.err.println("Errore nella chiusura del database: " + e.getMessage());
            }
        }
        System.out.println("🧹 Cleanup completato");
    }
}
