package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AssetDao;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.AdminMenu;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        Scanner scanner = new Scanner(System.in);

        ActivityLogDao logDao = new ActivityLogDao();
        UserDao userDao = new UserDao(logDao);
        AssetDao assetDao = new AssetDao(logDao);

        AdminMenu adminMenu = new AdminMenu(userDao, logDao, assetDao, scanner);
        Authentication auth = new Authentication(userDao, logDao, adminMenu, scanner);

        auth.start();
        scanner.close();
    }
}