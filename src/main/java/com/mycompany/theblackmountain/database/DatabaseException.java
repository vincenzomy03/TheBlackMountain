package com.mycompany.theblackmountain.database;

/**
 * Eccezione personalizzata per gli errori del database
 * Deve essere pubblica per essere accessibile dal package impl
 */
public class DatabaseException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Costruttore con solo messaggio
     */
    public DatabaseException(String message) {
        super(message);
    }
    
    /**
     * Costruttore con messaggio e causa
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Costruttore con solo causa
     */
    public DatabaseException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Costruttore completo
     */
    public DatabaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}