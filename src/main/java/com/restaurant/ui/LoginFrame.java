package com.restaurant.ui;

import com.restaurant.model.User;
import com.restaurant.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private final AuthService authService;

    private final JTextField usernameField =
            new JTextField(20);

    private final JPasswordField passwordField =
            new JPasswordField(20);

    private final JButton loginButton =
            new JButton("Đăng nhập");

    public LoginFrame(AuthService authService) {
        this.authService = authService;

        initializeFrame();
        initializeComponents();
    }

    private void initializeFrame() {
        setTitle("Hệ thống quản lý nhà hàng");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 360);
        setMinimumSize(new Dimension(480, 360));
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initializeComponents() {
        JPanel mainPanel = new JPanel(
                new GridBagLayout()
        );

        mainPanel.setBorder(
                new EmptyBorder(25, 40, 25, 40)
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        constraints.insets =
                new Insets(8, 8, 8, 8);

        JLabel titleLabel =
                new JLabel(
                        "QUẢN LÝ NHÀ HÀNG",
                        SwingConstants.CENTER
                );

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        24
                )
        );

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;

        mainPanel.add(titleLabel, constraints);

        JLabel descriptionLabel =
                new JLabel(
                        "Vui lòng đăng nhập để tiếp tục",
                        SwingConstants.CENTER
                );

        constraints.gridy = 1;
        mainPanel.add(descriptionLabel, constraints);

        constraints.gridwidth = 1;
        constraints.gridy = 2;
        constraints.gridx = 0;

        mainPanel.add(
                new JLabel("Tên đăng nhập:"),
                constraints
        );

        constraints.gridx = 1;
        mainPanel.add(usernameField, constraints);

        constraints.gridy = 3;
        constraints.gridx = 0;

        mainPanel.add(
                new JLabel("Mật khẩu:"),
                constraints
        );

        constraints.gridx = 1;
        mainPanel.add(passwordField, constraints);

        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 2;

        loginButton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );

        loginButton.addActionListener(
                event -> handleLogin()
        );

        mainPanel.add(loginButton, constraints);

        JLabel demoLabel = new JLabel(
                "<html><center>"
                        + "Tài khoản demo: "
                        + "manager / manager123"
                        + "</center></html>",
                SwingConstants.CENTER
        );

        demoLabel.setForeground(Color.GRAY);

        constraints.gridy = 5;
        mainPanel.add(demoLabel, constraints);

        getRootPane().setDefaultButton(loginButton);
        add(mainPanel);
    }

    private void handleLogin() {
        String username = usernameField
                .getText()
                .trim();

        char[] passwordCharacters =
                passwordField.getPassword();

        String password =
                new String(passwordCharacters);

        Arrays.fill(passwordCharacters, '\0');

        loginButton.setEnabled(false);

        try {
            Optional<User> userOptional =
                    authService.login(
                            username,
                            password
                    );

            if (userOptional.isEmpty()) {
                showError(
                        "Tên đăng nhập hoặc mật khẩu không đúng."
                );
                return;
            }

            openDashboard(userOptional.get());

        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());

        } catch (SQLException exception) {
            showError(
                    "Không thể kết nối cơ sở dữ liệu.\n"
                            + exception.getMessage()
            );

        } finally {
            loginButton.setEnabled(true);
        }
    }

    private void openDashboard(User user) {
        passwordField.setText("");
        setVisible(false);

        DashboardFrame dashboardFrame =
                new DashboardFrame(
                        user,
                        () -> {
                            usernameField.setText("");
                            passwordField.setText("");
                            setVisible(true);
                            usernameField.requestFocusInWindow();
                        }
                );

        dashboardFrame.setVisible(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Đăng nhập thất bại",
                JOptionPane.ERROR_MESSAGE
        );
    }
}