package com.restaurant.ui;

import com.restaurant.model.PayableOrder;
import com.restaurant.model.PaymentMethod;
import com.restaurant.model.PaymentReceipt;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import com.restaurant.service.PaymentService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PaymentFrame extends JFrame {

    private final PaymentService paymentService;
    private final User currentUser;

    private List<PayableOrder> payableOrders =
            List.of();

    private PaymentReceipt lastReceipt;

    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("vi-VN")
            );

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "Mã đơn",
                            "Bàn",
                            "Nhân viên phục vụ",
                            "Thời gian tạo",
                            "Tạm tính"
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

    private final JTable orderTable =
            new JTable(tableModel);

    private final JTextField discountField =
            new JTextField("0", 12);

    private final JComboBox<PaymentMethod>
            paymentMethodComboBox =
            new JComboBox<>(
                    PaymentMethod.values()
            );

    private final JLabel orderCountLabel =
            new JLabel("Đơn chờ thanh toán: 0");

    private final JLabel subtotalLabel =
            new JLabel("Tạm tính: 0 ₫");

    private final JLabel totalLabel =
            new JLabel("Thành tiền: 0 ₫");

    private final JButton payButton =
            new JButton("Thanh toán");

    private final JButton printButton =
            new JButton("Xem/In hóa đơn vừa tạo");

    private final JButton refreshButton =
            new JButton("Làm mới");

    private final JButton closeButton =
            new JButton("Đóng");

    public PaymentFrame(
            PaymentService paymentService,
            User currentUser
    ) {
        if (paymentService == null) {
            throw new IllegalArgumentException(
                    "PaymentService không được để trống"
            );
        }

        if (currentUser == null) {
            throw new IllegalArgumentException(
                    "Người dùng hiện tại không được để trống"
            );
        }

        if (currentUser.getRole() != Role.MANAGER
                && currentUser.getRole() != Role.CASHIER) {
            throw new IllegalArgumentException(
                    "Người dùng không có quyền thanh toán"
            );
        }

        this.paymentService = paymentService;
        this.currentUser = currentUser;

        initializeFrame();
        createLayout();
        registerEvents();

        payButton.setEnabled(false);
        printButton.setEnabled(false);

        loadPayableOrders();
    }

    private void initializeFrame() {
        setTitle("Thanh toán và hóa đơn");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
    }

    private void createLayout() {
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel =
                new JLabel(
                        "THANH TOÁN VÀ HÓA ĐƠN",
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

        JPanel topPanel =
                new JPanel(new BorderLayout());

        topPanel.add(
                titleLabel,
                BorderLayout.CENTER
        );

        orderCountLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        5,
                        15
                )
        );

        topPanel.add(
                orderCountLabel,
                BorderLayout.SOUTH
        );

        orderTable.setSelectionMode(
                javax.swing.ListSelectionModel
                        .SINGLE_SELECTION
        );

        orderTable.setRowHeight(25);
        orderTable.getTableHeader()
                .setReorderingAllowed(false);

        JScrollPane scrollPane =
                new JScrollPane(orderTable);

        scrollPane.setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        15,
                        0,
                        15
                )
        );

        JPanel paymentPanel =
                createPaymentPanel();

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(paymentPanel, BorderLayout.SOUTH);
    }

    private JPanel createPaymentPanel() {
        JPanel container =
                new JPanel(new BorderLayout(10, 10));

        container.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        15,
                        15,
                        15
                )
        );

        JPanel informationPanel =
                new JPanel(new GridLayout(
                        2,
                        4,
                        10,
                        8
                ));

        informationPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Thông tin thanh toán"
                )
        );

        informationPanel.add(
                new JLabel("Giảm giá (VNĐ):")
        );
        informationPanel.add(discountField);

        informationPanel.add(
                new JLabel("Phương thức:")
        );
        informationPanel.add(
                paymentMethodComboBox
        );

        subtotalLabel.setFont(
                subtotalLabel.getFont()
                        .deriveFont(Font.BOLD)
        );

        totalLabel.setFont(
                totalLabel.getFont()
                        .deriveFont(Font.BOLD)
        );

        informationPanel.add(subtotalLabel);
        informationPanel.add(totalLabel);
        informationPanel.add(new JLabel(""));
        informationPanel.add(new JLabel(""));

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttonPanel.add(payButton);
        buttonPanel.add(printButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        container.add(
                informationPanel,
                BorderLayout.CENTER
        );

        container.add(
                buttonPanel,
                BorderLayout.SOUTH
        );

        return container;
    }

    private void registerEvents() {
        orderTable.getSelectionModel()
                .addListSelectionListener(
                        event -> {
                            if (!event.getValueIsAdjusting()) {
                                updatePaymentInformation();
                            }
                        }
                );

        discountField.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            @Override
                            public void insertUpdate(
                                    DocumentEvent event
                            ) {
                                updatePaymentInformation();
                            }

                            @Override
                            public void removeUpdate(
                                    DocumentEvent event
                            ) {
                                updatePaymentInformation();
                            }

                            @Override
                            public void changedUpdate(
                                    DocumentEvent event
                            ) {
                                updatePaymentInformation();
                            }
                        }
                );

        payButton.addActionListener(
                event -> performPayment()
        );

        printButton.addActionListener(
                event -> showAndPrintLastReceipt()
        );

        refreshButton.addActionListener(
                event -> loadPayableOrders()
        );

        closeButton.addActionListener(
                event -> dispose()
        );
    }

    private void loadPayableOrders() {
        try {
            payableOrders =
                    paymentService
                            .getPayableOrders();

            tableModel.setRowCount(0);

            for (PayableOrder order : payableOrders) {
                tableModel.addRow(
                        new Object[]{
                                order.getOrderId(),
                                order.getTableNumber(),
                                order.getWaiterName(),
                                order.getCreatedAt()
                                        .format(
                                                dateTimeFormatter
                                        ),
                                formatCurrency(
                                        order.getSubtotal()
                                )
                        }
                );
            }

            orderCountLabel.setText(
                    "Đơn chờ thanh toán: "
                            + payableOrders.size()
            );

            orderTable.clearSelection();
            discountField.setText("0");

            subtotalLabel.setText(
                    "Tạm tính: 0 ₫"
            );

            totalLabel.setText(
                    "Thành tiền: 0 ₫"
            );

            payButton.setEnabled(false);

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh sách đơn chờ thanh toán.\n"
                            + exception.getMessage()
            );
        }
    }

    private PayableOrder getSelectedOrder() {
        int selectedRow =
                orderTable.getSelectedRow();

        if (selectedRow < 0
                || selectedRow >= payableOrders.size()) {
            return null;
        }

        return payableOrders.get(selectedRow);
    }

    private void updatePaymentInformation() {
        PayableOrder selectedOrder =
                getSelectedOrder();

        if (selectedOrder == null) {
            subtotalLabel.setText(
                    "Tạm tính: 0 ₫"
            );

            totalLabel.setText(
                    "Thành tiền: 0 ₫"
            );

            payButton.setEnabled(false);
            return;
        }

        BigDecimal subtotal =
                selectedOrder.getSubtotal();

        subtotalLabel.setText(
                "Tạm tính: "
                        + formatCurrency(subtotal)
        );

        try {
            BigDecimal discount =
                    parseDiscount();

            BigDecimal total =
                    subtotal.subtract(discount);

            if (discount.compareTo(BigDecimal.ZERO) < 0
                    || total.compareTo(BigDecimal.ZERO) < 0) {
                totalLabel.setText(
                        "Thành tiền: Không hợp lệ"
                );

                payButton.setEnabled(false);
                return;
            }

            totalLabel.setText(
                    "Thành tiền: "
                            + formatCurrency(total)
            );

            payButton.setEnabled(true);

        } catch (IllegalArgumentException exception) {
            totalLabel.setText(
                    "Thành tiền: Không hợp lệ"
            );

            payButton.setEnabled(false);
        }
    }

    private BigDecimal parseDiscount() {
        String text =
                discountField.getText();

        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        String normalized =
                text.trim()
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", "");

        if (!normalized.matches("-?\\d+")) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá phải là số"
            );
        }

        return new BigDecimal(normalized);
    }

    private void performPayment() {
        PayableOrder selectedOrder =
                getSelectedOrder();

        if (selectedOrder == null) {
            showError(
                    "Vui lòng chọn đơn cần thanh toán"
            );
            return;
        }

        try {
            BigDecimal discount =
                    parseDiscount();

            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Tiền giảm giá không được âm"
                );
            }

            if (discount.compareTo(
                    selectedOrder.getSubtotal()
            ) > 0) {
                throw new IllegalArgumentException(
                        "Tiền giảm giá không được lớn hơn tạm tính"
                );
            }

            PaymentMethod paymentMethod =
                    (PaymentMethod)
                            paymentMethodComboBox
                                    .getSelectedItem();

            BigDecimal total =
                    selectedOrder
                            .getSubtotal()
                            .subtract(discount);

            int confirmation =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Xác nhận thanh toán đơn #"
                                    + selectedOrder.getOrderId()
                                    + " của bàn "
                                    + selectedOrder.getTableNumber()
                                    + "?\nThành tiền: "
                                    + formatCurrency(total),
                            "Xác nhận thanh toán",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (confirmation
                    != JOptionPane.YES_OPTION) {
                return;
            }

            lastReceipt =
                    paymentService.pay(
                            selectedOrder.getOrderId(),
                            currentUser.getId(),
                            discount,
                            paymentMethod
                    );

            printButton.setEnabled(true);

            showReceipt(lastReceipt);
            loadPayableOrders();

        } catch (
                IllegalArgumentException
                | IllegalStateException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private void showReceipt(
            PaymentReceipt receipt
    ) {
        JTextArea receiptArea =
                createReceiptArea(receipt);

        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(receiptArea),
                "Thanh toán thành công",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showAndPrintLastReceipt() {
        if (lastReceipt == null) {
            showError(
                    "Chưa có hóa đơn vừa tạo"
            );
            return;
        }

        JTextArea receiptArea =
                createReceiptArea(lastReceipt);

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        new JScrollPane(receiptArea),
                        "Xem hóa đơn",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            boolean printed =
                    receiptArea.print();

            if (!printed) {
                JOptionPane.showMessageDialog(
                        this,
                        "Đã hủy thao tác in.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (PrinterException exception) {
            showError(
                    "Không thể in hóa đơn.\n"
                            + exception.getMessage()
            );
        }
    }

    private JTextArea createReceiptArea(
            PaymentReceipt receipt
    ) {
        JTextArea receiptArea =
                new JTextArea(
                        buildReceiptText(receipt),
                        15,
                        38
                );

        receiptArea.setEditable(false);
        receiptArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14
                )
        );

        receiptArea.setCaretPosition(0);

        return receiptArea;
    }

    private String buildReceiptText(
            PaymentReceipt receipt
    ) {
        return """
                NHÀ HÀNG
                ==============================
                HÓA ĐƠN THANH TOÁN

                Mã hóa đơn: %s
                Mã đơn:     %d
                Bàn:        %s
                Thu ngân:   %s
                Thời gian:  %s
                Phương thức:%s

                ------------------------------
                Tạm tính:   %s
                Giảm giá:   %s
                Thành tiền: %s
                ==============================
                Cảm ơn quý khách!
                """.formatted(
                receipt.getInvoiceCode(),
                receipt.getOrderId(),
                receipt.getTableNumber(),
                receipt.getCashierName(),
                receipt.getPaidAt()
                        .format(dateTimeFormatter),
                receipt.getPaymentMethod()
                        .getDisplayName(),
                formatCurrency(
                        receipt.getSubtotal()
                ),
                formatCurrency(
                        receipt.getDiscountAmount()
                ),
                formatCurrency(
                        receipt.getTotalAmount()
                )
        );
    }

    private String formatCurrency(
            BigDecimal amount
    ) {
        if (amount == null) {
            return "0 ₫";
        }

        return currencyFormat.format(amount);
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