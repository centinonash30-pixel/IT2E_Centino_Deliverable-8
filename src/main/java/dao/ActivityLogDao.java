package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final String INSERT_SQL =
            "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";

    private static final String SELECT_ALL_SQL =
            "SELECT id, user_id, action_type, description, timestamp " +
                    "FROM activity_logs ORDER BY timestamp DESC, id DESC";

    private static final String SELECT_BY_USER_SQL =
            "SELECT id, user_id, action_type, description, timestamp " +
                    "FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, id DESC";

    /** Inserts a new log entry. Logging must never crash the main feature, so errors are caught. */
    public void logAction(int userId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write activity log: " + e.getMessage());
        }
    }

    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch logs: " + e.getMessage());
        }
        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_SQL)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch logs for user " + userId + ": " + e.getMessage());
        }
        return logs;
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}