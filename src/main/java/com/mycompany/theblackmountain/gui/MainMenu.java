package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.thread.MusicManager;
import com.mycompany.theblackmountain.save.SaveManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class MainMenu extends JFrame {

    private JPanel backgroundPanel;
    private JButton newGameButton;
    private JButton loadGameButton;
    private JButton soundToggleButton;
    private JButton exitButton;

    private ImageIcon background;
    private boolean soundEffectsEnabled = true;

    public MainMenu() {
        URL bgURL = getClass().getResource("/images/background_menu.png");
        if (bgURL != null) {
            background = new ImageIcon(bgURL);
        } else {
            System.err.println("Background NON trovato!");
        }

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

        URL iconURL = getClass().getResource("/images/game_icon.png");
        if (iconURL != null) {
            Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
            setIconImage(icon);
        } else {
            System.err.println("Icona finestra NON trovata!");
        }

        backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (background != null) {
                    g.drawImage(background.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(new BorderLayout());

        // === TITOLO ===
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 100));
        
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        titlePanel.add(titleLabel);

        backgroundPanel.add(titlePanel, BorderLayout.NORTH);

        // === BOTTONI CENTRALI ===
        newGameButton = createImageButton("NUOVA PARTITA",
                "/images/settings/button_1.png",
                "/images/settings/button_1_hover.png",
                "/audio/button_click.wav");

        loadGameButton = createImageButton("CARICA PARTITA",
                "/images/settings/button_1.png",
                "/images/settings/button_1_hover.png",
                "/audio/button_click.wav");

        exitButton = createImageButton("ESCI",
                "/images/settings/button_1.png",
                "/images/settings/button_1_hover.png",
                "/audio/button_click.wav");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 450, 0, 450));

        buttonPanel.add(newGameButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(loadGameButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(exitButton);

        backgroundPanel.add(buttonPanel, BorderLayout.CENTER);

        // pulsante volume
        soundToggleButton = new JButton();
        soundToggleButton.setContentAreaFilled(false);
        soundToggleButton.setBorderPainted(false);
        soundToggleButton.setFocusPainted(false);
        soundToggleButton.setOpaque(false);
        soundToggleButton.setPreferredSize(new Dimension(64, 64));
        updateSoundButton();

        soundToggleButton.addActionListener(e -> {
            MusicManager musicManager = MusicManager.getInstance();
            musicManager.setMusicEnabled(!musicManager.isMusicEnabled());
            updateSoundButton();
        });

        // Panel per posizionare il bottone in basso a destra
        JPanel bottomRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomRightPanel.setOpaque(false);
        bottomRightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 20));
        bottomRightPanel.add(soundToggleButton);

        backgroundPanel.add(bottomRightPanel, BorderLayout.SOUTH);

        add(backgroundPanel);

        // Aggiungo ActionListener per i bottoni
        newGameButton.addActionListener(e -> startNewGame());
        loadGameButton.addActionListener(e -> loadGame());
        exitButton.addActionListener(e -> exitGame());
    }

    private JButton createImageButton(String text, String iconPath, String hoverIconPath, String hoverSoundPath) {
        URL iconURL = getClass().getResource(iconPath);
        URL hoverIconURL = getClass().getResource(hoverIconPath);

        ImageIcon defaultIcon = (iconURL != null) ? new ImageIcon(iconURL) : null;
        ImageIcon hoverIcon = (hoverIconURL != null) ? new ImageIcon(hoverIconURL) : null;

        JButton button = new JButton(text, defaultIcon);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setFont(new Font("SansSerif", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (hoverIcon != null) {
                    button.setIcon(hoverIcon);
                }
                // Suoni di hover 
                if (soundEffectsEnabled && hoverSoundPath != null) {
                    playSound(hoverSoundPath);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (defaultIcon != null) {
                    button.setIcon(defaultIcon);
                }
            }
        });

        return button;
    }

    private void playSound(String soundPath) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream(soundPath);
            if (audioSrc == null) {
                System.err.println("File audio non trovato: " + soundPath);
                return;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Errore nel suono: " + e.getMessage());
        }
    }

    private void startNewGame() {
        try {
            // Non fermare la musica - continua nel gioco
            GameGUI gameGUI = new GameGUI();
            gameGUI.setVisible(true);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Errore nell'avvio del gioco: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        JFileChooser fileChooser = new JFileChooser(new File(SaveManager.SAVE_DIRECTORY));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "File di salvataggio TBM", "dat"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String saveData = SaveManager.loadGame(fileChooser.getSelectedFile());
                
                // Non fermare la musica
                GameGUI gameGUI = new GameGUI(saveData);
                gameGUI.setVisible(true);
                this.dispose();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Errore nel caricamento: " + ex.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exitGame() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler uscire dal gioco?",
                "Conferma uscita",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            MusicManager.getInstance().stopMusic();
            System.exit(0);
        }
    }

    private void updateSoundButton() {
        URL iconURL;
        if (MusicManager.getInstance().isMusicEnabled()) {
            iconURL = getClass().getResource("/images/settings/volume_on1.png");
        } else {
            iconURL = getClass().getResource("/images/settings/volume_off1.png");
        }

        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            // Ridimensiona l'icona per renderla più piccola
            Image img = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            soundToggleButton.setIcon(new ImageIcon(img));
        } else {
            System.err.println("Icona volume NON trovata!");
        }
    }
}