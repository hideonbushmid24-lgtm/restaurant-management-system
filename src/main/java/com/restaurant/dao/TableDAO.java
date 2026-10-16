package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

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
            statement.executeUpdate();

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
                    AND status = 'AVAILABLE'
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

    public boolean deactivate(long id)
            throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET active = FALSE,
                    status = 'INACTIVE'
                WHERE id = ?
                  AND active = TRUE
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