package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Domain DAO for the Exchange Asset & Distribution Management System. */
public class AssetDao {

    private final ActivityLogDao logDao;

    public AssetDao(ActivityLogDao logDao) {
        this.logDao = logDao;
    }

    /** Registers a new asset -> logs ASSET_ADDED */
    public boolean addAsset(int actorId, String name, int quantity) {
        String sql = "INSERT INTO assets (name, quantity) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, quantity);
            if (ps.executeUpdate() == 0) {
                return false;
            }
            int assetId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    assetId = keys.getInt(1);
                }
            }
            logDao.logAction(actorId, "ASSET_ADDED",
                    String.format("Added Asset #%d %s (qty %d)", assetId, name, quantity));
            return true;
        } catch (SQLException e) {
            System.err.println("Add asset failed: " + e.getMessage());
            return false;
        }
    }

    /** Core business process: exchange / distribute stock to a recipient -> logs ASSET_EXCHANGED */
    public boolean exchangeAsset(int actorId, int assetId, String recipient, int qty) {
        String select = "SELECT name, quantity FROM assets WHERE id = ?";
        String update = "UPDATE assets SET quantity = quantity - ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String name;
            int available;
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    name = rs.getString("name");
                    available = rs.getInt("quantity");
                }
            }
            if (qty <= 0 || qty > available) {
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, qty);
                ps.setInt(2, assetId);
                boolean ok = ps.executeUpdate() > 0;
                if (ok) {
                    logDao.logAction(actorId, "ASSET_EXCHANGED",
                            String.format("Distributed %d x %s to %s", qty, name, recipient));
                }
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("Exchange asset failed: " + e.getMessage());
            return false;
        }
    }

    /** Lifecycle / state change -> logs ASSET_STATUS_UPDATED */
    public boolean updateStatus(int actorId, int assetId, String newStatus) {
        String select = "SELECT status FROM assets WHERE id = ?";
        String update = "UPDATE assets SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String oldStatus;
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    oldStatus = rs.getString("status");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setString(1, newStatus);
                ps.setInt(2, assetId);
                boolean ok = ps.executeUpdate() > 0;
                if (ok) {
                    logDao.logAction(actorId, "ASSET_STATUS_UPDATED",
                            String.format("Updated Asset #%d status from %s to %s",
                                    assetId, oldStatus, newStatus));
                }
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("Update status failed: " + e.getMessage());
            return false;
        }
    }
}