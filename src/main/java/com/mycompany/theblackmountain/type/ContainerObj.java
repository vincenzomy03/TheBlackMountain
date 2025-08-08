/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author vince
 */
public class ContainerObj extends GameObjects {

    private List<Object> list = new ArrayList<>();

    /**
     *
     * @param id
     */
    public ContainerObj(int id) {
        super(id);
    }

    /**
     *
     * @param id
     * @param name
     */
    public ContainerObj(int id, String name) {
        super(id, name);
    }

    /**
     *
     * @param id
     * @param name
     * @param description
     */
    public ContainerObj(int id, String name, String description) {
        super(id, name, description);
    }

    /**
     *
     * @param id
     * @param name
     * @param description
     * @param alias
     */
    public ContainerObj(int id, String name, String description, Set<String> alias) {
        super(id, name, description, alias);
    }

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
