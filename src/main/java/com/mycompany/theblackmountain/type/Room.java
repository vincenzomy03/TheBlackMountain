package com.mycompany.theblackmountain.type;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private final int id;

    private String name;
    private String description;
    private String look;
    private boolean visible = true;

    private Room south = null;
    private Room north = null;
    private Room east = null;
    private Room west = null;

    private final List<GameObjects> objects = new ArrayList<>();
    private final List<GameCharacter> enemies = new ArrayList<>();

    public Room(int id) {
        this.id = id;
    }

    public Room(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Room(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters e setters

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLook() {
        return look;
    }

    public void setLook(String look) {
        this.look = look;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Room getSouth() {
        return south;
    }

    public void setSouth(Room south) {
        this.south = south;
    }

    public Room getNorth() {
        return north;
    }

    public void setNorth(Room north) {
        this.north = north;
    }

    public Room getEast() {
        return east;
    }

    public void setEast(Room east) {
        this.east = east;
    }

    public Room getWest() {
        return west;
    }

    public void setWest(Room west) {
        this.west = west;
    }

    public List<GameObjects> getObjects() {
        return objects;
    }

    public List<GameCharacter> getEnemies() {
        return enemies;
    }

    // Metodi per oggetti

    public GameObjects getObject(int id) {
        for (GameObjects o : objects) {
            if (o.getId() == id) {
                return o;
            }
        }
        return null;
    }

    public boolean removeObject(GameObjects obj) {
        return objects.remove(obj);
    }

    // Metodi per nemici

    public GameCharacter getEnemy(int id) {
        for (GameCharacter c : enemies) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public boolean removeEnemy(GameCharacter enemy) {
        return enemies.remove(enemy);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Room other = (Room) obj;
        return this.id == other.id;
    }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
