package com.mycompany.theblackmountain.database.dao;

import com.mycompany.theblackmountain.database.DatabaseException;
import com.mycompany.theblackmountain.database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe base per tutti i DAO con operazioni comuni
 * @author vince
 */
public abstract class BaseDAO<T, ID> {
    
    protected final DatabaseManager databaseManager;
    
    protected BaseDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }
    
    // Metodi astratti che devono essere implementati dai DAO concreti
    protected abstract String getTableName();
    protected abstract String getIdColumn();
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    protected abstract void mapEntityToInsertStatement(PreparedStatement stmt, T entity) throws SQLException;
    protected abstract void mapEntityToUpdateStatement(PreparedStatement stmt, T entity) throws SQLException;
    protected abstract ID getEntityId(T entity);
    
    /**
     * Trova un'entità per ID
     */
    public Optional<T> findById(ID id) throws SQLException {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameter(stmt, 1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Trova tutte le entità
     */
    public List<T> findAll() throws SQLException {
        String sql = "SELECT * FROM " + getTableName() + " ORDER BY " + getIdColumn();
        List<T> entities = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                entities.add(mapResultSetToEntity(rs));
            }
        }
        return entities;
    }
    
    /**
     * Salva una nuova entità
     */
    public void save(T entity) throws SQLException {
        String sql = buildInsertSql();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToInsertStatement(stmt, entity);
            int affected = stmt.executeUpdate();
            
            if (affected == 0) {
                throw new SQLException("Inserimento fallito, nessuna riga interessata");
            }
        }
    }
    
    /**
     * Aggiorna un'entità esistente
     */
    public void update(T entity) throws SQLException {
        String sql = buildUpdateSql();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            mapEntityToUpdateStatement(stmt, entity);
            int affected = stmt.executeUpdate();
            
            if (affected == 0) {
                throw new SQLException("Aggiornamento fallito, entità non trovata");
            }
        }
    }
    
    /**
     * Elimina un'entità per ID
     */
    public boolean deleteById(ID id) throws SQLException {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameter(stmt, 1, id);
            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }
    
    /**
     * Conta il numero totale di entità
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    /**
     * Verifica se esiste un'entità con l'ID specificato
     */
    public boolean existsById(ID id) throws SQLException {
        String sql = "SELECT 1 FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameter(stmt, 1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Salva o aggiorna (upsert) un'entità
     */
    public void saveOrUpdate(T entity) throws SQLException {
        ID id = getEntityId(entity);
        if (id != null && existsById(id)) {
            update(entity);
        } else {
            save(entity);
        }
    }
    
    /**
     * Esegue una query personalizzata che restituisce una lista di entità
     */
    protected List<T> executeQuery(String sql, Object... parameters) throws SQLException {
        List<T> results = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.length; i++) {
                setParameter(stmt, i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
            }
        }
        return results;
    }
    
    /**
     * Esegue una query che restituisce un singolo risultato
     */
    protected Optional<T> executeQuerySingle(String sql, Object... parameters) throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.length; i++) {
                setParameter(stmt, i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Esegue un update/insert/delete personalizzato
     */
    protected int executeUpdate(String sql, Object... parameters) throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.length; i++) {
                setParameter(stmt, i + 1, parameters[i]);
            }
            
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Esegue una query che restituisce un valore singolo (count, sum, etc.)
     */
    protected <R> Optional<R> executeScalarQuery(String sql, Class<R> resultType, Object... parameters) throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.length; i++) {
                setParameter(stmt, i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    if (value != null && resultType.isAssignableFrom(value.getClass())) {
                        return Optional.of(resultType.cast(value));
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    // Metodi di utilità
    
    protected void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else {
            stmt.setObject(index, value);
        }
    }
    
    protected Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
    
    protected void setIntegerOrNull(PreparedStatement stmt, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(parameterIndex, Types.INTEGER);
        } else {
            stmt.setInt(parameterIndex, value);
        }
    }
    
    // Metodi da implementare opzionalmente per personalizzare le query SQL
    
    protected String buildInsertSql() {
        throw new UnsupportedOperationException("Insert SQL deve essere implementato nel DAO concreto");
    }
    
    protected String buildUpdateSql() {
        throw new UnsupportedOperationException("Update SQL deve essere implementato nel DAO concreto");
    }
    
    /**
     * Template method per validazione pre-salvataggio
     */
    protected void validateBeforeSave(T entity) throws SQLException {
        // Implementazione di default vuota, può essere sovrascritta
    }
    
    /**
     * Template method per azioni post-salvataggio
     */
    protected void afterSave(T entity) throws SQLException {
        // Implementazione di default vuota, può essere sovrascritta
    }
}