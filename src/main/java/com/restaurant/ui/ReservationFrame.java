package com.restaurant.ui;

import com.restaurant.model.Reservation;
import com.restaurant.model.ReservationStatus;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.User;
import com.restaurant.service.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReservationFrame extends JFrame {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm"
            );

    private final ReservationService reservationService;
    private final User currentUser;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Bàn",
                            "Khách hàng",
                            "Số điện thoại",
                            "Thời gian",
                            "Số khách",
                            "Trạng thái"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(
                        int row,
                        int column
                ) {
                    return false;
                }
            };

    private final JTable reservationTable =
            new JTable(tableModel);

    private List<Reservation> currentReservations =
            List.of();

    public ReservationFrame(
            ReservationService reservationService,
            User currentUser
    ) {
        this.reservationService = reservationService;
        this.currentUser = currentUser;

        initializeFrame();
        initializeComponents();
        loadReservations();
    }

    private void initializeFrame() {
        setTitle("Quản lý đặt bàn");
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
        setSize(1050, 600);
        setMinimumSize(new Dimension(950, 550));
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        JPanel mainPanel =
                new JPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel =
                new JLabel("DANH SÁCH ĐẶT BÀN");

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        mainPanel.add(
                titleLabel,
                BorderLayout.NORTH
        );

        reservationTable.setRowHeight(26);
        reservationTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        reservationTable.setAutoCreateRowSorter(true);

        mainPanel.add(
                new JScrollPane(reservationTable),
                BorderLayout.CENTER
        );

        mainPanel.add(
                createActionPanel(),
                BorderLayout.SOUTH
        );

        add(mainPanel);
    }

    private JPanel createActionPanel() {
        JPanel panel =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT
                ));

        JButton reloadButton =
                new JButton("Tải lại");

        JButton createButton =
                new JButton("Đặt bàn mới");

        JButton seatButton =
                new JButton("Khách nhận bàn");

        JButton cancelButton =
                new JButton("Hủy đặt bàn");

        JButton closeButton =
                new JButton("Đóng");

        reloadButton.addActionListener(
                event -> loadReservations()
        );

        createButton.addActionListener(
                event -> showCreateDialog()
        );

        seatButton.addActionListener(
                event -> seatSelectedReservation()
        );

        cancelButton.addActionListener(
                event -> cancelSelectedReservation()
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        panel.add(reloadButton);
        panel.add(createButton);
        panel.add(seatButton);
        panel.add(cancelButton);
        panel.add(closeButton);

        return panel;
    }

    private void loadReservations() {
        try {
            currentReservations =
                    reservationService
                            .getAllReservations();

            tableModel.setRowCount(0);

            for (Reservation reservation
                    : currentReservations) {

                tableModel.addRow(new Object[]{
                        reservation.getId(),
                        reservation.getTableNumber(),
                        reservation.getCustomerName(),
                        reservation.getCustomerPhone(),
                        reservation
                                .getReservationTime()
                                .format(DATE_TIME_FORMAT),
                        reservation.getGuestCount(),
                        reservation
                                .getStatus()
                                .getDisplayName()
                });
            }

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh sách đặt bàn.\n"
                            + exception.getMessage()
            );
        }
    }

    private void showCreateDialog() {
        try {
            List<RestaurantTable> availableTables =
                    reservationService
                            .getAvailableTables();

            if (availableTables.isEmpty()) {
                showError(
                        "Hiện không có bàn trống."
                );
                return;
            }

            JComboBox<RestaurantTable> tableComboBox =
                    new JComboBox<>(
                            availableTables.toArray(
                                    RestaurantTable[]::new
                            )
                    );

            configureTableRenderer(tableComboBox);

            JTextField customerNameField =
                    new JTextField(20);

            JTextField phoneField =
                    new JTextField(20);

            JTextField timeField =
                    new JTextField(
                            LocalDateTime.now()
                                    .plusMinutes(30)
                                    .withSecond(0)
                                    .withNano(0)
                                    .format(
                                            DATE_TIME_FORMAT
                                    ),
                            20
                    );

            JSpinner guestCountSpinner =
                    new JSpinner(
                            new SpinnerNumberModel(
                                    2,
                                    1,
                                    50,
                                    1
                            )
                    );

            JTextArea noteArea =
                    new JTextArea(3, 20);

            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);

            JPanel formPanel =
                    new JPanel(new GridBagLayout());

            GridBagConstraints constraints =
                    new GridBagConstraints();

            constraints.insets =
                    new Insets(6, 6, 6, 6);

            constraints.fill =
                    GridBagConstraints.HORIZONTAL;

            addFormRow(
                    formPanel,
                    constraints,
                    0,
                    "Bàn:",
                    tableComboBox
            );

            addFormRow(
                    formPanel,
                    constraints,
                    1,
                    "Tên khách hàng:",
                    customerNameField
            );

            addFormRow(
                    formPanel,
                    constraints,
                    2,
                    "Số điện thoại:",
                    phoneField
            );

            addFormRow(
                    formPanel,
                    constraints,
                    3,
                    "Thời gian:",
                    timeField
            );

            addFormRow(
                    formPanel,
                    constraints,
                    4,
                    "Số khách:",
                    guestCountSpinner
            );

            addFormRow(
                    formPanel,
                    constraints,
                    5,
                    "Ghi chú:",
                    new JScrollPane(noteArea)
            );

            JLabel formatLabel =
                    new JLabel(
                            "Định dạng thời gian: "
                                    + "yyyy-MM-dd HH:mm"
                    );

            formatLabel.setForeground(Color.GRAY);

            constraints.gridx = 1;
            constraints.gridy = 6;
            formPanel.add(formatLabel, constraints);

            int result =
                    JOptionPane.showConfirmDialog(
                            this,
                            formPanel,
                            "Tạo đặt bàn",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            RestaurantTable selectedTable =
                    (RestaurantTable)
                            tableComboBox
                                    .getSelectedItem();

            if (selectedTable == null) {
                showError("Vui lòng chọn bàn.");
                return;
            }

            LocalDateTime reservationTime =
                    LocalDateTime.parse(
                            timeField.getText().trim(),
                            DATE_TIME_FORMAT
                    );

            int guestCount =
                    (Integer)
                            guestCountSpinner.getValue();

            reservationService.createReservation(
                    selectedTable.getId(),
                    customerNameField.getText(),
                    phoneField.getText(),
                    reservationTime,
                    guestCount,
                    noteArea.getText(),
                    currentUser.getId()
            );

            showSuccess("Đặt bàn thành công.");
            loadReservations();

        } catch (DateTimeParseException exception) {
            showError(
                    "Thời gian không đúng định dạng "
                            + "yyyy-MM-dd HH:mm"
            );

        } catch (
                IllegalArgumentException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void configureTableRenderer(
            JComboBox<RestaurantTable> comboBox
    ) {
        comboBox.setRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {
                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );

                        if (value
                                instanceof RestaurantTable table) {

                            setText(
                                    table.getTableNumber()
                                            + " — "
                                            + table.getCapacity()
                                            + " chỗ"
                            );
                        }

                        return this;
                    }
                }
        );
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            Component component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;

        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;

        panel.add(component, constraints);
    }

    private Reservation getSelectedReservation() {
        int selectedViewRow =
                reservationTable.getSelectedRow();

        if (selectedViewRow < 0) {
            showError(
                    "Vui lòng chọn một lượt đặt bàn."
            );
            return null;
        }

        int selectedModelRow =
                reservationTable
                        .convertRowIndexToModel(
                                selectedViewRow
                        );

        return currentReservations.get(
                selectedModelRow
        );
    }

    private void seatSelectedReservation() {
        Reservation reservation =
                getSelectedReservation();

        if (reservation == null) {
            return;
        }

        if (reservation.getStatus()
                != ReservationStatus.CONFIRMED) {

            showError(
                    "Chỉ đặt bàn đã xác nhận "
                            + "mới được nhận bàn."
            );
            return;
        }

        try {
            reservationService.seatReservation(
                    reservation.getId()
            );

            showSuccess(
                    "Khách đã nhận bàn "
                            + reservation.getTableNumber()
                            + "."
            );

            loadReservations();

        } catch (
                IllegalArgumentException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void cancelSelectedReservation() {
        Reservation reservation =
                getSelectedReservation();

        if (reservation == null) {
            return;
        }

        if (reservation.getStatus()
                != ReservationStatus.CONFIRMED) {

            showError(
                    "Chỉ đặt bàn đã xác nhận "
                            + "mới được hủy."
            );
            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Bạn có chắc muốn hủy đặt bàn "
                                + reservation
                                        .getTableNumber()
                                + " của khách "
                                + reservation
                                        .getCustomerName()
                                + " không?",
                        "Xác nhận hủy",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            reservationService.cancelReservation(
                    reservation.getId()
            );

            showSuccess("Hủy đặt bàn thành công.");
            loadReservations();

        } catch (
                IllegalArgumentException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Lỗi",
                JOptionPane.ERROR_MESSAGE
        );
    }
}