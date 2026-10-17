package com.restaurant.ui;

import com.restaurant.dao.MenuDAO;
import com.restaurant.dao.ReservationDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import com.restaurant.service.MenuService;
import com.restaurant.service.ReservationService;
import com.restaurant.service.TableService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final Runnable logoutAction;

    private JButton tableButton;
    private JButton reservationButton;
    private JButton menuButton;
    private JButton kitchenButton;
    private JButton paymentButton;

    public DashboardFrame(
            User currentUser,
            Runnable logoutAction
    ) {
        if (currentUser == null) {
            throw new IllegalArgumentException(
                    "Người dùng hiện tại không được null"
            );
        }

        this.currentUser = currentUser;
        this.logoutAction = logoutAction;

        initializeFrame();
        createLayout();
        configurePermissions();
    }

    private void initializeFrame() {
        setTitle("Hệ thống quản lý nhà hàng");
        setSize(900, 600);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void createLayout() {
        setLayout(new BorderLayout());

        add(
                createHeaderPanel(),
                BorderLayout.NORTH
        );

        add(
                createMenuPanel(),
                BorderLayout.CENTER
        );
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel =
                new JPanel(new BorderLayout());

        headerPanel.setBorder(
                new EmptyBorder(
                        15,
                        20,
                        15,
                        20
                )
        );

        String greeting =
                "Xin chào, "
                        + currentUser.getFullName()
                        + " — "
                        + getRoleDisplayName(
                                currentUser.getRole()
                        );

        JLabel greetingLabel =
                new JLabel(greeting);

        greetingLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        17
                )
        );

        JButton logoutButton =
                new JButton("Đăng xuất");

        logoutButton.addActionListener(
                event -> logout()
        );

        headerPanel.add(
                greetingLabel,
                BorderLayout.WEST
        );

        headerPanel.add(
                logoutButton,
                BorderLayout.EAST
        );

        return headerPanel;
    }

    private JPanel createMenuPanel() {
        JPanel container =
                new JPanel(new BorderLayout());

        container.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        30,
                        30,
                        30
                )
        );

        JLabel titleLabel =
                new JLabel(
                        "CHỨC NĂNG HỆ THỐNG",
                        SwingConstants.CENTER
                );

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        24
                )
        );

        titleLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        25,
                        10
                )
        );

        JPanel buttonPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                3,
                                20,
                                20
                        )
                );

        tableButton =
                createMenuButton("Quản lý bàn");

        reservationButton =
                createMenuButton("Đặt bàn");

        menuButton =
                createMenuButton(
                        "Thực đơn và gọi món"
                );

        kitchenButton =
                createMenuButton("Xử lý tại bếp");

        paymentButton =
                createMenuButton(
                        "Thanh toán và hóa đơn"
                );

        JButton exitButton =
                createMenuButton("Thoát chương trình");

        tableButton.addActionListener(
                event -> openTableManagementFrame()
        );

        reservationButton.addActionListener(
                event -> openReservationFrame()
        );

        menuButton.addActionListener(
                event -> openMenuFrame()
        );

        kitchenButton.addActionListener(
                event -> showPendingFeature(
                        "Xử lý tại bếp"
                )
        );

        paymentButton.addActionListener(
                event -> showPendingFeature(
                        "Thanh toán và hóa đơn"
                )
        );

        exitButton.addActionListener(
                event -> exitApplication()
        );

        buttonPanel.add(tableButton);
        buttonPanel.add(reservationButton);
        buttonPanel.add(menuButton);
        buttonPanel.add(kitchenButton);
        buttonPanel.add(paymentButton);
        buttonPanel.add(exitButton);

        container.add(
                titleLabel,
                BorderLayout.NORTH
        );

        container.add(
                buttonPanel,
                BorderLayout.CENTER
        );

        return container;
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

    private void configurePermissions() {
        Role role = currentUser.getRole();

        boolean isManager =
                role == Role.MANAGER;

        boolean isReceptionist =
                role == Role.RECEPTIONIST;

        boolean isWaiter =
                role == Role.WAITER;

        boolean isKitchen =
                role == Role.KITCHEN;

        boolean isCashier =
                role == Role.CASHIER;

        tableButton.setEnabled(
                isManager || isReceptionist
        );

        reservationButton.setEnabled(
                isManager || isReceptionist
        );

        menuButton.setEnabled(
                isManager
                        || isReceptionist
                        || isWaiter
        );

        kitchenButton.setEnabled(
                isManager || isKitchen
        );

        paymentButton.setEnabled(
                isManager || isCashier
        );
    }

    private void openTableManagementFrame() {
        TableDAO tableDAO =
                new TableDAO();

        TableService tableService =
                new TableService(tableDAO);

        boolean canManageTables =
                currentUser.getRole() == Role.MANAGER
                        || currentUser.getRole()
                        == Role.RECEPTIONIST;

        TableManagementFrame frame =
                new TableManagementFrame(
                        tableService,
                        canManageTables
                );

        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    private void openReservationFrame() {
        ReservationDAO reservationDAO =
                new ReservationDAO();

        ReservationService reservationService =
                new ReservationService(
                        reservationDAO
                );

        ReservationFrame frame =
                new ReservationFrame(
                        reservationService,
                        currentUser
                );

        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    private void openMenuFrame() {
        MenuDAO menuDAO =
                new MenuDAO();

        MenuService menuService =
                new MenuService(menuDAO);

        MenuFrame menuFrame =
                new MenuFrame(menuService);

        menuFrame.setLocationRelativeTo(this);
        menuFrame.setVisible(true);
    }

    private String getRoleDisplayName(Role role) {
        if (role == null) {
            return "Không xác định";
        }

        return switch (role) {
            case MANAGER -> "Quản lý";
            case RECEPTIONIST ->
                    "Nhân viên tiếp nhận";
            case WAITER ->
                    "Nhân viên phục vụ";
            case KITCHEN ->
                    "Nhân viên bếp";
            case CASHIER ->
                    "Thu ngân";
        };
    }

    private void showPendingFeature(
            String feature
    ) {
        JOptionPane.showMessageDialog(
                this,
                "Chức năng “"
                        + feature
                        + "” sẽ được triển khai tiếp theo.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void logout() {
        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Bạn có muốn đăng xuất không?",
                        "Xác nhận đăng xuất",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (result == JOptionPane.YES_OPTION) {
            dispose();

            if (logoutAction != null) {
                logoutAction.run();
            }
        }
    }

    private void exitApplication() {
        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Bạn có muốn thoát chương trình không?",
                        "Xác nhận thoát",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}