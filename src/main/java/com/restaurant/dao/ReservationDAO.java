package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Reservation;
import com.restaurant.model.ReservationStatus;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public List<Reservation> findAll()
            throws SQLException {

        String sql = """
                SELECT
                    r.id,
                    r.table_id,
                    t.table_number,
                    c.full_name AS customer_name,
                    c.phone AS customer_phone,
                    r.reservation_time,
                    r.guest_count,
                    r.note,
                    r.status
                FROM reservations r
                JOIN restaurant_tables t
                  ON t.id = r.table_id
                JOIN customers c
                  ON c.id = r.customer_id
                ORDER BY r.reservation_time DESC
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            List<Reservation> reservations =
                    new ArrayList<>();

            while (resultSet.next()) {
                reservations.add(
                        mapReservation(resultSet)
                );
            }

            return reservations;
        }
    }

    public List<RestaurantTable> findAvailableTables()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    table_number,
                    capacity,
                    status,
                    active
                FROM restaurant_tables
                WHERE active = TRUE
                  AND status = 'AVAILABLE'
                ORDER BY table_number
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            List<RestaurantTable> tables =
                    new ArrayList<>();

            while (resultSet.next()) {
                tables.add(
                        new RestaurantTable(
                                resultSet.getLong("id"),
                                resultSet.getString(
                                        "table_number"
                                ),
                                resultSet.getInt("capacity"),
                                TableStatus.valueOf(
                                        resultSet.getString(
                                                "status"
                                        )
                                ),
                                resultSet.getBoolean("active")
                        )
                );
            }

            return tables;
        }
    }

    public long createReservation(
            long tableId,
            String customerName,
            String customerPhone,
            LocalDateTime reservationTime,
            int guestCount,
            String note,
            long createdBy
    ) throws SQLException {

        String lockTableSql = """
                SELECT capacity, status, active
                FROM restaurant_tables
                WHERE id = ?
                FOR UPDATE
                """;

        String insertReservationSql = """
                INSERT INTO reservations (
                    table_id,
                    customer_id,
                    reservation_time,
                    guest_count,
                    note,
                    status,
                    created_by
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    'CONFIRMED', ?
                )
                """;

        String updateTableSql = """
                UPDATE restaurant_tables
                SET status = 'RESERVED'
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                validateTableForReservation(
                        connection,
                        lockTableSql,
                        tableId,
                        guestCount
                );

                /*
                 * Tìm khách theo số điện thoại, chưa có thì tạo mới.
                 * Nằm trong cùng transaction với việc đặt bàn.
                 */
                long customerId = findOrCreateCustomer(
                        connection,
                        customerName,
                        customerPhone
                );

                long reservationId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertReservationSql,
                                     Statement
                                             .RETURN_GENERATED_KEYS
                             )) {

                    statement.setLong(1, tableId);
                    statement.setLong(2, customerId);
                    statement.setTimestamp(
                            3,
                            Timestamp.valueOf(
                                    reservationTime
                            )
                    );
                    statement.setInt(4, guestCount);
                    statement.setString(5, note);
                    statement.setLong(6, createdBy);

                    statement.executeUpdate();

                    try (ResultSet generatedKeys =
                                 statement
                                         .getGeneratedKeys()) {

                        if (!generatedKeys.next()) {
                            throw new SQLException(
                                    "Không lấy được mã đặt bàn"
                            );
                        }

                        reservationId =
                                generatedKeys.getLong(1);
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateTableSql
                             )) {

                    statement.setLong(1, tableId);
                    statement.executeUpdate();
                }

                connection.commit();
                return reservationId;

            } catch (
                    SQLException
                    | RuntimeException exception
            ) {
                connection.rollback();
                throw exception;
            }
        }
    }

    /**
     * Tìm khách theo số điện thoại; nếu chưa có thì tạo mới.
     * Cùng số điện thoại được xem là cùng một khách, tên được
     * cập nhật theo lần đặt gần nhất.
     *
     * Dựa vào ràng buộc UNIQUE của cột phone nên hai người cùng
     * đặt bàn một lúc cũng không tạo ra hai khách trùng số.
     */
    private long findOrCreateCustomer(
            Connection connection,
            String customerName,
            String customerPhone
    ) throws SQLException {

        String upsertSql = """
                INSERT INTO customers (full_name, phone)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE
                    full_name = VALUES(full_name)
                """;

        String findSql = """
                SELECT id
                FROM customers
                WHERE phone = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(upsertSql)) {

            statement.setString(1, customerName);
            statement.setString(2, customerPhone);
            statement.executeUpdate();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(findSql)) {

            statement.setString(1, customerPhone);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Không lấy được mã khách hàng"
                    );
                }

                return resultSet.getLong("id");
            }
        }
    }

    public void seatReservation(long reservationId)
            throws SQLException {

        changeReservationStatus(
                reservationId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.SEATED,
                TableStatus.OCCUPIED
        );
    }

    public void cancelReservation(long reservationId)
            throws SQLException {

        changeReservationStatus(
                reservationId,
                ReservationStatus.CONFIRMED,
                ReservationStatus.CANCELLED,
                TableStatus.AVAILABLE
        );
    }

    private void changeReservationStatus(
            long reservationId,
            ReservationStatus expectedStatus,
            ReservationStatus newStatus,
            TableStatus newTableStatus
    ) throws SQLException {

        String findReservationSql = """
                SELECT table_id, status
                FROM reservations
                WHERE id = ?
                FOR UPDATE
                """;

        String updateReservationSql = """
                UPDATE reservations
                SET status = ?
                WHERE id = ?
                """;

        String updateTableSql = """
                UPDATE restaurant_tables
                SET status = ?
                WHERE id = ?
                  AND active = TRUE
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long tableId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     findReservationSql
                             )) {

                    statement.setLong(
                            1,
                            reservationId
                    );

                    try (ResultSet resultSet =
                                 statement.executeQuery()) {

                        if (!resultSet.next()) {
                            throw new IllegalArgumentException(
                                    "Không tìm thấy lượt đặt bàn"
                            );
                        }

                        ReservationStatus currentStatus =
                                ReservationStatus.valueOf(
                                        resultSet.getString(
                                                "status"
                                        )
                                );

                        if (currentStatus
                                != expectedStatus) {

                            throw new IllegalArgumentException(
                                    "Trạng thái đặt bàn không hợp lệ"
                            );
                        }

                        tableId =
                                resultSet.getLong(
                                        "table_id"
                                );
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateReservationSql
                             )) {

                    statement.setString(
                            1,
                            newStatus.name()
                    );

                    statement.setLong(
                            2,
                            reservationId
                    );

                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateTableSql
                             )) {

                    statement.setString(
                            1,
                            newTableStatus.name()
                    );

                    statement.setLong(2, tableId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (
                    SQLException
                    | RuntimeException exception
            ) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void validateTableForReservation(
            Connection connection,
            String sql,
            long tableId,
            int guestCount
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, tableId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Không tìm thấy bàn"
                    );
                }

                if (!resultSet.getBoolean("active")) {
                    throw new IllegalArgumentException(
                            "Bàn đã ngừng hoạt động"
                    );
                }

                TableStatus status =
                        TableStatus.valueOf(
                                resultSet.getString("status")
                        );

                if (status != TableStatus.AVAILABLE) {
                    throw new IllegalArgumentException(
                            "Bàn không còn trống"
                    );
                }

                int capacity =
                        resultSet.getInt("capacity");

                if (guestCount > capacity) {
                    throw new IllegalArgumentException(
                            "Số khách vượt quá sức chứa của bàn"
                    );
                }
            }
        }
    }

    private Reservation mapReservation(
            ResultSet resultSet
    ) throws SQLException {

        return new Reservation(
                resultSet.getLong("id"),
                resultSet.getLong("table_id"),
                resultSet.getString("table_number"),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_phone"),
                resultSet
                        .getTimestamp("reservation_time")
                        .toLocalDateTime(),
                resultSet.getInt("guest_count"),
                resultSet.getString("note"),
                ReservationStatus.valueOf(
                        resultSet.getString("status")
                )
        );
    }
}