package com.mycompany.theblackmountain.database.dao;

/**
 * Factory per creare istanze DAO
 */
public class DAOFactory {
    
    private static DAOFactory instance;
    private RoomDAO roomDAO;
    private CharacterDAO characterDAO;
    private ObjectDAO objectDAO;
    private WeaponDAO weaponDAO;
    
    private DAOFactory() {}
    
    public static synchronized DAOFactory getInstance() {
        if (instance == null) {
            instance = new DAOFactory();
        }
        return instance;
    }
    
    public RoomDAO getRoomDAO() {
        if (roomDAO == null) {
            roomDAO = new RoomDAO();
        }
        return roomDAO;
    }
    
    public CharacterDAO getCharacterDAO() {
        if (characterDAO == null) {
            characterDAO = new CharacterDAO();
        }
        return characterDAO;
    }
    
    public ObjectDAO getObjectDAO() {
        if (objectDAO == null) {
            objectDAO = new ObjectDAO();
        }
        return objectDAO;
    }
    
    public WeaponDAO getWeaponDAO() {
        if (weaponDAO == null) {
            weaponDAO = new WeaponDAO();
        }
        return weaponDAO;
    }
    
    /**
     * Chiude tutte le connessioni DAO (se necessario)
     */
    public void shutdown() {
        // Eventuali operazioni di cleanup
        roomDAO = null;
        characterDAO = null;
        objectDAO = null;
        weaponDAO = null;
    }
}
