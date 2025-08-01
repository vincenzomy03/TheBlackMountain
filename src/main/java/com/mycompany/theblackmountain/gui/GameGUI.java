package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.save.SaveManager;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.thread.MusicManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;

public class GameGUI extends JFrame {

    private final TBMGame game;
    private final Parser parser;

    private JTextArea outputArea;
    private JTextField inputField;
    private JButton northButton, southButton, eastButton, westButton;
    private JButton lookButton, inventoryButton, saveButton, attackButton, soundToggleButton;
    private JLabel mapLabel;

    private long gameStartTime;
    private long totalPlayTime;

    public GameGUI() throws Exception {
        this(null);
    }

    public GameGUI(String saveData) throws Exception {
        game = new TBMGame();
        game.init();

        if (saveData != null) {
            SaveManager.applyLoadedData(game, saveData, game.getCombatSystem());
            for (String line : saveData.split("\n")) {
                if (line.startsWith("play.time=")) {
                    try {
                        totalPlayTime = Long.parseLong(line.substring("play.time=".length()));
                    } catch (NumberFormatException e) {
                        totalPlayTime = 0;
                    }
                    break;
                }
            }
        } else {
            totalPlayTime = 0;
        }

        gameStartTime = System.currentTimeMillis();
        parser = new Parser(new HashSet<>());

        setupUI();

        if (saveData == null) {
            appendToOutput("\n=== BENVENUTO IN THE BLACK MOUNTAIN ===");
            appendToOutput("Un'avventura testuale nella fortezza maledetta");
            appendToOutput("Usa i comandi per esplorare, combattere e sopravvivere!");
            appendToOutput("================================================\n");
        } else {
            appendToOutput("\n=== PARTITA CARICATA ===");
            appendToOutput("Benvenuto di nuovo, avventuriero!");
            long seconds = totalPlayTime / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            String formattedTime = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
            appendToOutput("Tempo di gioco: " + formattedTime);
            appendToOutput("========================\n");
        }

        appendToOutput(game.getCurrentRoom().getName());
        appendToOutput("================================================");
        appendToOutput(game.getCurrentRoom().getDescription());

        updateDirectionalButtons();
        updateMapImage();
    }

    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1900, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Font gameFont = loadCustomFont(16f);

