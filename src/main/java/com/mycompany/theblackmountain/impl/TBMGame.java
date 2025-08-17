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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            forceCompleteReset();
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
        debugAfterLoad();
        System.out.println("Dati di gioco caricati");
    }

    private void initializeCombatSystem() {
        if (player != null) {
            combatSystem = new CombatSystem(this);
            combatSystem.setPlayer(player);
            System.out.println("Sistema di combattimento inizializzato");
        } else {
            System.err.println("Impossibile inizializzare il combattimento: giocatore non trovato");
        }
    }

    /**
     * Reset forzato completo del database per evitare stati inconsistenti
     * VERSIONE CORRETTA CHE NON USA gameLoader (che è ancora null)
     */
    private void forceCompleteReset() {
        System.out.println("Reset forzato del database per stato pulito...");

        try (Connection conn = database.getConnection()) {

            // 1. Reset COMPLETO del giocatore
            String resetPlayerSql = """
        UPDATE CHARACTERS 
        SET CURRENT_HP = MAX_HP, IS_ALIVE = TRUE, ROOM_ID = 0 
        WHERE CHARACTER_TYPE = 'PLAYER' AND ID = 0
    """;
            try (var stmt = conn.prepareStatement(resetPlayerSql)) {
                stmt.executeUpdate();
                System.out.println("Player stato ripristinato");
            }

            // 2. Reset inventario a quello iniziale
            String clearInventorySql = "DELETE FROM INVENTORY WHERE CHARACTER_ID = 0";
            try (var stmt = conn.prepareStatement(clearInventorySql)) {
                stmt.executeUpdate();
            }

            String restoreInventorySql = """
        INSERT INTO INVENTORY (CHARACTER_ID, OBJECT_ID) 
        SELECT 0, 2 WHERE NOT EXISTS (SELECT 1 FROM INVENTORY WHERE CHARACTER_ID = 0 AND OBJECT_ID = 2)
        UNION ALL
        SELECT 0, 12 WHERE NOT EXISTS (SELECT 1 FROM INVENTORY WHERE CHARACTER_ID = 0 AND OBJECT_ID = 12)
    """;
            try (var stmt = conn.prepareStatement(restoreInventorySql)) {
                stmt.executeUpdate();
                System.out.println("Inventario iniziale ripristinato");
            }

            // 3. Reset tutti i nemici a vivi
            String resetEnemiesSql = """
        UPDATE CHARACTERS 
        SET CURRENT_HP = MAX_HP, IS_ALIVE = TRUE 
        WHERE CHARACTER_TYPE != 'PLAYER'
    """;
            try (var stmt = conn.prepareStatement(resetEnemiesSql)) {
                int updated = stmt.executeUpdate();
                System.out.println(updated + " nemici ripristinati");
            }

            // 4. Reset tutte le casse a chiuse
            String resetChestsSql = "UPDATE OBJECTS SET IS_OPEN = FALSE WHERE ID >= 100 AND ID <= 103";
            try (var stmt = conn.prepareStatement(resetChestsSql)) {
                stmt.executeUpdate();
                System.out.println("Casse chiuse");
            }

            // 5. Rimuovi SOLO il contenuto delle casse dalle stanze, NON le casse stesse
            String removeChestContentsSql = """
        DELETE FROM ROOM_OBJECTS 
        WHERE OBJECT_ID IN (1, 2, 5, 6, 8, 9, 10)
    """;
            try (var stmt = conn.prepareStatement(removeChestContentsSql)) {
                int removed = stmt.executeUpdate();
                System.out.println(removed + " contenuti casse rimossi dalle stanze");
            }

            // 6. ASSICURATI CHE LE CASSE SIANO PRESENTI NELLE STANZE
            // NON usare gameLoader qui perché è ancora null!
            ensureChestsInRoomsStatic(conn);

            // 7. DEBUG: Verifica presenza casse nel database
            String debugSql = "SELECT ROOM_ID, OBJECT_ID FROM ROOM_OBJECTS WHERE OBJECT_ID >= 100 ORDER BY ROOM_ID";
            try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(debugSql)) {
                System.out.println("DEBUG: Casse nel database dopo reset:");
                while (rs.next()) {
                    System.out.println("  - Cassa " + rs.getInt("OBJECT_ID") + " in stanza " + rs.getInt("ROOM_ID"));
                }
            }

            System.out.println("Reset forzato completato!");

        } catch (SQLException e) {
            System.err.println("Errore nel reset forzato: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Metodo statico per assicurarsi che le casse siano nelle stanze corrette
     * Versione che non dipende da gameLoader
     */
    private static void ensureChestsInRoomsStatic(Connection conn) throws SQLException {
        System.out.println("Verifica presenza casse nelle stanze...");

        // Array con le casse e le loro stanze
        int[][] chestRoomPairs = {
            {0, 100}, // Stanza 0, Cassa 100 (Ingresso)
            {3, 101}, // Stanza 3, Cassa 101 (Dormitorio)
            {4, 102}, // Stanza 4, Cassa 102 (Sala Guardie)
            {6, 103} // Stanza 6, Cassa 103 (Torture)
        };

        String checkSql = "SELECT COUNT(*) FROM ROOM_OBJECTS WHERE ROOM_ID = ? AND OBJECT_ID = ?";
        String insertSql = "INSERT INTO ROOM_OBJECTS (ROOM_ID, OBJECT_ID) VALUES (?, ?)";

        int totalInserted = 0;

        for (int[] pair : chestRoomPairs) {
            int roomId = pair[0];
            int chestId = pair[1];

            // Controlla se la cassa è già nella stanza
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, roomId);
                checkStmt.setInt(2, chestId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // La cassa non c'è, inseriscila
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, roomId);
                            insertStmt.setInt(2, chestId);
                            insertStmt.executeUpdate();
                            totalInserted++;
                            System.out.println("  Cassa " + chestId + " aggiunta alla stanza " + roomId);
                        }
                    } else {
                        System.out.println("  Cassa " + chestId + " già presente nella stanza " + roomId);
                    }
                }
            }
        }

        System.out.println("Totale casse inserite: " + totalInserted);
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
            // *** COMANDI DEBUG SEMPLIFICATI ***
            String commandName = p.getCommand().getName();

            if (commandName.equals("kill")) {
                if (player != null) {
                    int oldHp = player.getCurrentHp();
                    player.setCurrentHp(0);
                    updateCharacterState(player);
                    out.println("DEBUG: HP da " + oldHp + " a " + player.getCurrentHp()
                            + ", Alive: " + player.isAlive());
                } else {
                    out.println("DEBUG: Player non trovato!");
                }
                return;
            }

            if (commandName.equals("fullheal")) {
                if (player != null) {
                    int oldHp = player.getCurrentHp();
                    player.setCurrentHp(player.getMaxHp());
                    updateCharacterState(player);
                    out.println("DEBUG: HP da " + oldHp + " a " + player.getCurrentHp()
                            + "/" + player.getMaxHp() + ", Alive: " + player.isAlive());
                } else {
                    out.println("DEBUG: Player non trovato!");
                }
                return;
            }

            if (commandName.equals("lowhp")) {
                if (player != null) {
                    int oldHp = player.getCurrentHp();
                    player.setCurrentHp(1);
                    updateCharacterState(player);
                    out.println("DEBUG: HP da " + oldHp + " a " + player.getCurrentHp()
                            + "/" + player.getMaxHp() + ", Alive: " + player.isAlive());
                } else {
                    out.println("DEBUG: Player non trovato!");
                }
                return;
            }

            if (commandName.equals("status")) {
                if (player != null) {
                    out.println("=== STATO PLAYER ===");
                    out.println("HP: " + player.getCurrentHp() + "/" + player.getMaxHp());
                    out.println("Alive: " + player.isAlive());
                    out.println("Attack: " + player.getAttack());
                    out.println("Defense: " + player.getDefense());
                    out.println("==================");
                } else {
                    out.println("DEBUG: Player non trovato!");
                }
                return;
            }

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
                    System.err.println("WARNING: Errore in observer " + observer.getClass().getSimpleName() + ": " + e.getMessage());
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

            // *** CORREZIONE: Controlla il game over SOLO DOPO aver elaborato il comando ***
            if (isGameOver()) {
                out.println("\n" + getGameOverMessage());
                out.flush();
                System.out.println("DEBUG: GAME OVER confermato - Player HP: "
                        + (player != null ? player.getCurrentHp() : "null"));
            }

        } catch (Exception e) {
            System.err.println("ERROR: Errore nell'elaborazione comando: " + e.getMessage());
            e.printStackTrace();
            out.println("Si è verificato un errore nell'elaborazione del comando.");
            out.flush();
        }
    }

    /**
     * Controlla se il giocatore è morto - VERSIONE CORRETTA
     *
     * @return true se il giocatore è morto (HP <= 0)
     */
    public boolean isGameOver() {
        if (player == null) {
            System.out.println("DEBUG: Player è null - NO GAME OVER");
            return false;
        }

        int currentHp = player.getCurrentHp();
        boolean isDead = currentHp <= 0;
        boolean isNotAlive = !player.isAlive();

        System.out.println("DEBUG Game Over Check:");
        System.out.println("  - Player: " + player.getName());
        System.out.println("  - Current HP: " + currentHp);
        System.out.println("  - Max HP: " + player.getMaxHp());
        System.out.println("  - Is Alive Flag: " + player.isAlive());
        System.out.println("  - HP <= 0: " + isDead);
        System.out.println("  - Not Alive: " + isNotAlive);
        System.out.println("  - Should be Game Over: " + (isDead || isNotAlive));

        // Verifica entrambe le condizioni
        return isDead || isNotAlive;
    }

    /**
     * Forza la fine del game over e ripristina il giocatore
     */
    public void resetGameOverState() {
        if (player != null) {
            // Assicurati che il player sia vivo e con HP positivi
            if (player.getCurrentHp() <= 0) {
                player.setCurrentHp(1); // HP minimo per evitare game over
            }
            player.setCurrentHp(Math.max(player.getCurrentHp(), 1));
            // Il flag isAlive viene automaticamente aggiornato quando HP > 0

            // Aggiorna il database
            updateCharacterState(player);

            System.out.println("Game Over state resettato - Player HP: " + player.getCurrentHp() + ", Alive: " + player.isAlive());
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

        // Comando per creare arco
        Command create = new Command(CommandType.CREATE, "crea");
        create.setAlias(new String[]{"costruisci", "build", "create"});
        getCommands().add(create);

        // COMANDI DEBUG
        // Comando per uccidere il player
        Command kill = new Command(CommandType.USE, "kill");
        kill.setAlias(new String[]{"die", "muori"});
        getCommands().add(kill);

        // Comando per curare il player
        Command fullheal = new Command(CommandType.USE, "fullheal");
        fullheal.setAlias(new String[]{"cura", "heal"});
        getCommands().add(fullheal);

        // Comando per HP bassi
        Command lowHP = new Command(CommandType.USE, "lowhp");
        getCommands().add(lowHP);

        // Comando per vedere stato
        Command status = new Command(CommandType.USE, "status");
        status.setAlias(new String[]{"stato", "hp"});
        getCommands().add(status);
        System.out.println("Inizializzati " + getCommands().size() + " comandi");
    }

    /**
     * Reset del gioco per una nuova partita
     */
    // In TBMGame.java, sostituisci il metodo resetForNewGame() con questa versione corretta:
    public void resetForNewGame() {
        try {
            System.out.println("DEBUG: Resetting gioco per nuova partita...");

            // 1. Reset posizione giocatore all'ingresso
            Room entrance = null;
            for (Room room : getRooms()) {
                if (room.getId() == 0) {
                    entrance = room;
                    break;
                }
            }

            if (entrance != null) {
                setCurrentRoom(entrance);
                System.out.println("DEBUG: Giocatore riposizionato all'ingresso");
            }

            // 2. Reset COMPLETO del giocatore nel database
            if (database != null) {
                try (Connection conn = database.getConnection()) {
                    // Reset HP e posizione giocatore
                    String resetPlayerSql = "UPDATE CHARACTERS SET CURRENT_HP = MAX_HP, IS_ALIVE = TRUE, ROOM_ID = 0 WHERE CHARACTER_TYPE = 'PLAYER' AND ID = 0";
                    try (var stmt = conn.prepareStatement(resetPlayerSql)) {
                        stmt.executeUpdate();
                        System.out.println("DEBUG: Player HP ripristinato nel database");
                    }

                    // *** CORREZIONE PRINCIPALE: Ricarica il player dal database ***
                    String loadPlayerSql = "SELECT * FROM CHARACTERS WHERE CHARACTER_TYPE = 'PLAYER' AND ID = 0";
                    try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(loadPlayerSql)) {
                        if (rs.next()) {
                            // Aggiorna l'oggetto player in memoria con i valori del database
                            int dbCurrentHp = rs.getInt("CURRENT_HP");
                            int dbMaxHp = rs.getInt("MAX_HP");
                            boolean dbIsAlive = rs.getBoolean("IS_ALIVE");

                            player.setMaxHp(dbMaxHp);
                            player.setCurrentHp(dbCurrentHp);

                            // Il flag isAlive viene automaticamente gestito da setCurrentHp()
                            // Se HP > 0, il personaggio sarà automaticamente vivo
                            // Se HP <= 0, sarà automaticamente morto
                            // Se il database dice che è vivo ma HP è 0, forziamo HP positivo
                            if (dbIsAlive && player.getCurrentHp() <= 0) {
                                player.setCurrentHp(1); // Forza almeno 1 HP se dovrebbe essere vivo
                            }

                            System.out.println("DEBUG: Player ricaricato dal database - HP: " + player.getCurrentHp()
                                    + "/" + player.getMaxHp() + ", Alive: " + player.isAlive());
                        }
                    }

                    // 3. Reset COMPLETO inventario - svuota tutto
                    String clearInventorySql = "DELETE FROM INVENTORY WHERE CHARACTER_ID = 0";
                    try (var stmt = conn.prepareStatement(clearInventorySql)) {
                        stmt.executeUpdate();
                        System.out.println("DEBUG: Inventario svuotato nel database");
                    }

                    // 4. Ripristina inventario iniziale (pozione + spada)
                    String restoreInventorySql = "INSERT INTO INVENTORY (CHARACTER_ID, OBJECT_ID) VALUES (0, 2), (0, 12)";
                    try (var stmt = conn.prepareStatement(restoreInventorySql)) {
                        stmt.executeUpdate();
                        System.out.println("DEBUG: Inventario iniziale ripristinato nel database");
                    }

                    // 5. Reset stato nemici - li rimette tutti vivi
                    String resetEnemiesSql = "UPDATE CHARACTERS SET CURRENT_HP = MAX_HP, IS_ALIVE = TRUE WHERE CHARACTER_TYPE != 'PLAYER'";
                    try (var stmt = conn.prepareStatement(resetEnemiesSql)) {
                        int updated = stmt.executeUpdate();
                        System.out.println("DEBUG: " + updated + " nemici ripristinati nel database");
                    }

                    // 6. Reset casse nel database (chiudi tutte e rimuovi contenuti dalle stanze)
                    if (gameLoader != null) {
                        gameLoader.resetAllChests();
                        System.out.println("DEBUG: Casse resettate tramite GameLoader");
                    } else {
                        // Fallback se gameLoader è null
                        System.out.println("WARNING: GameLoader null, uso reset casse diretto");

                        // Reset casse
                        String resetChestsSql = "UPDATE OBJECTS SET IS_OPEN = FALSE WHERE ID >= 100 AND ID <= 103";
                        try (var stmt = conn.prepareStatement(resetChestsSql)) {
                            stmt.executeUpdate();
                            System.out.println("DEBUG: Casse chiuse");
                        }

                        // Rimuovi contenuti casse
                        String removeContentSql = "DELETE FROM ROOM_OBJECTS WHERE OBJECT_ID IN (1, 2, 5, 6, 8, 9, 10)";
                        try (var stmt = conn.prepareStatement(removeContentSql)) {
                            int removed = stmt.executeUpdate();
                            System.out.println("DEBUG: " + removed + " contenuti casse rimossi");
                        }

                        // Assicura presenza casse
                        ensureChestsInRoomsStatic(conn);
                    }

                } catch (SQLException e) {
                    System.err.println("ERROR: Errore nel reset database: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // *** NON SOVRASCRIVERE PIÙ L'HP QUI - È GIÀ STATO FATTO SOPRA ***
            // Il player è già stato aggiornato dal database sopra
            // 7. Reset inventario in memoria - svuota e ripristina quello iniziale
            getInventory().clear();

            // Ricarica oggetti iniziali dall'inventory
            if (gameLoader != null && database != null) {
                try (Connection conn = database.getConnection()) {
                    String loadInventorySql = """
                SELECT o.*, w.WEAPON_TYPE, w.ATTACK_BONUS, w.CRITICAL_CHANCE, 
                       w.CRITICAL_MULTIPLIER, w.IS_POISONED, w.POISON_DAMAGE, w.SPECIAL_EFFECT
                FROM INVENTORY i
                JOIN OBJECTS o ON i.OBJECT_ID = o.ID
                LEFT JOIN WEAPONS w ON o.ID = w.OBJECT_ID
                WHERE i.CHARACTER_ID = 0
                ORDER BY o.ID
            """;

                    try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(loadInventorySql)) {
                        while (rs.next()) {
                            // Usa il metodo createObjectFromResultSet di GameLoader
                            GameObjects obj = createObjectFromResultSet(rs);
                            if (obj != null) {
                                getInventory().add(obj);
                            }
                        }
                        System.out.println("DEBUG: Inventario ricaricato: " + getInventory().size() + " oggetti");
                    }

                } catch (SQLException e) {
                    System.err.println("WARNING: Errore ricaricamento inventario, uso fallback");
                    // Fallback: crea oggetti manualmente
                    createDefaultInventory();
                }
            } else {
                // Fallback se gameLoader è null
                createDefaultInventory();
            }

            // 8. Ricarica nemici nelle stanze
            for (Room room : getRooms()) {
                room.getEnemies().clear();
            }
            reloadCharactersFromDatabase();

            // 9. Termina eventuali combattimenti in corso
            if (combatSystem != null && combatSystem.isInCombat()) {
                combatSystem.endCombat();
                System.out.println("DEBUG: Combattimento in corso terminato");
            }

            // 10. Debug finale dello stato
            System.out.println("DEBUG: Reset completato - Player HP: "
                    + (player != null ? player.getCurrentHp() + "/" + player.getMaxHp() : "null")
                    + ", Is Alive: " + (player != null ? player.isAlive() : "null"));

            System.out.println("DEBUG: Gioco pronto per una nuova avventura");

        } catch (Exception e) {
            System.err.println("ERROR: Errore nel reset del gioco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reset del sistema porte
     */
    private void resetDoorSystems() {
        try {
            System.out.println("Resettando sistemi porte...");

            // Reset del DoorSystem nel Move observer
            for (GameObserver observer : observers) {
                if (observer instanceof Move) {
                    Move moveObserver = (Move) observer;
                    moveObserver.resetForNewGame();
                    break;
                }
            }

            System.out.println("Sistemi porte resettati");
        } catch (Exception e) {
            System.err.println("Errore nel reset porte: " + e.getMessage());
        }
    }

    /**
     * Crea oggetti iniziali di default come fallback
     */
    private void createDefaultInventory() {
        getInventory().clear();

        // Pozione di cura (ID 2)
        GameObjects potion = new GameObjects(2, "pozione di cura", "Una fiala dal liquido rosso, emana un lieve calore.");
        potion.setPickupable(true);
        getInventory().add(potion);

        // Spada (ID 12)
        com.mycompany.theblackmountain.type.Weapon sword = new com.mycompany.theblackmountain.type.Weapon(
                12, "spada", "Una spada d'acciaio ben bilanciata. Arma affidabile per il combattimento.",
                8, com.mycompany.theblackmountain.type.WeaponType.SWORD, 10, 2
        );
        sword.setPickupable(true);
        getInventory().add(sword);

        System.out.println("Inventario di default creato: " + getInventory().size() + " oggetti");
    }

    /**
     * Metodo helper per createObjectFromResultSet (reso pubblico)
     */
    private GameObjects createObjectFromResultSet(java.sql.ResultSet rs) throws SQLException {
        // Questo metodo esiste già in GameLoader, lo richiamiamo
        if (gameLoader != null) {
            try {
                java.lang.reflect.Method method = GameLoader.class.getDeclaredMethod("createObjectFromResultSet", java.sql.ResultSet.class);
                method.setAccessible(true);
                return (GameObjects) method.invoke(gameLoader, rs);
            } catch (Exception e) {
                System.err.println("Errore reflection: " + e.getMessage());
                return null;
            }
        }
        return null;
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

                System.out.println("Personaggi ricaricati dalle stanze");
            }
        } catch (Exception e) {
            System.err.println("Errore nel ricaricamento personaggi: " + e.getMessage());
            // Fallback: ricrea manualmente i nemici nelle stanze
            createDefaultEnemiesInRooms();
        }
    }

    /**
     * Restituisce un messaggio di game over basato sullo stato del giocatore
     *
     * @return messaggio di morte
     */
    public String getGameOverMessage() {
        if (player == null) {
            return "Errore di sistema - giocatore non trovato.";
        }

        if (player.getCurrentHp() <= 0) {
            return "La tua avventura è finita... I tuoi HP sono scesi a zero e le tenebre ti avvolgono.";
        }

        if (!player.isAlive()) {
            return "Il tuo spirito vitale si è spento... Non puoi più continuare l'avventura.";
        }

        return "Game Over - La tua avventura è terminata.";
    }

    /**
     * Gestisce la risposta del giocatore al game over Chiamalo dal main loop
     * quando il giocatore risponde alla domanda di restart
     */
    public boolean handleGameOverResponse(String response) {
        if (response == null) {
            return false;
        }

        String cleanResponse = response.trim().toLowerCase();

        if (cleanResponse.equals("si") || cleanResponse.equals("sì")
                || cleanResponse.equals("s") || cleanResponse.equals("yes")
                || cleanResponse.equals("y") || cleanResponse.equals("ricomincio")) {

            System.out.println("Il giocatore ha scelto di ricominciare...");
            resetForNewGame();
            return true; // Indica che il gioco è stato resettato

        } else if (cleanResponse.equals("no") || cleanResponse.equals("n")
                || cleanResponse.equals("esci") || cleanResponse.equals("quit")) {

            System.out.println("Il giocatore ha scelto di uscire dal gioco");
            return false; // Indica che il giocatore vuole uscire

        } else {
            // Risposta non riconosciuta, richiedi di nuovo
            System.out.println("Risposta non riconosciuta: " + response);
            return false; // Non è né restart né quit, chiedi di nuovo
        }
    }

    /**
     * Verifica se il gioco è in stato di attesa game over
     */
    public boolean isWaitingForGameOverResponse() {
        return isGameOver();
    }

    /**
     * Debug: Forza un game over per test
     */
    public void forceGameOver() {
        if (player != null) {
            player.setCurrentHp(0); // Questo automaticamente imposta isAlive = false
            updateCharacterState(player);
            System.out.println("DEBUG: Game Over forzato");
        }
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

    /**
     * Debug completo dello stato del gioco dopo il caricamento
     */
    public void debugGameState() {
        System.out.println("\n========== DEBUG STATO GIOCO ==========");

        // 1. Stato stanze
        System.out.println("\nSTANZE CARICATE:");
        for (Room room : getRooms()) {
            System.out.println("Stanza " + room.getId() + ": " + room.getName());

            // Oggetti nella stanza
            if (!room.getObjects().isEmpty()) {
                System.out.println("  Oggetti (" + room.getObjects().size() + "):");
                for (GameObjects obj : room.getObjects()) {
                    System.out.println("    - " + obj.getName() + " (ID:" + obj.getId()
                            + ", Apribile:" + obj.isOpenable() + ", Aperto:" + obj.isOpen() + ")");
                }
            } else {
                System.out.println("  Nessun oggetto");
            }

            // Nemici nella stanza
            if (!room.getEnemies().isEmpty()) {
                System.out.println("  Nemici (" + room.getEnemies().size() + "):");
                for (GameCharacter enemy : room.getEnemies()) {
                    System.out.println("    - " + enemy.getName() + " (HP:" + enemy.getCurrentHp()
                            + "/" + enemy.getMaxHp() + ", Vivo:" + enemy.isAlive() + ")");
                }
            } else {
                System.out.println("  Nessun nemico");
            }
            System.out.println();
        }

        // 2. Stato giocatore
        System.out.println("\nGIOCATORE:");
        if (player != null) {
            System.out.println("  Nome: " + player.getName());
            System.out.println("  HP: " + player.getCurrentHp() + "/" + player.getMaxHp());
            System.out.println("  Stanza corrente: " + (getCurrentRoom() != null
                    ? getCurrentRoom().getName() + " (ID:" + getCurrentRoom().getId() + ")" : "NESSUNA"));
        } else {
            System.out.println("  GIOCATORE NON TROVATO!");
        }

        // 3. Inventario
        System.out.println("\nINVENTARIO (" + getInventory().size() + " oggetti):");
        for (GameObjects obj : getInventory()) {
            System.out.println("  - " + obj.getName() + " (ID:" + obj.getId() + ")");
        }

        System.out.println("\n======================================\n");
    }

    private void debugAfterLoad() {
        System.out.println("DEBUG: Stato del gioco dopo caricamento");
        debugGameState();
    }

    public void shutdown() {
        cleanup();
    }

    // GameObservable implementation
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

    // Getter / Setter
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
