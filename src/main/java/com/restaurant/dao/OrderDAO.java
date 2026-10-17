package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.OrderStatus;
import com.restaurant.model.RestaurantOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.restaurant.model.KitchenStatus;
import com.restaurant.model.OrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public RestaurantOrder findActiveByTableId(
            long tableId
    ) throws SQLException {

        String sql = """
                SELECT
                    o.id,
                    o.table_id,
                    t.table_number,
                    o.reservation_id,
                    o.waiter_id,
                    u.full_name AS waiter_name,
                    o.status,
                    o.note,
                    o.created_at
                FROM orders o
                JOIN restaurant_tables t
                    ON t.id = o.table_id
                JOIN users u
                    ON u.id = o.waiter_id
                WHERE o.table_id = ?
                  AND o.status NOT IN (
                      'PAID',
                      'CANCELLED'
                  )
                ORDER BY o.id DESC
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, tableId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (resultSet.next()) {
                    return mapOrder(resultSet);
                }

                return null;
            }
        }
    }

    public long create(
            long tableId,
            Long reservationId,
            long waiterId,
            String note
    ) throws SQLException {

        String insertOrderSql = """
                INSERT INTO orders (
                    table_id,
                    reservation_id,
                    waiter_id,
                    status,
                    note
                )
                VALUES (?, ?, ?, 'OPEN', ?)
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection()
        ) {
            try {
                connection.setAutoCommit(false);

                validateTable(
                        connection,
                        tableId
                );

                validateNoActiveOrder(
                        connection,
                        tableId
                );

                validateWaiter(
                        connection,
                        waiterId
                );

                if (reservationId != null) {
                    validateReservation(
                            connection,
                            reservationId,
                            tableId
                    );
                }

                long orderId;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        insertOrderSql,
                                        Statement.RETURN_GENERATED_KEYS
                                )
                ) {
                    statement.setLong(1, tableId);

                    if (reservationId == null) {
                        statement.setNull(
                                2,
                                java.sql.Types.BIGINT
                        );
                    } else {
                        statement.setLong(
                                2,
                                reservationId
                        );
                    }

                    statement.setLong(3, waiterId);
                    statement.setString(4, note);

                    statement.executeUpdate();

                    try (
                            ResultSet generatedKeys =
                                    statement.getGeneratedKeys()
                    ) {
                        if (!generatedKeys.next()) {
                            throw new SQLException(
                                    "Không lấy được mã đơn gọi món"
                            );
                        }

                        orderId =
                                generatedKeys.getLong(1);
                    }
                }

                updateTableToOccupied(
                        connection,
                        tableId
                );

                if (reservationId != null) {
                    updateReservationToSeated(
                            connection,
                            reservationId
                    );
                }

                connection.commit();

                return orderId;

            } catch (
                    SQLException
                    | RuntimeException exception
            ) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validateTable(
            Connection connection,
            long tableId
    ) throws SQLException {

        String sql = """
                SELECT status, active
                FROM restaurant_tables
                WHERE id = ?
                FOR UPDATE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, tableId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Bàn không tồn tại"
                    );
                }

                if (!resultSet.getBoolean("active")) {
                    throw new IllegalStateException(
                            "Bàn đã ngừng hoạt động"
                    );
                }

                String status =
                        resultSet.getString("status");

                if ("INACTIVE".equals(status)) {
                    throw new IllegalStateException(
                            "Không thể tạo đơn cho bàn ngừng hoạt động"
                    );
                }
            }
        }
    }

    private void validateNoActiveOrder(
            Connection connection,
            long tableId
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM orders
                WHERE table_id = ?
                  AND status NOT IN (
                      'PAID',
                      'CANCELLED'
                  )
                LIMIT 1
                FOR UPDATE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, tableId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (resultSet.next()) {
                    throw new IllegalStateException(
                            "Bàn đang có một đơn chưa hoàn tất"
                    );
                }
            }
        }
    }

    private void validateWaiter(
            Connection connection,
            long waiterId
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM users
                WHERE id = ?
                  AND active = TRUE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, waiterId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Nhân viên không tồn tại hoặc đã bị khóa"
                    );
                }
            }
        }
    }

    private void validateReservation(
            Connection connection,
            long reservationId,
            long tableId
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM reservations
                WHERE id = ?
                  AND table_id = ?
                  AND status IN (
                      'CONFIRMED',
                      'SEATED'
                  )
                FOR UPDATE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, reservationId);
            statement.setLong(2, tableId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Đặt bàn không hợp lệ hoặc không thuộc bàn đã chọn"
                    );
                }
            }
        }
    }

    private void updateTableToOccupied(
            Connection connection,
            long tableId
    ) throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET status = 'OCCUPIED'
                WHERE id = ?
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, tableId);
            statement.executeUpdate();
        }
    }

    private void updateReservationToSeated(
            Connection connection,
            long reservationId
    ) throws SQLException {

        String sql = """
                UPDATE reservations
                SET status = 'SEATED'
                WHERE id = ?
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, reservationId);
            statement.executeUpdate();
        }
    }
