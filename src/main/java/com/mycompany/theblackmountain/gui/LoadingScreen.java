package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Schermata di caricamento per il gioco
 * @author vince
 */
public class LoadingScreen extends JDialog {
    
    private JProgressBar progressBar;
    private JLabel loadingLabel;
    private JLabel statusLabel;
    private Timer animationTimer;
    private Timer progressTimer;
    private int progress = 0;
    private int animationFrame = 0;
    private String[] loadingTexts = {
        "Inizializzazione della fortezza...",
        "Caricamento delle stanze...",
        "Evocazione dei nemici...",
        "Preparazione degli oggetti...",
        "Attivazione degli incantesimi...",
        "Finalizzazione dell'avventura..."
    };
    
    public LoadingScreen(JFrame parent) {
        super(parent, "Caricamento", true);
        setupUI();
        startLoadingAnimation();
    }
    
    private void setupUI() {
        setSize(500, 300);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        // Pannello principale con sfondo personalizzato
        JPanel mainPanel = new LoadingPanel();
        mainPanel.setLayout(new BorderLayout());
        
        // === TITOLO ===
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("THE BLACK MOUNTAIN", JLabel.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(200, 180, 120));
        titlePanel.add(titleLabel);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // === CENTRO: Animazione e Progress ===
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        
        // Etichetta con animazione
        loadingLabel = new JLabel("Preparazione dell'avventura", JLabel.CENTER);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loadingLabel.setForeground(UIComponents.TEXT_COLOR);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Progress bar personalizzata
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setBackground(UIComponents.DARK_BACKGROUND);
        progressBar.setForeground(new Color(100, 50, 150));
        progressBar.setBorder(BorderFactory.createLineBorder(UIComponents.ACCENT_COLOR, 1));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(400, 25));
        
        // Status label
        statusLabel = new JLabel("Inizializzazione...", JLabel.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(new Color(160, 160, 160));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPanel.add(loadingLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(progressBar);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(statusLabel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // === FOOTER ===
        JPanel footerPanel = new JPanel();
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        
        JLabel footerLabel = new JLabel("Caricamento in corso, attendere prego...", JLabel.CENTER);
        footerLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(120, 120, 120));
        footerPanel.add(footerLabel);
        
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * Avvia l'animazione di caricamento
     */
    private void startLoadingAnimation() {
        // Timer per l'animazione del testo
        animationTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationFrame = (animationFrame + 1) % 4;
                String dots = ".".repeat(animationFrame);
                loadingLabel.setText("Preparazione dell'avventura" + dots);
            }
        });
        animationTimer.start();
        
        // Timer per la progress bar
        progressTimer = new Timer(200, new ActionListener() {
            private int step = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                
                // Progressione non lineare per sembrare più realistica
                if (step < 5) {
                    progress += 8; // Veloce all'inizio
                } else if (step < 15) {
                    progress += 4; // Rallenta
                } else if (step < 20) {
                    progress += 6; // Riaccellera
                } else {
                    progress += 2; // Finale lento
                }
                
                progress = Math.min(progress, 100);
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
                
                // Cambia il testo di stato
                if (step < loadingTexts.length) {
                    statusLabel.setText(loadingTexts[Math.min(step, loadingTexts.length - 1)]);
                }
                
                // Quando arriva a 100%, ferma e chiudi
                if (progress >= 100) {
                    progressTimer.stop();
                    animationTimer.stop();
                    
                    // Pausa breve prima di chiudere
                    Timer closeTimer = new Timer(800, evt -> {
                        dispose();
                        ((Timer)evt.getSource()).stop();
                    });
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                }
            }
        });
        progressTimer.start();
    }
    
    /**
     * Pannello con sfondo gradiente personalizzato
     */
    private static class LoadingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Gradiente dal centro verso l'esterno
            Color color1 = new Color(30, 20, 40);
            Color color2 = new Color(15, 10, 25);
            
            GradientPaint gradient = new GradientPaint(
                0, 0, color1,
                getWidth(), getHeight(), color2
            );
            
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Aggiungi pattern di punti per effetto texture
            g2d.setColor(new Color(60, 40, 80, 30));
            for (int x = 0; x < getWidth(); x += 40) {
                for (int y = 0; y < getHeight(); y += 40) {
                    g2d.fillOval(x, y, 2, 2);
                }
            }
            
            g2d.dispose();
        }
    }
    
    /**
     * Mostra la schermata di caricamento
     * @param parent finestra genitore
     * @param onComplete azione da eseguire al completamento
     */
    public static void showLoadingScreen(JFrame parent, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            LoadingScreen loadingScreen = new LoadingScreen(parent);
            
            // Avvia il caricamento del gioco in background
            SwingWorker<Void, Void> gameLoader = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Simula il tempo di caricamento reale
                    // In realtà qui potresti fare il caricamento effettivo
                    Thread.sleep(4000); // 4 secondi di caricamento
                    return null;
                }
                
                @Override
                protected void done() {
                    // Quando il caricamento è completato
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            };
            
            gameLoader.execute();
            loadingScreen.setVisible(true);
        });
    }
}