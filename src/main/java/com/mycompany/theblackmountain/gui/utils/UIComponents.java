package com.mycompany.theblackmountain.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Factory per creare componenti UI riutilizzabili e consistenti
 * @author vince
 */
public class UIComponents {
    
    // Colori del tema
    public static final Color DARK_BACKGROUND = new Color(20, 20, 30);
    public static final Color MEDIUM_BACKGROUND = new Color(40, 40, 50);
    public static final Color LIGHT_BACKGROUND = new Color(60, 60, 70);
    public static final Color ACCENT_COLOR = new Color(80, 40, 120);
    public static final Color TEXT_COLOR = new Color(220, 220, 200);
    public static final Color HOVER_COLOR = new Color(100, 60, 140);
    
    // Dimensioni standard
    public static final Dimension BUTTON_SIZE = new Dimension(80, 80);
    public static final Dimension SMALL_BUTTON_SIZE = new Dimension(60, 60);
    public static final Dimension LARGE_BUTTON_SIZE = new Dimension(120, 80);
    
    /**
     * Crea un pulsante con immagine personalizzabile
     * @param imageName nome base dell'immagine (senza estensione)
     * @param tooltip testo tooltip
     * @param text testo del pulsante (può essere null)
     * @param action azione da eseguire
     * @param size dimensione del pulsante
     * @return JButton configurato
     */
    public static JButton createImageButton(String imageName, String tooltip, String text, 
                                          ActionListener action, Dimension size) {
        JButton button = new JButton();
        
        // Carica immagini
        UIImageManager imageManager = UIImageManager.getInstance();
        ImageIcon[] buttonIcons = imageManager.loadButtonImages(imageName, size.width - 10, size.height - 10);
        
        ImageIcon normalIcon = buttonIcons[0];
        ImageIcon hoverIcon = buttonIcons[1];
        
        // Configura il pulsante
        button.setIcon(normalIcon);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        
        if (text != null && !text.isEmpty()) {
            button.setText("<html><center>" + text + "</center></html>");
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
        }
        
        // Stile del pulsante
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(TEXT_COLOR);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Effetti hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(hoverIcon);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(normalIcon);
            }
        });
        
        // Aggiungi action listener
        if (action != null) {
            button.addActionListener(action);
        }
        
        return button;
    }
    
    /**
     * Crea un pulsante con immagine di dimensione standard
     */
    public static JButton createImageButton(String imageName, String tooltip, String text, ActionListener action) {
        return createImageButton(imageName, tooltip, text, action, BUTTON_SIZE);
    }
    
    /**
     * Crea un pulsante di direzione (per i movimenti)
     * @param direction direzione (north, south, east, west)
     * @param action azione da eseguire
     * @return JButton configurato per la direzione
     */
    public static JButton createDirectionButton(String direction, ActionListener action) {
        String imageName = getDirectionImageName(direction);
        String tooltip = "Vai a " + direction;
        
        JButton button = createImageButton(imageName, tooltip, "", action, BUTTON_SIZE);
        
        // Aggiungi bordo per i pulsanti di direzione
        button.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1));
        
        return button;
    }
    
    /**
     * Ottiene il nome dell'immagine per una direzione
     */
    private static String getDirectionImageName(String direction) {
        switch (direction.toLowerCase()) {
            case "north": case "nord": return "arrow_up";
            case "south": case "sud": return "arrow_down";
            case "east": case "est": return "arrow_right";
            case "west": case "ovest": return "arrow_left";
            default: return "arrow_up";
        }
    }
    
    /**
     * Crea un pulsante di azione (inventario, attacca, salva, etc.)
     * @param actionType tipo di azione
     * @param action azione da eseguire
     * @return JButton configurato
     */
    public static JButton createActionButton(ActionType actionType, ActionListener action) {
        return createImageButton(
            actionType.getImageName(), 
            actionType.getTooltip(), 
            actionType.getText(), 
            action,
            actionType.getSize()
        );
    }
    
    /**
     * Crea un pannello con sfondo personalizzato
     * @param backgroundColor colore di sfondo
     * @param layout layout manager
     * @return JPanel configurato
     */
    public static JPanel createThemedPanel(Color backgroundColor, LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(backgroundColor);
        panel.setOpaque(true);
        return panel;
    }
    
    /**
     * Crea un pannello con sfondo scuro standard
     */
    public static JPanel createDarkPanel(LayoutManager layout) {
        return createThemedPanel(DARK_BACKGROUND, layout);
    }
    
    /**
     * Crea un'area di testo per l'output del gioco
     * @param font font da utilizzare
     * @return JTextArea configurata
     */
    public static JTextArea createOutputArea(Font font) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(font);
        textArea.setBackground(DARK_BACKGROUND);
        textArea.setForeground(TEXT_COLOR);
        textArea.setCaretColor(TEXT_COLOR);
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        return textArea;
    }
    
    /**
     * Crea un campo di input per i comandi
     * @param font font da utilizzare
     * @param action azione da eseguire quando si preme invio
     * @return JTextField configurato
     */
    public static JTextField createInputField(Font font, ActionListener action) {
        JTextField textField = new JTextField();
        textField.setFont(font);
        textField.setBackground(LIGHT_BACKGROUND);
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(TEXT_COLOR);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        if (action != null) {
            textField.addActionListener(action);
        }
        
        return textField;
    }
    
    /**
     * Crea un pannello per visualizzare le mappe
     * @param width larghezza
     * @param height altezza
     * @return MapPanel personalizzato
     */
    public static MapPanel createMapPanel(int width, int height) {
        return new MapPanel(width, height);
    }
    
    /**
     * Crea un pannello con immagine di sfondo
     * @param backgroundImagePath percorso dell'immagine di sfondo
     * @param layout layout manager
     * @return BackgroundPanel personalizzato
     */
    public static BackgroundPanel createBackgroundPanel(String backgroundImagePath, LayoutManager layout) {
        return new BackgroundPanel(backgroundImagePath, layout);
    }
    
    /**
     * Enum per i tipi di azione
     */
    public enum ActionType {
        INVENTORY("inventory", "Inventario", "", BUTTON_SIZE),
        ATTACK("sword", "Attacca", "", BUTTON_SIZE),
        SAVE("save", "Salva Partita", "", BUTTON_SIZE),
        LOOK("magnifier", "Osserva", "", BUTTON_SIZE),
        SOUND_ON("volume_on", "Disattiva Audio", "", SMALL_BUTTON_SIZE),
        SOUND_OFF("volume_off", "Attiva Audio", "", SMALL_BUTTON_SIZE);
        
        private final String imageName;
        private final String tooltip;
        private final String text;
        private final Dimension size;
        
        ActionType(String imageName, String tooltip, String text, Dimension size) {
            this.imageName = imageName;
            this.tooltip = tooltip;
            this.text = text;
            this.size = size;
        }
        
        public String getImageName() { return imageName; }
        public String getTooltip() { return tooltip; }
        public String getText() { return text; }
        public Dimension getSize() { return size; }
    }
    
    /**
     * Classe per pannelli con mappa
     */
    public static class MapPanel extends JPanel {
        private ImageIcon currentMap;
        private String roomName = "";
        
        public MapPanel(int width, int height) {
            setPreferredSize(new Dimension(width, height));
            setBackground(DARK_BACKGROUND);
            setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 2));
        }
        
        public void updateMap(String roomName) {
            this.roomName = roomName;
            UIImageManager imageManager = UIImageManager.getInstance();
            this.currentMap = imageManager.loadRoomMap(roomName, getWidth() - 20, getHeight() - 20);
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (currentMap != null) {
                int x = (getWidth() - currentMap.getIconWidth()) / 2;
                int y = (getHeight() - currentMap.getIconHeight()) / 2;
                g.drawImage(currentMap.getImage(), x, y, this);
            } else {
                // Disegna placeholder
                g.setColor(TEXT_COLOR);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g.getFontMetrics();
                String text = roomName.isEmpty() ? "Caricamento mappa..." : roomName;
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = getHeight() / 2;
                g.drawString(text, textX, textY);
            }
        }
    }
    
    /**
     * Classe per pannelli con sfondo
     */
    public static class BackgroundPanel extends JPanel {
        private ImageIcon backgroundImage;
        
        public BackgroundPanel(String backgroundImagePath, LayoutManager layout) {
            super(layout);
            UIImageManager imageManager = UIImageManager.getInstance();
            this.backgroundImage = imageManager.loadImage(backgroundImagePath);
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (backgroundImage != null) {
                // Disegna l'immagine scalata per riempire il pannello
                g.drawImage(backgroundImage.getImage(), 0, 0, getWidth(), getHeight(), this);
            } else {
                // Sfondo di fallback
                g.setColor(DARK_BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}