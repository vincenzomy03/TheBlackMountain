/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.gui;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.impl.TBMGame;
import com.mycompany.theblackmountain.parser.Parser;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;

/**
 *
 * @author vince
 */
public class GameGUI extends JFrame {
    private final GameDescription game;
    private final Parser parser;
    
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton northButton, southButton, eastButton, westButton;
    private JButton lookButton, inventoryButton;
    private JPanel mapPanel;
    
    public GameGUI() throws Exception {
        // Initialize game
        game = new TBMGame();
        game.init();
        
        // Initialize parser with empty stopwords (replace with actual stopwords loading)
        parser = new Parser(new HashSet<>());
        
        // Set up the GUI
        setupUI();
        
        // Display welcome message
        appendToOutput(game.getWelcomeMsg());
        appendToOutput("\n" + game.getCurrentRoom().getName());
        appendToOutput("================================================");
        appendToOutput(game.getCurrentRoom().getDescription());
        
        // Update directional buttons based on available exits
        updateDirectionalButtons();
    }
    
    private void setupUI() {
        setTitle("The Black Mountain");
        setSize(1900, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Output text area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputField.addActionListener(this::processInput);
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(this::processInput);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
        
        // Direction buttons panel
        JPanel directionPanel = new JPanel(new GridLayout(3, 3));
        
        // Empty corners
        directionPanel.add(new JLabel());
        
        // North button
        northButton = new JButton("North");
        northButton.addActionListener(e -> moveDirection("nord"));
        directionPanel.add(northButton);
        directionPanel.add(new JLabel());
        
        // West button
        westButton = new JButton("West");
        westButton.addActionListener(e -> moveDirection("ovest"));
        directionPanel.add(westButton);
        
        // Look button in center
        lookButton = new JButton("Look");
        lookButton.addActionListener(e -> look());
        directionPanel.add(lookButton);
        
        // East button
        eastButton = new JButton("East");
        eastButton.addActionListener(e -> moveDirection("est"));
        directionPanel.add(eastButton);
        
        directionPanel.add(new JLabel());
        
        // South button
        southButton = new JButton("South");
        southButton.addActionListener(e -> moveDirection("sud"));
        directionPanel.add(southButton);
        
        // Inventory button
        inventoryButton = new JButton("Inventory");
        inventoryButton.addActionListener(e -> showInventory());
        directionPanel.add(inventoryButton);
        
        // Add direction panel to the right
        add(directionPanel, BorderLayout.EAST);
        
        // Map panel (placeholder for now)
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        
        // Handle window closing to clean up resources
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (game instanceof TBMGame tBMGame) {
                    tBMGame.cleanup();
                }
            }
        });
    }
    
    private void processInput(ActionEvent e) {
        String command = inputField.getText().trim();
        if (command.isEmpty()) return;
        
        // Echo the command
        appendToOutput("\n> " + command);
        
        // Parse and process the command
        ParserOutput output = parser.parse(command, game.getCommands(), 
                                           game.getCurrentRoom().getObjects(), 
                                           game.getInventory());
        
        // Custom PrintStream that redirects to our output area
        PrintStream out = new PrintStream(System.out) {
            @Override
            public void println(String x) {
                appendToOutput("\n" + x);
            }
        };
        
        // Process the move
        game.nextMove(output, out);
        
        // Update UI after move
        updateDirectionalButtons();
        mapPanel.repaint();
        
        // Clear input field
        inputField.setText("");
    }
    
    private void moveDirection(String direction) {
        // Simulate typing the direction command
        inputField.setText(direction);
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, direction));
    }
    
    private void look() {
        inputField.setText("osserva");
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "look"));
    }
    
    private void showInventory() {
        inputField.setText("inventario");
        processInput(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "inventory"));
    }
    
    private void appendToOutput(String text) {
        outputArea.append(text + "\n");
        // Scroll to bottom
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    private void updateDirectionalButtons() {
        Room currentRoom = game.getCurrentRoom();
        if (currentRoom == null) return;
        
        northButton.setEnabled(currentRoom.getNorth() != null);
        southButton.setEnabled(currentRoom.getSouth() != null);
        eastButton.setEnabled(currentRoom.getEast() != null);
        westButton.setEnabled(currentRoom.getWest() != null);
    }
}