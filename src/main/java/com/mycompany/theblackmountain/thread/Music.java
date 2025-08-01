/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.thread;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

/**
 *
 * @author vince
 */
public class Music implements Runnable {

    private boolean paused = false;
    private boolean stopped = false;
    private float volume = 1;
    private Clip audioClip;
    private FloatControl volumeControl;

    @Override
    public final void run() {
        AudioInputStream audioStream = null;
        try {
            // Carica il file audio come un InputStream
            File audioFile = new File("src/main/resources/audio/background_music.wav");
            if (!audioFile.exists()) {
                System.out.println("Il file audio non esiste: " + audioFile.getAbsolutePath());
                return;
            }

            audioStream = AudioSystem.getAudioInputStream(audioFile);

            // Ottieni un formato di dati audio
            AudioFormat format = audioStream.getFormat();

            // Ottieni un DataLine.Info object che contiene informazioni sul formato
            // dell'audio
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            // Ottieni il Clip dal sistema audio
            audioClip = (Clip) AudioSystem.getLine(info);

            // Apri il Clip con l'AudioInputStream
            audioClip.open(audioStream);
            
            // Ottieni il controllo del volume
            volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);

            // Imposta il volume iniziale al massimo
            float initialVolume = volumeControl.getMaximum() - 2.0f;
            this.volume = initialVolume;
            volumeControl.setValue(initialVolume);

        } catch (Exception e) {
            System.err.println("Errore nell'inizializzazione della musica: " + e.getMessage());
            return;
        }

        // Avvia la musica in loop
        audioClip.loop(Clip.LOOP_CONTINUOUSLY);
        
        // Loop principale per gestire pausa/resume e volume
        while (!stopped) {
            try {
                Thread.sleep(50);
                
                // Gestisci pausa/resume
                if (paused && audioClip.isRunning()) {
                    audioClip.stop();
                } else if (!paused && !audioClip.isRunning() && !stopped) {
                    audioClip.start();
                }
                
                // Aggiorna il volume se cambiato
                if (volumeControl != null) {
                    float currentVolume = volumeControl.getValue();
                    if (Math.abs(currentVolume - this.volume) > 0.1f) {
                        volumeControl.setValue(this.volume);
                    }
                }
                
            } catch (Exception ex) {
                System.out.println("Errore nel loop musicale: " + ex.getMessage());
            }
        }
        
        // Cleanup quando il thread termina
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
        }
    }

    /**
     * Mette in pausa la musica
     */
    public final void pause() {
        this.paused = true;
    }
    
    /**
     * Riprende la riproduzione della musica
     */
    public final void resume() {
        this.paused = false;
    }
    
    /**
     * Ferma definitivamente la musica
     */
    public final void stop() {
        this.stopped = true;
        this.paused = true;
    }
    
    /**
     * Metodo di compatibilità con il vecchio sistema
     * Alterna tra pausa e resume
     */
    public final void pausa() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    /**
     * Imposta il volume (valore tra 0.0 e 1.0)
     */
    public final void setVolume(final float f) {
        if (volumeControl != null) {
            // Converte il valore 0.0-1.0 nel range del controllo volume
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            
            if (f <= 0.0f) {
                // Volume 0 = minimo possibile
                this.volume = min;
            } else {
                // Scala il volume nel range disponibile
                this.volume = min + (max - min) * f;
            }
        }
    }
    
    /**
     * Verifica se la musica è in pausa
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Verifica se la musica è stata fermata
     */
    public boolean isStopped() {
        return stopped;
    }
}