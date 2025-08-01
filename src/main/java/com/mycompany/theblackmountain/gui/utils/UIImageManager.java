package com.mycompany.theblackmountain.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Gestore centralizzato per il caricamento e la gestione delle immagini
 * @author vince
 */
public class UIImageManager {
    
    private static UIImageManager instance;
    private Map<String, ImageIcon> imageCache;
    
    // Percorsi delle immagini
    public static final String IMAGES_PATH = "/images/";
    public static final String BUTTONS_PATH = IMAGES_PATH + "buttons/";
    public static final String MAPS_PATH = IMAGES_PATH + "maps/";
    public static final String BACKGROUNDS_PATH = IMAGES_PATH + "backgrounds/";
    public static final String ICONS_PATH = IMAGES_PATH + "icons/";
    
    private UIImageManager() {
        imageCache = new HashMap<>();
    }
    
    public static UIImageManager getInstance() {
        if (instance == null) {
            instance = new UIImageManager();
        }
        return instance;
    }
    
    /**
     * Carica un'immagine dal percorso specificato
     * @param imagePath percorso dell'immagine
     * @return ImageIcon o null se non trovata
     */
    public ImageIcon loadImage(String imagePath) {
        // Controlla prima nella cache
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath);
        }
        
        try {
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            if (imageStream == null) {
                System.err.println("Immagine non trovata: " + imagePath);
                return createPlaceholderIcon(40, 40, imagePath);
            }
            
            BufferedImage bufferedImage = ImageIO.read(imageStream);
            ImageIcon icon = new ImageIcon(bufferedImage);
            
            // Salva nella cache
            imageCache.put(imagePath, icon);
            
            System.out.println("Immagine caricata: " + imagePath);
            return icon;
            
        } catch (Exception e) {
            System.err.println("Errore caricamento immagine " + imagePath + ": " + e.getMessage());
            return createPlaceholderIcon(40, 40, imagePath);
        }
    }
    
    /**
     * Carica un'immagine ridimensionata
     * @param imagePath percorso dell'immagine
     * @param width larghezza desiderata
     * @param height altezza desiderata
     * @return ImageIcon ridimensionata
     */
    public ImageIcon loadScaledImage(String imagePath, int width, int height) {
        String cacheKey = imagePath + "_" + width + "x" + height;
        
        // Controlla nella cache
        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }
        
        ImageIcon originalIcon = loadImage(imagePath);
        if (originalIcon == null) {
            return createPlaceholderIcon(width, height, imagePath);
        }
        
        // Ridimensiona l'immagine
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        
        // Salva nella cache
        imageCache.put(cacheKey, scaledIcon);
        
        return scaledIcon;
    }
    
    /**
     * Crea un'icona placeholder quando l'immagine non è trovata
     * @param width larghezza
     * @param height altezza
     * @param imageName nome dell'immagine mancante
     * @return ImageIcon placeholder
     */
    private ImageIcon createPlaceholderIcon(int width, int height, String imageName) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        
        // Sfondo grigio
        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.fillRect(0, 0, width, height);
        
        // Bordo
        g2d.setColor(Color.WHITE);
        g2d.drawRect(0, 0, width - 1, height - 1);
        
        // Testo "?"
        g2d.setFont(new Font("Arial", Font.BOLD, Math.min(width, height) / 3));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "?";
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = (height + fm.getAscent()) / 2;
        g2d.drawString(text, textX, textY);
        
        g2d.dispose();
        
        return new ImageIcon(placeholder);
    }
    
    /**
     * Carica immagine per pulsante con stati normal/hover
     * @param baseName nome base del file (senza estensione)
     * @param width larghezza
     * @param height altezza
     * @return array con [normal, hover] ImageIcon
     */
    public ImageIcon[] loadButtonImages(String baseName, int width, int height) {
        String normalPath = BUTTONS_PATH + baseName + ".png";
        String hoverPath = BUTTONS_PATH + baseName + "_hover.png";
        
        ImageIcon normalIcon = loadScaledImage(normalPath, width, height);
        ImageIcon hoverIcon = loadScaledImage(hoverPath, width, height);
        
        // Se hover non esiste, usa la normale con effetto
        if (hoverIcon == normalIcon || !resourceExists(hoverPath)) {
            hoverIcon = createHoverEffect(normalIcon);
        }
        
        return new ImageIcon[]{normalIcon, hoverIcon};
    }
    
    /**
     * Crea un effetto hover su un'icona esistente
     * @param original icona originale
     * @return icona con effetto hover
     */
    private ImageIcon createHoverEffect(ImageIcon original) {
        if (original == null) return null;
        
        BufferedImage originalImage = new BufferedImage(
            original.getIconWidth(), 
            original.getIconHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = originalImage.createGraphics();
        g2d.drawImage(original.getImage(), 0, 0, null);
        
        // Aggiungi un overlay luminoso
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, original.getIconWidth(), original.getIconHeight());
        
        g2d.dispose();
        
        return new ImageIcon(originalImage);
    }
    
    /**
     * Controlla se una risorsa esiste
     * @param path percorso della risorsa
     * @return true se esiste
     */
    private boolean resourceExists(String path) {
        return getClass().getResourceAsStream(path) != null;
    }
    
    /**
     * Carica mappa per una stanza
     * @param roomName nome della stanza
     * @param width larghezza desiderata
     * @param height altezza desiderata
     * @return ImageIcon della mappa
     */
    public ImageIcon loadRoomMap(String roomName, int width, int height) {
        String mapPath = MAPS_PATH + roomName.toLowerCase().replace(" ", "_") + ".png";
        ImageIcon mapIcon = loadScaledImage(mapPath, width, height);
        
        if (mapIcon == null) {
            // Crea mappa placeholder con nome stanza
            return createMapPlaceholder(width, height, roomName);
        }
        
        return mapIcon;
    }
    
    /**
     * Crea una mappa placeholder
     * @param width larghezza
     * @param height altezza
     * @param roomName nome della stanza
     * @return ImageIcon placeholder
     */
    private ImageIcon createMapPlaceholder(int width, int height, String roomName) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        
        // Sfondo scuro
        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRect(0, 0, width, height);
        
        // Bordo
        g2d.setColor(new Color(100, 100, 120));
        g2d.drawRect(5, 5, width - 10, height - 10);
        
        // Testo nome stanza
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        
        String[] lines = roomName.split(" ");
        int totalHeight = lines.length * fm.getHeight();
        int startY = (height - totalHeight) / 2 + fm.getAscent();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int textX = (width - fm.stringWidth(line)) / 2;
            int textY = startY + (i * fm.getHeight());
            g2d.drawString(line, textX, textY);
        }
        
        // Indicatore "Mappa non disponibile"
        g2d.setFont(new Font("Arial", Font.ITALIC, 12));
        g2d.setColor(new Color(150, 150, 150));
        String subtitle = "Mappa non disponibile";
        int subtitleX = (width - g2d.getFontMetrics().stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subtitleX, height - 20);
        
        g2d.dispose();
        
        return new ImageIcon(placeholder);
    }
    
    /**
     * Pulisce la cache delle immagini
     */
    public void clearCache() {
        imageCache.clear();
        System.out.println("Cache immagini pulita");
    }
    
    /**
     * Restituisce informazioni sulla cache
     * @return informazioni sulla cache
     */
    public String getCacheInfo() {
        return "Immagini in cache: " + imageCache.size();
    }
}