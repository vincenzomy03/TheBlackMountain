package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.gui.utils.UIComponents;
import com.mycompany.theblackmountain.gui.utils.UIImageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Finestra di dialogo per comandi aggiuntivi
 * @author vince
 */
public class CommandsDialog extends JDialog {
    
    private Consumer<String> commandCallback;
    private JButton openChestButton;
    // Placeholder per futuri pulsanti
    private JButton futureCommand1Button;
    private JButton futureCommand2Button;
    private JButton futureCommand3Button;
    
    public CommandsDialog(JFrame parent, Consumer<String> commandCallback) {
        super(parent, "Comandi Aggiuntivi", true);
        this.commandCallback = commandCallback;
        setupUI();
    }
    
    private void setupUI() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        // Pannello principale con sfondo scuro
        JPanel mainPanel = UIComponents.createThemedPanel(
            UIComponents.DARK_BACKGROUND, 
            new BorderLayout()
        );
        
        // Titolo
        JLabel titleLabel = new JLabel("Seleziona un comando:", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(UIComponents.TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Pannello pulsanti
        JPanel buttonsPanel = createButtonsPanel();
        mainPanel.add(buttonsPanel, BorderLayout.CENTER);
        
        // Pannello inferiore con pulsante chiudi
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        int spacing = 15;
        
        // Pulsante "Apri Cassa"
        openChestButton = createCommandButton(
            "open_chest", 
            "Apri Cassa", 
            "Tenta di aprire una cassa nella stanza",
            e -> executeCommand("apri cassa")
        );
        
        // Pulsanti placeholder per futuri comandi
        futureCommand1Button = createCommandButton(
            "command_placeholder", 
            "Comando Futuro 1", 
            "Questo comando sarà implementato in futuro",
            e -> showNotImplemented("Comando Futuro 1")
        );
        futureCommand1Button.setEnabled(false);
        
        futureCommand2Button = createCommandButton(
            "command_placeholder", 
            "Comando Futuro 2", 
            "Questo comando sarà implementato in futuro",
            e -> showNotImplemented("Comando Futuro 2")
        );
        futureCommand2Button.setEnabled(false);
        
        futureCommand3Button = createCommandButton(
            "command_placeholder", 
            "Comando Futuro 3", 
            "Questo comando sarà implementato in futuro",
            e -> showNotImplemented("Comando Futuro 3")
        );
        futureCommand3Button.setEnabled(false);
        
        // Aggiungi pulsanti al pannello
        panel.add(Box.createVerticalGlue());
        panel.add(createCenteredButton(openChestButton));
        panel.add(Box.createVerticalStrut(spacing));
        panel.add(createCenteredButton(futureCommand1Button));
        panel.add(Box.createVerticalStrut(spacing));
        panel.add(createCenteredButton(futureCommand2Button));
        panel.add(Box.createVerticalStrut(spacing));
        panel.add(createCenteredButton(futureCommand3Button));
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JButton createCommandButton(String imageName, String text, String tooltip, ActionListener action) {
        JButton button = UIComponents.createImageButton(
            imageName, 
            tooltip, 
            text, 
            action, 
            new Dimension(250, 40)
        );
        
        // Stile personalizzato per i pulsanti comando
        button.setBackground(UIComponents.MEDIUM_BACKGROUND);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIComponents.ACCENT_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        
        // Effetto hover personalizzato per i pulsanti di comando
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            private Color originalColor = button.getBackground();
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(UIComponents.HOVER_COLOR);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = UIComponents.createThemedPanel(
            UIComponents.MEDIUM_BACKGROUND, 
            new FlowLayout(FlowLayout.RIGHT)
        );
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton closeButton = new JButton("Chiudi");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.setBackground(UIComponents.ACCENT_COLOR);
        closeButton.setForeground(UIComponents.TEXT_COLOR);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        
        panel.add(closeButton);
        return panel;
    }
    
    private JPanel createCenteredButton(JButton button) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        wrapper.add(button);
        return wrapper;
    }
    
    private void executeCommand(String command) {
        if (commandCallback != null) {
            commandCallback.accept(command);
        }
        dispose();
    }
    
    private void showNotImplemented(String commandName) {
        JOptionPane.showMessageDialog(this,
            "Il comando '" + commandName + "' non è ancora implementato.\n" +
            "Sarà disponibile in una versione futura!",
            "Funzione non disponibile",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Metodo per abilitare/disabilitare comandi futuri quando saranno implementati
     */
    public void enableFutureCommand(int commandNumber, String newText, String newTooltip, ActionListener newAction) {
        JButton targetButton = null;
        
        switch (commandNumber) {
            case 1: targetButton = futureCommand1Button; break;
            case 2: targetButton = futureCommand2Button; break;
            case 3: targetButton = futureCommand3Button; break;
        }
        
        if (targetButton != null) {
            targetButton.setText(newText);
            targetButton.setToolTipText(newTooltip);
            targetButton.setEnabled(true);
            
            // Rimuovi i vecchi action listeners
            for (ActionListener al : targetButton.getActionListeners()) {
                targetButton.removeActionListener(al);
            }
            
            // Aggiungi il nuovo
            targetButton.addActionListener(newAction);
        }
    }
}