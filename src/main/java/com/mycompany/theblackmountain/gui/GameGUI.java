/*
 * GameGUI aggiornata con supporto per salvataggio e caricamento
 */
package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.save.GameSaveData;
import com.mycompany.theblackmountain.save.GameSaveManager;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.thread.Music;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;

/**
 * Interfaccia grafica principale del gioco con supporto per salvataggio
 * @author vince
 */
public class GameGUI extends JFrame {

    private final TBMGame game;
    private final Parser parser;

    private Thread musicThread;
    private Music music;

    private JTextArea outputArea;
    private JTextField inputField;
    private JButton northButton, southButton, eastButton, westButton;
    private JButton lookButton, inventoryButton, saveButton;
    private JPanel mapPanel;
    
    // Variabili per il tracking del tempo di gioco
    private long gameStartTime;
    private long totalPlayTime;

    /**
     * Costruttore per nuova partita
     */
    public GameGUI() throws Exception {
        this(null);
    }
    
    /**
     * Costruttore per partita caricata
     */
    public GameGUI(GameSaveData saveData) throws Exception {
        // Initialize game
        game = new TBMGame();
        game.init();
        
        // Se ci sono dati salvati, applicali
        if (saveData != null) {
            GameSaveManager.applyLoadedData(game, saveData, game.getCombatSystem());
            totalPlayTime = saveData.getPlayTimeMillis();
        } else {
            totalPlayTime = 0;
        }
        
        gameStartTime = System.currentTimeMillis();

        // Initialize parser with empty stopwords (replace with actual stopwords loading)
        parser = new Parser(new HashSet<>());

        // Set up the GUI
        setupUI();

        // Avvia la musica
        music = new Music();
        musicThread = new Thread(music);
        musicThread.start();

        // Display welcome message
        if (saveData == null) {
            appendToOutput("\n=== BENVENUTO IN THE BLACK MOUNTAIN ===");
            appendToOutput("Un'avventura testuale nella fortezza maledetta");
            appendToOutput("Usa i comandi per esplorare, combattere e sopravvivere!");
            appendToOutput("================================================\n");
        } else {
            appendToOutput("\n=== PARTITA CARICATA ===");
            appendToOutput("Benvenuto di nuovo, avventuriero!");
            appendToOutput("Tempo di gioco: " + saveData.getFormattedPlayTime());
            appendToOutput("========================\n");
        }
        
        appendToOutput(game.getCurrentRoom().getName());
        appendToOutput("================================================");
        appendToOutput(game.getCurrentRoom().getDescription());

        // Update directional buttons based on available exits
        updateDirectionalButtons();
    }

    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1900, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Imposta font
        Font gameFont = loadCustomFont(16f);

        // Imposta l'icona della finestra
        // TODO: Inserire path per l'icona del gioco
        // Image icon = Toolkit.getDefaultToolkit().getImage("path/to/game_icon.png");
        // setIconImage(icon);

        setVisible(true);

        // Menu bar
        setupMenuBar();

