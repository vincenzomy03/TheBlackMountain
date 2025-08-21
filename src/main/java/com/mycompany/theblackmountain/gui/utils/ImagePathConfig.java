package com.mycompany.theblackmountain.gui.utils;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Classe di configurazione per gestire i path delle immagini Permette di
 * modificare facilmente i percorsi senza toccare il codice
 *
 * @author vince
 */
public class ImagePathConfig {

    private static ImagePathConfig instance;
    private Properties pathProperties;

    // PATH PREDEFINITI (fallback se il file config non esiste)
    public static final String DEFAULT_IMAGES_PATH = "/images/";
    public static final String DEFAULT_BUTTONS_PATH = DEFAULT_IMAGES_PATH + "buttons/";
    public static final String DEFAULT_MAPS_PATH = DEFAULT_IMAGES_PATH + "maps/";
    public static final String DEFAULT_BACKGROUNDS_PATH = DEFAULT_IMAGES_PATH + "backgrounds/";
    public static final String DEFAULT_ICONS_PATH = DEFAULT_IMAGES_PATH + "icons/";

    private ImagePathConfig() {
        pathProperties = new Properties();
        loadConfiguration();
    }

    public static ImagePathConfig getInstance() {
        if (instance == null) {
            instance = new ImagePathConfig();
        }
        return instance;
    }

    /**
     * Carica la configurazione dei path dal file properties
     */
    private void loadConfiguration() {
        try {
            InputStream configStream = getClass().getResourceAsStream("/config/image_paths.properties");
            if (configStream != null) {
                pathProperties.load(configStream);
                System.out.println("✅ Configurazione path immagini caricata");
            } else {
                System.out.println("⚠️ File configurazione non trovato, uso path predefiniti");
                setDefaultPaths();
            }
        } catch (IOException e) {
            System.err.println("❌ Errore caricamento configurazione: " + e.getMessage());
            setDefaultPaths();
        }
    }

    /**
     * Imposta i path predefiniti
     */
    private void setDefaultPaths() {
        pathProperties.setProperty("images.base", DEFAULT_IMAGES_PATH);
        pathProperties.setProperty("images.buttons", DEFAULT_BUTTONS_PATH);
        pathProperties.setProperty("images.maps", DEFAULT_MAPS_PATH);
        pathProperties.setProperty("images.backgrounds", DEFAULT_BACKGROUNDS_PATH);
        pathProperties.setProperty("images.icons", DEFAULT_ICONS_PATH);
    }

    // GETTER PER I PATH
    public String getImagesPath() {
        return pathProperties.getProperty("images.base", DEFAULT_IMAGES_PATH);
    }

    public String getButtonsPath() {
        return pathProperties.getProperty("images.buttons", DEFAULT_BUTTONS_PATH);
    }

    public String getMapsPath() {
        return pathProperties.getProperty("images.maps", DEFAULT_MAPS_PATH);
    }

    public String getBackgroundsPath() {
        return pathProperties.getProperty("images.backgrounds", DEFAULT_BACKGROUNDS_PATH);
    }

    public String getIconsPath() {
        return pathProperties.getProperty("images.icons", DEFAULT_ICONS_PATH);
    }

    public String getLoadingBackground() {
        return pathProperties.getProperty("loading.background", getBackgroundsPath() + "loading_background.png");
    }

    // GETTER PER IMMAGINI SPECIFICHE
    public String getMenuBackground() {
        return pathProperties.getProperty("menu.background", getBackgroundsPath() + "menu_background.png");
    }

    public String getGameBackground() {
        return pathProperties.getProperty("game.background", getBackgroundsPath() + "game_background.png");
    }

    public String getGameIcon() {
        return pathProperties.getProperty("game.icon", getIconsPath() + "game_icon.png");
    }

    // METODI PER COSTRUIRE PATH DINAMICI
    public String getButtonImage(String buttonName) {
        return getButtonsPath() + buttonName + ".png";
    }

    public String getButtonHoverImage(String buttonName) {
        return getButtonsPath() + buttonName + "_hover.png";
    }

    public String getMapImage(String roomName) {
        String sanitizedName = roomName.toLowerCase().replace(" ", "_");
        return getMapsPath() + sanitizedName + ".png";
    }

    public String getIconImage(String iconName) {
        return getIconsPath() + iconName + ".png";
    }

    /**
     * Ricarica la configurazione (utile per hot-reload durante sviluppo)
     */
    public void reload() {
        pathProperties.clear();
        loadConfiguration();
        System.out.println("🔄 Configurazione path ricaricata");
    }

    /**
     * Stampa tutti i path configurati (debug)
     */
    public void printAllPaths() {
        System.out.println("=== CONFIGURAZIONE PATH IMMAGINI ===");
        System.out.println("Base: " + getImagesPath());
        System.out.println("Buttons: " + getButtonsPath());
        System.out.println("Maps: " + getMapsPath());
        System.out.println("Backgrounds: " + getBackgroundsPath());
        System.out.println("Icons: " + getIconsPath());
        System.out.println("Menu Background: " + getMenuBackground());
        System.out.println("Game Background: " + getGameBackground());
        System.out.println("=====================================");
    }
}
