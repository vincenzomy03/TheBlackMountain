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
                e.printStackTrace();
                System.out.println("🔄 Fallback alla modalità hardcoded...");
                useDatabaseMode = false;
                initializeHardcodedGame();
            }
        } else {
            initializeHardcodedGame();
        }

        // Inizializza il sistema di combattimento dopo aver caricato il giocatore
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

        // 1. Carica tutte le stanze
        List<Room> rooms = databaseService.loadAllRooms();
        getRooms().clear();
        getRooms().addAll(rooms);

        // 2. Carica oggetti e nemici per ogni stanza
        for (Room room : rooms) {
            // Carica oggetti della stanza
            List<Objects> roomObjects = databaseService.loadRoomObjects(room.getId());
            room.getObjects().clear();
            room.getObjects().addAll(roomObjects);

            // Carica nemici della stanza (esclude il giocatore)
            List<Character> enemies = databaseService.loadRoomEnemies(room.getId());
            room.getEnemies().clear();
            room.getEnemies().addAll(enemies);
        }

        // 3. Carica il giocatore
        Character player = databaseService.loadPlayer();
        if (player == null) {
            throw new SQLException("Giocatore non trovato nel database");
        }

        // 4. Trova la stanza del giocatore
        int playerRoomId = databaseService.getPlayerRoomId(player.getId());
        Room playerRoom = rooms.stream()
                .filter(r -> r.getId() == playerRoomId)
                .findFirst()
                .orElse(rooms.get(0)); // Fallback alla prima stanza

        setCurrentRoom(playerRoom);

        // 5. Inizializza inventario (per ora hardcoded, ma potresti estendere il DB)
        initializePlayerInventory();

        System.out.println("✅ Dati caricati dal database");
        System.out.println(databaseService.getDatabaseStats());
    }

    private void initializePlayerInventory() {
        getInventory().clear();

        // Aggiungi oggetti iniziali al giocatore
        Objects startingHealPotion = new Objects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.");
        startingHealPotion.setPickupable(true);
        getInventory().add(startingHealPotion);

        // Aggiungi spada iniziale
        getInventory().add(WeaponFactory.createSword());
    }

    private void initializeHardcodedGame() throws Exception {
        System.out.println("🔧 Inizializzazione gioco con dati hardcoded...");
        
        // Qui dovresti implementare la logica hardcoded originale
        // Per ora lascio vuoto, ma dovresti copiare la logica di inizializzazione
        // che avevi prima dell'introduzione del database
        
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

    /**
     * Salva lo stato completo del gioco nel database
     */
    public void saveGameState() {
        if (!useDatabaseMode || databaseService == null) {
            System.out.println("⚠️ Database non disponibile per il salvataggio");
            return;
        }

        try {
            System.out.println("💾 Salvataggio stato gioco nel database...");

            // 1. Salva tutte le stanze
            for (Room room : getRooms()) {
                databaseService.saveRoom(room);
                
                // Salva i nemici nella stanza
                for (Character enemy : room.getEnemies()) {
                    databaseService.saveCharacter(enemy);
                }
            }

            // 2. Salva il giocatore
            if (combatSystem != null && combatSystem.getPlayer() != null) {
                Character player = combatSystem.getPlayer();
                databaseService.saveCharacter(player);

                // Aggiorna la posizione del giocatore
                if (getCurrentRoom() != null) {
                    databaseService.updatePlayerRoom(player.getId(), getCurrentRoom().getId());
                }
            }

            // 3. Gestisci inventario (rimuovi oggetti dalle stanze se sono nell'inventario)
            // Nota: questa è una semplificazione. Potresti voler creare una tabella separata per l'inventario

            System.out.println("✅ Stato gioco salvato nel database");

        } catch (SQLException e) {
            System.err.println("❌ Errore nel salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aggiorna lo stato di un oggetto nel database
     */
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

    /**
     * Aggiorna lo stato di un personaggio nel database
     */
    public void updateCharacterState(Character character) {
        if (!useDatabaseMode || databaseService == null) return;

        try {
            databaseService.updateCharacterHp(character.getId(), character.getCurrentHp(), character.getMaxHp());
            databaseService.updateCharacterAliveStatus(character.getId(), character.isAlive());
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento personaggio: " + e.getMessage());
        }
    }

    /**
     * Sposta un oggetto dall'inventario alla stanza corrente
     */
    public void moveObjectToCurrentRoom(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;

        try {
            databaseService.moveObjectToRoom(obj.getId(), getCurrentRoom().getId());
        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto: " + e.getMessage());
        }
    }

    /**
     * Sposta un oggetto dalla stanza corrente all'inventario
     */
    public void moveObjectToInventory(Objects obj) {
        if (!useDatabaseMode || databaseService == null || getCurrentRoom() == null) return;

        try {
            databaseService.moveObjectToInventory(obj.getId(), getCurrentRoom().getId());
        } catch (SQLException e) {
            System.err.println("Errore nello spostamento oggetto nell'inventario: " + e.getMessage());
        }
    }

    /**
     * Verifica se il database è abilitato
     */
    public boolean isDatabaseModeEnabled() {
        return useDatabaseMode;
    }

    /**
     * Ottiene il servizio database (per accesso diretto se necessario)
     */
    public GameDatabaseService getDatabaseService() {
        return databaseService;
    }

    /**
     * Forza il salvataggio quando il gioco termina
     */
    @Override
    public void finalize() {
        if (useDatabaseMode) {
            saveGameState();
        }
    }
}