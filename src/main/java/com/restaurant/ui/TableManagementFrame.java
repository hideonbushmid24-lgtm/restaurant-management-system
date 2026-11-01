package com.restaurant.ui;

import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;
import com.restaurant.service.TableService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class TableManagementFrame extends JFrame {

    private final TableService tableService;
    private final boolean canManage;

    private final JTextField searchField =
            new JTextField(15);

    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{
                    "Tất cả",
                    "Trống",
                    "Đã đặt",
                    "Đang sử dụng"
            });

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Số bàn",
                            "Sức chứa",
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

    private final JTable table =
            new JTable(tableModel);

    private List<RestaurantTable> currentTables =
            List.of();

    public TableManagementFrame(
            TableService tableService,
            boolean canManage
    ) {
        this.tableService = tableService;
        this.canManage = canManage;

        initializeFrame();
        initializeComponents();
        loadTables();
    }

    private void initializeFrame() {
        setTitle("Quản lý bàn");
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
        setSize(850, 550);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        JPanel mainPanel =
                new JPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        mainPanel.add(
                createSearchPanel(),
                BorderLayout.NORTH
        );

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        mainPanel.add(
                createActionPanel(),
                BorderLayout.SOUTH
        );

        add(mainPanel);
    }

    private JPanel createSearchPanel() {
        JPanel panel =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT
                ));

        JButton searchButton =
                new JButton("Tìm kiếm");

        JButton resetButton =
                new JButton("Đặt lại");

        panel.add(new JLabel("Số bàn:"));
        panel.add(searchField);
        panel.add(new JLabel("Trạng thái:"));
        panel.add(statusFilter);
        panel.add(searchButton);
        panel.add(resetButton);

        searchButton.addActionListener(
                event -> loadTables()
        );

        statusFilter.addActionListener(
                event -> loadTables()
        );

        searchField.addActionListener(
                event -> loadTables()
        );

        resetButton.addActionListener(event -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadTables();
        });

        return panel;
    }

    private JScrollPane createTablePanel() {
        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);

        table.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(50);

        table.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(150);

        table.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(120);

        table.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(180);

        return new JScrollPane(table);
    }

    private JPanel createActionPanel() {
        JPanel panel =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT
                ));

        JButton refreshButton =
                new JButton("Tải lại");

        JButton addButton =
                new JButton("Thêm bàn");

        JButton editButton =
                new JButton("Sửa bàn");

        JButton deleteButton =
                new JButton("Xóa bàn");

        JButton closeButton =
                new JButton("Đóng");

        addButton.setEnabled(canManage);
        editButton.setEnabled(canManage);
        deleteButton.setEnabled(canManage);

        refreshButton.addActionListener(
                event -> loadTables()
        );

        addButton.addActionListener(
                event -> showTableDialog(null)
        );

        editButton.addActionListener(event -> {
            RestaurantTable selected =
                    getSelectedTable();

            if (selected != null) {
                showTableDialog(selected);
            }
        });

        deleteButton.addActionListener(
                event -> deleteSelectedTable()
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        panel.add(refreshButton);
        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(closeButton);

        return panel;
    }

    private void loadTables() {
        try {
            TableStatus selectedStatus =
                    getSelectedStatus();

            currentTables = tableService.search(
                    searchField.getText(),
                    selectedStatus
            );

            tableModel.setRowCount(0);

            for (RestaurantTable restaurantTable
                    : currentTables) {

                tableModel.addRow(new Object[]{
                        restaurantTable.getId(),
                        restaurantTable.getTableNumber(),
                        restaurantTable.getCapacity(),
                        restaurantTable
                                .getStatus()
                                .getDisplayName()
                });
            }

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh sách bàn.\n"
                            + exception.getMessage()
            );
        }
    }

    private TableStatus getSelectedStatus() {
        String selected =
                (String) statusFilter.getSelectedItem();

        if (selected == null) {
            return null;
        }

        return switch (selected) {
            case "Trống" ->
                    TableStatus.AVAILABLE;

            case "Đã đặt" ->
                    TableStatus.RESERVED;

            case "Đang sử dụng" ->
                    TableStatus.OCCUPIED;

            default -> null;
        };
    }

    private RestaurantTable getSelectedTable() {
        int selectedViewRow =
                table.getSelectedRow();

        if (selectedViewRow < 0) {
            showError(
                    "Vui lòng chọn một bàn trong danh sách."
            );
            return null;
        }

        int selectedModelRow =
                table.convertRowIndexToModel(
                        selectedViewRow
                );

        return currentTables.get(selectedModelRow);
    }

    private void showTableDialog(
            RestaurantTable existingTable
    ) {
        JTextField tableNumberField =
                new JTextField(15);

        JSpinner capacitySpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                2,
                                1,
                                50,
                                1
                        )
                );

        JComboBox<TableStatus> statusComboBox =
                new JComboBox<>(new TableStatus[]{
                        TableStatus.AVAILABLE,
                        TableStatus.RESERVED,
                        TableStatus.OCCUPIED
                });

        boolean editing =
                existingTable != null;

        if (editing) {
            tableNumberField.setText(
                    existingTable.getTableNumber()
            );

            capacitySpinner.setValue(
                    existingTable.getCapacity()
            );

            statusComboBox.setSelectedItem(
                    existingTable.getStatus()
            );
        } else {
            statusComboBox.setSelectedItem(
                    TableStatus.AVAILABLE
            );

            statusComboBox.setEnabled(false);
        }

        JPanel formPanel =
                new JPanel(new GridLayout(
                        0,
                        2,
                        10,
                        10
                ));

        formPanel.add(new JLabel("Số bàn:"));
        formPanel.add(tableNumberField);

        formPanel.add(new JLabel("Sức chứa:"));
        formPanel.add(capacitySpinner);

        formPanel.add(new JLabel("Trạng thái:"));
        formPanel.add(statusComboBox);

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        formPanel,
                        editing
                                ? "Sửa thông tin bàn"
                                : "Thêm bàn mới",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String tableNumber =
                    tableNumberField
                            .getText()
                            .trim();

            int capacity =
                    (Integer) capacitySpinner
                            .getValue();

            if (editing) {
                TableStatus status =
                        (TableStatus)
                                statusComboBox
                                        .getSelectedItem();

                tableService.updateTable(
                        existingTable.getId(),
                        tableNumber,
                        capacity,
                        status
                );

                showSuccess(
                        "Cập nhật bàn thành công."
                );

            } else {
                tableService.addTable(
                        tableNumber,
                        capacity
                );

                showSuccess(
                        "Thêm bàn thành công."
                );
            }

            loadTables();

        } catch (
                IllegalArgumentException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void deleteSelectedTable() {
        RestaurantTable selected =
                getSelectedTable();

        if (selected == null) {
            return;
        }

        if (selected.getStatus()
                != TableStatus.AVAILABLE) {

            showError(
                    "Chỉ có thể xóa bàn đang trống."
            );
            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Bạn có chắc muốn xóa bàn "
                                + selected.getTableNumber()
                                + " không?",
                        "Xác nhận xóa",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            tableService.deleteTable(
                    selected.getId()
            );

            showSuccess("Xóa bàn thành công.");
            loadTables();

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