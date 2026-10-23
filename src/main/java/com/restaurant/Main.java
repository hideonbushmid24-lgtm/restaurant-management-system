package com.restaurant;

import com.restaurant.dao.UserDAO;
import com.restaurant.service.AuthService;
import com.restaurant.ui.LoginFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();

            UserDAO userDAO = new UserDAO();
            AuthService authService =
                    new AuthService(userDAO);

            LoginFrame loginFrame =
                    new LoginFrame(authService);

            loginFrame.setVisible(true);
        });
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager
                            .getSystemLookAndFeelClassName()
            );
        } catch (
                ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException exception
        ) {
            System.err.println(
                    "Không thể thiết lập giao diện hệ thống: "
                            + exception.getMessage()
            );
        }
    }
}