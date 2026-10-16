package com.restaurant.ui;

import com.restaurant.model.Role;
import com.restaurant.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import com.restaurant.dao.TableDAO;
import com.restaurant.service.TableService;

import com.restaurant.dao.ReservationDAO;
import com.restaurant.service.ReservationService;

public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final Runnable logoutAction;

    public DashboardFrame(
            User currentUser,
            Runnable logoutAction
    ) {
        this.currentUser = currentUser;
        this.logoutAction = logoutAction;

        initializeFrame();
        initializeComponents();
    }

    private void initializeFrame() {
        setTitle("Hệ thống quản lý nhà hàng");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMenuPanel(), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel =
                new JPanel(new BorderLayout());

        headerPanel.setBorder(
                new EmptyBorder(15, 20, 15, 20)
        );

        JLabel userLabel = new JLabel(
                "Xin chào, "
                        + currentUser.getFullName()
                        + " — "
                        + getRoleDisplayName(
                                currentUser.getRole()
                        )
        );

        userLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        17
                )
        );

        JButton logoutButton =
                new JButton("Đăng xuất");

        logoutButton.addActionListener(
                event -> handleLogout()
        );

        headerPanel.add(
                userLabel,
                BorderLayout.WEST
        );

        headerPanel.add(
                logoutButton,
                BorderLayout.EAST
        );

        return headerPanel;
    }

    private JPanel createMenuPanel() {
        JPanel menuPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                3,
                                20,
                                20
                        )
                );

        menuPanel.setBorder(
                new EmptyBorder(40, 40, 40, 40)
        );

        JButton tableButton =
                createMenuButton("Quản lý bàn");

        JButton reservationButton =
                createMenuButton("Đặt bàn");

        JButton orderButton =
                createMenuButton(
                        "Thực đơn và gọi món"
                );

        JButton kitchenButton =
                createMenuButton("Xử lý tại bếp");

        JButton paymentButton =
                createMenuButton(
                        "Thanh toán và hóa đơn"
                );

        JButton exitButton =
                createMenuButton("Thoát chương trình");

        configurePermissions(
                tableButton,
                reservationButton,
                orderButton,
                kitchenButton,
                paymentButton
        );

        tableButton.addActionListener(event -> {
            boolean canManageTables =
                currentUser.getRole() == Role.MANAGER
                    || currentUser.getRole()
                    == Role.RECEPTIONIST;

            TableService tableService =
                new TableService(new TableDAO());

            TableManagementFrame frame =
                new TableManagementFrame(
                    tableService,
                    canManageTables
            );

        frame.setVisible(true);
        });

        reservationButton.addActionListener(event -> {
                ReservationService reservationService =
                        new ReservationService(
                        new ReservationDAO()
                );

                ReservationFrame frame =
                        new ReservationFrame(
                    reservationService,
                    currentUser
            );

        frame.setVisible(true);
        });

        orderButton.addActionListener(
                event -> showPendingFeature(
                        "Thực đơn và gọi món"
                )
        );

        kitchenButton.addActionListener(
                event -> showPendingFeature(
                        "Xử lý món tại bếp"
                )
        );

        paymentButton.addActionListener(
                event -> showPendingFeature(
                        "Thanh toán và hóa đơn"
                )
        );

        exitButton.addActionListener(
                event -> System.exit(0)
        );

        menuPanel.add(tableButton);
        menuPanel.add(reservationButton);
        menuPanel.add(orderButton);
        menuPanel.add(kitchenButton);
        menuPanel.add(paymentButton);
        menuPanel.add(exitButton);

        return menuPanel;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        button.setFocusPainted(false);

        return button;
    }

    private void configurePermissions(
            JButton tableButton,
            JButton reservationButton,
            JButton orderButton,
            JButton kitchenButton,
            JButton paymentButton
    ) {
        Role role = currentUser.getRole();

        boolean isManager =
                role == Role.MANAGER;

        tableButton.setEnabled(
                isManager
                        || role == Role.RECEPTIONIST
                        || role == Role.WAITER
        );

        reservationButton.setEnabled(
                isManager
                        || role == Role.RECEPTIONIST
        );

        orderButton.setEnabled(
                isManager
                        || role == Role.WAITER
        );

        kitchenButton.setEnabled(
                isManager
                        || role == Role.KITCHEN
        );

        paymentButton.setEnabled(
                isManager
                        || role == Role.CASHIER
        );
    }

    private String getRoleDisplayName(Role role) {
        return switch (role) {
            case MANAGER -> "Quản lý";
            case RECEPTIONIST -> "Nhân viên tiếp nhận";
            case WAITER -> "Nhân viên phục vụ";
            case KITCHEN -> "Nhân viên bếp";
            case CASHIER -> "Thu ngân";
        };
    }

    private void showPendingFeature(String feature) {
        JOptionPane.showMessageDialog(
                this,
                "Chức năng “"
                        + feature
                        + "” sẽ được triển khai tiếp theo.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có muốn đăng xuất không?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            logoutAction.run();
        }
    }
}