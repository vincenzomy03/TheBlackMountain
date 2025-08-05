package com.mycompany.theblackmountain.database.entities;

/**
 * Entity che rappresenta un oggetto nel database
 * @author vince
 */
public class ObjectEntity {
    
    private int id;
    private String name;
    private String description;
    private String aliases;
    private boolean isOpenable;
    private boolean isPickupable;
    private boolean isPushable;
    private boolean isOpen;
    private boolean isPushed;
    private String objectType;
    
    // Costruttori
    public ObjectEntity() {}
    
    public ObjectEntity(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isPickupable = true;
        this.objectType = "NORMAL";
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
    
    public String getAliases() {
        return aliases;
    }
    
    public void setAliases(String aliases) {
        this.aliases = aliases;
    }
    
    public boolean isOpenable() {
        return isOpenable;
    }
    
    public void setOpenable(boolean openable) {
        isOpenable = openable;
    }
    
    public boolean isPickupable() {
        return isPickupable;
    }
    
    public void setPickupable(boolean pickupable) {
        isPickupable = pickupable;
    }
    
    public boolean isPushable() {
        return isPushable;
    }
    
    public void setPushable(boolean pushable) {
        isPushable = pushable;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setOpen(boolean open) {
        isOpen = open;
    }
    
    public boolean isPushed() {
        return isPushed;
    }
    
    public void setPushed(boolean pushed) {
        isPushed = pushed;
    }
    
    public String getObjectType() {
        return objectType;
    }
    
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
    
    @Override
    public String toString() {
        return "ObjectEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", objectType='" + objectType + '\'' +
                '}';
    }
}