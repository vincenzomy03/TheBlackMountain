/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.type;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vince
 */
public class Inventory {

    private List<Object> list = new ArrayList<>();

    /**
     *
     * @return
     */
    public List<Object> getList() {
        return list;
    }

    /**
     *
     * @param list
     */
    public void setList(List<Object> list) {
        this.list = list;
    }

    /**
     *
     * @param o
     */
    public void add(Object o) {
        list.add(o);
    }

    /**
     *
     * @param o
     */
    public void remove(Object o) {
        list.remove(o);
    }
}
