package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.KitchenItem;
import com.restaurant.model.KitchenStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KitchenDAO {

    public List<KitchenItem> findActiveItems()
            throws SQLException {

        String sql = """
                SELECT
                    oi.id,
                    oi.order_id,
                    t.table_number,
                    mi.item_name,
                    oi.quantity,
                    oi.note,
                    oi.kitchen_status,
                    oi.created_at
                FROM order_items oi
                JOIN orders o
                    ON o.id = oi.order_id
                JOIN restaurant_tables t
                    ON t.id = o.table_id
                JOIN menu_items mi
                    ON mi.id = oi.menu_item_id
                WHERE o.status IN (
                    'IN_KITCHEN',
                    'READY'
                )
                  AND oi.kitchen_status IN (
                    'PENDING',
                    'PREPARING',
                    'READY'
                )
                ORDER BY
                    CASE oi.kitchen_status
                        WHEN 'PREPARING' THEN 1
                        WHEN 'PENDING' THEN 2
                        WHEN 'READY' THEN 3
                        ELSE 4
                    END,
                    oi.created_at,
                    oi.id
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            List<KitchenItem> items =
                    new ArrayList<>();

            while (resultSet.next()) {
                items.add(
                        mapRow(resultSet)
                );
            }

            return items;
        }
    }

    public void updateStatus(
            long orderItemId,
            KitchenStatus newStatus
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseConnection.getConnection()
        ) {
            try {
                connection.setAutoCommit(false);

                KitchenStatus currentStatus =
                        findCurrentStatus(
                                connection,
                                orderItemId
                        );

                validateTransition(
                        currentStatus,
                        newStatus
                );

                String updateItemSql = """
                        UPDATE order_items
                        SET kitchen_status = ?
                        WHERE id = ?
                        """;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        updateItemSql
                                )
                ) {
                    statement.setString(
                            1,
                            newStatus.name()
                    );

                    statement.setLong(
                            2,
                            orderItemId
                    );

                    int affectedRows =
                            statement.executeUpdate();

                    if (affectedRows != 1) {
                        throw new SQLException(
                                "Không thể cập nhật trạng thái món"
                        );
                    }
                }

                long orderId =
                        findOrderId(
                                connection,
                                orderItemId
                        );

                synchronizeOrderStatus(
                        connection,
                        orderId
                );

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

    private KitchenStatus findCurrentStatus(
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
                            "Món cần xử lý không tồn tại"
                    );
                }

                String orderStatus =
                        resultSet.getString(
                                "order_status"
                        );

                if (!"IN_KITCHEN".equals(orderStatus)
                        && !"READY".equals(orderStatus)) {

                    throw new IllegalStateException(
                            "Đơn không còn trong quy trình bếp"
                    );
                }

                return KitchenStatus.valueOf(
                        resultSet.getString(
                                "kitchen_status"
                        )
                );
            }
        }
    }

    private void validateTransition(
            KitchenStatus currentStatus,
            KitchenStatus newStatus
    ) {
        boolean valid =
                currentStatus == KitchenStatus.PENDING
                        && newStatus
                        == KitchenStatus.PREPARING
                || currentStatus
                        == KitchenStatus.PREPARING
                        && newStatus
                        == KitchenStatus.READY;

        if (!valid) {
            throw new IllegalStateException(
                    "Chuyển trạng thái món không hợp lệ: "
                            + currentStatus
                            + " → "
                            + newStatus
            );
        }
    }

    private long findOrderId(
            Connection connection,
            long orderItemId
    ) throws SQLException {

        String sql = """
                SELECT order_id
                FROM order_items
                WHERE id = ?
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
                    throw new SQLException(
                            "Không tìm thấy đơn của món"
                    );
                }

                return resultSet.getLong(
                        "order_id"
                );
            }
        }
    }

    private void synchronizeOrderStatus(
            Connection connection,
            long orderId
    ) throws SQLException {

        String countSql = """
                SELECT
                    SUM(
                        CASE
                            WHEN kitchen_status IN (
                                'PENDING',
                                'PREPARING'
                            )
                            THEN 1
                            ELSE 0
                        END
                    ) AS unfinished_count,
                    SUM(
                        CASE
                            WHEN kitchen_status <> 'CANCELLED'
                            THEN 1
                            ELSE 0
                        END
                    ) AS active_count
                FROM order_items
                WHERE order_id = ?
                """;

        int unfinishedCount;
        int activeCount;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                countSql
                        )
        ) {
            statement.setLong(1, orderId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                resultSet.next();

                unfinishedCount =
                        resultSet.getInt(
                                "unfinished_count"
                        );

                activeCount =
                        resultSet.getInt(
                                "active_count"
                        );
            }
        }

        String newOrderStatus =
                activeCount > 0
                        && unfinishedCount == 0
                        ? "READY"
                        : "IN_KITCHEN";

        String updateSql = """
                UPDATE orders
                SET status = ?
                WHERE id = ?
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                updateSql
                        )
        ) {
            statement.setString(
                    1,
                    newOrderStatus
            );

            statement.setLong(
                    2,
                    orderId
            );

            statement.executeUpdate();
        }
    }

    private KitchenItem mapRow(
            ResultSet resultSet
    ) throws SQLException {

        return new KitchenItem(
                resultSet.getLong("id"),
                resultSet.getLong("order_id"),
                resultSet.getString(
                        "table_number"
                ),
                resultSet.getString(
                        "item_name"
                ),
                resultSet.getInt("quantity"),
                resultSet.getString("note"),
                KitchenStatus.valueOf(
                        resultSet.getString(
                                "kitchen_status"
                        )
                ),
                resultSet.getTimestamp(
                        "created_at"
                ).toLocalDateTime()
        );
    }
}