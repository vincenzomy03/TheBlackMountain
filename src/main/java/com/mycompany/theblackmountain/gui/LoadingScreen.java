package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;
import com.mycompany.theblackmountain.gui.utils.ImagePathConfig;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.save.SaveManager;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.thread.MusicManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

/**
 * Schermata di caricamento reale per il gioco
 *
 * @author vince
 */
public class LoadingScreen extends JDialog {

    private JProgressBar progressBar;
    private JLabel loadingLabel;
    private JLabel statusLabel;
    private Timer animationTimer;
    private int animationFrame = 0;

    // Callback per quando il caricamento è completato
    private LoadingCompleteCallback onComplete;

    // Dati per il gioco
    private String saveData;
    private TBMGame loadedGame;
    private long totalPlayTime;

    public LoadingScreen(JFrame parent, String saveData, LoadingCompleteCallback onComplete) {
        super(parent, "Caricamento", true);
        this.saveData = saveData;
        this.onComplete = onComplete;
        setupUI();
        startRealLoading();
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
        loadingLabel = new JLabel("Inizializzazione", JLabel.CENTER);
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
        statusLabel = new JLabel("Preparazione in corso...", JLabel.CENTER);
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
     * Avvia il caricamento reale del gioco
     */
    private void startRealLoading() {
        // Timer per l'animazione del testo
        animationTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationFrame = (animationFrame + 1) % 4;
                String currentText = statusLabel.getText();
                if (currentText.contains("...")) {
                    currentText = currentText.substring(0, currentText.indexOf("..."));
                }
                String dots = ".".repeat(animationFrame);
                statusLabel.setText(currentText + dots);
            }
        });
        animationTimer.start();

        // Avvia il caricamento reale in background
        SwingWorker<TBMGame, LoadingStep> gameLoader = new SwingWorker<TBMGame, LoadingStep>() {
            @Override
            protected TBMGame doInBackground() throws Exception {
                // Fase 1: Inizializzazione sistema di gioco
                publish(new LoadingStep(10, "Inizializzazione del sistema", "Avvio dei componenti base"));
                Thread.sleep(300); // Piccolo delay per mostrare il progresso

                TBMGame game = new TBMGame();

                // Fase 2: Caricamento delle stanze
                publish(new LoadingStep(25, "Caricamento mondo di gioco", "Costruzione delle stanze"));
                game.init();
                Thread.sleep(400);

                // Fase 3: Inizializzazione parser
                publish(new LoadingStep(40, "Configurazione parser", "Preparazione comandi"));
                Parser parser = new Parser(new HashSet<>());
                Thread.sleep(300);

                // Fase 4: Caricamento immagini UI
                publish(new LoadingStep(55, "Caricamento risorse grafiche", "Pre-caricamento immagini"));
                preloadImages();
                Thread.sleep(500);

                // Fase 5: Applicazione dati di salvataggio (se presenti)
                if (saveData != null) {
                    publish(new LoadingStep(70, "Ripristino salvataggio", "Applicazione dati salvati"));
                    applySaveData(game);
                    Thread.sleep(400);
                } else {
                    publish(new LoadingStep(70, "Configurazione nuova partita", "Impostazione stato iniziale"));
                    Thread.sleep(300);
                }

                // Fase 6: Inizializzazione audio
                publish(new LoadingStep(85, "Inizializzazione sistema audio", "Caricamento effetti sonori"));
                initializeAudioSystem();
                Thread.sleep(300);

                // Fase 7: Finalizzazione
                publish(new LoadingStep(100, "Finalizzazione", "Completamento caricamento"));
                Thread.sleep(200);

                return game;
            }

            @Override
            protected void process(java.util.List<LoadingStep> chunks) {
                for (LoadingStep step : chunks) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(step.getProgress());
                        progressBar.setString(step.getProgress() + "%");
                        loadingLabel.setText(step.getMainText());
                        statusLabel.setText(step.getDetailText());
                    });
                }
            }

            @Override
            protected void done() {
                animationTimer.stop();

                try {
                    loadedGame = get();

                    // Pausa breve per mostrare il completamento
                    Timer completeTimer = new Timer(500, e -> {
                        dispose();
                        if (onComplete != null) {
                            onComplete.onLoadingComplete(loadedGame, totalPlayTime);
                        }
                        ((Timer) e.getSource()).stop();
                    });
                    completeTimer.setRepeats(false);
                    completeTimer.start();

                } catch (Exception e) {
                    // Gestione errori
                    animationTimer.stop();
                    JOptionPane.showMessageDialog(LoadingScreen.this,
                            "Errore durante il caricamento: " + e.getMessage(),
                            "Errore",
                            JOptionPane.ERROR_MESSAGE);
                    dispose();
                    e.printStackTrace();
                }
            }
        };

        gameLoader.execute();
    }

    /**
     * Pre-carica le immagini più importanti per evitare ritardi durante il
     * gioco
     */
    private void preloadImages() {
        UIImageManager imageManager = UIImageManager.getInstance();

        // Pre-carica immagini di sfondo
        imageManager.loadImage(UIImageManager.BACKGROUNDS_PATH + "game_background.png");

        // Pre-carica icone dei pulsanti più usati
        String[] commonButtons = {
            "inventory", "use_attack", "use_sword", "use_bow", "use_potion",
            "arrow_up", "arrow_down", "arrow_left", "arrow_right",
            "magnifier", "save", "volume_on", "volume_off", "commands", "help"
        };

        for (String buttonName : commonButtons) {
            imageManager.loadButtonImages(buttonName, 50, 50);
        }
    }

    /**
     * Applica i dati di salvataggio al gioco
     */
    private void applySaveData(TBMGame game) throws Exception {
        if (saveData != null) {
            SaveManager.applyLoadedData(game, saveData, game.getCombatSystem());

            // Estrai il tempo di gioco salvato
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
    }

    /**
     * Inizializza il sistema audio
     */
    private void initializeAudioSystem() {
        MusicManager.getInstance();
    }

    /**
     * Classe per rappresentare un passo del caricamento
     */
    private static class LoadingStep {

        private final int progress;
        private final String mainText;
        private final String detailText;

        public LoadingStep(int progress, String mainText, String detailText) {
            this.progress = progress;
            this.mainText = mainText;
            this.detailText = detailText;
        }

        public int getProgress() {
            return progress;
        }

        public String getMainText() {
            return mainText;
        }

        public String getDetailText() {
            return detailText;
        }
    }

    /**
     * Interfaccia per il callback di completamento
     */
    public interface LoadingCompleteCallback {

        void onLoadingComplete(TBMGame game, long totalPlayTime);
    }

    /**
     * Pannello con sfondo immagine personalizzato (o gradiente di fallback)
     */
    private static class LoadingPanel extends JPanel {

        private Image backgroundImage;

        public LoadingPanel() {
            try {
                String bgPath = ImagePathConfig.getInstance().getLoadingBackground();
                backgroundImage = new ImageIcon(getClass().getResource(bgPath)).getImage();
            } catch (Exception e) {
                System.err.println("⚠️ Impossibile caricare background loading: " + e.getMessage());
                backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (backgroundImage != null) {
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                // Gradiente fallback
                Color color1 = new Color(30, 20, 40);
                Color color2 = new Color(15, 10, 25);

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), getHeight(), color2
                );

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(new Color(60, 40, 80, 30));
                for (int x = 0; x < getWidth(); x += 40) {
                    for (int y = 0; y < getHeight(); y += 40) {
                        g2d.fillOval(x, y, 2, 2);
                    }
                }
            }

            g2d.dispose();
        }
    }

    /**
     * Mostra la schermata di caricamento reale
     */
    public static void showRealLoadingScreen(JFrame parent, String saveData, LoadingCompleteCallback onComplete) {
        SwingUtilities.invokeLater(() -> {
            LoadingScreen loadingScreen = new LoadingScreen(parent, saveData, onComplete);
            loadingScreen.setVisible(true);
        });
    }
}
