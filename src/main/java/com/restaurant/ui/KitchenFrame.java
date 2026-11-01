package com.restaurant.ui;

import com.restaurant.model.KitchenItem;
import com.restaurant.model.KitchenStatus;
import com.restaurant.service.KitchenService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KitchenFrame extends JFrame {

    private final KitchenService kitchenService;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Đơn",
                            "Bàn",
                            "Tên món",
                            "Số lượng",
                            "Ghi chú",
                            "Trạng thái",
                            "Thời gian gọi"
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

    private final JTable kitchenTable =
            new JTable(tableModel);

    private final JButton preparingButton =
            new JButton("Bắt đầu chế biến");

    private final JButton readyButton =
            new JButton("Hoàn thành món");

    private final JLabel summaryLabel =
            new JLabel(
                    "Chờ: 0 | Đang chế biến: 0 | Hoàn thành: 0"
            );

    private List<KitchenItem> currentItems =
            List.of();

    public KitchenFrame(
            KitchenService kitchenService
    ) {
        if (kitchenService == null) {
            throw new IllegalArgumentException(
                    "KitchenService không được null"
            );
        }

        this.kitchenService = kitchenService;

        initializeFrame();
        createLayout();
        configureTable();
        registerEvents();

        preparingButton.setEnabled(false);
        readyButton.setEnabled(false);

        loadKitchenItems();
    }

    private void initializeFrame() {
        setTitle("Xử lý món tại bếp");
        setSize(1100, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
    }

    private void createLayout() {
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel =
                new JLabel(
                        "DANH SÁCH MÓN TẠI BẾP",
                        SwingConstants.CENTER
                );

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        titleLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        10,
                        5,
                        10
                )
        );

        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel =
                new JPanel(new BorderLayout(10, 10));

        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        5,
                        15
                )
        );

        centerPanel.add(
                new JScrollPane(kitchenTable),
                BorderLayout.CENTER
        );

        add(centerPanel, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createBottomPanel() {
        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        15,
                        15
                )
        );

        summaryLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );

        JButton refreshButton =
                new JButton("Làm mới");

        JButton closeButton =
                new JButton("Đóng");

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttonPanel.add(preparingButton);
        buttonPanel.add(readyButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        refreshButton.addActionListener(
                event -> loadKitchenItems()
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        panel.add(summaryLabel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void configureTable() {
        kitchenTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        kitchenTable.setRowHeight(28);
        kitchenTable.setAutoCreateRowSorter(false);

        kitchenTable.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(45);

        kitchenTable.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(55);

        kitchenTable.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(60);

        kitchenTable.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(190);

        kitchenTable.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(70);

        kitchenTable.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(250);

        kitchenTable.getColumnModel()
                .getColumn(6)
                .setPreferredWidth(130);

        kitchenTable.getColumnModel()
                .getColumn(7)
                .setPreferredWidth(130);

        DefaultTableCellRenderer centerRenderer =
                new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        kitchenTable.getColumnModel()
                .getColumn(0)
                .setCellRenderer(centerRenderer);

        kitchenTable.getColumnModel()
                .getColumn(1)
                .setCellRenderer(centerRenderer);

        kitchenTable.getColumnModel()
                .getColumn(2)
                .setCellRenderer(centerRenderer);

        kitchenTable.getColumnModel()
                .getColumn(4)
                .setCellRenderer(centerRenderer);

        kitchenTable.getColumnModel()
                .getColumn(6)
                .setCellRenderer(centerRenderer);

        kitchenTable.getColumnModel()
                .getColumn(7)
                .setCellRenderer(centerRenderer);
    }

    private void registerEvents() {
        preparingButton.addActionListener(
                event -> startPreparing()
        );

        readyButton.addActionListener(
                event -> markSelectedItemReady()
        );

        kitchenTable.getSelectionModel()
                .addListSelectionListener(
                        event -> {
                            if (!event.getValueIsAdjusting()) {
                                updateActionButtons();
                            }
                        }
                );
    }

    private void loadKitchenItems() {
        try {
            currentItems =
                    kitchenService.getActiveItems();

            tableModel.setRowCount(0);

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm"
                    );

            for (KitchenItem item : currentItems) {
                tableModel.addRow(new Object[]{
                        item.getId(),
                        item.getOrderId(),
                        item.getTableNumber(),
                        item.getItemName(),
                        item.getQuantity(),
                        item.getNote(),
                        item.getStatus(),
                        item.getCreatedAt()
                                .format(formatter)
                });
            }

            updateSummary();

            kitchenTable.clearSelection();
            updateActionButtons();

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh sách món tại bếp.\n"
                            + exception.getMessage()
            );
        }
    }

    private KitchenItem getSelectedItem() {
        int selectedRow =
                kitchenTable.getSelectedRow();

        if (selectedRow < 0) {
            return null;
        }

        int modelRow =
                kitchenTable.convertRowIndexToModel(
                        selectedRow
                );

        if (modelRow < 0
                || modelRow >= currentItems.size()) {

            return null;
        }

        return currentItems.get(modelRow);
    }

    private void updateActionButtons() {
        KitchenItem selectedItem =
                getSelectedItem();

        preparingButton.setEnabled(
                selectedItem != null
                        && selectedItem.getStatus()
                        == KitchenStatus.PENDING
        );

        readyButton.setEnabled(
                selectedItem != null
                        && selectedItem.getStatus()
                        == KitchenStatus.PREPARING
        );
    }

    private void startPreparing() {
        KitchenItem selectedItem =
                getSelectedItem();

        if (selectedItem == null) {
            showError(
                    "Vui lòng chọn món cần chế biến"
            );
            return;
        }

        try {
            kitchenService.startPreparing(
                    selectedItem.getId()
            );

            loadKitchenItems();

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void markSelectedItemReady() {
        KitchenItem selectedItem =
                getSelectedItem();

        if (selectedItem == null) {
            showError(
                    "Vui lòng chọn món đã hoàn thành"
            );
            return;
        }

        try {
            kitchenService.markReady(
                    selectedItem.getId()
            );

            loadKitchenItems();

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void updateSummary() {
        long pendingCount =
                currentItems.stream()
                        .filter(
                                item ->
                                        item.getStatus()
                                                == KitchenStatus.PENDING
                        )
                        .count();

        long preparingCount =
                currentItems.stream()
                        .filter(
                                item ->
                                        item.getStatus()
                                                == KitchenStatus.PREPARING
                        )
                        .count();

        long readyCount =
                currentItems.stream()
                        .filter(
                                item ->
                                        item.getStatus()
                                                == KitchenStatus.READY
                        )
                        .count();

        summaryLabel.setText(
                "Chờ: "
                        + pendingCount
                        + " | Đang chế biến: "
                        + preparingCount
                        + " | Hoàn thành: "
                        + readyCount
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