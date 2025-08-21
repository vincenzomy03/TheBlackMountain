/*
 * Engine principale aggiornato con il nuovo sistema di salvataggio e musica
 */
package com.mycompany.theblackmountain;

import com.mycompany.theblackmountain.gui.MainMenu;
import com.mycompany.theblackmountain.gui.GameGUI;
import com.mycompany.theblackmountain.gui.SplashScreen;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.CommandType;
import com.mycompany.theblackmountain.thread.MusicManager;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Engine principale del gioco The Black Mountain
 * @author vince
 */
public class TBM_engine {

    private final GameDescription game;
    private Parser parser;

    /**
     * Costruttore per modalità console (mantenuto per compatibilità)
     * @param game istanza del gioco
     */
    public TBM_engine(GameDescription game) {
        this.game = game;
        try {
            this.game.init();
        } catch (Exception ex) {
            System.err.println(ex);
        }
        try {
            Set<String> stopwords = Utils.loadFileListInSet(new File("./resources/stopwords"));
            parser = new Parser(stopwords);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    /**
     * Esegue il gioco in modalità console (mantenuto per compatibilità)
     */
    public void execute() {
        System.out.println("=== THE BLACK MOUNTAIN - Modalità Console ===");
        System.out.println("Per una migliore esperienza, usa la modalità grafica!");
        System.out.println("===============================================");
        System.out.println();
        System.out.println("Ti trovi qui: " + game.getCurrentRoom().getName());
        System.out.println();
        System.out.println(game.getCurrentRoom().getDescription());
        System.out.println();
        System.out.print(">: ");
        
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            ParserOutput p = parser.parse(command, game.getCommands(), game.getCurrentRoom().getObjects(), game.getInventory());
            if (p == null || p.getCommand() == null) {
                System.out.println("Non capisco quello che mi vuoi dire.");
            } else if (p.getCommand() != null && p.getCommand().getType() == CommandType.END) {
                System.out.println("Sei un fifone, addio!");
                break;
            } else {
                game.nextMove(p, System.out);
                if (game.getCurrentRoom() == null) {
                    System.out.println("La tua avventura termina qui! Complimenti!");
                    System.exit(0);
                }
            }
            System.out.print(">: ");
        }
    }

    /**
     * Metodo principale - avvia il menu principale
     * @param args argomenti della riga di comando
     */
    public static void main(String[] args) {
        // Verifica se l'utente vuole la modalità console
        if (args.length > 0 && args[0].equals("--console")) {
            // Modalità console per debug o preferenze utente
            System.out.println("Avvio in modalità console...");
            TBMGame consoleGame = new TBMGame();
            TBM_engine engine = new TBM_engine(consoleGame);
            engine.execute();
        } else {
            // Modalità grafica (default)
            SwingUtilities.invokeLater(() -> {
                try {
                    new SplashScreen(() -> {
                    MainMenu mainMenu = new MainMenu();
                    mainMenu.setVisible(true);
                    });
                    
                } catch (Exception e) {
                    System.err.println("Errore nell'avvio del menu principale: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Fallback alla modalità console in caso di errore grafico
                    JOptionPane.showMessageDialog(null, 
                            "Errore nell'avvio della modalità grafica.\nAvvio in modalità console...",
                            "Errore", JOptionPane.ERROR_MESSAGE);
                    
                    TBMGame consoleGame = new TBMGame();
                    TBM_engine engine = new TBM_engine(consoleGame);
                    engine.execute();
                }
            });
        }
    }
}