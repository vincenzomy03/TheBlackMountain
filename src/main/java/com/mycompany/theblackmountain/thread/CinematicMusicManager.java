package com.mycompany.theblackmountain.thread;

import java.io.InputStream;
import java.io.BufferedInputStream;
import javax.sound.sampled.*;

/**
 * Manager per la musica cinematica (intro e outro)
 *
 * @author vince
 */
public class CinematicMusicManager {

    private Clip currentClip;

    /**
     * Avvia la musica dell'intro
     */
    public void startIntroMusic() {
        playMusic("intro_music.wav");
    }

    /**
     * Avvia la musica dell'outro
     */
    public void startOutroMusic() {
        playMusic("outro_music.wav");
    }

    /**
     * Ferma la musica dell'intro
     */
    public void stopIntroMusic() {
        stopCurrentMusic();
    }

    /**
     * Ferma la musica dell'outro
     */
    public void stopOutroMusic() {
        stopCurrentMusic();
    }

    /**
     * Metodo unificato per riprodurre musica
     */
    private void playMusic(String fileName) {
        stopCurrentMusic(); // Ferma la musica precedente

        try {
            InputStream audioStream = getClass().getResourceAsStream("/audio/" + fileName);
            if (audioStream == null) {
                System.err.println("File audio non trovato: " + fileName);
                return;
            }

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(audioStream)
            );

            currentClip = AudioSystem.getClip();
            currentClip.open(audioInputStream);

            // Imposta volume se supportato
            if (currentClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
                volumeControl.setValue(volumeControl.getMaximum() - 5.0f); // Volume leggermente ridotto
            }

            currentClip.start();
            System.out.println("Musica cinematica avviata: " + fileName);

        } catch (Exception e) {
            System.err.println("Errore riproduzione musica " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Ferma la musica corrente
     */
    private void stopCurrentMusic() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
    }
}
