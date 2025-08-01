package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.thread.Music;
import com.mycompany.theblackmountain.save.GameSaveData;

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

    private boolean soundEnabled = true;

    private Thread musicThread;
    private Music music;

    private ImageIcon background;

    public MainMenu() {

        URL bgURL = getClass().getResource("/images/background_menu.png");
        if (bgURL != null) {
            background = new ImageIcon(bgURL);
        } else {
            System.err.println("Background NON trovato!");
        }

        setupUI();
        startBackgroundMusic();
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

        
        // da aggiustare
        soundToggleButton = new JButton();
        soundToggleButton.setContentAreaFilled(false);
        soundToggleButton.setBorderPainted(false);
        soundToggleButton.setFocusPainted(false);
        soundToggleButton.setOpaque(false);
        updateSoundButton(); // Imposta l'icona iniziale
        
        

        soundToggleButton.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            updateSoundButton();
        });

        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topLeftPanel.setOpaque(false);
        topLeftPanel.add(soundToggleButton);
        backgroundPanel.add(topLeftPanel, BorderLayout.WEST);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 100));
        
        
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        titlePanel.add(titleLabel);

        

        backgroundPanel.add(titlePanel, BorderLayout.NORTH);

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

        // Aggiungo i bottoni al centro con un BoxLayout verticale
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
                if (soundEnabled && hoverSoundPath != null) {
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
            stopBackgroundMusic();
            GameGUI gameGUI = new GameGUI();
            gameGUI.setVisible(true);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Errore nell'avvio del gioco: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "File di salvataggio", "dat"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try {
                GameSaveData saveData = loadGameData(saveFile);
                stopBackgroundMusic();
                GameGUI gameGUI = new GameGUI(saveData);
                gameGUI.setVisible(true);
                this.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Errore nel caricamento: " + e.getMessage(),
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
            stopBackgroundMusic();
            System.exit(0);
        }
    }

    private void startBackgroundMusic() {
        if (soundEnabled) {
            music = new Music();
            musicThread = new Thread(music);
            musicThread.start();
        }
    }

    private void stopBackgroundMusic() {
        if (music != null) {
            music.pausa();
        }
    }

    private GameSaveData loadGameData(File saveFile) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            return (GameSaveData) ois.readObject();
        }
    }

    private void updateSoundButton() {
        URL iconURL;
        if (soundEnabled) {
            iconURL = getClass().getResource("/images/settings/volume_on1.png");
        } else {
            iconURL = getClass().getResource("/images/settings/volume_off1.png");
        }

        if (iconURL != null) {
            soundToggleButton.setIcon(new ImageIcon(iconURL));
        } else {
            System.err.println("Icona volume NON trovata!");
        }
    }

}
