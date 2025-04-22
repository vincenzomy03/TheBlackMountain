/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.impl;

import com.mycompany.theblackmountain.GameDescription;
import com.mycompany.theblackmountain.parser.ParserOutput;
import com.mycompany.theblackmountain.GameObserver;


/**
 *
 * @author vince
 */
public class Move extends GameObserver{

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    @Override
    public String update(GameDescription description, ParserOutput parserOutput) {
        if (null != parserOutput.getCommand().getType()) 
            switch (parserOutput.getCommand().getType()) {
            case NORD:
                if (description.getCurrentRoom().getNorth() != null) {
                    description.setCurrentRoom(description.getCurrentRoom().getNorth());
                } else {
                    return "Da quella parte non si può andare!";
                }   break;
            case SOUTH:
                if (description.getCurrentRoom().getSouth() != null) {
                    description.setCurrentRoom(description.getCurrentRoom().getSouth());
                } else {
                    return "Da quella parte non si può andare!";
                }   break;
            case EAST:
                if (description.getCurrentRoom().getEast() != null) {
                    description.setCurrentRoom(description.getCurrentRoom().getEast());
                } else {
                    return "Da quella parte non si può andare!";
                }   break;
            case WEST:
                if (description.getCurrentRoom().getWest() != null) {
                    description.setCurrentRoom(description.getCurrentRoom().getWest());
                } else {
                    return "Da quella parte non si può andare!";
                }   break;
            default:
                break;
        }
        return "";
    }

}
