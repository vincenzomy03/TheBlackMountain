package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.thread.MusicManager;
import javax.swing.SwingUtilities;

/**
 * Gestisce il completamento del gioco e l'avvio dell'outro
 * @author vince
 */
public class GameCompletionHandler {
    
    /**
     * Chiamato quando il giocatore completa il gioco
     */
    public static void handleGameCompletion(GameGUI gameGUI) {
        handleCompletion(gameGUI, false);
    }
    
    /**
     * Versione che chiude completamente il gioco dopo l'outro
     */
    public static void handleGameCompletionWithExit(GameGUI gameGUI) {
        handleCompletion(gameGUI, true);
    }
    
    /**
     * Metodo unificato per gestire il completamento
     */
    private static void handleCompletion(GameGUI gameGUI, boolean exitAfterOutro) {
        if (gameGUI == null) {
            System.err.println("GameGUI null nel completamento");
            return;
        }
        
        // Ferma la musica di background del gioco
        MusicManager.getInstance().setMusicEnabled(false);
        
        // Mostra l'outro
        OutroScreen.showOutro(gameGUI, () -> {
            SwingUtilities.invokeLater(() -> {
                gameGUI.dispose();
                
                if (exitAfterOutro) {
                    // Chiudi completamente
                    MusicManager.getInstance().stopMusic();
                    System.exit(0);
                } else {
                    // Torna al menu principale
                    try {
                        new MainMenu();
                    } catch (Exception e) {
                        System.err.println("Errore nel ritorno al menu: " + e.getMessage());
                        System.exit(0);
                    }
                }
            });
        });
    }
}