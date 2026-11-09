package com.scc.dao;

import com.scc.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private final ActivityLogDao logDao;

    public UserDao(ActivityLogDao logDao) {
        this.logDao = logDao;
    }

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** New accounts are STUDENT + PENDING until an admin approves them. */
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, 'STUDENT', 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT id, username, role, status FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role, status FROM users WHERE status = 'PENDING' ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load pending users: " + e.getMessage());
        }
        return users;
    }

    /** Deliverable 5: approve an account, then log USER_APPROVED. */
    public boolean approveUser(int adminId, int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(adminId, "USER_APPROVED",
                        "Admin " + adminId + " approved account for User ID " + userId);
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("Approval failed: " + e.getMessage());
            return false;
        }
    }

    /** Deliverable 7: change role and status, then log USER_UPDATED. */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        String sql = "UPDATE users SET role = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setString(2, newStatus);
            ps.setInt(3, userId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(adminId, "USER_UPDATED",
                        "Admin " + adminId + " changed role of User " + userId + " to " + newRole);
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"),
                rs.getString("role"), rs.getString("status"));
    }
}