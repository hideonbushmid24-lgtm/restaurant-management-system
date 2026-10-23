package com.restaurant.ui;

import com.restaurant.model.MenuItem;
import com.restaurant.service.MenuService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuFrame extends JFrame {

    private final MenuService menuService;

    private final JTextField searchField =
            new JTextField(18);

    private final JComboBox<String> categoryComboBox =
            new JComboBox<>();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Tên món",
                        "Danh mục",
                        "Mô tả",
                        "Giá"
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
                if (columnIndex == 0) {
                    return Long.class;
                }

                if (columnIndex == 4) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable menuTable =
            new JTable(tableModel);

    private final JLabel resultLabel =
            new JLabel("Tổng số món: 0");

    private List<MenuItem> currentMenuItems =
            List.of();

    public MenuFrame(MenuService menuService) {
        if (menuService == null) {
            throw new IllegalArgumentException(
                    "MenuService không được null"
            );
        }

        this.menuService = menuService;

        initializeFrame();
        createLayout();
        configureTable();
        loadCategories();
        loadMenuItems();
    }

    private void initializeFrame() {
        setTitle("Thực đơn nhà hàng");
        setSize(950, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
    }

    private void createLayout() {
        setLayout(new BorderLayout(10, 10));

        JPanel titlePanel =
                new JPanel(new BorderLayout());

        titlePanel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        15,
                        5,
                        15
                )
        );

        JLabel titleLabel =
                new JLabel(
                        "THỰC ĐƠN NHÀ HÀNG",
                        SwingConstants.CENTER
                );

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        titlePanel.add(
                titleLabel,
                BorderLayout.CENTER
        );

        add(titlePanel, BorderLayout.NORTH);

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
                createSearchPanel(),
                BorderLayout.NORTH
        );

        centerPanel.add(
                new JScrollPane(menuTable),
                BorderLayout.CENTER
        );

        add(centerPanel, BorderLayout.CENTER);

        add(
                createBottomPanel(),
                BorderLayout.SOUTH
        );
    }

    private JPanel createSearchPanel() {
        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                5
                        )
                );

        JButton searchButton =
                new JButton("Tìm kiếm");

        JButton resetButton =
                new JButton("Đặt lại");

        panel.add(new JLabel("Tên món:"));
        panel.add(searchField);

        panel.add(new JLabel("Danh mục:"));
        panel.add(categoryComboBox);

        panel.add(searchButton);
        panel.add(resetButton);

        searchButton.addActionListener(
                event -> loadMenuItems()
        );

        searchField.addActionListener(
                event -> loadMenuItems()
        );

        categoryComboBox.addActionListener(
                event -> loadMenuItems()
        );

        resetButton.addActionListener(event -> {
            searchField.setText("");

            if (categoryComboBox.getItemCount() > 0) {
                categoryComboBox.setSelectedIndex(0);
            }

            loadMenuItems();
        });

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

        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        refreshButton.addActionListener(
                event -> {
                    loadCategories();
                    loadMenuItems();
                }
        );

        closeButton.addActionListener(
                event -> dispose()
        );

        panel.add(
                resultLabel,
                BorderLayout.WEST
        );

        panel.add(
                buttonPanel,
                BorderLayout.EAST
        );

        return panel;
    }

    private void configureTable() {
        menuTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
    );

        menuTable.setRowHeight(28);

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(tableModel);

    sorter.setSortable(0, false);
    sorter.setSortable(1, false);
    sorter.setSortable(2, false);
    sorter.setSortable(3, false);
    sorter.setSortable(4, true);

    menuTable.setRowSorter(sorter);

    menuTable.getColumnModel()
            .getColumn(0)
            .setPreferredWidth(50);

    menuTable.getColumnModel()
            .getColumn(1)
            .setPreferredWidth(180);

    menuTable.getColumnModel()
            .getColumn(2)
            .setPreferredWidth(120);

    menuTable.getColumnModel()
            .getColumn(3)
            .setPreferredWidth(350);

    menuTable.getColumnModel()
            .getColumn(4)
            .setPreferredWidth(110);

    DefaultTableCellRenderer priceRenderer =
            new DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    if (value instanceof BigDecimal amount) {
                        setText(formatCurrency(amount));
                    } else {
                        setText("");
                    }
                }
            };

        priceRenderer.setHorizontalAlignment(
            SwingConstants.RIGHT
    );

        menuTable.getColumnModel()
            .getColumn(4)
            .setCellRenderer(priceRenderer);
    }

    private void loadCategories() {
        try {
            String selectedCategory =
                    (String) categoryComboBox
                            .getSelectedItem();

            categoryComboBox.removeAllItems();
            categoryComboBox.addItem("Tất cả");

            List<String> categories =
                    menuService
                            .getAvailableCategories();

            for (String category : categories) {
                categoryComboBox.addItem(category);
            }

            if (selectedCategory != null) {
                categoryComboBox.setSelectedItem(
                        selectedCategory
                );
            }

            if (categoryComboBox.getSelectedIndex() < 0) {
                categoryComboBox.setSelectedIndex(0);
            }

        } catch (SQLException exception) {
            showError(
                    "Không thể tải danh mục món ăn.\n"
                            + exception.getMessage()
            );
        }
    }

    private void loadMenuItems() {
        try {
            String selectedCategory =
                    (String) categoryComboBox
                            .getSelectedItem();

            currentMenuItems =
                    menuService.searchAvailableItems(
                            searchField.getText(),
                            selectedCategory
                    );

            tableModel.setRowCount(0);

            for (MenuItem menuItem
                    : currentMenuItems) {

                tableModel.addRow(new Object[]{
                        menuItem.getId(),
                        menuItem.getItemName(),
                        menuItem.getCategory(),
                        menuItem.getDescription(),
                        menuItem.getPrice()
                        
                });
            }

            resultLabel.setText(
                    "Tổng số món: "
                            + currentMenuItems.size()
            );

        } catch (
                IllegalArgumentException
                | SQLException exception
        ) {
            showError(exception.getMessage());
        }
    }

    private String formatCurrency(
            BigDecimal amount
    ) {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        Locale.of("vi", "VN")
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