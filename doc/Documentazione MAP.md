# The Black Mountain
### Indice
1. [**Introduzione**](#1-introduzione)
2. [**Progettazione**](#2-progettazione)
3. [**Specifiche Algebriche**](#3-specifiche-algebriche)
   - 3.1 [**Specifiche algebriche della Lista**](#31---specifica-algebrica-della-lista)
4. [**Applicazione Argomenti del Corso**](#4-applicazione-argomenti-del-corso)
   - 4.1 [**File**](#file)
   - 4.2 [**Thread**](#thread)
   - 4.3 [**Database**](#database)
   - 4.4 [**Lambda Expressions**](#lambda-expressions)
   - 4.5 [**Graphic User Interface**](#graphic-user-interface---swing)
5. [**Manuale Utente**](#5-manuale-utente)



# 1. Introduzione

Questo progetto è stato sviluppato come prova per il conseguimento del corso di Metodi Avanzati di Programmazione, che punta a formare gli studenti sulla programmazione ad oggetti e a formarsi individualmente su nuovi argomenti richiedendone l'utilizzo per la realizzazione di questo progetto.

Nello specifico il progetto richiede la realizzazione di un'avventura prevalentemente testuale, con l'aggiunta di una Graphic User Interface per la visualizzazione delle informazioni principali del gioco.

Le avventure testuali sono una tipologia di videogioco che si incentra molto sulla narrazione e sulla risoluzione di enigmi, infatti non si disponeva di engine di grafica per la realizzazione di texture e animazioni, nonostante ciò il genere ha avuto grande successo e si trattava di una novità, nonché i primi passi verso ciò che abbiamo oggi come giochi di ruolo.

### Caratteristiche Principali

- **Interfaccia Testuale**: l'interazione avviene tramite comandi scritti, "prendi pozione di cura", "osserva" ecc...
- **Descrizioni Dettagliate**: il gioco descrive le scene, gli oggetti e le azioni attraverso testi e delle immagini a nell’interfaccia
    

## Trama

Un’oscurità si è abbattuta sul regno: la principessa, fulgida erede della corona, è stata rapita dai goblin e trascinata nelle viscere della Montagna Nera, dimora di mostri e tenebre. Nessun cavaliere ha osato affrontare tale destino, finché il re ha invocato il tuo nome: avventuriero leggendario, plasmato dal coraggio e dalla sfida. Guidato dall’onore e dal richiamo dell’impresa, hai varcato i cancelli della montagna. Nei suoi abissi hai superato trappole mortali, combattuto orde di creature e avanzato tra corridoi in cui l’oscurità stessa sembrava respirare. Ogni vittoria ti ha condotto più vicino al cuore del male, fino al confronto con il Signore dei Goblin.

La battaglia fu lunga e furiosa, ma con l’ultimo colpo il silenzio tornò a regnare nelle profondità. La principessa, finalmente libera, ti accolse con gratitudine infinita.

Il tuo ritorno al regno fu trionfale: accolto come un eroe, consacrato come leggenda. Eppure, oltre le lodi e gli onori, resterà l’eco di un’impresa immortale: quella di colui che osò sfidare la Montagna Nera.

### Struttura del Progetto

- **Parser**: il giocatore interagisce attraverso comandi testuali che devono essere interpretati dal gioco, il parser è dunque una componente fondamentale
- **Thread**: si tratta di un progetto che richiede l'esecuzione contemporanea di più elementi insieme, come ad esempio la musica
- **File di Configurazione**: il gioco include la possibilità di salvare una partita in un dato momento e include la raccolta dei path delle immagini in un unico file di configurazione
- **Database**: il gioco registra le stanze, le descrizioni, gli oggetti e i personaggi su un database H2
- **GUI**: nonostante il gioco sia testuale, è stata implementata una GUI per visualizzare le informazioni principali del gioco

#### [Ritorna all'Indice](#indice)



# 2. Progettazione

La fase di progettazione si concentra sul definire le  classi e le interazioni tra di esse, in modo da avere una visione chiara  di come il progetto sarà strutturato.

Nello specifico si rappresenta la parte più significativa del progetto

**Diagramma delle classi**

Di seguito viene rappresentata la parte che gestisce la logica del videogioco, ovvero le classi che rappresentano le entità del gioco e le loro interazioni. Vengono quindi esclusi i componenti graficie e le classi di supporto.

![[pt1 UML.png]]

![[pt2 UML.png]]

**Descrizione delle classi**

1. **TBMGame** : Classe principale che coordina tutto il gioco, gestisce il loop di gioco, stato della partita e comunicazione tra componenti
2. **GameDescription** : Classe astratta base che mantiene lo stato fondamentale del gioco (stanze, inventario, comandi)
3. **GameObservable** : Interfaccia per implementare il pattern Observer nel gioco
4. **GameObserver** : Classe astratta base per tutti gli observer che processano i comandi del giocatore
5. **LookAt** : Observer che gestisce il comando "osserva" per descrivere l'ambiente circostante
6. **Move** : Observer che gestisce i movimenti del giocatore tra le stanze e la logica di uscita finale
7. **OpenInventory** : Observer che gestisce la visualizzazione dell'inventario del giocatore
8. **PickUp** : Observer che gestisce la raccolta di oggetti dall'ambiente e il trasferimento all'inventario
9. **Use** : Observer che gestisce l'utilizzo di oggetti, pozioni, armi e la creazione di nuovi items
10. **Open** : Observer che gestisce l'apertura di contenitori come casse e porte
11. **CombatObserver** : Observer che gestisce tutti i comandi relativi al combattimento e alle battaglie
12. **Room** : Rappresenta una stanza del gioco con descrizione, connessioni, oggetti e nemici presenti
13. **GameObjects** : Classe base per tutti gli oggetti del gioco (pozioni, chiavi, contenitori, etc.)
14. **Weapon** : Specializzazione di GameObjects per le armi con statistiche di combattimento e effetti speciali
15. **GameCharacter** : Rappresenta personaggi giocabili e non (player, nemici) con HP, attacco e difesa
16. **CombatSystem** : Gestisce tutta la logica del combattimento, turni, danni e risultati delle battaglie
17. **DoorSystem** : Gestisce le porte bloccate, chiavi necessarie e passaggi tra stanze
18. **TBMDatabase** : Singleton che gestisce la connessione e operazioni sul database del gioco
19. **GameLoader** : Carica e salva i dati del gioco dal/sul database, sincronizza stato memoria-database
20. **Command** : Rappresenta un comando disponibile nel gioco con tipo, nome e alias
21. **ParserOutput** : Struttura che contiene il risultato del parsing di un comando utente
22. **GameUtils** : Classe utility con metodi statici per operazioni comuni sugli oggetti di gioco
23. **CommandType** : Enum che definisce tutti i tipi di comando disponibili (NORD, SUD, PICK_UP, etc.)
24. **WeaponType** : Enum che categorizza i tipi di armi (SWORD, BOW, STAFF)
25. **CharacterType** : Enum che definisce i tipi di personaggi (PLAYER, GOBLIN, GIANT_RAT, DEMON_DOG)

#### [Ritorna all'Indice](#indice)



# 3. Specifiche Algebriche
Una delle strutture dati più utilizzate nel progetto sono la **Lista** e in questa sezione verrà presentata la sua specifica algebrica.

### 3.1 Specifica algebrica della Lista
Una lista è una struttura dati lineare che consente di memorizzare una sequenza di elementi in un ordine specifico, permettendo operazioni di inserimento, rimozione e accesso agli elementi basate sulla loro posizione nella sequenza.

Nel gioco _The Black Mountain_ la struttura dati **Lista** è stata utilizzata in più punti:
- per rappresentare la **collezione di stanze** caricate dal database;
- per gestire gli **oggetti contenuti in una stanza**;
- per memorizzare l’**inventario del giocatore**;
- per mantenere l’elenco dei **nemici presenti in una stanza**.

La seguente specifica algebrica formalizza in termini astratti le operazioni consentite sulla Lista, indipendentemente dal tipo di elemento memorizzato (stanze, oggetti, nemici, ecc.).

### Specifica sintattica

- **Tipi:**  `List`, `Element`, `Int`, `Boolean`
    
- **Operatori:**
    - `newList() -> List`  
        Crea una nuova lista vuota
        
    - `addElement(List, Element, Int) -> List`  
        Aggiunge un elemento in una posizione specificata
        
    - `isEmpty(List) -> Boolean`  
        Restituisce `true` se la lista è vuota, `false` altrimenti
        
    - `getSize(List) -> Int`  
        Restituisce la dimensione della lista
        
    - `getIndex(List, Element) -> Int`  
        Restituisce l’indice del primo elemento uguale a quello specificato
        
    - `getElement(List, Int) -> Element`  
        Restituisce l’elemento in una data posizione
        
    - `removeElement(List, Int) -> List`  
        Rimuove l’elemento in una posizione data
        
    - `contains(List, Element) -> Boolean`  
        Restituisce `true` se l’elemento è presente, `false` altrimenti
        

---

### Costruttori e Osservazioni

| Osservazioni           | `newList` | `addElement(l, el, i)`                                                   |
| ---------------------- | --------- | ------------------------------------------------------------------------ |
| `isEmpty(l)`           | `true`    | `false`                                                                  |
| `getSize(l)`           | `error`   | se `isEmpty(l)` allora `1` altrimenti `getSize(l) + 1`                   |
| `getIndex(l, el')`     | `error`   | se `el = el'` allora `i` altrimenti `getIndex(l, el')`                   |
| `getElement(l, i')`    | `error`   | se `i = i'` allora `el` altrimenti `getElement(l, i')`                   |
| `removeElement(l, i')` | `error`   | se `i = i'` allora `l` altrimenti `addElement(removeElement(l, i'), el)` |
| `contains(l, el')`     | `false`   | se `el = el'` allora `true` altrimenti `contains(l, el')`                |
### Specifica semantica

- **Dichiarazioni**
    - `l`, `l'`: `List`
    - `id`, `id'`: `Integer`
    - `el` `el'`: `Element`

- **Operazioni**
	- `isEmpty(newList) = true`
	- `isEmpty(addElement(l, el, i)) = false`
	- `getSize(addElement(l, el, i)) = if isEmpty(l) then 1 else getSize(l) + 1`
	- `getIndex(addElement(l, el, i), el') = if el = el' then i else getIndex(l, el')`
	- `getElement(addElement(l, el, i), i') = if i = i' then el else getElement(l, i')`
	- `removeElement(addElement(l, el, i), i') = if i = i' then l else addElement(removeElement(l, i'), el)`
	- `contains(newList, el') = false`
	- `contains(addElement(l, el, i), el') = if el = el' then true else contains(l, el')`

---

### Specifica di restrizione

- `getSize(newList)` = `error`
- `getIndex(newList, el')` = `error`
- `getElement(newList, id')` = `error`
- `removeElement(newList, id')` = `error`

#### [Ritorna all'Indice](#indice)



# 4. Applicazione Argomenti del Corso

## File

I file in Java sono rappresentazioni di percorsi del filesystem che permettono di leggere, scrivere e manipolare dati persistenti su disco. La classe `File` di Java fornisce un'astrazione per interagire con il filesystem del sistema operativo.

Nel progetto The Black Mountain, i file vengono utilizzati principalmente per il sistema di salvataggio e caricamento delle partite, oltre che per la gestione delle risorse audio e grafiche.

### Applicazioni varie

**Sistema di Salvataggio:**

```java
public static boolean saveGame(GameDescription gameDescription, CombatSystem combatSystem,
                           String saveName, long playTimeMillis) {
    try {
        // Percorso completo per salvataggi
        String saveDirPath = "src/main/saves";
        File saveDir = new File(saveDirPath);
        if (!saveDir.exists()) {
            boolean created = saveDir.mkdirs();
            System.out.println("Directory 'saves' creata? " + created);
        }

        // Salvataggio su file
        String filename = sanitizeFilename(saveName) + ".dat";
        File saveFile = new File(saveDir, filename);
        
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            saveData.store(fos, "The Black Mountain - Save Game");
        }
    } catch (Exception e) {
        System.err.println("Errore nel salvataggio: " + e.getMessage());
        return false;
    }
}
```

**Caricamento dei file di salvataggio:**

```java
public static String loadGame(File saveFile) throws Exception {
    Properties saveData = new Properties();
    
    try (FileInputStream fis = new FileInputStream(saveFile)) {
        saveData.load(fis);
    }
    
    System.out.println("Gioco caricato da: " + saveFile.getAbsolutePath());
    return result.toString();
}
```

**Gestione directory di salvataggio:**

```java
static {
    // Crea la directory dei salvataggi se non esiste
    File saveDir = new File(SAVE_DIRECTORY);
    if (!saveDir.exists()) {
        saveDir.mkdirs();
    }
}
```

---

## Thread

I thread in Java rappresentano flussi di esecuzione indipendenti che permettono l'esecuzione concorrente di più operazioni. Consentono di eseguire operazioni in background senza bloccare l'interfaccia utente principale.

Nel progetto, i thread sono utilizzati per la gestione della musica di background, permettendo la riproduzione audio senza interferire con il gameplay.

### Applicazioni varie

**Gestione Musica di Background:**

```java
public class MusicManager {
    private Clip audioClip;
    private boolean musicEnabled = true;
    
    public void startMusic() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(audioStream)
            );
            
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            // Avvia in loop su thread separato
            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            
        } catch (Exception e) {
            System.err.println("Errore avvio musica: " + e.getMessage());
        }
    }
}
```

**Musica Cinematica (Thread Audio):**

```java
public class CinematicMusicManager {
    private void playMusic(String fileName) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(audioStream)
            );

            currentClip = AudioSystem.getClip();
            currentClip.open(audioInputStream);
            currentClip.start(); // Eseguito su thread audio separato
            
        } catch (Exception e) {
            System.err.println("Errore riproduzione musica " + fileName);
        }
    }
}
```

---

## Database

Un database è un sistema organizzato per la memorizzazione, gestione e recupero di dati strutturati. Nel progetto utilizziamo H2, un database relazionale embedded scritto in Java.

Il database viene utilizzato per memorizzare tutti gli elementi del gioco: stanze, oggetti, personaggi, connessioni tra stanze, inventario del giocatore e stato del mondo di gioco.

### Applicazioni varie

**Configurazione Database:**

```java
public class TBMDatabase {
    private static final String DB_URL = "jdbc:h2:./data/theblackmountain;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
```

**Creazione Tabelle:**

```java
private void createTables(Connection conn) throws SQLException {
    Statement stm = conn.createStatement();
    stm.execute("CREATE TABLE IF NOT EXISTS ROOMS("
            + "ID INT PRIMARY KEY,"
            + "NAME VARCHAR(255) NOT NULL,"
            + "DESCRIPTION TEXT,"
            + "LOOK_DESCRIPTION TEXT,"
            + "VISIBLE BOOLEAN DEFAULT TRUE,"
            + "IMAGE_PATH VARCHAR(255));");
    
    stm.execute("CREATE TABLE IF NOT EXISTS OBJECTS("
            + "ID INT PRIMARY KEY,"
            + "NAME VARCHAR(255) NOT NULL,"
            + "DESCRIPTION TEXT,"
            + "ALIASES VARCHAR(500),"
            + "OPENABLE BOOLEAN DEFAULT FALSE,"
            + "PICKUPABLE BOOLEAN DEFAULT TRUE,"
            + "OBJECT_TYPE VARCHAR(50) DEFAULT 'NORMAL');");
}
```

**Inserimento Dati:**

```java
private void insertRoomsIfEmpty(Connection conn) throws SQLException {
    String sql = "INSERT INTO ROOMS (ID, NAME, DESCRIPTION, LOOK_DESCRIPTION, VISIBLE, IMAGE_PATH) VALUES (?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        // Stanza 0 - Ingresso
        stmt.setInt(1, 0);
        stmt.setString(2, "Ingresso della Fortezza");
        stmt.setString(3, "Ti trovi all'ingresso della fortezza maledetta...");
        stmt.setBoolean(5, true);
        stmt.setString(6, "entrance");
        stmt.addBatch();
        
        stmt.executeBatch();
    }
}
```

---

## Graphic User Interface - Swing

**Swing** è un toolkit per la creazione di interfacce grafiche in Java, è stato introdotto con Java 1.2 e permette di creare interfacce grafiche per applicazioni desktop in modo semplice e flessibile.
Swing è basato su **AWT** (Abstract Window Toolkit) e fornisce un set di componenti grafici più avanzati e personalizzabili rispetto ad AWT.

Basato su un modello di programmazione ad eventi, in cui i componenti generano eventi in risposta alle azioni dell'utente, e i listener catturano e gestiscono questi eventi.
La GUI è composta da un insieme di componenti grafici:
- Layout
- Dialoghi visivi
- Bottoni
### Applicazioni varie

**Creazione Menu Principale:**

```java
public class MainMenu extends JFrame {
    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Pannello con sfondo personalizzato
        backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "menu_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);
    }
    
    private JButton createImageMenuButton(String imageName, String text, String tooltip,
            Dimension size, ActionListener action) {
        
        JButton button = new JButton();
        button.setText(text);
        button.setPreferredSize(size);
        
        // Effetto hover con immagine
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (soundEffectsEnabled) {
                    playHoverSound();
                }
            }
        });
        
        return button;
    }
}
```

---

## Lambda Expression

Le lambda expressions sono state introdotte in Java 8 e permettono di scrivere codice più conciso e leggibile, permettendo di passare funzioni come argomenti ad altri metodi, e di scrivere funzioni anonime in modo più semplice.
Queste fanno parte di un nuovo paradigma di programmazione chiamato programmazione funzionale, che permette di scrivere codice più pulito e manutenibile e funzioni più efficienti.

Può essere trattata come un'istanza di un'interfaccia funzionale (un'interfaccia con un unico metodo astratto). La sintassi generale è:

- (parameters) -> expression         Se la lambda expression ha un solo statement
- (parameters) -> { statements; }    Se la lambda expression ha più di uno statement

Permettono di trattare le funzioni come oggetti, rendendo il codice più flessibile e modulare.

### Applicazioni varie

**Gestione Eventi Pulsanti:**

```java
// Creazione pulsanti con lambda per ActionListener
newGameButton = createImageMenuButton("new_game", "NUOVA PARTITA",
        "Inizia una nuova avventura", buttonSize, e -> startNewGame());

loadGameButton = createImageMenuButton("load_game", "CARICA PARTITA",
        "Continua un'avventura salvata", buttonSize, e -> loadGame());

exitButton = createImageMenuButton("exit", "ESCI",
        "Chiudi il gioco", buttonSize, e -> exitGame());
```

#### [Ritorna all'Indice](#indice)


# 5. Manuale Utente

![[ingresso_della_fortezza.png]]

Di seguito una piccola guida sul gioco così da poter avere sempre un riferimento nel caso di smarrimento:

- Primi passi:
    - Avviare il gioco
    - Iniziare una nuova partita o caricarne una salvata

#### L'ingresso
In questa stanza troviamo una cassa contenente "chiave d'ingresso" e "pozione di cura". Per poter proseguire bisognerà aprire la porta verso est, ma per farlo bisognerà prima sconfiggere il Goblin che blocca la porta. Solo una volta sconfitto potremo proseguire avanti

#### Stanza del topo
In questa stanza, prima di poter proseguire, bisognerà sconfiggere il topo gigante. Successivamente, se vogliamo, possiamo raccogliere la "ragnatela" per poterla poi utilizzare in seguito. Da qui possiamo prendere due strade, una lenta e sicura che ci porta a potenziarci un minimo prima dello scontro con il boss, l'altra più veloce e rischiosa, per veri temerari!

#### Mensa abbandonata
Se ci troviamo in questa stanza vuol dire che siamo cauti e non abbiamo sottovalutato il boss. In questa stanza dovremo affrontare due nemici prima di poter proseguire.

#### Dormitorio delle guardie
Questa è la prima stanza senza l'ombra di un nemico. Qui c'è una cassa da aprire contenente un "bastone" e una "pozione di cura totale".

#### Sala delle guardie
Questa stanza è abbastanza dura da superare. Ci sono due nemici molto più forti da sconfiggere. Una volta sconfitti però potremo aprire la cassa e prendere il "libro incantesimo del fuoco" e un'ampolla di "veleno". Il libro non è altro che un'arma magica in grado di generare palle di fuoco che fanno danno ad area. Il veleno può essere messo sulla spada per renderla più potente.

#### Sala degli incantesimi
Se nella stanza del topo e nel dormitorio delle guardie abbiamo preso ragnatela e bastone possiamo craftare un arco. Attenzione, per poterlo creare occorrerà offrire in sacrificio 20 HP, in cambio otterremo un'arma in grado di sconfiggere il boss finale.

#### Stanza delle torture
Questa è l'ultima stanza prima di affrontare il boss. Non ci sono nemici da sconfiggere ma una cassa da aprire. Al suo interno troveremo la "chiave cella principessa" per entrare nella stanza finale.

#### Sala del signore dei goblin
In quest'ampia stanza troveremo la principessa ad attenderci in una cella, sorvegliata da un cane demone. Il procedimento è semplice: impugna le tue armi e sconfiggilo! Una volta sconfitto dropperà la "chiave del collo del boss". Con questa chiave potremo finalmente liberare la principessa e uscire dalla Montagna.


Nel caso in cui non si dovessero conoscere determinati comandi o come costruire il comando nel gioco, basta cliccare sull'icona del **?** rosso e usciranno tutti i vari comandi.

Buon divertimento!
#### [Ritorna all'Indice](#indice)
