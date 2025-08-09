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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
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
    private JButton commandsButton;
    private JButton helpButton;
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
        commandsButton = UIComponents.createActionButton(
                UIComponents.ActionType.COMMANDS, e -> openCommandsDialog()
        );
        helpButton = UIComponents.createActionButton(
                UIComponents.ActionType.HELP, e -> helpGame()
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
        JPanel directionPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        directionPanel.setOpaque(false);
        directionPanel.setPreferredSize(new Dimension(150, 150));

        // Layout 3x3 per i pulsanti di direzione
        // Riga 1 (Nord al centro)
        directionPanel.add(new JLabel()); // vuoto
        northButton = UIComponents.createDirectionButton("nord", e -> moveDirection("nord"));
        directionPanel.add(northButton);
        directionPanel.add(new JLabel()); // vuoto

        // Riga 2 (Ovest - Osserva - Est)
        westButton = UIComponents.createDirectionButton("ovest", e -> moveDirection("ovest"));
        directionPanel.add(westButton);

        JButton centerLookButton = UIComponents.createActionButton(
                UIComponents.ActionType.LOOK,
                e -> performAction("osserva")
        );
        directionPanel.add(centerLookButton);

        eastButton = UIComponents.createDirectionButton("est", e -> moveDirection("est"));
        directionPanel.add(eastButton);

        // Riga 3 (Help - Sud - Comandi)
        helpButton = UIComponents.createActionButton(
                UIComponents.ActionType.HELP,
                e -> helpGame()
        );
        directionPanel.add(helpButton);

        southButton = UIComponents.createDirectionButton("sud", e -> moveDirection("sud"));
        directionPanel.add(southButton);

        commandsButton = UIComponents.createActionButton(
                UIComponents.ActionType.COMMANDS,
                e -> openCommandsDialog()
        );
        directionPanel.add(commandsButton);

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

        // ===== FIX PRINCIPALE =====
        // Crea un StringWriter per catturare l'output
        StringWriter stringWriter = new StringWriter();
        PrintStream out = new PrintStream(new OutputStream() {
            public void write(int b) {
                stringWriter.write(b);
            }

            public void write(byte[] b, int off, int len) {
                stringWriter.write(new String(b, off, len));
            }
        }) {
            @Override
            public void println(String x) {
                stringWriter.write(x + "\n");
            }

            @Override
            public void print(String x) {
                stringWriter.write(x);
            }
        };

        // Esegui il comando
        game.nextMove(output, out);

        // Stampa la risposta catturata
        String response = stringWriter.toString().trim();
        if (!response.isEmpty()) {
            appendToOutput(response);
        }
        // ==========================

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

    /**
     * Apre la finestra dei comandi aggiuntivi
     */
    private void openCommandsDialog() {
        CommandsDialog dialog = new CommandsDialog(this, command -> {
            // Callback per eseguire il comando selezionato
            performAction(command);
        });
        dialog.setVisible(true);
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

    /**
     * Mostra la finestra di aiuto con la lista dei comandi organizzata per
     * schede
     */
    private void helpGame() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(500, 400));

        // --- MOVIMENTO ---
        JTextArea movimentoArea = createHelpTextArea(
                "=== COMANDI DI MOVIMENTO ===\n\n"
                + "nord  - Spostati verso nord\n"
                + "sud   - Spostati verso sud\n"
                + "est   - Spostati verso est\n"
                + "ovest - Spostati verso ovest\n\n"
                + "Suggerimento: puoi usare anche le frecce direzionali sullo schermo."
        );

        tabbedPane.addTab("Movimento", new JScrollPane(movimentoArea));

        // --- INTERAZIONE ---
        JTextArea interazioneArea = createHelpTextArea(
                "=== COMANDI DI INTERAZIONE ===\n\n"
                + "osserva     - Descrive la stanza e gli oggetti presenti\n"
                + "inventario  - Mostra gli oggetti in tuo possesso\n"
                + "apri cassa  - Tenta di aprire una cassa nella stanza (se presente)\n"
        );
        tabbedPane.addTab("Interazione", new JScrollPane(interazioneArea));

        // --- COMBATTIMENTO ---
        JTextArea combattimentoArea = createHelpTextArea(
                "=== COMANDI DI COMBATTIMENTO ===\n\n"
                + "attacca              - Attacca un nemico presente nella stanza\n"
                + "usa spada            - Attacca usando la spada\n"
                + "usa arco             - Attacca usando l'arco (se lo possiedi)\n"
                + "usa pozione di cura  - Recupera punti vita usando una pozione"
        );
        tabbedPane.addTab("Combattimento", new JScrollPane(combattimentoArea));

        // --- ALTRO ---
        JTextArea altroArea = createHelpTextArea(
                "=== ALTRI COMANDI ===\n\n"
                + "salva    - Salva la partita corrente\n"
                + "comandi  - Apre il menu dei comandi aggiuntivi\n"
                + "help     - Mostra questa finestra di aiuto\n"
        );
        tabbedPane.addTab("Altro", new JScrollPane(altroArea));

        // Mostra dialog con le schede
        JOptionPane.showMessageDialog(
                this,
                tabbedPane,
                "Guida ai Comandi",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Crea un'area di testo formattata per la finestra di aiuto
     */
    private JTextArea createHelpTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(50, 50, 50));
        textArea.setForeground(Color.WHITE);
        textArea.setCaretPosition(0);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
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

    /**
     * Metodo per ricaricare specificamente i pulsanti azione
     */
    public void reloadActionButtons() {
        UIImageManager.getInstance().clearCache();

        // Ricrea tutti i pulsanti azione con le nuove immagini
        Container actionParent = inventoryButton.getParent();
        if (actionParent != null) {
            // Rimuovi tutti i componenti
            actionParent.removeAll();

            // Ricrea i pulsanti con le immagini aggiornate
            int actionButtonSpacing = 5;

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

            // Ricrea anche il pulsante look
            lookButton = UIComponents.createActionButton(
                    UIComponents.ActionType.LOOK, e -> performAction("osserva")
            );

            // Ricostruisci il layout
            actionParent.add(createCenteredComponent(saveButton));
            actionParent.add(createCenteredComponent(soundToggleButton));
            actionParent.add(Box.createVerticalStrut(actionButtonSpacing));
            actionParent.add(createCenteredComponent(inventoryButton));
            actionParent.add(Box.createVerticalStrut(actionButtonSpacing));
            actionParent.add(createCenteredComponent(useAttackButton));
            actionParent.add(Box.createVerticalStrut(actionButtonSpacing));
            actionParent.add(createCenteredComponent(useSwordButton));
            actionParent.add(Box.createVerticalStrut(actionButtonSpacing));
            actionParent.add(createCenteredComponent(useBowButton));
            actionParent.add(Box.createVerticalStrut(actionButtonSpacing));
            actionParent.add(createCenteredComponent(usePotionButton));

            actionParent.revalidate();
            actionParent.repaint();
        }

        // Ricrea anche il pulsante look centrale nelle direzioni
        Container directionParent = northButton.getParent();
        if (directionParent != null) {
            // Trova e sostituisci il pulsante look centrale
            Component[] components = directionParent.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JButton) {
                    JButton btn = (JButton) components[i];
                    if ("Osserva".equals(btn.getToolTipText())) {
                        directionParent.remove(i);
                        JButton newLookButton = UIComponents.createActionButton(
                                UIComponents.ActionType.LOOK,
                                e -> performAction("osserva")
                        );
                        directionParent.add(newLookButton, i);
                        break;
                    }
                }
            }
            directionParent.revalidate();
            directionParent.repaint();
        }

        updateWeaponButtons();
        System.out.println("Pulsanti azione ricaricati con nuove immagini");
    }
}
