package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    /**
     * Tìm kiếm và lọc danh sách bàn đang hoạt động.
     */
    public List<RestaurantTable> search(
            String keyword,
            TableStatus status
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    table_number,
                    capacity,
                    status,
                    active
                FROM restaurant_tables
                WHERE active = TRUE
                """);

        List<Object> parameters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND LOWER(table_number) LIKE ?
                    """);

            parameters.add(
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        }

        if (status != null) {
            sql.append("""
                     AND status = ?
                    """);

            parameters.add(status.name());
        }

        sql.append(" ORDER BY table_number");

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

                List<RestaurantTable> tables =
                        new ArrayList<>();

                while (resultSet.next()) {
                    tables.add(mapRow(resultSet));
                }

                return tables;
            }
        }
    }

    /**
     * Thêm một bàn hoàn toàn mới.
     */
    public long insert(
            String tableNumber,
            int capacity
    ) throws SQLException {

        String sql = """
                INSERT INTO restaurant_tables (
                    table_number,
                    capacity,
                    status,
                    active
                )
                VALUES (?, ?, 'AVAILABLE', TRUE)
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setString(1, tableNumber);
            statement.setInt(2, capacity);

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException(
                        "Không thể thêm bàn mới"
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Không lấy được mã bàn vừa tạo"
                    );
                }

                return generatedKeys.getLong(1);
            }
        }
    }

    /**
     * Cập nhật thông tin và trạng thái bàn.
     *
     * Cho phép chuyển từ RESERVED hoặc OCCUPIED
     * quay lại AVAILABLE.
     */
    public boolean update(
            long id,
            String tableNumber,
            int capacity,
            TableStatus status
    ) throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET table_number = ?,
                    capacity = ?,
                    status = ?
                WHERE id = ?
                  AND active = TRUE
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, tableNumber);
            statement.setInt(2, capacity);
            statement.setString(3, status.name());
            statement.setLong(4, id);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Xóa mềm một bàn đang trống.
     *
     * Bàn được chuyển sang INACTIVE và không còn
     * xuất hiện trong danh sách bàn đang hoạt động.
     */
    public boolean deactivate(long id)
            throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET active = FALSE,
                    status = 'INACTIVE'
                WHERE id = ?
                  AND active = TRUE
                  AND status = 'AVAILABLE'
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Tìm ID của bàn đã bị xóa mềm.
     *
     * Trả về null nếu không tìm thấy.
     */
    public Long findInactiveIdByTableNumber(
            String tableNumber
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM restaurant_tables
                WHERE table_number = ?
                  AND active = FALSE
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, tableNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }

                return null;
            }
        }
    }

    /**
     * Kích hoạt lại một bàn đã bị xóa mềm.
     *
     * Bàn được đưa về trạng thái AVAILABLE.
     */
    public boolean reactivate(
            long id,
            int capacity
    ) throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET capacity = ?,
                    status = 'AVAILABLE',
                    active = TRUE
                WHERE id = ?
                  AND active = FALSE
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, capacity);
            statement.setLong(2, id);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Kiểm tra số bàn đang hoạt động đã tồn tại chưa.
     *
     * excludedId được sử dụng khi sửa bàn để loại trừ
     * chính bàn đang được cập nhật.
     */
    public boolean existsByTableNumber(
            String tableNumber,
            Long excludedId
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS total
                FROM restaurant_tables
                WHERE table_number = ?
                  AND active = TRUE
                """);

        if (excludedId != null) {
            sql.append(" AND id <> ?");
        }

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {
            statement.setString(1, tableNumber);

            if (excludedId != null) {
                statement.setLong(2, excludedId);
            }

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                resultSet.next();

                return resultSet.getInt("total") > 0;
            }
        }
    }

    /**
     * Chuyển dữ liệu ResultSet thành RestaurantTable.
     */
    private RestaurantTable mapRow(
            ResultSet resultSet
    ) throws SQLException {

        return new RestaurantTable(
                resultSet.getLong("id"),
                resultSet.getString("table_number"),
                resultSet.getInt("capacity"),
                TableStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getBoolean("active")
        );
    }
}