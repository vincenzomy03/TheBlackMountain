package com.mycompany.theblackmountain.thread;

/**
 * Manager per la musica cinematica (intro e outro) separata dalla musica di background
 * @author vince
 */
public class CinematicMusicManager {
    
    private Thread introMusicThread;
    private Thread outroMusicThread;
    private CinematicMusic introMusic;
    private CinematicMusic outroMusic;
    
    /**
     * Avvia la musica dell'intro
     */
    public void startIntroMusic() {
        stopIntroMusic(); // Ferma eventuali musiche precedenti
        
        introMusic = new CinematicMusic("intro_music.wav");
        introMusicThread = new Thread(introMusic);
        introMusicThread.setName("IntroMusicThread");
        introMusicThread.start();
        
        System.out.println(" Musica intro avviata");
    }
    
    /**
     * Ferma la musica dell'intro
     */
    public void stopIntroMusic() {
        if (introMusic != null) {
            introMusic.stop();
            introMusic = null;
        }
        if (introMusicThread != null) {
            introMusicThread.interrupt();
            introMusicThread = null;
        }
    }
    
    /**
     * Avvia la musica dell'outro
     */
    public void startOutroMusic() {
        stopOutroMusic(); // Ferma eventuali musiche precedenti
        
        outroMusic = new CinematicMusic("outro_music.wav");
        outroMusicThread = new Thread(outroMusic);
        outroMusicThread.setName("OutroMusicThread");
        outroMusicThread.start();
        
        System.out.println(" Musica outro avviata");
    }
    
    /**
     * Ferma la musica dell'outro
     */
    public void stopOutroMusic() {
        if (outroMusic != null) {
            outroMusic.stop();
            outroMusic = null;
        }
        if (outroMusicThread != null) {
            outroMusicThread.interrupt();
            outroMusicThread = null;
        }
    }
    
    /**
     * Ferma tutte le musiche (intro e outro)
     */
    public void stopAllMusic() {
        stopIntroMusic();
        stopOutroMusic();
        System.out.println(" Tutte le musiche cinematiche fermate");
    }
}
