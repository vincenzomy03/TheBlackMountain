/*
 * Package database
 */
package com.mycompany.theblackmountain.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe di inizializzazione del database per The Black Mountain. Approccio
 * minimal e diretto senza DAO/Entity layers.
 */
public class InitDatabase {

    private static final String DB_URL = "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    /**
     * Metodo di inizializzazione del database di gioco.
     */
    public final void initDatabase() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // ==================
            // CREAZIONE TABELLE
            // ==================
            // Tabella ROOMS - Stanze del gioco
            Statement stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOMS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "LOOK_DESCRIPTION TEXT,"
                    + "VISIBLE BOOLEAN DEFAULT TRUE,"
                    + "IMAGE_PATH VARCHAR(255));");
            stm.close();

            // Tabella ROOM_CONNECTIONS - Connessioni tra stanze
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOM_CONNECTIONS("
                    + "ROOM_ID INT PRIMARY KEY,"
                    + "NORTH_ID INT,"
                    + "SOUTH_ID INT,"
                    + "EAST_ID INT,"
                    + "WEST_ID INT,"
                    + "FOREIGN KEY (ROOM_ID) REFERENCES ROOMS(ID));");
            stm.close();

            // Tabella OBJECTS - Oggetti del gioco
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS OBJECTS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "ALIASES VARCHAR(500),"
                    + "OPENABLE BOOLEAN DEFAULT FALSE,"
                    + "PICKUPABLE BOOLEAN DEFAULT TRUE,"
                    + "PUSHABLE BOOLEAN DEFAULT FALSE,"
                    + "IS_OPEN BOOLEAN DEFAULT FALSE,"
                    + "IS_PUSHED BOOLEAN DEFAULT FALSE,"
                    + "OBJECT_TYPE VARCHAR(50) DEFAULT 'NORMAL');");
            stm.close();

            // Tabella WEAPONS - Armi (estende oggetti)
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS WEAPONS("
                    + "OBJECT_ID INT PRIMARY KEY,"
                    + "WEAPON_TYPE VARCHAR(50),"
                    + "ATTACK_BONUS INT DEFAULT 0,"
                    + "CRITICAL_CHANCE DOUBLE DEFAULT 5.0,"
                    + "CRITICAL_MULTIPLIER DOUBLE DEFAULT 2.0,"
                    + "IS_POISONED BOOLEAN DEFAULT FALSE,"
                    + "POISON_DAMAGE INT DEFAULT 0,"
                    + "SPECIAL_EFFECT VARCHAR(255),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();

            // Tabella CHARACTERS - Personaggi del gioco
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS CHARACTERS("
                    + "ID INT PRIMARY KEY,"
                    + "NAME VARCHAR(255) NOT NULL,"
                    + "DESCRIPTION TEXT,"
                    + "CHARACTER_TYPE VARCHAR(50) NOT NULL,"
                    + "MAX_HP INT DEFAULT 100,"
                    + "CURRENT_HP INT DEFAULT 100,"
                    + "ATTACK INT DEFAULT 10,"
                    + "DEFENSE INT DEFAULT 5,"
                    + "IS_ALIVE BOOLEAN DEFAULT TRUE,"
                    + "ROOM_ID INT DEFAULT 1);");
            stm.close();

            // Tabella ROOM_OBJECTS - Oggetti presenti nelle stanze
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS ROOM_OBJECTS("
                    + "ROOM_ID INT,"
                    + "OBJECT_ID INT,"
                    + "PRIMARY KEY (ROOM_ID, OBJECT_ID),"
                    + "FOREIGN KEY (ROOM_ID) REFERENCES ROOMS(ID),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();

            // Tabella INVENTORY - Inventario del giocatore
            stm = conn.createStatement();
            stm.execute("CREATE TABLE IF NOT EXISTS INVENTORY("
                    + "CHARACTER_ID INT,"
                    + "OBJECT_ID INT,"
                    + "PRIMARY KEY (CHARACTER_ID, OBJECT_ID),"
                    + "FOREIGN KEY (CHARACTER_ID) REFERENCES CHARACTERS(ID),"
                    + "FOREIGN KEY (OBJECT_ID) REFERENCES OBJECTS(ID));");
            stm.close();

            // ==================
            // INSERIMENTO DATI
            // ==================
            // Verifica e inserimento ROOMS
            String checkDataSql = "SELECT COUNT(*) FROM ROOMS";
            stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(checkDataSql);
            rs.next();
            int count = rs.getInt(1);
            rs.close();

            // Verifica e inserimento ROOMS - Aggiornato con le stanze del gioco effettivo
            checkDataSql = "SELECT COUNT(*) FROM ROOMS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                // Stanze del gioco "The Black Mountain" 
                stm.execute("INSERT INTO ROOMS VALUES "
                        + "(0, 'Ingresso della Fortezza', 'Ti trovi all''ingresso della fortezza maledetta. L''aria è densa di umidità e il pavimento è cosparso di muschio. Un enorme goblin dalla pelle verde scuro ti osserva con occhi colmi d''odio.', "
                        + "'Scorgi una vecchia cassa, probabilmente abbandonata dai precedenti avventurieri. Dentro potrebbe esserci qualcosa di utile per iniziare il tuo cammino.', TRUE, 'entrance'), "
                        + "(1, 'Stanza del Topo', 'Le pareti sono coperte di ragnatele e muffa. Un enorme topo, con denti giallastri e occhi rossi, grugnisce in un angolo buio.', "
                        + "'Tra le ragnatele, potresti trovare qualcosa di utile o... disgustoso.', TRUE, 'rat_room'), "
                        + "(2, 'Mensa Abbandonata', 'Le tavole di legno sono rovesciate, piatti infranti ovunque. Due goblin chiassosi stanno litigando per un osso ancora sanguinante.', "
                        + "'Non sembra esserci nulla di valore qui, a parte i goblin pronti a combattere.', TRUE, 'mess_hall'), "
                        + "(3, 'Dormitorio delle Guardie', 'Letti rotti e coperte lacerate giacciono sparsi ovunque. Il silenzio regna sovrano: nessun nemico in vista.', "
                        + "'Una cassa giace accanto a un letto distrutto. Dentro, potresti trovare qualcosa di prezioso.', TRUE, 'dormitory'), "
                        + "(4, 'Sala delle Guardie', 'I resti di un banchetto interrotto sono sparsi ovunque. Un goblin gigante impugna una clava insanguinata.', "
                        + "'Tra i resti di una barricata, spunta una cassa semiaperta.', TRUE, 'guard_hall'), "
                        + "(5, 'Sala degli Incantesimi', 'Le pareti sono incise con simboli magici che pulsano di luce blu. Nel centro, un altare emette un suono basso e costante.', "
                        + "'Un''iscrizione recita: Offri parte della tua essenza, e il tuo braccio sarà guidato da un arco etereo.', TRUE, 'magic_room'), "
                        + "(6, 'Stanza delle Torture', 'Catene arrugginite pendono dal soffitto. Il pavimento è macchiato di vecchio sangue secco. Non ci sono nemici.', "
                        + "'Una cassa chiusa giace in un angolo, apparentemente dimenticata.', TRUE, 'torture_room'), "
                        + "(7, 'Sala del Signore dei Goblin', 'L''aria è irrespirabile. Un fumo denso copre il volto del cane demone, una creatura infernale con zanne fumanti.', "
                        + "'Sul piedistallo alle spalle del demone c''è una chiave che potrebbe aprire la cella.', TRUE, 'boss_room'), "
                        + "(8, 'Uscita', 'L''uscita dalla Montagna Nera. La libertà ti aspetta.', "
                        + "'Finalmente libero!', TRUE, 'exit');");
                stm.close();
            }

            // Verifica e inserimento ROOM_CONNECTIONS
            checkDataSql = "SELECT COUNT(*) FROM ROOM_CONNECTIONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            // Verifica e inserimento ROOM_CONNECTIONS - Aggiornato con le connessioni corrette
            checkDataSql = "SELECT COUNT(*) FROM ROOM_CONNECTIONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                // Connessioni secondo data.sql
                stm.execute("INSERT INTO ROOM_CONNECTIONS VALUES "
                        + "(0, NULL, NULL, 1, NULL), " // Ingresso: est=Stanza del Topo
                        + "(1, NULL, 6, 2, 0), " // Stanza del Topo: est=Mensa, ovest=Ingresso, sud=Torture
                        + "(2, NULL, NULL, 3, 1), " // Mensa: est=Dormitorio, ovest=Stanza del Topo
                        + "(3, NULL, 4, NULL, 2), " // Dormitorio: sud=Sala Guardie, ovest=Mensa
                        + "(4, 3, NULL, NULL, 5), " // Sala Guardie: nord=Dormitorio, ovest=Sala Incantesimi
                        + "(5, NULL, NULL, 4, NULL), " // Sala Incantesimi: est=Sala Guardie
                        + "(6, 1, 7, NULL, NULL), " // Torture: nord=Stanza Topo, sud=Boss
                        + "(7, 6, NULL, NULL, 8), " // Boss: nord=Torture, ovest=Uscita
                        + "(8, NULL, NULL, 7, NULL);"); // Uscita: est=Boss
                stm.close();
            }

            // Verifica e inserimento OBJECTS
            checkDataSql = "SELECT COUNT(*) FROM OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            // Verifica e inserimento OBJECTS - Oggetti del gioco effettivo
            checkDataSql = "SELECT COUNT(*) FROM OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                // Oggetti effettivi del gioco
                stm.execute("INSERT INTO OBJECTS VALUES "
                        + "(1, 'chiave ingresso', 'Una chiave d''ottone annerita dal tempo. Potrebbe aprire la porta principale della fortezza.', 'chiave,key', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(2, 'pozione di cura', 'Una fiala dal liquido rosso, emana un lieve calore.', 'pozione,cura,healing', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(3, 'frecce', 'Un piccolo fascio di frecce con punte d''acciaio. Sembrano leggere ma letali.', 'arrow,arrows', FALSE, TRUE, FALSE, FALSE, FALSE, 'WEAPON'), "
                        + "(4, 'stringhe di ragnatela', 'Filamenti spessi e resistenti. Potrebbero servire per creare qualcosa di utile.', 'ragnatela,filo', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(5, 'pozione cura totale', 'Una pozione brillante di colore dorato. Ti riempie di energia solo a guardarla.', 'pozione totale,cura totale', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(6, 'bastone', 'Un robusto bastone di legno. Può essere combinato con altri oggetti.', 'staff,stick', FALSE, TRUE, FALSE, FALSE, FALSE, 'WEAPON'), "
                        + "(7, 'arco magico', 'Un arco leggero ma potente, creato con energia arcana e materiali raccolti nella fortezza.', 'arco,bow', FALSE, TRUE, FALSE, FALSE, FALSE, 'WEAPON'), "
                        + "(8, 'libro incantesimo del fuoco', 'Un grimorio antico, le sue pagine brillano di energia arcana. Contiene l''Incantesimo del Fuoco.', 'libro,grimorio,fuoco', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(9, 'veleno', 'Una boccetta scura. Può essere applicata su armi per aumentare il danno.', 'poison,boccetta', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(10, 'chiave cella principessa', 'Una chiave dorata e decorata, diversa da tutte le altre. Probabilmente apre la cella della principessa.', 'chiave principessa,chiave dorata', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(11, 'chiave del collo del boss', 'Una chiave pesante, con pendaglio di ferro annerito. Cade dal collo del demone canino quando lo sconfiggi.', 'chiave boss,chiave finale', FALSE, TRUE, FALSE, FALSE, FALSE, 'NORMAL'), "
                        + "(12, 'spada', 'Una spada d''acciaio ben bilanciata. Arma affidabile per il combattimento.', 'sword,lama', FALSE, TRUE, FALSE, FALSE, FALSE, 'WEAPON'), "
                        + "(100, 'cassa', 'Una vecchia cassa di legno, chiusa ma non bloccata.', 'baule,contenitore,scrigno', TRUE, FALSE, FALSE, FALSE, FALSE, 'CONTAINER'), "
                        + "(101, 'cassa', 'Una cassa giace accanto a un letto distrutto.', 'baule,contenitore,scrigno', TRUE, FALSE, FALSE, FALSE, FALSE, 'CONTAINER'), "
                        + "(102, 'cassa', 'Una cassa semiaperta tra i resti di una barricata.', 'baule,contenitore,scrigno', TRUE, FALSE, FALSE, FALSE, FALSE, 'CONTAINER'), "
                        + "(103, 'cassa', 'Una cassa chiusa giace in un angolo, apparentemente dimenticata.', 'baule,contenitore,scrigno', TRUE, FALSE, FALSE, FALSE, FALSE, 'CONTAINER');");
                stm.close();
            }

            // Verifica e inserimento WEAPONS
            checkDataSql = "SELECT COUNT(*) FROM WEAPONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            // Verifica e inserimento WEAPONS - Armi del gioco effettivo
            checkDataSql = "SELECT COUNT(*) FROM WEAPONS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                stm.execute("INSERT INTO WEAPONS VALUES "
                        + "(3, 'ARROWS', 3, 5.0, 2.0, FALSE, 0, NULL), " // Frecce
                        + "(6, 'STAFF', 5, 5.0, 2.0, FALSE, 0, NULL), " // Bastone
                        + "(7, 'MAGIC', 12, 15.0, 2.0, FALSE, 0, NULL), " // Arco magico
                        + "(12, 'SWORD', 8, 10.0, 2.0, FALSE, 0, NULL);");  // Spada
                stm.close();
            }

            // Verifica e inserimento CHARACTERS - Aggiornato con i personaggi corretti del gioco
            checkDataSql = "SELECT COUNT(*) FROM CHARACTERS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                // Inserimento personaggi effettivi del gioco
                stm.execute("INSERT INTO CHARACTERS VALUES "
                        + "(0, 'Giocatore', 'Il coraggioso avventuriero che si addentra nella Montagna Nera per salvare la principessa.', 'PLAYER', 100, 100, 15, 5, TRUE, 0), "
                        + "(1, 'Goblin', 'Una creatura malvagia dalla pelle verde scuro, con occhi pieni d''odio e artigli affilati.', 'GOBLIN', 40, 40, 12, 3, TRUE, 0), "
                        + "(2, 'Topo Gigante', 'Un enorme roditore con denti giallastri e occhi rossi.', 'GIANT_RAT', 25, 25, 8, 2, TRUE, 1), "
                        + "(3, 'Goblin Chiassoso', 'Un goblin aggressivo che litiga per un osso.', 'GOBLIN', 35, 35, 10, 3, TRUE, 2), "
                        + "(4, 'Goblin Rissoso', 'Un altro goblin altrettanto aggressivo.', 'GOBLIN', 30, 30, 9, 2, TRUE, 2), "
                        + "(5, 'Goblin Gigante', 'Un goblin enorme che impugna una clava insanguinata.', 'GOBLIN', 60, 60, 16, 5, TRUE, 4), "
                        + "(6, 'Goblin Minuto', 'Un goblin più piccolo ma altrettanto minaccioso.', 'GOBLIN', 25, 25, 8, 2, TRUE, 4), "
                        + "(7, 'Cane Demone', 'Una creatura infernale con zanne fumanti e occhi di fuoco.', 'DEMON_DOG', 120, 120, 25, 8, TRUE, 7);");
                stm.close();
            }

            // Verifica e inserimento ROOM_OBJECTS
            checkDataSql = "SELECT COUNT(*) FROM ROOM_OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            // Verifica e inserimento ROOM_OBJECTS - Posizionamento oggetti nelle stanze
            checkDataSql = "SELECT COUNT(*) FROM ROOM_OBJECTS";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();

            if (count == 0) {
                stm = conn.createStatement();
                // Posizionamento secondo data.sql
                stm.execute("INSERT INTO ROOM_OBJECTS VALUES "
                        + "(0, 100), " // Cassa nell'Ingresso
                        + "(1, 4), " // Stringhe ragnatela nella Stanza del Topo
                        + "(3, 101), " // Cassa nel Dormitorio
                        + "(4, 102), " // Cassa nella Sala delle Guardie
                        + "(6, 103);"); // Cassa nella Stanza delle Torture
                stm.close();
            }

            // Verifica e inserimento INVENTORY - Inventario iniziale del giocatore
            checkDataSql = "SELECT COUNT(*) FROM INVENTORY WHERE CHARACTER_ID = 0";
            stm = conn.createStatement();
            rs = stm.executeQuery(checkDataSql);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            if (count == 0) {
                // Oggetti contenuti nell'inventario del giocatore
                stm = conn.createStatement();
                stm.execute("INSERT INTO INVENTORY VALUES "
                        + "(0, 2), " // CHARACTER_ID=0 (giocatore), OBJECT_ID=2 (pozione di cura)
                        + "(0, 12);"); // CHARACTER_ID=0 (giocatore), OBJECT_ID=12 (spada)
                stm.close();
                System.out.println("Giocatore inizia con oggetti di base");
            }
            conn.close();
            System.out.println("✅ Database inizializzato con successo!");

        } catch (SQLException e) {
            System.err.println("❌ Errore nell'inizializzazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Metodo di utilità per testare la connessione
     */
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Connessione fallita: " + e.getMessage());
            return false;
        }
    }
}
