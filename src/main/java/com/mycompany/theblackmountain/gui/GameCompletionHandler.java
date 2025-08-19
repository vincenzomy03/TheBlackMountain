package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.thread.MusicManager;
import javax.swing.SwingUtilities;

/**
 * Gestisce il completamento del gioco e l'avvio dell'outro
 * @author vince
 */
public class GameCompletionHandler {
    
    /**
     * Chiamato quando il giocatore completa il gioco (esce dalla montagna o sconfigge il boss finale)
     * Questo metodo dovrebbe essere chiamato dal sistema di gioco quando si verifica la condizione di vittoria
     */
    public static void handleGameCompletion(GameGUI gameGUI) {
        // Ferma la musica di background del gioco
        MusicManager.getInstance().setMusicEnabled(false);
        
        // Mostra l'outro
        OutroScreen.showOutro(gameGUI, () -> {
            // Callback quando l'outro finisce
            
            // Chiudi il gioco e torna al menu principale
            SwingUtilities.invokeLater(() -> {
                gameGUI.dispose();
                
                try {
                    // Crea un nuovo menu principale
                    new MainMenu();
                } catch (Exception e) {
                    System.err.println("Errore nel ritorno al menu: " + e.getMessage());
                    System.exit(0);
                }
            });
        });
    }
    
    /**
     * Versione alternativa che chiude completamente il gioco dopo l'outro
     */
    public static void handleGameCompletionWithExit(GameGUI gameGUI) {
        // Ferma la musica di background del gioco
        MusicManager.getInstance().setMusicEnabled(false);
        
        // Mostra l'outro
        OutroScreen.showOutro(gameGUI, () -> {
            // Callback quando l'outro finisce - esci dal gioco
            SwingUtilities.invokeLater(() -> {
                gameGUI.dispose();
                MusicManager.getInstance().stopMusic();
                System.exit(0);
            });
        });
    }
}