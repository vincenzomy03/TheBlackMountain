package com.mycompany.theblackmountain.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SplashScreen extends JPanel {

    private float alpha = 0f; // trasparenza iniziale
    private final ImageIcon splashImage;
    private final JFrame frame;
    private boolean started = false; // flag di sicurezza

    public SplashScreen(Runnable onFinish) {
        splashImage = new ImageIcon(SplashScreen.class.getResource("/images/backgrounds/splash.png"));

        // Crea il JFrame
        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(540, 720);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(this);
        frame.setVisible(true);

        // Timer per fade-in
        Timer fadeInTimer = new Timer(50, null);
        fadeInTimer.addActionListener((ActionEvent e) -> {
            alpha += 0.05f;
            if (alpha >= 1f) {
                alpha = 1f;
                fadeInTimer.stop();

                // Timer per fade-out dopo 2 secondi di pausa
                new Timer(2000, evt -> startFadeOut(onFinish)).start();
            }
            repaint();
        });
        fadeInTimer.start();
    }

    private void startFadeOut(Runnable onFinish) {
        Timer fadeOutTimer = new Timer(50, null);
        fadeOutTimer.addActionListener((ActionEvent e) -> {
            alpha -= 0.05f;
            if (alpha <= 0f) {
                alpha = 0f;
                fadeOutTimer.stop();
                frame.dispose(); // chiude lo splash
                if (!started) {  // esegue solo una volta
                    started = true;
                    onFinish.run();  // avvia il gioco
                }
            }
            repaint();
        });
        fadeOutTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(splashImage.getImage(), 0, 0, getWidth(), getHeight(), this);
        g2d.dispose();
    }
}
