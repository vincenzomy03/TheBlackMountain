package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.thread.MusicManager;
import com.mycompany.theblackmountain.save.SaveManager;
import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class MainMenu extends JFrame {

    private UIComponents.BackgroundPanel backgroundPanel;
    private JButton newGameButton;
    private JButton loadGameButton;
    private JButton soundToggleButton;
    private JButton exitButton;

    private boolean soundEffectsEnabled = true;

    public MainMenu() {
        setupUI();

        // Avvia la musica usando il MusicManager
        MusicManager.getInstance().startMusic();
    }

    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Imposta icona della finestra se disponibile
        setWindowIcon();

        // === PANNELLO PRINCIPALE CON SFONDO ===
        backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "menu_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);

       // === TITOLO CON IMMAGINE ===
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 80));

        // Pannello contenitore per titolo e immagine
        JPanel titleContainer = createTitleWithImage();
        titlePanel.add(titleContainer);
        
        backgroundPanel.add(titlePanel, BorderLayout.NORTH);

        // === BOTTONI CENTRALI ===
        JPanel mainButtonPanel = createMainButtonPanel();

        // Contenitore per centrare i pulsanti
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(mainButtonPanel, BorderLayout.CENTER);

        backgroundPanel.add(centerContainer, BorderLayout.CENTER);

        // === PANNELLO INFERIORE ===
        JPanel bottomPanel = createBottomPanel();
        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createMainButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 50, 0));

        Dimension buttonSize = new Dimension(300, 80); // Pulsanti più grandi per le immagini
        int spacing = 25;

        // NUOVA PARTITA - usando UIComponents per supportare immagini future
        newGameButton = createImageMenuButton("new_game", "NUOVA PARTITA",
                "Inizia una nuova avventura", buttonSize, e -> startNewGame());

        // CARICA PARTITA
        loadGameButton = createImageMenuButton("load_game", "CARICA PARTITA",
                "Continua un'avventura salvata", buttonSize, e -> loadGame());

        // ESCI
        exitButton = createImageMenuButton("exit", "ESCI",
                "Chiudi il gioco", buttonSize, e -> exitGame());

        // Aggiungi i pulsanti con spaziatura
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(createCenteredButton(newGameButton));
        buttonPanel.add(Box.createVerticalStrut(spacing));
        buttonPanel.add(createCenteredButton(loadGameButton));
        buttonPanel.add(Box.createVerticalStrut(spacing));
        buttonPanel.add(createCenteredButton(exitButton));
        buttonPanel.add(Box.createVerticalGlue());

        return buttonPanel;
    }

    /**
     * Crea un pulsante del menu principale che supporta immagini personalizzate
     * Questo metodo usa UIComponents per supportare le immagini che aggiungerai
     * in futuro
     */
    private JButton createImageMenuButton(String imageName, String text, String tooltip,
            Dimension size, ActionListener action) {

        UIImageManager imageManager = UIImageManager.getInstance();

        // Prova a caricare le immagini personalizzate
        ImageIcon[] buttonIcons = imageManager.loadButtonImages(imageName, size.width - 20, size.height - 20);

        JButton button = new JButton();
        button.setText(text);
        button.setToolTipText(tooltip);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);

        // Se esistono immagini personalizzate, usale
        if (buttonIcons[0] != null && !isPlaceholderIcon(buttonIcons[0])) {
            button.setIcon(buttonIcons[0]);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.CENTER);

            // Effetto hover con immagine
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    button.setIcon(buttonIcons[1]);
                    if (soundEffectsEnabled) {
                        playHoverSound();
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    button.setIcon(buttonIcons[0]);
                }
            });
        } else {
            // Stile testuale elegante se non ci sono immagini
            setupTextualButton(button);
        }

        // Configurazione comune
        button.setFont(new Font("SansSerif", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (action != null) {
            button.addActionListener(action);
        }

        return button;
    }

    /**
     * Configura un pulsante con stile testuale quando non ci sono immagini
     */
    private void setupTextualButton(JButton button) {
        // Bordo personalizzato
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 50, 150), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Effetto sfondo al hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setOpaque(true);
                button.setBackground(new Color(100, 50, 150, 100));
                if (soundEffectsEnabled) {
                    playHoverSound();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setOpaque(false);
            }
        });
    }
    
    /**
     * Crea il pannello del titolo con immagine di sfondo
     */
    private JPanel createTitleWithImage() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new OverlayLayout(container));
        
        // Carica l'immagine per il titolo
        UIImageManager imageManager = UIImageManager.getInstance();
        ImageIcon titleImage = imageManager.loadScaledImage(
                UIImageManager.IMAGES_PATH + "title_frame.png", 600, 200);
        
        // Label per l'immagine di sfondo
        JLabel imageLabel = new JLabel(titleImage);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        // Pannello per il testo del titolo
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BorderLayout());
        textPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        // Label del titolo
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN");
        titleLabel.setFont(createTitleFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Aggiungi ombra al testo per migliorare la leggibilità
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        textPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Aggiungi i componenti al container (l'ordine è importante!)
        container.add(textPanel);  // Testo sopra
        container.add(imageLabel); // Immagine sotto
        
        return container;
    }


    /**
     * Verifica se un'icona è un placeholder (creato quando l'immagine non
     * esiste)
     */
    private boolean isPlaceholderIcon(ImageIcon icon) {
        // Un modo semplice per rilevare i placeholder: controllare se l'immagine è molto piccola
        // o se ha caratteristiche tipiche dei placeholder
        return icon.getIconWidth() <= 40 && icon.getIconHeight() <= 40;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        // Informazioni versione (sinistra)
        JLabel versionLabel = new JLabel("v1.0 - The Black Mountain Adventure");
        versionLabel.setForeground(new Color(150, 150, 150));
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bottomPanel.add(versionLabel, BorderLayout.WEST);

        // Pulsante audio (destra)
        soundToggleButton = createSoundButton();
        JPanel soundPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        soundPanel.setOpaque(false);
        soundPanel.add(soundToggleButton);
        bottomPanel.add(soundPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private JButton createSoundButton() {
        JButton button = new JButton();
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(64, 64));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        updateSoundButtonIcon(button);

        button.addActionListener(e -> {
            MusicManager musicManager = MusicManager.getInstance();
            musicManager.setMusicEnabled(!musicManager.isMusicEnabled());
            updateSoundButtonIcon(button);
        });

        return button;
    }

    private void updateSoundButtonIcon(JButton button) {
        boolean musicEnabled = MusicManager.getInstance().isMusicEnabled();
        UIImageManager imageManager = UIImageManager.getInstance();

        String imageName = musicEnabled ? "volume_on" : "volume_off";
        ImageIcon icon = imageManager.loadScaledImage(
                UIImageManager.ICONS_PATH + imageName + ".png", 48, 48);

        button.setIcon(icon);
        button.setToolTipText(musicEnabled ? "Disattiva Audio" : "Attiva Audio");
    }

    private JPanel createCenteredButton(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);
        panel.add(button);
        return panel;
    }

    private Font createTitleFont() {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.BOLD, 48f);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Font personalizzato non trovato per il titolo");
        }
        return new Font("Serif", Font.BOLD, 48);
    }

    private void setWindowIcon() {
        UIImageManager imageManager = UIImageManager.getInstance();
        ImageIcon icon = imageManager.loadImage(UIImageManager.ICONS_PATH + "game_icon.png");

        if (icon != null) {
            setIconImage(icon.getImage());
        } else {
            System.err.println("⚠️ Icona finestra non trovata");
        }
    }

    private void playHoverSound() {
        try {
            InputStream audioSrc = getClass().getResourceAsStream("/audio/button_hover.wav");
            if (audioSrc == null) {
                // Fallback sound
                audioSrc = getClass().getResourceAsStream("/audio/button_click.wav");
            }

            if (audioSrc != null) {
                InputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        } catch (Exception e) {
            // Suono non critico, ignora errori
        }
    }

    private void startNewGame() {
        try {
            GameGUI gameGUI = new GameGUI();
            gameGUI.setVisible(true);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Errore nell'avvio del gioco: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadGame() {
        JFileChooser fileChooser = new JFileChooser(new File(SaveManager.SAVE_DIRECTORY));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "File di salvataggio TBM", "dat"));

        // Personalizza il dialog
        fileChooser.setDialogTitle("Carica Partita Salvata");
        fileChooser.setApproveButtonText("Carica");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String saveData = SaveManager.loadGame(fileChooser.getSelectedFile());

                // Non fermare la musica
                GameGUI gameGUI = new GameGUI(saveData);
                gameGUI.setVisible(true);
                this.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Errore nel caricamento: " + ex.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void exitGame() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler uscire dal gioco?",
                "Conferma uscita",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            MusicManager.getInstance().stopMusic();
            System.exit(0);
        }
    }
}
