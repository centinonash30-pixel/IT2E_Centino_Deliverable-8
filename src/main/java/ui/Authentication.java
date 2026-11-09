package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Optional;
import java.util.Scanner;

public class Authentication {

    private static final String SYSTEM_TITLE = "EXCHANGE ASSET & DISTRIBUTION MANAGEMENT SYSTEM";

    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final AdminMenu adminMenu;
    private final Scanner scanner;

    public Authentication(UserDao userDao, ActivityLogDao logDao, AdminMenu adminMenu, Scanner scanner) {
        this.userDao = userDao;
        this.logDao = logDao;
        this.adminMenu = adminMenu;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("  " + SYSTEM_TITLE + " LOGIN");
            System.out.println("=".repeat(60));
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> running = false;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void register() {
        System.out.print("Choose a username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Choose a password: ");
        String password = scanner.nextLine();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return;
        }
        if (userDao.register(username, password)) {
            System.out.println("Registered! Wait for an admin to approve your account.");
        } else {
            System.out.println("Registration failed (username may already exist).");
        }
    }

    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.authenticate(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        if (!"APPROVED".equals(user.status())) {
            System.out.println("Your account is " + user.status() + ". Contact an admin.");
            return;
        }

        // Mandatory event 1a: login
        logDao.logAction(user.id(), "USER_LOGIN",
                String.format("User %s (ID: %d) logged in.", user.username(), user.id()));
        System.out.println("Welcome, " + user.username() + " (" + user.role() + ")!");

        if ("ADMIN".equals(user.role())) {
            adminMenu.start(user);
        } else {
            System.out.print("You are logged in. Press Enter to log out...");
            scanner.nextLine();
        }

        logout(user);
    }

    // Mandatory event 1b: logout
    private void logout(User user) {
        logDao.logAction(user.id(), "USER_LOGOUT",
                String.format("User %s (ID: %d) logged out.", user.username(), user.id()));
        System.out.println("Logged out.");
    }
}