package com.mycompany.theblackmountain.thread;

import java.io.InputStream;
import java.io.BufferedInputStream;
import javax.sound.sampled.*;

/**
 * Singleton per gestire la musica di background
 * @author vince
 */
public class MusicManager {
    private static MusicManager instance;
    
    private Clip audioClip;
    private boolean musicEnabled = true;
    private FloatControl volumeControl;
    
    private MusicManager() {}
    
    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }
    
    /**
     * Avvia la musica di background
     */
    public void startMusic() {
        if (audioClip != null && audioClip.isRunning()) {
            return; // Già in esecuzione
        }
        
        try {
            stopMusic(); // Ferma eventuali clip precedenti
            
            InputStream audioStream = getClass().getResourceAsStream("/audio/background_music.wav");
            if (audioStream == null) {
                System.err.println("File audio non trovato: background_music.wav");
                return;
            }
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(audioStream)
            );
            
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            // Ottieni controllo volume
            if (audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
            }
            
            // Imposta volume iniziale
            updateVolume();
            
            // Avvia in loop
            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            
        } catch (Exception e) {
            System.err.println("Errore avvio musica: " + e.getMessage());
        }
    }
    
    /**
     * Ferma la musica
     */
    public void stopMusic() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
            volumeControl = null;
        }
    }
    
    /**
     * Abilita/disabilita la musica
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        updateVolume();
    }
    
    /**
     * Aggiorna il volume in base alle impostazioni
     */
    private void updateVolume() {
        if (volumeControl != null) {
            if (musicEnabled) {
                // Volume normale (-10 dB dal massimo)
                volumeControl.setValue(volumeControl.getMaximum() - 10.0f);
            } else {
                // Silenzioso
                volumeControl.setValue(volumeControl.getMinimum());
            }
        }
    }
    
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
}