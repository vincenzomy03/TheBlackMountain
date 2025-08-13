package com.mycompany.theblackmountain.gui;

import javax.swing.*;

/**
 * Dialog semplice per la gestione del Game Over
 */
public class GameOverDialog {
    
    /**
     * Mostra il dialog di game over e restituisce la scelta dell'utente
     * @param parent finestra padre
     * @return true se vuole ricominciare, false se vuole uscire
     */
    public static boolean showGameOverDialog(JFrame parent) {
        Object[] options = {"Ricomincia", "Esci"};
        
        int choice = JOptionPane.showOptionDialog(
            parent,
            "GAME OVER\n\nSei stato sconfitto!\n\nVuoi ricominciare da capo?",
            "Game Over",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[0] // Default su "Ricomincia"
        );
        
        return choice == 0; // true se ha scelto "Ricomincia"
    }
}