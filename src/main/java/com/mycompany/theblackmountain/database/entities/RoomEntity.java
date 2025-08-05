package com.mycompany.theblackmountain.database.entities;

/**
 * Entity che rappresenta una stanza nel database
 * @author vince
 */
public class RoomEntity {
    
    private int id;
    private String name;
    private String description;
    private String lookDescription;
    private Integer northRoomId;
    private Integer southRoomId;
    private Integer eastRoomId;
    private Integer westRoomId;
    private boolean isVisible;
    
    // Costruttori
    public RoomEntity() {}
    
    public RoomEntity(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isVisible = true;
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
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
    
    public String getLookDescription() {
        return lookDescription;
    }
    
    public void setLookDescription(String lookDescription) {
        this.lookDescription = lookDescription;
    }
    
    public Integer getNorthRoomId() {
        return northRoomId;
    }
    
    public void setNorthRoomId(Integer northRoomId) {
        this.northRoomId = northRoomId;
    }
    
    public Integer getSouthRoomId() {
        return southRoomId;
    }
    
    public void setSouthRoomId(Integer southRoomId) {
        this.southRoomId = southRoomId;
    }
    
    public Integer getEastRoomId() {
        return eastRoomId;
    }
    
    public void setEastRoomId(Integer eastRoomId) {
        this.eastRoomId = eastRoomId;
    }
    
    public Integer getWestRoomId() {
        return westRoomId;
    }
    
    public void setWestRoomId(Integer westRoomId) {
        this.westRoomId = westRoomId;
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void setVisible(boolean visible) {
        isVisible = visible;
    }
    
    @Override
    public String toString() {
        return "RoomEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isVisible=" + isVisible +
                '}';
    }
}