public long addItem(
        long orderId,
        long menuItemId,
        int quantity,
        String note
) throws SQLException {

    String insertSql = """
            INSERT INTO order_items (
                order_id,
                menu_item_id,
                quantity,
                unit_price,
                note,
                kitchen_status
            )
            VALUES (?, ?, ?, ?, ?, 'PENDING')
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection()
    ) {
        try {
            connection.setAutoCommit(false);

            validateOpenOrder(
                    connection,
                    orderId
            );

            BigDecimal unitPrice =
                    findAvailableMenuItemPrice(
                            connection,
                            menuItemId
                    );

            long orderItemId;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    insertSql,
                                    Statement.RETURN_GENERATED_KEYS
                            )
            ) {
                statement.setLong(1, orderId);
                statement.setLong(2, menuItemId);
                statement.setInt(3, quantity);
                statement.setBigDecimal(
                        4,
                        unitPrice
                );
                statement.setString(5, note);

                statement.executeUpdate();

                try (
                        ResultSet generatedKeys =
                                statement.getGeneratedKeys()
                ) {
                    if (!generatedKeys.next()) {
                        throw new SQLException(
                                "Không lấy được mã món trong đơn"
                        );
                    }

                    orderItemId =
                            generatedKeys.getLong(1);
                }
            }

            connection.commit();

            return orderItemId;

        } catch (
                SQLException
                | RuntimeException exception
        ) {
            connection.rollback();
            throw exception;

        } finally {
            connection.setAutoCommit(true);
        }
    }
}
public List<OrderItem> findItemsByOrderId(
        long orderId
) throws SQLException {

    String sql = """
            SELECT
                oi.id,
                oi.order_id,
                oi.menu_item_id,
                mi.item_name,
                oi.quantity,
                oi.unit_price,
                oi.note,
                oi.kitchen_status
            FROM order_items oi
            JOIN menu_items mi
                ON mi.id = oi.menu_item_id
            WHERE oi.order_id = ?
            ORDER BY oi.id
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, orderId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            List<OrderItem> orderItems =
                    new ArrayList<>();

            while (resultSet.next()) {
                orderItems.add(
                        mapOrderItem(resultSet)
                );
            }

            return orderItems;
        }
    }
}
public BigDecimal calculateTotal(
        long orderId
) throws SQLException {

    String sql = """
            SELECT COALESCE(
                SUM(quantity * unit_price),
                0
            ) AS total
            FROM order_items
            WHERE order_id = ?
              AND kitchen_status <> 'CANCELLED'
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, orderId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            if (resultSet.next()) {
                return resultSet.getBigDecimal(
                        "total"
                );
            }

            return BigDecimal.ZERO;
        }
    }
}
private void validateOpenOrder(
        Connection connection,
        long orderId
) throws SQLException {

    String sql = """
            SELECT status
            FROM orders
            WHERE id = ?
            FOR UPDATE
            """;

    try (
            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, orderId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            if (!resultSet.next()) {
                throw new IllegalArgumentException(
                        "Đơn gọi món không tồn tại"
                );
            }

            String status =
                    resultSet.getString("status");

            if (!"OPEN".equals(status)) {
                throw new IllegalStateException(
                        "Chỉ được thêm món khi đơn đang mở"
                );
            }
        }
    }
}

private BigDecimal findAvailableMenuItemPrice(
        Connection connection,
        long menuItemId
) throws SQLException {

    String sql = """
            SELECT price
            FROM menu_items
            WHERE id = ?
              AND available = TRUE
            FOR UPDATE
            """;

    try (
            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, menuItemId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            if (!resultSet.next()) {
                throw new IllegalArgumentException(
                        "Món ăn không tồn tại hoặc đã ngừng bán"
                );
            }

            return resultSet.getBigDecimal("price");
        }
    }
}

public void updateItemQuantity(
        long orderItemId,
        int quantity
) throws SQLException {

    String sql = """
            UPDATE order_items
            SET quantity = ?
            WHERE id = ?
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection()
    ) {
        try {
            connection.setAutoCommit(false);

            validateEditableOrderItem(
                    connection,
                    orderItemId
            );

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {
                statement.setInt(1, quantity);
                statement.setLong(2, orderItemId);

                int affectedRows =
                        statement.executeUpdate();

                if (affectedRows != 1) {
                    throw new SQLException(
                            "Không thể cập nhật số lượng món"
                    );
                }
            }

            connection.commit();

        } catch (
                SQLException
                | RuntimeException exception
        ) {
            connection.rollback();
            throw exception;

        } finally {
            connection.setAutoCommit(true);
        }
    }
}
public void cancelItem(
        long orderItemId
) throws SQLException {

    String sql = """
            UPDATE order_items
            SET kitchen_status = 'CANCELLED'
            WHERE id = ?
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection()
    ) {
        try {
            connection.setAutoCommit(false);

            validateEditableOrderItem(
                    connection,
                    orderItemId
            );

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {
                statement.setLong(1, orderItemId);

                int affectedRows =
                        statement.executeUpdate();

                if (affectedRows != 1) {
                    throw new SQLException(
                            "Không thể hủy món"
                    );
                }
            }

            connection.commit();

        } catch (
                SQLException
                | RuntimeException exception
        ) {
            connection.rollback();
            throw exception;

        } finally {
            connection.setAutoCommit(true);
        }
    }
}
private void validateEditableOrderItem(
        Connection connection,
        long orderItemId
) throws SQLException {

    String sql = """
            SELECT
                oi.kitchen_status,
                o.status AS order_status
            FROM order_items oi
            JOIN orders o
                ON o.id = oi.order_id
            WHERE oi.id = ?
            FOR UPDATE
            """;

    try (
            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, orderItemId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            if (!resultSet.next()) {
                throw new IllegalArgumentException(
                        "Món trong đơn không tồn tại"
                );
            }

            String orderStatus =
                    resultSet.getString(
                            "order_status"
                    );

            String kitchenStatus =
                    resultSet.getString(
                            "kitchen_status"
                    );

            if (!"OPEN".equals(orderStatus)) {
                throw new IllegalStateException(
                        "Chỉ được sửa món khi đơn đang mở"
                );
            }

            if (!"PENDING".equals(kitchenStatus)) {
                throw new IllegalStateException(
                        "Chỉ được sửa món đang chờ chế biến"
                );
            }
        }
    }
}

public Long findSeatedReservationIdByTableId(
        long tableId
) throws SQLException {

    String sql = """
            SELECT id
            FROM reservations
            WHERE table_id = ?
              AND status = 'SEATED'
            ORDER BY reservation_time DESC, id DESC
            LIMIT 1
            """;

    try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {
        statement.setLong(1, tableId);

        try (
                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            if (resultSet.next()) {
                return resultSet.getLong("id");
            }

            return null;
        }
    }
}

private OrderItem mapOrderItem(
        ResultSet resultSet
) throws SQLException {

    return new OrderItem(
            resultSet.getLong("id"),
            resultSet.getLong("order_id"),
            resultSet.getLong("menu_item_id"),
            resultSet.getString("item_name"),
            resultSet.getInt("quantity"),
            resultSet.getBigDecimal("unit_price"),
            resultSet.getString("note"),
            KitchenStatus.valueOf(
                    resultSet.getString(
                            "kitchen_status"
                    )
            )
    );
}    
    private RestaurantOrder mapOrder(
            ResultSet resultSet
    ) throws SQLException {

        long reservationValue =
                resultSet.getLong("reservation_id");

        Long reservationId =
                resultSet.wasNull()
                        ? null
                        : reservationValue;

        return new RestaurantOrder(
                resultSet.getLong("id"),
                resultSet.getLong("table_id"),
                resultSet.getString("table_number"),
                reservationId,
                resultSet.getLong("waiter_id"),
                resultSet.getString("waiter_name"),
                OrderStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getString("note"),
                resultSet.getTimestamp("created_at")
                        .toLocalDateTime()
        );
    }
}