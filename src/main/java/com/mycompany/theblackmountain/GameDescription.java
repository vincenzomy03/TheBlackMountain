/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain;

import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Command;
import com.mycompany.theblackmountain.type.Room;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vince
 */
public abstract class GameDescription {

    private final List<Room> rooms = new ArrayList<>();

    private final List<Command> commands = new ArrayList<>();

    private final List<Objects> inventory = new ArrayList<>();

    private Room currentRoom;

    /**
     *
     * @return
     */
    public List<Room> getRooms() {
        return rooms;
    }

    /**
     *
     * @return
     */
    public List<Command> getCommands() {
        return commands;
    }

    /**
     *
     * @return
     */
    public Room getCurrentRoom() {
        return currentRoom;
    }

    /**
     *
     * @param currentRoom
     */
    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    /**
     *
     * @return
     */
    public List<Objects> getInventory() {
        return inventory;
    }

    /**
     *
     * @throws Exception
     */
    public abstract void init() throws Exception;

    /**
     *
     * @param p
     * @param out
     */
    public abstract void nextMove(ParserOutput p, PrintStream out);

}
