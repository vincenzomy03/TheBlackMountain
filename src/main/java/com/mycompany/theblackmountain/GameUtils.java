/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain;

import com.mycompany.theblackmountain.type.Objects;
import java.util.List;

/**
 *
 * @author vince
 */
public class GameUtils {

    /**
     *
     * @param inventory
     * @param id
     * @return
     */
    public static Objects getObjectFromInventory(List<Objects> inventory, int id) {
        for (Objects o : inventory) {
            if (o.getId() == id) {
                return o;
            }
        }
        return null;
    }

}
