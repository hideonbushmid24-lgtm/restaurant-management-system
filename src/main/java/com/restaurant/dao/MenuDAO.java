package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.MenuItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {

    /**
     * Tìm kiếm và lọc món ăn.
     *
     * @param keyword       từ khóa tìm theo tên món
     * @param category      danh mục món; null nghĩa là tất cả
     * @param availableOnly true nếu chỉ lấy món đang bán
     */
    public List<MenuItem> search(
            String keyword,
            String category,
            boolean availableOnly
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    item_name,
                    category,
                    description,
                    price,
                    available
                FROM menu_items
                WHERE 1 = 1
                """);

        List<Object> parameters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND LOWER(item_name) LIKE ?
                    """);

            parameters.add(
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        }

        if (category != null
                && !category.isBlank()) {

            sql.append("""
                     AND category = ?
                    """);

            parameters.add(category.trim());
        }

        if (availableOnly) {
            sql.append("""
                     AND available = TRUE
                    """);
        }

        sql.append("""
                 ORDER BY id ASC
                """);

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {
            for (int index = 0;
                 index < parameters.size();
                 index++) {

                statement.setObject(
                        index + 1,
                        parameters.get(index)
                );
            }

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                List<MenuItem> menuItems =
                        new ArrayList<>();

                while (resultSet.next()) {
                    menuItems.add(
                            mapRow(resultSet)
                    );
                }

                return menuItems;
            }
        }
    }

    /**
     * Lấy tất cả danh mục đang có trong thực đơn.
     */
    public List<String> findCategories()
            throws SQLException {

        String sql = """
                SELECT DISTINCT category
                FROM menu_items
                WHERE available = TRUE
                ORDER BY category
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            List<String> categories =
                    new ArrayList<>();

            while (resultSet.next()) {
                categories.add(
                        resultSet.getString("category")
                );
            }

            return categories;
        }
    }

    /**
     * Tìm món đang bán theo ID.
     *
     * Trả về null nếu món không tồn tại hoặc đã ngừng bán.
     */
    public MenuItem findAvailableById(long id)
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    item_name,
                    category,
                    description,
                    price,
                    available
                FROM menu_items
                WHERE id = ?
                  AND available = TRUE
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                return null;
            }
        }
    }

    /**
     * Chuyển một dòng ResultSet thành MenuItem.
     */
    private MenuItem mapRow(
            ResultSet resultSet
    ) throws SQLException {

        return new MenuItem(
                resultSet.getLong("id"),
                resultSet.getString("item_name"),
                resultSet.getString("category"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("price"),
                resultSet.getBoolean("available")
        );
    }
}