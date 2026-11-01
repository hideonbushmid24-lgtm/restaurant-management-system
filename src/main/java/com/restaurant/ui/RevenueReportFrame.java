package com.restaurant.ui;

import com.restaurant.model.PaymentMethod;
import com.restaurant.model.RevenueRecord;
import com.restaurant.model.RevenueSummary;
import com.restaurant.service.RevenueReportService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public class RevenueReportFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter
                    .ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss"
            );

    private final RevenueReportService revenueReportService;

    private final NumberFormat moneyFormat =
            NumberFormat.getNumberInstance(
                    Locale.of("vi", "VN")
            );

    private JTextField startDateField;
    private JTextField endDateField;

    private JLabel invoiceCountValue;
    private JLabel subtotalValue;
    private JLabel discountValue;
    private JLabel totalRevenueValue;

    private DefaultTableModel tableModel;
    private JTable revenueTable;

    public RevenueReportFrame(
            RevenueReportService revenueReportService
    ) {
        this.revenueReportService =
                revenueReportService;

        moneyFormat.setMaximumFractionDigits(0);
        moneyFormat.setMinimumFractionDigits(0);

        setTitle("Báo cáo doanh thu");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 620));
        setSize(1180, 680);
        setLocationRelativeTo(null);

        initializeComponents();
        initializeDefaultDates();
        loadReport();
    }

    private void initializeComponents() {
        JPanel container =
                new JPanel(new BorderLayout(10, 10));

        container.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        15,
                        15,
                        15
                )
        );

        JLabel titleLabel =
                new JLabel(
                        "BÁO CÁO DOANH THU",
                        SwingConstants.CENTER
                );

        titleLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        24
                )
        );

        JPanel headerPanel =
                new JPanel(new BorderLayout(10, 10));

        headerPanel.add(
                titleLabel,
                BorderLayout.NORTH
        );

        headerPanel.add(
                createFilterPanel(),
                BorderLayout.CENTER
        );

        headerPanel.add(
                createSummaryPanel(),
                BorderLayout.SOUTH
        );

        container.add(
                headerPanel,
                BorderLayout.NORTH
        );

        container.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        container.add(
                createBottomPanel(),
                BorderLayout.SOUTH
        );

        setContentPane(container);
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel =
                new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        startDateField = new JTextField(10);
        endDateField = new JTextField(10);

        JButton viewReportButton =
                new JButton("Xem báo cáo");

        viewReportButton.addActionListener(
                event -> loadReport()
        );

        filterPanel.add(new JLabel("Từ ngày:"));
        filterPanel.add(startDateField);

        filterPanel.add(new JLabel("Đến ngày:"));
        filterPanel.add(endDateField);

        filterPanel.add(
                new JLabel("(dd/MM/yyyy)")
        );

        filterPanel.add(viewReportButton);

        return filterPanel;
    }

    private JPanel createSummaryPanel() {
        JPanel summaryPanel =
                new JPanel(new GridLayout(1, 4, 10, 10));

        invoiceCountValue =
                createSummaryValueLabel();

        subtotalValue =
                createSummaryValueLabel();

        discountValue =
                createSummaryValueLabel();

        totalRevenueValue =
                createSummaryValueLabel();

        summaryPanel.add(
                createSummaryCard(
                        "Số hóa đơn",
                        invoiceCountValue
                )
        );

        summaryPanel.add(
                createSummaryCard(
                        "Tổng tiền món",
                        subtotalValue
                )
        );

        summaryPanel.add(
                createSummaryCard(
                        "Tổng giảm giá",
                        discountValue
                )
        );

        summaryPanel.add(
                createSummaryCard(
                        "Doanh thu thực nhận",
                        totalRevenueValue
                )
        );

        return summaryPanel;
    }

    private JLabel createSummaryValueLabel() {
        JLabel valueLabel =
                new JLabel(
                        "0",
                        SwingConstants.CENTER
                );

        valueLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        18
                )
        );

        return valueLabel;
    }

    private JPanel createSummaryCard(
            String title,
            JLabel valueLabel
    ) {
        JPanel card =
                new JPanel(new BorderLayout(5, 5));

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createEmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(
                        title,
                        SwingConstants.CENTER
                );

        card.add(
                titleLabel,
                BorderLayout.NORTH
        );

        card.add(
                valueLabel,
                BorderLayout.CENTER
        );

        return card;
    }

    private JScrollPane createTablePanel() {
        String[] columnNames = {
                "Mã hóa đơn",
                "Mã đơn",
                "Bàn",
                "Thu ngân",
                "Tiền món",
                "Giảm giá",
                "Thực thu",
                "Phương thức",
                "Thời gian thanh toán"
        };

        tableModel =
                new DefaultTableModel(
                        columnNames,
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

        revenueTable = new JTable(tableModel);

        revenueTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        revenueTable.setAutoCreateRowSorter(true);
        revenueTable.setRowHeight(25);

        return new JScrollPane(revenueTable);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel =
                new JPanel(
                        new FlowLayout(FlowLayout.RIGHT)
                );

        JButton closeButton =
                new JButton("Đóng");

        closeButton.addActionListener(
                event -> dispose()
        );

        bottomPanel.add(closeButton);

        return bottomPanel;
    }

    private void initializeDefaultDates() {
        LocalDate today = LocalDate.now();

        startDateField.setText(
                today
                        .withDayOfMonth(1)
                        .format(DATE_FORMATTER)
        );

        endDateField.setText(
                today.format(DATE_FORMATTER)
        );
    }

    private void loadReport() {
        try {
            LocalDate startDate =
                    parseDate(
                            startDateField.getText(),
                            "ngày bắt đầu"
                    );

            LocalDate endDate =
                    parseDate(
                            endDateField.getText(),
                            "ngày kết thúc"
                    );

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.WAIT_CURSOR
                    )
            );

            RevenueSummary summary =
                    revenueReportService.getSummary(
                            startDate,
                            endDate
                    );

            List<RevenueRecord> records =
                    revenueReportService.findRecords(
                            startDate,
                            endDate
                    );

            displaySummary(summary);
            displayRecords(records);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Dữ liệu không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể tải báo cáo doanh thu.\n"
                            + exception.getMessage(),
                    "Lỗi database",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            setCursor(
                    Cursor.getDefaultCursor()
            );
        }
    }

    private LocalDate parseDate(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Không được để trống " + fieldName + "."
            );
        }

        try {
            return LocalDate.parse(
                    value.trim(),
                    DATE_FORMATTER
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Sai định dạng "
                            + fieldName
                            + ". Vui lòng nhập dd/MM/yyyy."
            );
        }
    }

    private void displaySummary(
            RevenueSummary summary
    ) {
        invoiceCountValue.setText(
                String.valueOf(summary.invoiceCount())
        );

        subtotalValue.setText(
                formatMoney(summary.subtotal())
        );

        discountValue.setText(
                formatMoney(summary.discountAmount())
        );

        totalRevenueValue.setText(
                formatMoney(summary.totalAmount())
        );
    }

    private void displayRecords(
            List<RevenueRecord> records
    ) {
        tableModel.setRowCount(0);

        for (RevenueRecord record : records) {
            tableModel.addRow(
                    new Object[]{
                            record.invoiceCode(),
                            record.orderId(),
                            record.tableNumber(),
                            record.cashierName(),
                            formatMoney(record.subtotal()),
                            formatMoney(
                                    record.discountAmount()
                            ),
                            formatMoney(
                                    record.totalAmount()
                            ),
                            formatPaymentMethod(
                                    record.paymentMethod()
                            ),
                            record.paidAt().format(
                                    DATE_TIME_FORMATTER
                            )
                    }
            );
        }
    }

    private String formatMoney(
            BigDecimal amount
    ) {
        return moneyFormat.format(amount) + " đ";
    }

    private String formatPaymentMethod(
            PaymentMethod paymentMethod
    ) {
        return switch (paymentMethod) {
            case CASH -> "Tiền mặt";
            case CARD -> "Thẻ";
            case TRANSFER -> "Chuyển khoản";
        };
    }
}