/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.parser;

import com.mycompany.theblackmountain.type.Objects;
import com.mycompany.theblackmountain.type.Command;

/**
 *
 * @author vince
 */
public class ParserOutput {

    private Command command;

    private Objects object;
    
    private Objects invObject;

    /**
     *
     * @param command
     * @param object
     */
    public ParserOutput(Command command, Objects object) {
        this.command = command;
        this.object = object;
    }

    /**
     *
     * @param command
     * @param object
     * @param invObejct
     */
    public ParserOutput(Command command, Objects object, Objects invObejct) {
        this.command = command;
        this.object = object;
        this.invObject = invObejct;
    }

    /**
     *
     * @return
     */
    public Command getCommand() {
        return command;
    }

    /**
     *
     * @param command
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     *
     * @return
     */
    public Objects getObject() {
        return object;
    }

    /**
     *
     * @param object
     */
    public void setObject(Objects object) {
        this.object = object;
    }

    /**
     *
     * @return
     */
    public Objects getInvObject() {
        return invObject;
    }

    /**
     *
     * @param invObject
     */
    public void setInvObject(Objects invObject) {
        this.invObject = invObject;
    }

}
