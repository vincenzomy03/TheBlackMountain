package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.save.SaveManager;
import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.thread.MusicManager;
import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;

public class GameGUI extends JFrame {

    private final TBMGame game;
    private final Parser parser;

    // Componenti UI
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton northButton, southButton, eastButton, westButton;
    private JButton lookButton, inventoryButton, saveButton, soundToggleButton;
    private JButton useAttackButton, useSwordButton, useBowButton, usePotionButton;
    private UIComponents.MapPanel mapPanel;
    private UIComponents.BackgroundPanel backgroundPanel;

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
        initializeGame(saveData);
    }

    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1900, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Carica font personalizzato
        Font gameFont = loadCustomFont(16f);

        // === PANNELLO PRINCIPALE CON SFONDO ===
        backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "game_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);

        // === CENTRO: Output + Mappa ===
        JPanel centerPanel = UIComponents.createThemedPanel(
                new Color(0, 0, 0, 100), // Trasparente per mostrare lo sfondo
                new BorderLayout()
        );

        // Area di output
        outputArea = UIComponents.createOutputArea(gameFont);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(1200, 600));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIComponents.TEXT_COLOR, 1),
                "Console di Gioco",
                0, 0, gameFont, UIComponents.TEXT_COLOR
        ));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Pannello mappa
        mapPanel = UIComponents.createMapPanel(800, 600);
        mapPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIComponents.TEXT_COLOR, 1),
                "Mappa",
                0, 0, gameFont, UIComponents.TEXT_COLOR
        ));
        centerPanel.add(mapPanel, BorderLayout.EAST);

        backgroundPanel.add(centerPanel, BorderLayout.CENTER);

        // === SUD: Solo Input (senza pulsanti) ===
        JPanel bottomPanel = UIComponents.createThemedPanel(
                new Color(40, 40, 50, 200), // Semi-trasparente
                new BorderLayout()
        );
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel commandLabel = new JLabel(" Comando: ");
        commandLabel.setForeground(UIComponents.TEXT_COLOR);
        commandLabel.setFont(gameFont);

        inputField = UIComponents.createInputField(gameFont, this::processInput);

        bottomPanel.add(commandLabel, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        // === OVEST: Pulsanti azione centrati + Frecce direzionali ===
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));
        westPanel.setOpaque(false);
        westPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === Pulsanti azione centrati ===
        JPanel actionButtonsPanel = new JPanel();
        actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.Y_AXIS));
        actionButtonsPanel.setOpaque(false);

        int actionButtonSpacing = 5;

        // Crea pulsanti azione
        inventoryButton = UIComponents.createActionButton(
                UIComponents.ActionType.INVENTORY, e -> performAction("inventario")
        );
        useAttackButton = UIComponents.createActionButton(
                UIComponents.ActionType.ATTACK, e -> performAction("attacca")
        );
        useSwordButton = UIComponents.createActionButton(
                UIComponents.ActionType.USE_SWORD, e -> performAction("usa spada")
        );
        useBowButton = UIComponents.createActionButton(
                UIComponents.ActionType.USE_BOW, e -> performAction("usa arco")
        );
        usePotionButton = UIComponents.createActionButton(
                UIComponents.ActionType.USE_POTION, e -> performAction("usa pozione di cura")
        );
        lookButton = UIComponents.createActionButton(
                UIComponents.ActionType.LOOK, e -> performAction("osserva")
        );
        saveButton = UIComponents.createActionButton(
                UIComponents.ActionType.SAVE, e -> saveGame()
        );
        soundToggleButton = UIComponents.createActionButton(
                UIComponents.ActionType.SOUND_ON, e -> toggleSound()
        );

        // Aggiungi pulsanti centrati
        actionButtonsPanel.add(createCenteredComponent(saveButton));
        actionButtonsPanel.add(createCenteredComponent(soundToggleButton));
        actionButtonsPanel.add(Box.createVerticalStrut(actionButtonSpacing));
        actionButtonsPanel.add(createCenteredComponent(inventoryButton));
        actionButtonsPanel.add(Box.createVerticalStrut(actionButtonSpacing));
        actionButtonsPanel.add(createCenteredComponent(useAttackButton));
        actionButtonsPanel.add(Box.createVerticalStrut(actionButtonSpacing));
        actionButtonsPanel.add(createCenteredComponent(useSwordButton));
        actionButtonsPanel.add(Box.createVerticalStrut(actionButtonSpacing));
        actionButtonsPanel.add(createCenteredComponent(useBowButton));
        actionButtonsPanel.add(Box.createVerticalStrut(actionButtonSpacing));
        actionButtonsPanel.add(createCenteredComponent(usePotionButton));

        // === Frecce direzionali ===
        JPanel directionPanel = new JPanel(new GridLayout(3, 3, 2, 2));
        directionPanel.setOpaque(false);
        directionPanel.setPreferredSize(new Dimension(150, 150));

        // Layout 3x3 per i pulsanti di direzione
        directionPanel.add(new JLabel()); // vuoto
        northButton = UIComponents.createDirectionButton("nord", e -> moveDirection("nord"));
        directionPanel.add(northButton);
        directionPanel.add(new JLabel()); // vuoto

        westButton = UIComponents.createDirectionButton("ovest", e -> moveDirection("ovest"));
        directionPanel.add(westButton);

        JButton centerLookButton = UIComponents.createActionButton(
                UIComponents.ActionType.LOOK,
                e -> performAction("osserva")
        );
        directionPanel.add(centerLookButton);

        eastButton = UIComponents.createDirectionButton("est", e -> moveDirection("est"));
        directionPanel.add(eastButton);

        directionPanel.add(new JLabel()); // vuoto
        southButton = UIComponents.createDirectionButton("sud", e -> moveDirection("sud"));
        directionPanel.add(southButton);
        directionPanel.add(new JLabel()); // vuoto

        // Contenitore per direzioni centrato in basso
        JPanel directionWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        directionWrapper.setOpaque(false);
        directionWrapper.add(directionPanel);

        // === Assemblaggio pannello ovest ===
        westPanel.add(actionButtonsPanel);        // Pulsanti azione in alto
        westPanel.add(Box.createVerticalGlue());  // Spazio flessibile
        westPanel.add(directionWrapper);          // Frecce in fondo

        backgroundPanel.add(westPanel, BorderLayout.WEST);

        // Listener per chiusura finestra
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (game != null) {
                    game.cleanup();
                }
                MusicManager.getInstance().stopMusic();
            }
        });

        setVisible(true);
    }

    /**
     * Crea un pannello che centra un componente
     */
    private JPanel createCenteredComponent(JComponent component) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(component);
        return wrapper;
    }

    private void initializeGame(String saveData) {
        if (saveData == null) {
            appendToOutput("=== BENVENUTO IN THE BLACK MOUNTAIN ===");
            appendToOutput("Un'avventura testuale nella fortezza maledetta");
            appendToOutput("Usa i comandi per esplorare, combattere e sopravvivere!");
            appendToOutput("================================================\n");
        } else {
            appendToOutput("=== PARTITA CARICATA ===");
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

        updateUI();
    }

    private void processInput(ActionEvent e) {
        String command = inputField.getText().trim();
        if (command.isEmpty()) {
            return;
        }

        appendToOutput("\n> " + command);

        ParserOutput output = parser.parse(command, game.getCommands(),
                game.getCurrentRoom().getObjects(), game.getInventory());

        PrintStream out = new PrintStream(System.out) {
            @Override
            public void println(String x) {
                appendToOutput(x);
            }
        };

        game.nextMove(output, out);
        updateUI();
        inputField.setText("");
        inputField.requestFocus();
    }

    private void performAction(String action) {
        inputField.setText(action);
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action));
    }

    private void moveDirection(String direction) {
        performAction(direction);
    }

    private void saveGame() {
        String saveName = JOptionPane.showInputDialog(this,
                "Inserisci un nome per il salvataggio:",
                "Salva Partita",
                JOptionPane.QUESTION_MESSAGE);

        if (saveName != null && !saveName.trim().isEmpty()) {
            long currentPlayTime = totalPlayTime + (System.currentTimeMillis() - gameStartTime);
            boolean success = SaveManager.saveGame(game, game.getCombatSystem(),
                    saveName.trim(), currentPlayTime);

            if (success) {
                appendToOutput("\nPartita salvata con successo: " + saveName);
                JOptionPane.showMessageDialog(this,
                        "Partita salvata con successo!",
                        "Salvataggio",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                appendToOutput("\nErrore nel salvataggio della partita!");
                JOptionPane.showMessageDialog(this,
                        "Errore nel salvataggio!",
                        "Errore",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleSound() {
        MusicManager musicManager = MusicManager.getInstance();
        musicManager.setMusicEnabled(!musicManager.isMusicEnabled());
        updateSoundButton();
    }

    private void updateSoundButton() {
        boolean musicEnabled = MusicManager.getInstance().isMusicEnabled();

        // Rimuovi il pulsante esistente
        Container parent = soundToggleButton.getParent();
        if (parent != null) {
            parent.remove(soundToggleButton);

            // Crea nuovo pulsante con icona corretta
            soundToggleButton = UIComponents.createActionButton(
                    musicEnabled ? UIComponents.ActionType.SOUND_ON : UIComponents.ActionType.SOUND_OFF,
                    e -> toggleSound()
            );

            // Ricrea il wrapper centrato
            JPanel centeredWrapper = createCenteredComponent(soundToggleButton);
            parent.add(centeredWrapper);
            parent.revalidate();
            parent.repaint();
        }
    }

    private void updateUI() {
        updateDirectionalButtons();
        updateMapDisplay();
        updateWeaponButtons();
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

        // Cambia opacità per pulsanti disabilitati
        setButtonOpacity(northButton, currentRoom.getNorth() != null);
        setButtonOpacity(southButton, currentRoom.getSouth() != null);
        setButtonOpacity(eastButton, currentRoom.getEast() != null);
        setButtonOpacity(westButton, currentRoom.getWest() != null);
    }

    private void setButtonOpacity(JButton button, boolean enabled) {
        if (enabled) {
            button.setBackground(UIComponents.ACCENT_COLOR);
        } else {
            button.setBackground(new Color(UIComponents.ACCENT_COLOR.getRed(),
                    UIComponents.ACCENT_COLOR.getGreen(),
                    UIComponents.ACCENT_COLOR.getBlue(), 100));
        }
    }

    private void updateMapDisplay() {
        if (game.getCurrentRoom() != null) {
            mapPanel.updateMap(game.getCurrentRoom().getName());
        }
    }

    /**
     * Aggiorna la disponibilità dei pulsanti arma in base all'inventario
     */
    private void updateWeaponButtons() {
        // Controlla se il giocatore ha l'arco nell'inventario
        boolean hasBow = game.getInventory().stream()
                .anyMatch(item -> item.getName().toLowerCase().contains("arco"));

        boolean hasPotion = game.getInventory().stream() // controllo pozione
                .anyMatch(item -> item.getName().toLowerCase().contains("pozione"));

        // Il pulsante spada è sempre disponibile (il giocatore inizia con una spada)
        useSwordButton.setEnabled(true);
        useAttackButton.setEnabled(true);

        // Il pulsante è disponibile solo se si ha l'oggetto
        useBowButton.setEnabled(hasBow);
        usePotionButton.setEnabled(hasPotion);

        // Cambia l'opacità per mostrare visivamente la disponibilità
        setButtonOpacity(useSwordButton, true);
        setButtonOpacity(useAttackButton, true);
        setButtonOpacity(useBowButton, hasBow);
        setButtonOpacity(usePotionButton, hasPotion);

    }

    private void appendToOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private Font loadCustomFont(float size) {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is == null) {
                System.err.println("Font personalizzato non trovato, uso font di sistema");
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            System.err.println("Errore caricamento font: " + e.getMessage());
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }

    /**
     * Metodo per debug - mostra informazioni sulla cache delle immagini
     */
    public void showImageCacheInfo() {
        UIImageManager imageManager = UIImageManager.getInstance();
        System.out.println("=== INFO CACHE IMMAGINI ===");
        System.out.println(imageManager.getCacheInfo());
        System.out.println("===========================");
    }

    /**
     * Metodo per ricaricare le immagini (utile durante lo sviluppo)
     */
    public void reloadImages() {
        UIImageManager.getInstance().clearCache();
        updateUI();
        repaint();
        System.out.println("Immagini ricaricate");
    }
}
