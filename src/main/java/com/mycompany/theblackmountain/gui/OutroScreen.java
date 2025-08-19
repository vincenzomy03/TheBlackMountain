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
 * Schermata di conclusione del gioco con animazioni di testo, musica e credits
 * @author vince
 */
public class OutroScreen extends JFrame {
    
    private UIComponents.BackgroundPanel backgroundPanel;
    private JTextArea storyArea;
    private JButton continueButton;
    private Timer textTimer;
    private CinematicMusicManager musicManager;
    
    private List<String> outroSegments;
    private int currentSegment = 0;
    private AtomicBoolean isSkipped = new AtomicBoolean(false);
    private AtomicBoolean isCompleted = new AtomicBoolean(false);
    private AtomicBoolean showingCredits = new AtomicBoolean(false);
    
    // Callback da chiamare quando l'outro finisce
    private Runnable onOutroComplete;
    
    // Velocità di visualizzazione
    private static final int PARAGRAPH_DELAY = 4500; // 4.5 secondi tra paragrafi
    private static final int CREDITS_DELAY = 2000;   // 2 secondi per i credits
    
    public OutroScreen(Runnable onOutroComplete) {
        this.onOutroComplete = onOutroComplete;
        initializeOutroText();
        setupUI();
        startOutroSequence();
    }
    
    private void initializeOutroText() {
        outroSegments = new ArrayList<>();
        
        // Storia della vittoria
        outroSegments.add("Con l'ultimo colpo, il silenzio tornò a regnare nelle profondità della Montagna Nera.");
        
        outroSegments.add("Le urla dei goblin si spensero, e l'oscurità parve farsi meno opprimente, come se la montagna stessa si fosse arresa alla tua volontà.");
        
        outroSegments.add("Davanti a te, la principessa era finalmente libera, gli occhi colmi di gratitudine e sollievo.");
        
        outroSegments.add("Avevi affrontato mostri, superato trappole e sfidato le tenebre, eppure eri giunto alla fine, vittorioso.");
        
        outroSegments.add("Il ritorno al regno fu un trionfo. Il re ti accolse come un eroe, e il popolo esultò al tuo passaggio.");
        
        outroSegments.add("Ma più delle lodi e delle ricompense, ciò che rimarrà scolpito per sempre è la leggenda di colui che osò sfidare la Montagna Nera e ne uscì vivo.");
        
        outroSegments.add("Per molti sarai un mito, per altri un salvatore: per te, resterà l'eco di un'avventura che nessun uomo dimenticherà mai.");
        
        // Separatore per i credits
        outroSegments.add("---CREDITS---");
        
        // Credits
        outroSegments.add("CREDITS\n\nSviluppo dell'Avventura\n• Geniale Storia e Narrazione: Vincenzo My\n• Programmazione e Meccaniche di Gioco: Vincenzo My\n• Design Testuale e Struttura: Vincenzo My");
        
        outroSegments.add("Arte e Atmosfera\n• Illustrazioni e Grafica: Vincenzo My (e asset da itch.io)\n• Colonna sonora / Effetti sonori: Vincenzo My (e musica da itch.io)");
        
        outroSegments.add("Il Tuo Avventuriero\n• Protagonista senza nome: Tu, che hai affrontato la Montagna Nera.\n\nGrazie per aver giocato!");
    }
    
    private void setupUI() {
        setTitle("The Black Mountain - Epilogo");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Imposta icona della finestra
        setWindowIcon();
        
        // === PANNELLO PRINCIPALE CON SFONDO ===
        backgroundPanel = UIComponents.createBackgroundPanel(
                UIImageManager.BACKGROUNDS_PATH + "outro_background.png",
                new BorderLayout()
        );
        add(backgroundPanel);
        
        // === AREA DEL TESTO ===
        setupStoryArea();
        
        // === PULSANTE CONTINUA ===
        setupContinueButton();
        
        // === PANNELLO INFERIORE ===
        setupBottomPanel();
        
        setVisible(true);
    }
    