        // === CENTRO: Output + Mappa ===
        JPanel centerPanel = new JPanel(new BorderLayout());

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(gameFont);
        outputArea.setBackground(new Color(20, 20, 30));
        outputArea.setForeground(new Color(220, 220, 200));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(1200, 600));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Mappa
        JPanel mapPanel = new JPanel(new BorderLayout());
        mapPanel.setPreferredSize(new Dimension(600, 600));
        mapPanel.setBackground(new Color(15, 15, 25));
        mapLabel = new JLabel("Caricamento mappa...", SwingConstants.CENTER);
        mapLabel.setForeground(Color.WHITE);
        mapLabel.setFont(gameFont);
        mapPanel.add(mapLabel, BorderLayout.CENTER);
        centerPanel.add(mapPanel, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        // === SUD: Input + Azioni ===
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        // Input
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
        inputPanel.add(new JLabel(" Comando: "), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        bottomPanel.add(inputPanel);

        // Azioni
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionPanel.setBackground(new Color(30, 30, 40));

        // TODO: Assicurati che i path siano corretti
        inventoryButton = createImageButton("/img/inventory1.png", "Inventario", "", e -> showInventory());
        attackButton = createImageButton("/img/attack.png", "Attacca", "", e -> attack());
        saveButton = createImageButton("/img/save1.png", "Salva", "", e -> saveGame());
        soundToggleButton = createImageButton("/img/volume_on1.png", "Volume", "", e -> {
            MusicManager musicManager = MusicManager.getInstance();
            musicManager.setMusicEnabled(!musicManager.isMusicEnabled());
            updateSoundButton();
        });

        actionPanel.add(inventoryButton);
        actionPanel.add(attackButton);
        actionPanel.add(saveButton);
        actionPanel.add(soundToggleButton);
        bottomPanel.add(actionPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // === DIREZIONI ===
        JPanel directionPanel = new JPanel(new GridLayout(3, 3));
        directionPanel.setPreferredSize(new Dimension(300, 200));
        directionPanel.setBackground(new Color(25, 25, 35));

        // TODO: Immagini frecce + osserva
        directionPanel.add(new JLabel());
        northButton = createImageButton("/img/up.png", "Vai a nord", "", e -> moveDirection("nord"));
        directionPanel.add(northButton);
        directionPanel.add(new JLabel());

        westButton = createImageButton("/img/left.png", "Vai a ovest", "", e -> moveDirection("ovest"));
        directionPanel.add(westButton);

        lookButton = createImageButton("/img/look.png", "Osserva", "", e -> look());
        directionPanel.add(lookButton);

        eastButton = createImageButton("/img/right.png", "Vai a est", "", e -> moveDirection("est"));
        directionPanel.add(eastButton);

        directionPanel.add(new JLabel());
        southButton = createImageButton("/img/down.png", "Vai a sud", "", e -> moveDirection("sud"));
        directionPanel.add(southButton);
        directionPanel.add(new JLabel());

        add(directionPanel, BorderLayout.WEST);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (game != null) game.cleanup();
            }
        });

        setVisible(true);
    }

    private JButton createImageButton(String imagePath, String tooltip, String text, ActionListener action) {
        JButton button = new JButton();

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath)); // TODO: Controlla il path
            Image scaled = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            System.err.println("Errore caricamento immagine: " + imagePath);
        }

        if (text != null && !text.isEmpty()) {
            button.setText("<html><center>" + text + "</center></html>");
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setFocusPainted(false);
        button.setBackground(new Color(80, 40, 120));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(80, 80));
        return button;
    }

    private void updateSoundButton() {
        if (MusicManager.getInstance().isMusicEnabled()) {
            soundToggleButton.setText("<html><center>🔊<br>Volume</center></html>");
        } else {
            soundToggleButton.setText("<html><center>🔇<br>Muto</center></html>");
        }
    }

    private void processInput(ActionEvent e) {
        String command = inputField.getText().trim();
        if (command.isEmpty()) return;

        appendToOutput("\n> " + command);

        ParserOutput output = parser.parse(command, game.getCommands(), game.getCurrentRoom().getObjects(), game.getInventory());

        PrintStream out = new PrintStream(System.out) {
            @Override public void println(String x) { appendToOutput(x); }
        };

        game.nextMove(output, out);
        updateDirectionalButtons();
        updateMapImage();
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
        String saveName = JOptionPane.showInputDialog(this, "Inserisci un nome per il salvataggio:", "Salva Partita", JOptionPane.QUESTION_MESSAGE);
        if (saveName != null && !saveName.trim().isEmpty()) {
            long currentPlayTime = totalPlayTime + (System.currentTimeMillis() - gameStartTime);
            boolean success = SaveManager.saveGame(game, game.getCombatSystem(), saveName.trim(), currentPlayTime);
            if (success) {
                appendToOutput("\n Partita salvata con successo: " + saveName);
            } else {
                appendToOutput("\n Errore nel salvataggio della partita!");
                JOptionPane.showMessageDialog(this, "Errore nel salvataggio!", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateDirectionalButtons() {
        Room currentRoom = game.getCurrentRoom();
        if (currentRoom == null) return;
        northButton.setEnabled(currentRoom.getNorth() != null);
        southButton.setEnabled(currentRoom.getSouth() != null);
        eastButton.setEnabled(currentRoom.getEast() != null);
        westButton.setEnabled(currentRoom.getWest() != null);
    }

    private void appendToOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void updateMapImage() {
        String roomName = game.getCurrentRoom().getName().toLowerCase().replace(" ", "_");
        // TODO: Cambia path se diverso
        String imagePath = "/img/maps/" + roomName + ".png";
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            Image scaled = icon.getImage().getScaledInstance(580, 580, Image.SCALE_SMOOTH);
            mapLabel.setIcon(new ImageIcon(scaled));
            mapLabel.setText("");
        } catch (Exception e) {
            System.err.println("⚠️ Mappa non trovata: " + imagePath);
            mapLabel.setIcon(null);
            mapLabel.setText("Mappa non disponibile");
        }
    }

    private Font loadCustomFont(float size) {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf"); // TODO: verifica path font
            if (is == null) return new Font("SansSerif", Font.PLAIN, (int) size);
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }
}
