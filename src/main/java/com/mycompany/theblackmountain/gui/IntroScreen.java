package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;
import com.mycompany.theblackmountain.thread.CinematicMusicManager;

import javax.swing.*;
import java.awt.*;

/**
 * Schermata di introduzione semplificata
 *
 * @author vince
 */
public class IntroScreen extends JFrame {

    private JTextArea storyArea;
    private Timer textTimer;
    private CinematicMusicManager musicManager;
    private Runnable onIntroComplete;
    private boolean introCompleted = false;

    private String[] storyParagraphs = {
        "Il regno era scosso dal dolore: la principessa era stata rapita dai goblin, trascinata nelle profondità della Montagna Nera.",
        "Nessun cavaliere aveva osato inseguirli, ma il re si era rivolto a te, avventuriero rinomato per coraggio e geste.",
        "Davanti al trono, con voce grave, ti aveva implorato di riportare indietro sua figlia.",
        "Non era l'oro né l'onore a muoverti, ma il richiamo dell'impresa: un destino che non potevi rifiutare.",
        "Dopo giorni di viaggio, hai raggiunto i piedi della Montagna Nera. La sua sagoma si erge minacciosa contro il cielo.",
        "L'ingresso sembra spalancare le fauci di una creatura antica. Con torcia e lama, scruti l'oscurità che ti attende.",
        "Le leggende narrano di cunicoli infiniti e mostri che non vedono la luce. Eppure, oltre quelle tenebre, c'è la principessa.",
        "La tua avventura ha inizio ora."
    };

    private int currentParagraph = 0;
    private boolean isSkipped = false;

    public IntroScreen(Runnable onIntroComplete) {
        this.onIntroComplete = onIntroComplete;
        setupUI();
        startIntroSequence();
    }

    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Pannello principale con sfondo
        UIComponents.BackgroundPanel backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "intro_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);

        // Area testo
        storyArea = new JTextArea();
        storyArea.setEditable(false);
        storyArea.setOpaque(false);
        storyArea.setForeground(Color.BLACK);
        storyArea.setFont(createStoryFont());
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JScrollPane scrollPane = new JScrollPane(storyArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        // Pannello inferiore con titolo e pulsante skip
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Pulsante skip
        JButton skipButton = createSkipButton();
        JPanel skipPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        skipPanel.setOpaque(false);
        skipPanel.add(skipButton);
        bottomPanel.add(skipPanel, BorderLayout.EAST);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private JButton createSkipButton() {
        JButton skipButton = new JButton("SALTA INTRODUZIONE");
        skipButton.setPreferredSize(new Dimension(200, 50));
        skipButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        skipButton.setForeground(Color.WHITE);
        skipButton.setBackground(Color.BLACK); // <-- sfondo nero
        skipButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2)); // contorno bianco
        skipButton.setFocusPainted(false);
        skipButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        skipButton.addActionListener(e -> skipIntro());

        // Effetto hover (grigio scuro)
        skipButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                skipButton.setBackground(new Color(30, 30, 30));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                skipButton.setBackground(Color.BLACK);
            }
        });

        return skipButton;
    }

    private void startIntroSequence() {
        musicManager = new CinematicMusicManager();
        musicManager.startIntroMusic();

        // Mostra il primo paragrafo immediatamente
        displayNextParagraph();

        // Timer per i paragrafi successivi (4 secondi di intervallo)
        textTimer = new Timer(5000, e -> {
            if (!isSkipped) {
                displayNextParagraph();
            }
        });
        textTimer.start();
    }

    private void displayNextParagraph() {
        if (currentParagraph < storyParagraphs.length && !isSkipped) {
            if (currentParagraph > 0) {
                storyArea.append("\n\n");
            }
            storyArea.append(storyParagraphs[currentParagraph]);
            storyArea.setCaretPosition(storyArea.getDocument().getLength());
            currentParagraph++;

            if (currentParagraph >= storyParagraphs.length) {
                // Tutti i paragrafi mostrati → pausa 3 secondi prima di chiudere
                if (textTimer != null) {
                    textTimer.stop();
                }
                new Timer(3000, e -> completeIntro()).start();
            }
        }
    }

    private void skipIntro() {
        isSkipped = true;
        completeIntro();
    }

    private void completeIntro() {
        if (introCompleted) {
            return; 
        }
        introCompleted = true;

        if (textTimer != null) {
            textTimer.stop();
        }
        if (musicManager != null) {
            musicManager.stopIntroMusic();
        }

        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onIntroComplete != null) {
                onIntroComplete.run();
            }
        });
    }

    private Font createStoryFont() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.PLAIN, 22f);
            }
        } catch (Exception e) {
            System.err.println("Font personalizzato non trovato per l'intro");
        }
        return new Font("Serif", Font.PLAIN, 18);
    }


    /**s
     * Metodo statico per mostrare l'intro
     */
    public static void showIntro(Component parent, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            IntroScreen intro = new IntroScreen(onComplete);
            intro.setLocationRelativeTo(parent);
        });
    }

    @Override
    public void dispose() {
        if (textTimer != null) {
            textTimer.stop();
        }
        if (musicManager != null) {
            musicManager.stopIntroMusic();
        }
        super.dispose();
    }
}
