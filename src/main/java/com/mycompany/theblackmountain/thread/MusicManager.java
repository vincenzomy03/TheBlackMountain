package com.mycompany.theblackmountain.thread;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

/**
 * Singleton per gestire la musica di background in tutto il gioco
 * @author vince
 */
public class MusicManager {
    private static MusicManager instance;
    
    private Thread musicThread;
    private Music music;
    private boolean musicEnabled = true;
    
    private MusicManager() {
        // Private constructor per Singleton
    }
    
    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }
    
    /**
     * Avvia la musica di background se non è già in esecuzione
     */
    public void startMusic() {
        if (music == null || musicThread == null || !musicThread.isAlive()) {
            music = new Music();
            musicThread = new Thread(music);
            musicThread.start();
            
            if (!musicEnabled) {
                music.setVolume(0.0f);
            }
        }
    }
    
    /**
     * Ferma la musica
     */
    public void stopMusic() {
        if (music != null) {
            music.stop();
            music = null;
        }
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
    }
    
    /**
     * Abilita/disabilita la musica
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (music != null) {
            if (enabled) {
                music.setVolume(0.8f); // Volume normale
            } else {
                music.setVolume(0.0f); // Silenzioso
            }
        }
    }
    
    /**
     * Verifica se la musica è abilitata
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    /**
     * Ottiene l'istanza della musica corrente
     */
    public Music getMusic() {
        return music;
    }
}