        // Output text area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(gameFont);
        outputArea.setBackground(new Color(20, 20, 30));
        outputArea.setForeground(new Color(220, 220, 200));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(40, 40, 50));
        
        inputField = new JTextField();
        inputField.setFont(gameFont);
        inputField.setBackground(new Color(60, 60, 70));
        inputField.setForeground(Color.WHITE);
        inputField.addActionListener(this::processInput);
        
        JButton submitButton = new JButton("Invio");
        submitButton.addActionListener(this::processInput);
        submitButton.setBackground(new Color(80, 40, 120));
        submitButton.setForeground(Color.WHITE);
        
        inputPanel.add(new JLabel("Comando: "), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Direction buttons panel
        JPanel directionPanel = new JPanel(new GridLayout(4, 3));
        directionPanel.setBackground(new Color(30, 30, 40));

        // Riga 1: Empty, North, Empty
        directionPanel.add(new JLabel());
        northButton = createDirectionButton("Nord", "⬆");
        northButton.addActionListener(e -> moveDirection("nord"));
        directionPanel.add(northButton);
        directionPanel.add(new JLabel());

        // Riga 2: West, Look, East
        westButton = createDirectionButton("Ovest", "⬅");
        westButton.addActionListener(e -> moveDirection("ovest"));
        directionPanel.add(westButton);

        lookButton = createActionButton("Osserva", "👁");
        lookButton.addActionListener(e -> look());
        directionPanel.add(lookButton);

        eastButton = createDirectionButton("Est", "➡");
        eastButton.addActionListener(e -> moveDirection("est"));
        directionPanel.add(eastButton);

        // Riga 3: Empty, South, Empty
        directionPanel.add(new JLabel());
        southButton = createDirectionButton("Sud", "⬇");
        southButton.addActionListener(e -> moveDirection("sud"));
        directionPanel.add(southButton);
        directionPanel.add(new JLabel());

        // Riga 4: Inventory, Save, Attack
        inventoryButton = createActionButton("Inventario", "🎒");
        inventoryButton.addActionListener(e -> showInventory());
        directionPanel.add(inventoryButton);

        saveButton = createActionButton("Salva", "💾");
        saveButton.addActionListener(e -> saveGame());
        directionPanel.add(saveButton);

        JButton attackButton = createActionButton("Attacca", "⚔");
        attackButton.addActionListener(e -> attack());
        directionPanel.add(attackButton);

        // Add direction panel to the right
        add(directionPanel, BorderLayout.EAST);

        // Handle window closing to clean up resources
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (game != null) {
                    game.cleanup();
                }
                if (music != null) {
                    music.pausa();
                }
            }
        });
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(50, 50, 60));
        
        // Menu File
        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(Color.WHITE);
        
        JMenuItem saveMenuItem = new JMenuItem("Salva Partita");
        saveMenuItem.addActionListener(e -> saveGame());
        // TODO: Inserire path per l'icona salva
        // saveMenuItem.setIcon(new ImageIcon("path/to/save_icon.png"));
        
        JMenuItem loadMenuItem = new JMenuItem("Carica Partita");
        loadMenuItem.addActionListener(e -> loadGame());
        // TODO: Inserire path per l'icona carica
        // loadMenuItem.setIcon(new ImageIcon("path/to/load_icon.png"));
        
        JMenuItem exitMenuItem = new JMenuItem("Esci");
        exitMenuItem.addActionListener(e -> exitToMenu());
        // TODO: Inserire path per l'icona esci
        // exitMenuItem.setIcon(new ImageIcon("path/to/exit_icon.png"));
        
        fileMenu.add(saveMenuItem);
        fileMenu.add(loadMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        
        // Menu Aiuto
        JMenu helpMenu = new JMenu("Aiuto");
        helpMenu.setForeground(Color.WHITE);
        
        JMenuItem commandsMenuItem = new JMenuItem("Comandi");
        commandsMenuItem.addActionListener(e -> showCommands());
        
        JMenuItem aboutMenuItem = new JMenuItem("Info");
        aboutMenuItem.addActionListener(e -> showAbout());
        
        helpMenu.add(commandsMenuItem);
        helpMenu.add(aboutMenuItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }

    private JButton createDirectionButton(String text, String symbol) {
        JButton button = new JButton("<html><center>" + symbol + "<br>" + text + "</center></html>");
        button.setPreferredSize(new Dimension(100, 60));
        button.setBackground(new Color(70, 70, 80));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private JButton createActionButton(String text, String symbol) {
        JButton button = new JButton("<html><center>" + symbol + "<br>" + text + "</center></html>");
        button.setPreferredSize(new Dimension(100, 60));
        button.setBackground(new Color(80, 40, 120));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void processInput(ActionEvent e) {
        String command = inputField.getText().trim();
        if (command.isEmpty()) {
            return;
        }

        // Echo the command
        appendToOutput("\n> " + command);

        // Parse and process the command
        ParserOutput output = parser.parse(command, game.getCommands(),
                game.getCurrentRoom().getObjects(),
                game.getInventory());

        // Custom PrintStream that redirects to our output area
        PrintStream out = new PrintStream(System.out) {
            @Override
            public void println(String x) {
                appendToOutput(x);
            }
        };

        // Process the move
        game.nextMove(output, out);

        // Update UI after move
        updateDirectionalButtons();

        // Clear input field
        inputField.setText("");
    }

    private void moveDirection(String direction) {
        inputField.setText(direction);
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, direction));
    }

    private void look() {
        inputField.setText("osserva");
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "osserva"));
    }

    private void showInventory() {
        inputField.setText("inventario");
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "inventario"));
    }
    
    private void attack() {
        inputField.setText("attacca");
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "attacca"));
    }

    private void saveGame() {
        String saveName = JOptionPane.showInputDialog(this, 
                "Inserisci un nome per il salvataggio:", 
                "Salva Partita", 
                JOptionPane.QUESTION_MESSAGE);
        
        if (saveName != null && !saveName.trim().isEmpty()) {
            long currentPlayTime = totalPlayTime + (System.currentTimeMillis() - gameStartTime);
            
            boolean success = GameSaveManager.saveGame(game, game.getCombatSystem(), saveName.trim(), currentPlayTime);
            
            if (success) {
                appendToOutput("\n💾 Partita salvata con successo: " + saveName);
            } else {
                appendToOutput("\n❌ Errore nel salvataggio della partita!");
                JOptionPane.showMessageDialog(this, "Errore nel salvataggio della partita!", 
                        "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadGame() {
        JFileChooser fileChooser = new JFileChooser("saves/");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "File di salvataggio TBM", "tbm"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                GameSaveData saveData = GameSaveManager.loadGame(fileChooser.getSelectedFile());
                
                // Chiudi la finestra corrente
                this.dispose();
                
                // Apri una nuova finestra con i dati caricati
                GameGUI newGameGUI = new GameGUI(saveData);
                newGameGUI.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Errore nel caricamento: " + ex.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exitToMenu() {
        int choice = JOptionPane.showConfirmDialog(this, 
                "Vuoi salvare prima di uscire?", 
                "Esci", 
                JOptionPane.YES_NO_CANCEL_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            saveGame();
            returnToMainMenu();
        } else if (choice == JOptionPane.NO_OPTION) {
            returnToMainMenu();
        }
        // Se CANCEL, non fare nulla
    }
    
    private void returnToMainMenu() {
        if (music != null) {
            music.pausa();
        }
        this.dispose();
        SwingUtilities.invokeLater(() -> {
            MainMenu mainMenu = new MainMenu();
            mainMenu.setVisible(true);
        });
    }
    
    private void showCommands() {
        String commandsText = """
                COMANDI DISPONIBILI:
                
                MOVIMENTO:
                • nord, sud, est, ovest - Muoviti nelle direzioni
                • n, s, e, o - Abbreviazioni per le direzioni
                
                AZIONI:
                • osserva, guarda - Osserva l'ambiente
                • inventario, inv - Mostra l'inventario
                • raccogli [oggetto] - Raccogli un oggetto
                • usa [oggetto] - Utilizza un oggetto
                • apri [oggetto] - Apri contenitori
                • attacca - Inizia/continua il combattimento
                
                SISTEMA:
                • end, esci - Termina il gioco
                
                COMBATTIMENTO:
                • attacca - Attacca i nemici
                • usa [arma] - Usa un'arma specifica
                • usa cura - Usa una pozione di cura
                """;
        
        JTextArea textArea = new JTextArea(commandsText);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Comandi di Gioco", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAbout() {
        String aboutText = """
                THE BLACK MOUNTAIN
                Versione 1.0
                
                Un'avventura testuale nella fortezza maledetta.
                Esplora, combatti e sopravvivi per salvare la principessa!
                
                Sviluppato con Java Swing
                © 2024
                """;
        
        JOptionPane.showMessageDialog(this, aboutText, "Informazioni", JOptionPane.INFORMATION_MESSAGE);
    }

    private void appendToOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");
            // Scroll to bottom
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void updateDirectionalButtons() {
        Room currentRoom = game.getCurrentRoom();
        if (currentRoom == null) {
            return;
        }

        northButton.setEnabled(currentRoom.getNorth() != null);
        southButton.setEnabled(currentRoom.getSouth() != null);
        eastButton.setEnabled(currentRoom.getEast() != null);
        westButton.setEnabled(currentRoom.getWest() != null);
    }

    private Font loadCustomFont(float size) {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is == null) {
                System.err.println("⚠️ Font non trovato!");
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }
}