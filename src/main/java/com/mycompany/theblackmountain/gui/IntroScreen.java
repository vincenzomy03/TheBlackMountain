package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;
import com.mycompany.theblackmountain.thread.CinematicMusicManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Schermata di introduzione del gioco con animazioni di testo e musica dedicata
 * @author vince
 */
public class IntroScreen extends JFrame {
    
    private UIComponents.BackgroundPanel backgroundPanel;
    private JTextArea storyArea;
    private JButton skipButton;
    private Timer textTimer;
    private CinematicMusicManager musicManager;
    
    private List<String> storyParagraphs;
    private int currentParagraph = 0;
    private AtomicBoolean isSkipped = new AtomicBoolean(false);
    private AtomicBoolean isCompleted = new AtomicBoolean(false);
    
    // Callback da chiamare quando l'intro finisce
    private Runnable onIntroComplete;
    
    // Velocità di visualizzazione (millisecondi tra i paragrafi)
    private static final int PARAGRAPH_DELAY = 4000; // 4 secondi tra paragrafi
    
    public IntroScreen(Runnable onIntroComplete) {
        this.onIntroComplete = onIntroComplete;
        initializeStoryText();
        setupUI();
        startIntroSequence();
    }
    
    private void initializeStoryText() {
        storyParagraphs = new ArrayList<>();
        
        storyParagraphs.add("Il regno era scosso dal dolore e dalla paura: la principessa era stata rapita dai goblin, trascinata nelle oscure profondità della Montagna Nera.");
        
        storyParagraphs.add("Nessun cavaliere aveva osato inseguirli, ma il re, disperato, si era rivolto a te, avventuriero rinomato per le tue gesta e il tuo coraggio.");
        
        storyParagraphs.add("Davanti al trono, con voce grave, ti aveva implorato di riportare indietro sua figlia, promettendo gloria e riconoscenza eterna.");
        
        storyParagraphs.add("Non era l'oro né l'onore a muoverti, ma il richiamo dell'impresa: un destino che non potevi rifiutare.");
        
        storyParagraphs.add("Dopo giorni di viaggio, hai finalmente raggiunto i piedi della Montagna Nera. La sua sagoma si erge contro il cielo come un monolite minaccioso.");
        
        storyParagraphs.add("L'ingresso alla sua caverna sembra spalancare le fauci di una creatura millenaria. Con la torcia in una mano e la lama nell'altra, ti fermi un istante a scrutare l'oscurità che ti attende.");
        
        storyParagraphs.add("Le leggende narrano di cunicoli senza fine e di mostri che non vedono la luce del sole. Eppure, oltre quelle tenebre, c'è la principessa... e il destino che ti attende.");
        
        storyParagraphs.add("La tua avventura ha inizio ora.");
    }
    
    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Imposta icona della finestra
        setWindowIcon();
        
        // === PANNELLO PRINCIPALE CON SFONDO ===
        backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "intro_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);
        
        // === AREA DEL TESTO ===
        setupStoryArea();
        
        // === PULSANTE SKIP ===
        setupSkipButton();
        
        // === PANNELLO INFERIORE ===
        setupBottomPanel();
        
        setVisible(true);
    }
    
    private void setupStoryArea() {
        // Area di testo per la storia
        storyArea = new JTextArea();
        storyArea.setEditable(false);
        storyArea.setOpaque(false);
        storyArea.setForeground(new Color(220, 220, 220));
        storyArea.setFont(createStoryFont());
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        // Scroll pane con sfondo trasparente
        JScrollPane scrollPane = new JScrollPane(storyArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupSkipButton() {
        skipButton = new JButton("SALTA INTRODUZIONE");
        skipButton.setPreferredSize(new Dimension(200, 50));
        skipButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        skipButton.setForeground(Color.WHITE);
        skipButton.setBackground(new Color(100, 50, 50, 150));
        skipButton.setBorder(BorderFactory.createLineBorder(new Color(150, 100, 100), 2));
        skipButton.setFocusPainted(false);
        skipButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        skipButton.addActionListener(e -> skipIntro());
        
        // Effetto hover
        skipButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                skipButton.setBackground(new Color(150, 75, 75, 200));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                skipButton.setBackground(new Color(100, 50, 50, 150));
            }
        });
    }
    
    private void setupBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Pulsante skip a destra
        JPanel skipPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        skipPanel.setOpaque(false);
        skipPanel.add(skipButton);
        
        // Titolo del gioco in basso al centro
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN", SwingConstants.CENTER);
        titleLabel.setFont(createTitleFont());
        titleLabel.setForeground(new Color(100, 50, 50, 180));
        
        bottomPanel.add(titleLabel, BorderLayout.CENTER);
        bottomPanel.add(skipPanel, BorderLayout.EAST);
        
        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void startIntroSequence() {
        // Avvia la musica dell'intro
        musicManager = new CinematicMusicManager();
        musicManager.startIntroMusic();
        
        // Inizia con il primo paragrafo
        displayNextParagraph();
        
        // Timer per i paragrafi successivi
        textTimer = new Timer(PARAGRAPH_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isSkipped.get()) {
                    displayNextParagraph();
                }
            }
        });
        textTimer.setRepeats(true);
        textTimer.start();
    }
    
    private void displayNextParagraph() {
        if (currentParagraph < storyParagraphs.size() && !isSkipped.get()) {
            String paragraph = storyParagraphs.get(currentParagraph);
            
            // Aggiungi il nuovo paragrafo al testo esistente
            if (currentParagraph > 0) {
                storyArea.append("\n\n");
            }
            
            // Effetto typing (opzionale - qui aggiungiamo tutto il paragrafo insieme)
            storyArea.append(paragraph);
            
            // Auto-scroll verso il basso
            storyArea.setCaretPosition(storyArea.getDocument().getLength());
            
            currentParagraph++;
            
            // Se abbiamo finito tutti i paragrafi
            if (currentParagraph >= storyParagraphs.size()) {
                completeIntro();
            }
        }
    }
    
    private void skipIntro() {
        isSkipped.set(true);
        completeIntro();
    }
    
    private void completeIntro() {
        if (isCompleted.get()) {
            return; // Evita chiamate multiple
        }
        
        isCompleted.set(true);
        
        // Ferma il timer
        if (textTimer != null) {
            textTimer.stop();
        }
        
        // Ferma la musica dell'intro
        if (musicManager != null) {
            musicManager.stopIntroMusic();
        }
        
        // Chiudi la finestra e chiama il callback
        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onIntroComplete != null) {
                onIntroComplete.run();
            }
        });
    }
    
    private Font createStoryFont() {
        try {
            // Usa lo stesso font del gioco principale
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.PLAIN, 18f);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Font personalizzato non trovato per l'intro");
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
            System.err.println("⚠️ Font personalizzato non trovato per il titolo intro");
        }
        return new Font("Serif", Font.BOLD, 24);
    }
    
    private void setWindowIcon() {
        UIImageManager imageManager = UIImageManager.getInstance();
        ImageIcon icon = imageManager.loadImage(UIImageManager.ICONS_PATH + "game_icon.png");

        if (icon != null) {
            setIconImage(icon.getImage());
        }
    }
    
    /**
     * Metodo statico per mostrare l'intro e gestire il callback
     */
    public static void showIntro(Component parent, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            IntroScreen intro = new IntroScreen(onComplete);
            intro.setLocationRelativeTo(parent);
        });
    }
    
    /**
     * Cleanup quando la finestra viene chiusa
     */
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