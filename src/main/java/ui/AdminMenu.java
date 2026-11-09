package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AssetDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class AdminMenu {

    private static final Set<String> ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");
    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final Set<String> ASSET_STATUSES = Set.of("AVAILABLE", "RESERVED", "DISTRIBUTED", "DAMAGED");
    private static final String LOG_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";

    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final AssetDao assetDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, ActivityLogDao logDao, AssetDao assetDao, Scanner scanner) {
        this.userDao = userDao;
        this.logDao = logDao;
        this.assetDao = assetDao;
        this.scanner = scanner;
    }

    public void start(User admin) {
        boolean running = true;

        while (running) {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. Approve Pending Accounts");
            System.out.println("2. Update User Role/Status");
            System.out.println("3. Add Asset");
            System.out.println("4. Exchange / Distribute Asset");
            System.out.println("5. Update Asset Status");
            System.out.println("6. View System Audit Logs");   // [NEW]
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> approveAccounts(admin);
                case "2" -> updateUser(admin);
                case "3" -> addAsset(admin);
                case "4" -> exchangeAsset(admin);
                case "5" -> updateAssetStatus(admin);
                case "6" -> viewAuditLogs();
                case "7" -> running = false;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    // ---------- Account management ----------

    private void approveAccounts(User admin) {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        System.out.println("Pending accounts:");
        for (User u : pending) {
            System.out.printf("  ID %d - %s%n", u.id(), u.username());
        }
        Integer id = readInt("Enter User ID to approve: ");
        if (id == null) return;

        System.out.println(userDao.approveUser(admin.id(), id) ? "Account approved." : "User not found.");
    }

    private void updateUser(User admin) {
        Integer id = readInt("Enter User ID: ");
        if (id == null) return;

        System.out.print("New role (ADMIN/TEACHER/STUDENT): ");
        String role = scanner.nextLine().trim().toUpperCase();
        System.out.print("New status (PENDING/APPROVED/REJECTED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (!ROLES.contains(role) || !STATUSES.contains(status)) {
            System.out.println("Invalid role or status.");
            return;
        }
        System.out.println(userDao.updateUserRoleAndStatus(admin.id(), id, role, status)
                ? "User updated." : "User not found.");
    }

    // ---------- Domain actions: Exchange Asset & Distribution ----------

    private void addAsset(User admin) {
        System.out.print("Asset name: ");
        String name = scanner.nextLine().trim();
        Integer qty = readInt("Quantity: ");
        if (name.isEmpty() || qty == null || qty < 0) {
            System.out.println("Invalid asset details.");
            return;
        }
        System.out.println(assetDao.addAsset(admin.id(), name, qty) ? "Asset added." : "Could not add asset.");
    }

    private void exchangeAsset(User admin) {
        Integer assetId = readInt("Asset ID: ");
        if (assetId == null) return;
        System.out.print("Recipient: ");
        String recipient = scanner.nextLine().trim();
        Integer qty = readInt("Quantity to distribute: ");
        if (qty == null || recipient.isEmpty()) {
            System.out.println("Recipient and quantity are required.");
            return;
        }
        System.out.println(assetDao.exchangeAsset(admin.id(), assetId, recipient, qty)
                ? "Asset distributed." : "Failed (check Asset ID and available stock).");
    }

    private void updateAssetStatus(User admin) {
        Integer assetId = readInt("Asset ID: ");
        if (assetId == null) return;
        System.out.print("New status (AVAILABLE/RESERVED/DISTRIBUTED/DAMAGED): ");
        String status = scanner.nextLine().trim().toUpperCase();
        if (!ASSET_STATUSES.contains(status)) {
            System.out.println("Invalid status.");
            return;
        }
        System.out.println(assetDao.updateStatus(admin.id(), assetId, status)
                ? "Asset status updated." : "Asset not found.");
    }

    // ---------- [NEW] Audit log interface ----------

    private void viewAuditLogs() {
        boolean back = false;

        while (!back) {
            System.out.println("\n===== SYSTEM AUDIT LOGS =====");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Back");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> printLogs(logDao.getAllLogs());
                case "2" -> {
                    Integer userId = readInt("Enter User ID: ");
                    if (userId != null) {
                        printLogs(logDao.getLogsByUserId(userId));
                    }
                }
                case "3" -> back = true;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void printLogs(List<ActivityLog> logs) {
        System.out.println();
        System.out.printf(LOG_FORMAT, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println("-".repeat(98));

        if (logs.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }

        for (ActivityLog log : logs) {
            String desc = log.description() == null ? "" : log.description();
            int width = 35;

            System.out.printf(LOG_FORMAT, log.id(), log.userId(), log.actionType(),
                    desc.substring(0, Math.min(width, desc.length())), log.timestamp());

            // long descriptions wrap onto extra rows so nothing is cut off
            for (int i = width; i < desc.length(); i += width) {
                System.out.printf(LOG_FORMAT, "", "", "",
                        desc.substring(i, Math.min(i + width, desc.length())), "");
            }
        }
    }

    // ---------- Input helpers: re-prompt instead of crashing ----------

    private Integer readInt(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a whole number.");
            return null;
        }
    }
}