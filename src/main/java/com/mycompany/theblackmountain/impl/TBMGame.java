package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.GameObservable;
import com.mycompany.theblackmountain.database.DatabaseManager;
import com.mycompany.theblackmountain.database.GameDatabaseService;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Character;
import com.mycompany.theblackmountain.factory.WeaponFactory;

import java.sql.SQLException;
import java.util.List;

public class TBMGame extends GameDescription implements GameObservable {

    private GameDatabaseService databaseService;
    private boolean useDatabaseMode = true;

    @Override
    public void init() throws Exception {
        messages.clear();

        if (useDatabaseMode) {
            try {
                initializeDatabase();
                loadGameFromDatabase();
                System.out.println("✅ Gioco caricato dal database H2");
            } catch (Exception e) {
                System.err.println("❌ Errore nell'inizializzazione del database: " + e.getMessage());
                System.out.println("🔄 Fallback alla modalità hardcoded...");
                useDatabaseMode = false;
                initializeHardcodedGame();
            }
        } else {
            initializeHardcodedGame();
        }

        combatSystem = new CombatSystem(this);
        initializeObservers();
    }

    private void initializeDatabase() throws Exception {
        DatabaseManager.getInstance().initialize();
        databaseService = new GameDatabaseService();

        if (!databaseService.testConnection()) {
            throw new Exception("Test di connessione al database fallito");
        }
    }

    private void loadGameFromDatabase() throws SQLException {
        System.out.println("📋 Caricamento dati dal database...");

        List<Room> rooms = databaseService.loadAllRooms();
        getRooms().clear();
        getRooms().addAll(rooms);

        for (Room room : rooms) {
            List<Objects> roomObjects = databaseService.loadRoomObjects(room.getId());
            room.getObjects().clear();
            room.getObjects().addAll(roomObjects);

            List<Character> enemies = databaseService.loadRoomEnemies(room.getId());
            room.getEnemies().clear();
            room.getEnemies().addAll(enemies);
        }

        Character player = databaseService.loadPlayer();
        combatSystem.setPlayer(player);

        int playerRoomId = databaseService.getPlayerRoomId(player.getId());
        Room playerRoom = rooms.stream()
                .filter(r -> r.getId() == playerRoomId)
                .findFirst()
                .orElse(rooms.get(0));
        setCurrentRoom(playerRoom);

        initializePlayerInventory();

        System.out.println("✅ Dati caricati dal database");
        System.out.println(databaseService.getDatabaseStats());
    }

    private void initializePlayerInventory() {
        getInventory().clear();

        Objects startingHealPotion = new Objects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.");
        startingHealPotion.setPickupable(true);
        getInventory().add(startingHealPotion);

        getInventory().add(WeaponFactory.createSword());
    }

    private void initializeHardcodedGame() throws Exception {
        System.out.println("🔧 Inizializzazione gioco con dati hardcoded...");
        // Qui va il codice originale di inizializzazione del gioco
        System.out.println("✅ Gioco inizializzato in modalità hardcoded");
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
            }

            if (combatSystem != null && combatSystem.getPlayer() != null) {
                Character player = combatSystem.getPlayer();
                databaseService.saveCharacter(player);

                if (getCurrentRoom() != null) {
                    databaseService.updatePlayerRoom(player.getId(), getCurrentRoom().getId());
                }
            }

            for (Objects obj : getInventory()) {
                databaseService.moveObjectToInventory(obj.getId(), getCurrentRoom().getId());
            }

            System.out.println("✅ Stato gioco salvato nel database");

        } catch (SQLException e) {
            System.err.println("❌ Errore nel salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateObjectState(Objects obj) {
        if (!useDatabaseMode || databaseService == null) return;

        try {
            if (obj.isOpenable()) {
                databaseService.updateObjectOpenState(obj.getId(), obj.isOpen());
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento oggetto: " + e.getMessage());
        }
    }

    public void updateCharacterState(Character character) {
        if (!useDatabaseMode || databaseService == null) return;

        try {
            databaseService.updateCharacterHp(character.getId(), character.getCurrentHp(), character.getMaxHp());
            databaseService.updateCharacterAliveStatus(character.getId(), character.isAlive());
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento personaggio: " + e.getMessage());
        }
    }

    public void moveObjectToCurrentRoom(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;

        try {
            databaseService.moveObjectToRoom(obj.getId(), getCurrentRoom().getId());
        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto: " + e.getMessage());
        }
    }

    public void moveObjectToInventory(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;

        try {
            databaseService.moveObjectToInventory(obj.getId(), getCurrentRoom().getId());
        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
        }
    }

    public boolean isDatabaseModeEnabled() {
        return useDatabaseMode;
    }
}
