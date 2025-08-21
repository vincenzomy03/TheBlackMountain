package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;
import com.mycompany.theblackmountain.thread.CinematicMusicManager;
import com.mycompany.theblackmountain.thread.MusicManager;

import javax.swing.*;
import java.awt.*;

/**
 * Schermata di conclusione semplificata
 *
 * @author vince
 */
public class OutroScreen extends JFrame {

    private JTextArea storyArea;
    private Timer textTimer;
    private CinematicMusicManager musicManager;
    private Runnable onOutroComplete;

    private String[] outroSegments = {
        "Con l'ultimo colpo, il silenzio tornò nelle profondità della Montagna Nera.",
        "Le urla dei goblin si spensero, e l'oscurità parve meno opprimente.",
        "Davanti a te, la principessa era finalmente libera, gli occhi colmi di gratitudine.",
        "Avevi affrontato mostri, superato trappole e sfidato le tenebre, eppure eri giunto alla fine, vittorioso.",
        "Il ritorno al regno fu un trionfo. Il re ti accolse come un eroe.",
        "Ma più delle lodi, ciò che rimarrà è la leggenda di colui che osò sfidare la Montagna Nera.",
        "Per molti sarai un mito, per altri un salvatore: per te, resterà l'eco di un'avventura indimenticabile.",
        "--- CREDITS ---\n\nGrazie per aver giocato!\n\nSviluppato da: Vincenzo My\nIspirato dalle grandi avventure testuali"
    };

    private int currentSegment = 0;
    private boolean isCompleted = false;

    public OutroScreen(Runnable onOutroComplete) {
        this.onOutroComplete = onOutroComplete;
        setupUI();
        startOutroSequence();
    }

    private void setupUI() {
        setTitle("The Black Mountain - Epilogo");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Pannello principale con sfondo
        UIComponents.BackgroundPanel backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "outro_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);

        // Area testo
        storyArea = new JTextArea();
        storyArea.setEditable(false);
        storyArea.setOpaque(false);
        storyArea.setForeground(new Color(240, 230, 200));
        storyArea.setFont(createStoryFont());
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JScrollPane scrollPane = new JScrollPane(storyArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        // Pannello inferiore
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Pulsante continua
        JButton continueButton = createContinueButton();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(continueButton);

        // Titolo
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN - VITTORIA", SwingConstants.CENTER);
        titleLabel.setFont(createTitleFont());
        titleLabel.setForeground(new Color(200, 180, 100, 200));

        bottomPanel.add(titleLabel, BorderLayout.SOUTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private JButton createContinueButton() {
        JButton continueButton = new JButton("CONTINUA");
        continueButton.setPreferredSize(new Dimension(150, 40));
        continueButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        continueButton.setForeground(Color.WHITE);
        continueButton.setBackground(new Color(50, 100, 50, 150));
        continueButton.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 100), 2));
        continueButton.setFocusPainted(false);
        continueButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueButton.addActionListener(e -> advanceOutro());

        // Effetto hover
        continueButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                continueButton.setBackground(new Color(75, 125, 75, 200));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                continueButton.setBackground(new Color(50, 100, 50, 150));
            }
        });

        return continueButton;
    }

    private void startOutroSequence() {
        musicManager = new CinematicMusicManager();
        MusicManager.getInstance().stopMusic();
        musicManager.startOutroMusic();

        // Mostra il primo segmento
        displayNextSegment();

        // Timer per i segmenti successivi (4.5 secondi)
        textTimer = new Timer(4500, e -> displayNextSegment());
        textTimer.start();
    }

    private void displayNextSegment() {
        if (currentSegment < outroSegments.length && !isCompleted) {
            String segment = outroSegments[currentSegment];

            if (currentSegment > 0) {
                storyArea.append("\n\n");
            }
            storyArea.append(segment);

            SwingUtilities.invokeLater(() -> {
                storyArea.setCaretPosition(storyArea.getDocument().getLength());
            });

            currentSegment++;

            if (currentSegment >= outroSegments.length) {
                completeOutro();
            }
        }
    }

    private void advanceOutro() {
        if (currentSegment >= outroSegments.length) {
            finalizeOutro();
        } else {
            // SKIP: mostra tutti i segmenti rimanenti immediatamente
            if (textTimer != null) {
                textTimer.stop();
            }

            // Mostra tutti i segmenti rimanenti
            while (currentSegment < outroSegments.length) {
                if (currentSegment > 0) {
                    storyArea.append("\n\n");
                }
                storyArea.append(outroSegments[currentSegment]);
                currentSegment++;
            }

            SwingUtilities.invokeLater(() -> {
                storyArea.setCaretPosition(storyArea.getDocument().getLength());
            });

            completeOutro();
        }
    }

    private void completeOutro() {
        if (isCompleted) {
            return;
        }
        isCompleted = true;

        if (textTimer != null) {
            textTimer.stop();
        }
    }

    private void finalizeOutro() {
        if (musicManager != null) {
            musicManager.stopOutroMusic();
        }

        // Ferma anche eventuali altre musiche
        MusicManager.getInstance().stopMusic();

        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onOutroComplete != null) {
                onOutroComplete.run();
            } else {
                // Fallback: chiudi il gioco se non c'è callback
                System.exit(0);
            }
        });
    }

    private Font createStoryFont() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.PLAIN, 18f);
            }
        } catch (Exception e) {
            System.err.println("Font personalizzato non trovato per l'outro");
        }
        return new Font("Serif", Font.PLAIN, 18);
    }

    private Font createTitleFont() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.BOLD, 24f);
            }
        } catch (Exception e) {
            System.err.println("Font personalizzato non trovato per il titolo outro");
        }
        return new Font("Serif", Font.BOLD, 24);
    }

    /**
     * Metodo statico per mostrare l'outro
     */
    public static void showOutro(Component parent, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            OutroScreen outro = new OutroScreen(onComplete);
            outro.setLocationRelativeTo(parent);
        });
    }

    @Override
    public void dispose() {
        if (textTimer != null) {
            textTimer.stop();
        }
        if (musicManager != null) {
            musicManager.stopOutroMusic();
        }
        super.dispose();
    }
}
