/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain;

import com.mycompany.theblackmountain.parser.ParserOutput;

/**
 *
 * @author vince
 */
public abstract class GameObserver {

    /**
     *
     * @param description
     * @param parserOutput
     * @return
     */
    public abstract String update(GameDescription description, ParserOutput parserOutput);

}
