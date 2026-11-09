package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'STUDENT',
                    status TEXT NOT NULL DEFAULT 'PENDING'
                )
                """;

        // [NEW] Deliverable 8 - audit trail table
        String createLogs = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        // Domain table: Exchange Asset & Distribution
        String createAssets = """
                CREATE TABLE IF NOT EXISTS assets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
                """;

        String seedAdmin = "INSERT OR IGNORE INTO users (username, password, role, status) " +
                "VALUES (?, ?, 'ADMIN', 'APPROVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createLogs);
            stmt.execute(createAssets);

            try (PreparedStatement ps = conn.prepareStatement(seedAdmin)) {
                ps.setString(1, "admin");
                ps.setString(2, UserDao.hash("admin123"));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}