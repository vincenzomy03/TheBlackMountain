package com.mycompany.theblackmountain.database;



import com.mycompany.theblackmountain.type.Room;
import com.mycompany.theblackmountain.type.Weapon;
import java.sql.*;
import java.util.*;

public class TBMDatabase {

    private static final String DB_URL = "jdbc:h2:./tbmgame;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    /* =====================
       CONNESSIONE BASE
       ===================== */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /* =====================
       TEST CONNESSIONE
       ===================== */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* =====================
       LOAD DATA
       ===================== */

    public static Room loadRoom(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Room(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<GameObject> loadRoomObjects(int roomId) {
        List<GameObject> objects = new ArrayList<>();
        String sql = "SELECT * FROM objects WHERE room_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                objects.add(processObjectEntity(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return objects;
    }

    private static GameObject processObjectEntity(ResultSet rs) throws SQLException {
        return new GameObject(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("pickupable")
        );
    }

    public static List<Enemy> loadRoomEnemies(int roomId) {
        List<Enemy> enemies = new ArrayList<>();
        String sql = "SELECT * FROM enemies WHERE room_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                enemies.add(processEnemyEntity(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enemies;
    }

    private static Enemy processEnemyEntity(ResultSet rs) throws SQLException {
        return new Enemy(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("hp"),
                rs.getInt("attack"),
                rs.getInt("defense")
        );
    }

    /* =====================
       SAVE / INSERT DATA
       ===================== */

    public static void saveRoom(Room room) {
        String sql = "INSERT INTO rooms (id, name, description) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, room.getId());
            ps.setString(2, room.getName());
            ps.setString(3, room.getDescription());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveWeapon(Weapon weapon) {
        String sql = "INSERT INTO weapons (name, damage, weight) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, weapon.getName());
            ps.setInt(2, weapon.getDamage());
            ps.setDouble(3, weapon.getWeight());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* =====================
       UPDATE DATA
       ===================== */

    public static void updatePlayerRoom(int playerId, int roomId) {
        String sql = "UPDATE players SET room_id = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setInt(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* =====================
       DELETE DATA
       ===================== */

    public static void deleteWeaponByName(String weaponName) {
        String sql = "DELETE FROM weapons WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, weaponName);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* =====================
       STATS DATABASE
       ===================== */
    public static Map<String, Integer> getDatabaseStats() {
        Map<String, Integer> stats = new HashMap<>();
        String[] tables = {"rooms", "objects", "enemies", "weapons", "players"};

        try (Connection conn = getConnection()) {
            for (String table : tables) {
                String sql = "SELECT COUNT(*) FROM " + table;
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        stats.put(table, rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}