    private void setupStoryArea() {
        // Area di testo per la storia
        storyArea = new JTextArea();
        storyArea.setEditable(false);
        storyArea.setOpaque(false);
        storyArea.setForeground(new Color(240, 230, 200));
        storyArea.setFont(createStoryFont());
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        // Scroll pane con sfondo trasparente
        JScrollPane scrollPane = new JScrollPane(storyArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupContinueButton() {
        continueButton = new JButton("CONTINUA");
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
    }
    
    private void setupBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Pulsante continua al centro
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(continueButton);
        
        // Titolo del gioco in basso
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN - VITTORIA", SwingConstants.CENTER);
        titleLabel.setFont(createTitleFont());
        titleLabel.setForeground(new Color(200, 180, 100, 200));
        
        bottomPanel.add(titleLabel, BorderLayout.SOUTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        
        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void startOutroSequence() {
        // Avvia la musica dell'outro
        musicManager = new CinematicMusicManager();
        musicManager.startOutroMusic();
        
        // Inizia con il primo segmento
        displayNextSegment();
        
        // Timer per i segmenti successivi
        textTimer = new Timer(PARAGRAPH_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isSkipped.get()) {
                    displayNextSegment();
                }
            }
        });
        textTimer.setRepeats(true);
        textTimer.start();
    }
    
    private void displayNextSegment() {
        if (currentSegment < outroSegments.size() && !isSkipped.get()) {
            String segment = outroSegments.get(currentSegment);
            
            // Controllo per i credits
            if (segment.equals("---CREDITS---")) {
                showingCredits.set(true);
                storyArea.setText(""); // Pulisci per i credits
                continueButton.setText("SALTA CREDITS");
                
                // Cambia il timer per i credits (più veloce)
                textTimer.setDelay(CREDITS_DELAY);
                
                currentSegment++;
                return;
            }
            
            if (showingCredits.get()) {
                // Durante i credits, sostituisci il contenuto invece di aggiungere
                storyArea.setText(segment);
                storyArea.setFont(createCreditsFont());
            } else {
                // Durante la storia, aggiungi i paragrafi
                if (currentSegment > 0) {
                    storyArea.append("\n\n");
                }
                storyArea.append(segment);
                storyArea.setFont(createStoryFont());
            }
            
            // Auto-scroll verso il basso
            SwingUtilities.invokeLater(() -> {
                storyArea.setCaretPosition(storyArea.getDocument().getLength());
            });
            
            currentSegment++;
            
            // Se abbiamo finito tutti i segmenti
            if (currentSegment >= outroSegments.size()) {
                completeOutro();
            }
        }
    }
    
    private void advanceOutro() {
        if (showingCredits.get()) {
            // Durante i credits, permetti di saltare
            isSkipped.set(true);
            completeOutro();
        } else {
            // Durante la storia, avanza al prossimo paragrafo
            if (textTimer != null) {
                textTimer.stop();
                displayNextSegment();
                if (currentSegment < outroSegments.size()) {
                    textTimer.start();
                }
            }
        }
    }
    
    private void completeOutro() {
        if (isCompleted.get()) {
            return; // Evita chiamate multiple
        }
        
        isCompleted.set(true);
        
        // Ferma il timer
        if (textTimer != null) {
            textTimer.stop();
        }
        
        // Mostra messaggio finale
        continueButton.setText("TERMINA");
        continueButton.removeActionListener(continueButton.getActionListeners()[0]);
        continueButton.addActionListener(e -> finalizeOutro());
        
        // Se non stiamo mostrando i credits, mostra messaggio finale
        if (!showingCredits.get()) {
            storyArea.append("\n\n--- FINE ---\n\nGrazie per aver giocato a The Black Mountain!");
        }
    }
    
    private void finalizeOutro() {
        // Ferma la musica dell'outro
        if (musicManager != null) {
            musicManager.stopOutroMusic();
        }
        
        // Chiudi la finestra e chiama il callback
        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onOutroComplete != null) {
                onOutroComplete.run();
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
            System.err.println("⚠️ Font personalizzato non trovato per l'outro");
        }
        return new Font("Serif", Font.PLAIN, 18);
    }
    
    private Font createCreditsFont() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.PLAIN, 16f);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Font personalizzato non trovato per i credits");
        }
        return new Font("Monospaced", Font.PLAIN, 16);
    }
    
    private Font createTitleFont() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/fonts/yoster.ttf");
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                return baseFont.deriveFont(Font.BOLD, 24f);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Font personalizzato non trovato per il titolo outro");
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
     * Metodo statico per mostrare l'outro e gestire il callback
     */
    public static void showOutro(Component parent, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            OutroScreen outro = new OutroScreen(onComplete);
            outro.setLocationRelativeTo(parent);
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
            musicManager.stopOutroMusic();
        }
        super.dispose();
    }
}