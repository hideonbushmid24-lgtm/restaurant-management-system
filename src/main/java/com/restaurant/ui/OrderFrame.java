package com.restaurant.ui;

import com.restaurant.model.MenuItem;
import com.restaurant.model.OrderItem;
import com.restaurant.model.RestaurantOrder;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;
import com.restaurant.model.User;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import com.restaurant.service.TableService;
import com.restaurant.model.KitchenStatus;
import com.restaurant.model.OrderStatus;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderFrame extends JFrame {

    private final OrderService orderService;
    private final MenuService menuService;
    private final TableService tableService;
    private final User currentUser;

    private final JComboBox<RestaurantTable> tableComboBox =
            new JComboBox<>();

    private final JComboBox<MenuItem> menuItemComboBox =
            new JComboBox<>();

    private final JTextField orderNoteField =
            new JTextField(20);

    private final JTextField itemNoteField =
            new JTextField(18);

    private final JSpinner quantitySpinner =
            new JSpinner(
                    new SpinnerNumberModel(
                            1,
                            1,
                            100,
                            1
                    )
            );

    private final JLabel orderLabel =
            new JLabel("Chưa chọn đơn");

    private final JLabel totalLabel =
            new JLabel("Tổng tiền: 0 ₫");

    private final JButton createOrderButton =
            new JButton("Tạo hoặc mở đơn");
    private final JButton increaseButton =
        new JButton("Tăng số lượng");

    private final JButton decreaseButton =
        new JButton("Giảm số lượng");

    private final JButton cancelItemButton =
        new JButton("Hủy món");

        private final JButton sendKitchenButton =
        new JButton("Gửi xuống bếp");

        private final JButton servedButton = new JButton("Xác nhận đã phục vụ");

    private List<OrderItem> currentOrderItems =
        List.of();

    private final JButton addItemButton =
            new JButton("Thêm món");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Tên món",
                            "Số lượng",
                            "Đơn giá",
                            "Thành tiền",
                            "Ghi chú",
                            "Trạng thái bếp"
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

                @Override
                public Class<?> getColumnClass(
                        int columnIndex
                ) {
                    if (columnIndex == 2) {
                        return Integer.class;
                    }

                    if (columnIndex == 3
                            || columnIndex == 4) {

                        return BigDecimal.class;
                    }

                    return Object.class;
                }
            };

    private final JTable orderItemTable =
            new JTable(tableModel);

    private RestaurantOrder currentOrder;

    public OrderFrame(
            OrderService orderService,
            MenuService menuService,
            TableService tableService,
            User currentUser
    ) {
        if (orderService == null
                || menuService == null
                || tableService == null
                || currentUser == null) {

            throw new IllegalArgumentException(
                    "Thông tin khởi tạo giao diện gọi món không hợp lệ"
            );
        }

        this.orderService = orderService;
        this.menuService = menuService;
        this.tableService = tableService;
        this.currentUser = currentUser;

        initializeFrame();
        createLayout();
        configureComboBoxes();
        configureTable();
        registerEvents();

        loadTables();
        loadMenuItems();

        addItemButton.setEnabled(false);
    }

    private void initializeFrame() {
        setTitle("Tạo đơn gọi món");
        setSize(1150, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
    }

    private void createLayout() {
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel =
                new JLabel(
                        "TẠO ĐƠN GỌI MÓN",
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
                        10,
                        10
                )
        );

        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel =
                new JPanel(new BorderLayout(10, 10));

        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        15,
                        5,
                        15
                )
        );

        centerPanel.add(
                createOrderPanel(),
                BorderLayout.NORTH
        );

        centerPanel.add(
                new JScrollPane(orderItemTable),
                BorderLayout.CENTER
        );

        centerPanel.add(
                createItemPanel(),
                BorderLayout.SOUTH
        );

        add(centerPanel, BorderLayout.CENTER);

        add(
                createBottomPanel(),
                BorderLayout.SOUTH
        );
    }

    private JPanel createOrderPanel() {
        JPanel container =
                new JPanel(new GridLayout(2, 1));

        JPanel inputPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                5
                        )
                );

        inputPanel.add(new JLabel("Chọn bàn:"));
        inputPanel.add(tableComboBox);

        inputPanel.add(new JLabel("Ghi chú đơn:"));
        inputPanel.add(orderNoteField);

        inputPanel.add(createOrderButton);

        JPanel informationPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        orderLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );

        informationPanel.add(orderLabel);

        container.add(inputPanel);
        container.add(informationPanel);

        return container;
    }

    private JPanel createItemPanel() {
        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                10
                        )
                );

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        "Thêm món vào đơn"
                )
        );

        panel.add(new JLabel("Món ăn:"));
        panel.add(menuItemComboBox);

        panel.add(new JLabel("Số lượng:"));
        panel.add(quantitySpinner);

        panel.add(new JLabel("Ghi chú:"));
        panel.add(itemNoteField);

        panel.add(addItemButton);
        panel.add(increaseButton);
        panel.add(decreaseButton);
        panel.add(cancelItemButton);
        panel.add(sendKitchenButton);
        
        return panel;
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

        totalLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        17
                )
        );

        JButton selectAnotherTableButton =
                new JButton("Chọn bàn khác");

        JButton viewMenuButton =
                new JButton("Xem thực đơn");

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
        
        buttonPanel.add(servedButton);
        buttonPanel.add(selectAnotherTableButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        selectAnotherTableButton.addActionListener(
                event -> selectAnotherTable()
        );

        refreshButton.addActionListener(
                event -> refreshCurrentOrder()
        );

        closeButton.addActionListener(
                event -> dispose()
        );
        viewMenuButton.addActionListener(
            event -> openMenuFrame()
        );

        panel.add(totalLabel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void configureComboBoxes() {
        tableComboBox.setRenderer(
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
                                    "Bàn "
                                            + table.getTableNumber()
                                            + " — "
                                            + table.getStatus()
                            );
                        }

                        return this;
                    }
                }
        );

        menuItemComboBox.setRenderer(
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

                        if (value instanceof MenuItem menuItem) {
                            setText(
                                    menuItem.getItemName()
                                            + " — "
                                            + formatCurrency(
                                                    menuItem.getPrice()
                                            )
                            );
                        }

                        return this;
                    }
                }
        );
    }

    private void configureTable() {
        orderItemTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        orderItemTable.setRowHeight(28);
        orderItemTable.setAutoCreateRowSorter(false);

        orderItemTable.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(45);

        orderItemTable.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(180);

        orderItemTable.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(70);

        orderItemTable.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(100);

        orderItemTable.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(110);

        orderItemTable.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(220);

        orderItemTable.getColumnModel()
                .getColumn(6)
                .setPreferredWidth(130);

        DefaultTableCellRenderer currencyRenderer =
                new DefaultTableCellRenderer() {
                    @Override
                    protected void setValue(Object value) {
                        if (value
                                instanceof BigDecimal amount) {

                            setText(
                                    formatCurrency(amount)
                            );
                        } else {
                            setText("");
                        }
                    }
                };

        currencyRenderer.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        orderItemTable.getColumnModel()
                .getColumn(3)
                .setCellRenderer(currencyRenderer);

        orderItemTable.getColumnModel()
                .getColumn(4)
                .setCellRenderer(currencyRenderer);
    }

    private void registerEvents() {
    createOrderButton.addActionListener(
            event -> createOrLoadOrder()
    );

    addItemButton.addActionListener(
            event -> addSelectedItem()
    );

    increaseButton.addActionListener(
            event -> changeSelectedItemQuantity(1)
    );

    decreaseButton.addActionListener(
            event -> changeSelectedItemQuantity(-1)
    );

    cancelItemButton.addActionListener(
            event -> cancelSelectedItem()
    );

    orderItemTable.getSelectionModel()
            .addListSelectionListener(
                    event -> {
                        if (!event.getValueIsAdjusting()) {
                            updateEditButtons();
                        }
                    }
            );
        sendKitchenButton.addActionListener(
                event -> sendCurrentOrderToKitchen()
        );

        servedButton.addActionListener(event -> markSelectedItemServed());
}

    private void loadTables() {
        try {
            List<RestaurantTable> tables =
                    tableService.search(
                            null,
                            null
                    );

            tableComboBox.removeAllItems();

            for (RestaurantTable table : tables) {
                if (table.getStatus()
                        != TableStatus.INACTIVE) {

                    tableComboBox.addItem(table);
                }
            }

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh sách bàn.\n"
                            + exception.getMessage()
            );
        }
    }

    private void loadMenuItems() {
        try {
            List<MenuItem> menuItems =
                    menuService.searchAvailableItems(
                            null,
                            null
                    );

            menuItemComboBox.removeAllItems();

            for (MenuItem menuItem : menuItems) {
                menuItemComboBox.addItem(menuItem);
            }

        } catch (SQLException exception) {
            showError(
                    "Không thể tải thực đơn.\n"
                            + exception.getMessage()
            );
        }
    }

    private void createOrLoadOrder() {
    RestaurantTable selectedTable =
            (RestaurantTable)
                    tableComboBox.getSelectedItem();

    if (selectedTable == null) {
        showError("Vui lòng chọn bàn");
        return;
    }

    try {
        RestaurantOrder activeOrder =
                orderService
                        .getActiveOrderByTable(
                                selectedTable.getId()
                        );

        if (activeOrder == null) {
            Long reservationId = null;

            if (selectedTable.getStatus()
                    == TableStatus.AVAILABLE) {

                reservationId = null;

            } else if (selectedTable.getStatus()
                    == TableStatus.OCCUPIED) {

                reservationId =
                        orderService
                                .getSeatedReservationIdByTable(
                                        selectedTable.getId()
                                );

                if (reservationId == null) {
                    showError(
                            "Bàn đang có khách nhưng không tìm thấy "
                                    + "thông tin đặt bàn đã nhận bàn"
                    );
                    return;
                }

            } else if (selectedTable.getStatus()
                    == TableStatus.RESERVED) {

                showError(
                        "Khách chưa nhận bàn. "
                                + "Vui lòng thực hiện nhận bàn trước khi gọi món."
                );
                return;

            } else {
                showError(
                        "Không thể tạo đơn cho bàn này"
                );
                return;
            }

            orderService.createOrder(
                    selectedTable.getId(),
                    reservationId,
                    currentUser.getId(),
                    orderNoteField.getText()
            );

            activeOrder =
                    orderService
                            .getActiveOrderByTable(
                                    selectedTable.getId()
                            );
        }

        if (activeOrder == null) {
            showError(
                    "Không thể tải đơn vừa tạo"
            );
            return;
        }

        currentOrder = activeOrder;

        tableComboBox.setEnabled(false);
        orderNoteField.setEnabled(false);
        createOrderButton.setEnabled(false);
        updateOrderActionButtons();

        updateOrderInformation();
        loadOrderItems();

    } catch (
            IllegalArgumentException
            | IllegalStateException
            | SQLException exception
    ) {
        showError(exception.getMessage());
    }
}

    private void addSelectedItem() {
        if (currentOrder == null) {
            showError(
                    "Vui lòng tạo hoặc mở đơn trước"
            );
            return;
        }

        MenuItem selectedMenuItem =
                (MenuItem)
                        menuItemComboBox.getSelectedItem();

        if (selectedMenuItem == null) {
            showError("Vui lòng chọn món ăn");
            return;
        }

        int quantity =
                (Integer) quantitySpinner.getValue();

        try {
            orderService.addItem(
                    currentOrder.getId(),
                    selectedMenuItem.getId(),
                    quantity,
                    itemNoteField.getText()
            );

            quantitySpinner.setValue(1);
            itemNoteField.setText("");

            loadOrderItems();

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void loadOrderItems() {
        if (currentOrder == null) {
            tableModel.setRowCount(0);
            totalLabel.setText("Tổng tiền: 0 ₫");
            return;
        }

        try {
            currentOrderItems =
                orderService.getOrderItems(
                    currentOrder.getId()
            );

            tableModel.setRowCount(0);

            for (OrderItem orderItem : currentOrderItems) {
                tableModel.addRow(new Object[]{
                        orderItem.getId(),
                        orderItem.getItemName(),
                        orderItem.getQuantity(),
                        orderItem.getUnitPrice(),
                        orderItem.getSubtotal(),
                        orderItem.getNote(),
                        orderItem.getKitchenStatus()
                });
            }

            BigDecimal total =
                    orderService.getOrderTotal(
                            currentOrder.getId()
                    );

            totalLabel.setText(
                    "Tổng tiền: "
                            + formatCurrency(total)
            );

        } catch (SQLException exception) {
            showError(
                    "Không thể tải chi tiết đơn.\n"
                            + exception.getMessage()
            );
        }
    }

    private void updateOrderInformation() {
        if (currentOrder == null) {
            orderLabel.setText("Chưa chọn đơn");
            return;
        }

        orderLabel.setText(
                "Đơn số: "
                        + currentOrder.getId()
                        + " | Bàn: "
                        + currentOrder.getTableNumber()
                        + " | Nhân viên: "
                        + currentOrder.getWaiterName()
                        + " | Trạng thái: "
                        + currentOrder.getStatus()
        );
    }

    private void refreshCurrentOrder() {
        loadMenuItems();
        if (currentOrder == null) {
        currentOrderItems = List.of();
        tableModel.setRowCount(0);
        totalLabel.setText("Tổng tiền: 0 ₫");
        updateEditButtons();
        return;
    }

        if (currentOrder != null) {
            try {
                RestaurantOrder refreshedOrder =
                        orderService
                                .getActiveOrderByTable(
                                        currentOrder.getTableId()
                                );

                if (refreshedOrder == null) {
                    selectAnotherTable();
                    return;
                }

                currentOrder = refreshedOrder;
                updateOrderInformation();
                loadOrderItems();
                updateOrderActionButtons();

            } catch (SQLException exception) {
                showError(exception.getMessage());
            }
        } else {
            loadTables();
        }
        orderItemTable.clearSelection();
        updateEditButtons();
    }

    private void selectAnotherTable() {
        currentOrder = null;
        currentOrderItems = List.of();
        tableComboBox.setEnabled(true);
        orderNoteField.setEnabled(true);
        createOrderButton.setEnabled(true);
        addItemButton.setEnabled(false);
        sendKitchenButton.setEnabled(false);
        servedButton.setEnabled(false);
        increaseButton.setEnabled(false);
        decreaseButton.setEnabled(false);
        cancelItemButton.setEnabled(false);
        sendKitchenButton.setEnabled(false);

        orderNoteField.setText("");
        itemNoteField.setText("");
        quantitySpinner.setValue(1);

        orderLabel.setText("Chưa chọn đơn");
        totalLabel.setText("Tổng tiền: 0 ₫");
        tableModel.setRowCount(0);
        updateEditButtons();

        loadTables();
    }

    private OrderItem getSelectedOrderItem() {
    int selectedRow =
            orderItemTable.getSelectedRow();

    if (selectedRow < 0) {
        return null;
    }

    int modelRow =
            orderItemTable.convertRowIndexToModel(
                    selectedRow
            );

    if (modelRow < 0
            || modelRow
            >= currentOrderItems.size()) {

        return null;
    }

    return currentOrderItems.get(modelRow);
}

private void updateEditButtons() {
    OrderItem selectedItem = getSelectedOrderItem();

    boolean editable = currentOrder != null
            && currentOrder.getStatus() == OrderStatus.OPEN
            && selectedItem != null
            && selectedItem.getKitchenStatus() == KitchenStatus.PENDING;

    increaseButton.setEnabled(editable);
    decreaseButton.setEnabled(editable);
    cancelItemButton.setEnabled(editable);

    boolean canServe = currentOrder != null
            && selectedItem != null
            && selectedItem.getKitchenStatus() == KitchenStatus.READY
            && (currentOrder.getStatus() == OrderStatus.IN_KITCHEN
                || currentOrder.getStatus() == OrderStatus.READY);

    servedButton.setEnabled(canServe);
}

private void updateOrderActionButtons() {
    boolean orderOpen =
            currentOrder != null
                    && currentOrder.getStatus()
                    == OrderStatus.OPEN;

    addItemButton.setEnabled(orderOpen);
    sendKitchenButton.setEnabled(orderOpen);

    updateEditButtons();
}

private void changeSelectedItemQuantity(
        int amount
) {
    OrderItem selectedItem =
            getSelectedOrderItem();

    if (selectedItem == null) {
        showError(
                "Vui lòng chọn món cần chỉnh sửa"
        );
        return;
    }

    int newQuantity =
            selectedItem.getQuantity() + amount;

    if (newQuantity <= 0) {
        showError(
                "Số lượng không được nhỏ hơn 1. "
                        + "Nếu không cần món này, hãy chọn Hủy món."
        );
        return;
    }

    try {
        orderService.updateItemQuantity(
                selectedItem.getId(),
                newQuantity
        );

        loadOrderItems();

    } catch (
            IllegalArgumentException
            | IllegalStateException
            | SQLException exception
    ) {
        showError(exception.getMessage());
    }
}

private void cancelSelectedItem() {
    OrderItem selectedItem =
            getSelectedOrderItem();

    if (selectedItem == null) {
        showError(
                "Vui lòng chọn món cần hủy"
        );
        return;
    }

    int result =
            JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn hủy món “"
                            + selectedItem.getItemName()
                            + "” không?",
                    "Xác nhận hủy món",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

    if (result != JOptionPane.YES_OPTION) {
        return;
    }

    try {
        orderService.cancelItem(
                selectedItem.getId()
        );

        loadOrderItems();

    } catch (
            IllegalArgumentException
            | IllegalStateException
            | SQLException exception
    ) {
        showError(exception.getMessage());
    }
}

private void openMenuFrame() {
    MenuFrame menuFrame =
            new MenuFrame(menuService);

    menuFrame.setLocationRelativeTo(this);
    menuFrame.setVisible(true);
}
private void sendCurrentOrderToKitchen() {
    if (currentOrder == null) {
        showError(
                "Vui lòng tạo hoặc mở đơn trước"
        );
        return;
    }

    int result =
            JOptionPane.showConfirmDialog(
                    this,
                    "Sau khi gửi xuống bếp, "
                            + "bạn không thể chỉnh sửa các món trong đơn.\n"
                            + "Bạn có muốn tiếp tục không?",
                    "Xác nhận gửi xuống bếp",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

    if (result != JOptionPane.YES_OPTION) {
        return;
    }

    try {
        orderService.sendToKitchen(
                currentOrder.getId()
        );

        RestaurantOrder refreshedOrder =
                orderService
                        .getActiveOrderByTable(
                                currentOrder.getTableId()
                        );

        if (refreshedOrder == null) {
            throw new IllegalStateException(
                    "Không thể tải lại đơn sau khi gửi"
            );
        }

        currentOrder = refreshedOrder;

        updateOrderInformation();
        loadOrderItems();
        updateOrderActionButtons();

        JOptionPane.showMessageDialog(
                this,
                "Đã gửi đơn xuống bếp thành công.",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE
        );

    } catch (
            IllegalArgumentException
            | IllegalStateException
            | SQLException exception
    ) {
        showError(exception.getMessage());
    }
}

private void markSelectedItemServed() {
    OrderItem selectedItem = getSelectedOrderItem();

    if (selectedItem == null) {
        JOptionPane.showMessageDialog(
                this,
                "Vui lòng chọn món cần xác nhận đã phục vụ.",
                "Chưa chọn món",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    int choice = JOptionPane.showConfirmDialog(
            this,
            "Xác nhận món đã chọn đã được phục vụ?",
            "Xác nhận phục vụ",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
    );

    if (choice != JOptionPane.YES_OPTION) {
        return;
    }

    try {
        orderService.markItemServed(selectedItem.getId());
        refreshCurrentOrder();

        JOptionPane.showMessageDialog(
                this,
                "Đã xác nhận món được phục vụ.",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE
        );
    } catch (IllegalArgumentException
             | IllegalStateException
             | SQLException exception) {
        JOptionPane.showMessageDialog(
                this,
                exception.getMessage(),
                "Không thể xác nhận phục vụ",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

    private String formatCurrency(
            BigDecimal amount
    ) {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("vi-VN")
                );

        return formatter.format(amount);
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