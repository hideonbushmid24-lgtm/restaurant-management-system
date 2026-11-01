package com.restaurant.dao;

import com.restaurant.model.PayableOrder;
import com.restaurant.model.PaymentMethod;
import com.restaurant.model.PaymentReceipt;
import com.restaurant.config.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public List<PayableOrder> findPayableOrders()
            throws SQLException {

        String sql = """
                SELECT
                    o.id AS order_id,
                    o.table_id,
                    t.table_number,
                    u.full_name AS waiter_name,
                    o.created_at,
                    COALESCE(
                        SUM(
                            CASE
                                WHEN oi.kitchen_status <> 'CANCELLED'
                                THEN oi.quantity * oi.unit_price
                                ELSE 0
                            END
                        ),
                        0
                    ) AS subtotal
                FROM orders o
                JOIN restaurant_tables t
                    ON t.id = o.table_id
                JOIN users u
                    ON u.id = o.waiter_id
                LEFT JOIN order_items oi
                    ON oi.order_id = o.id
                LEFT JOIN payments p
                    ON p.order_id = o.id
                WHERE o.status = 'SERVED'
                  AND p.id IS NULL
                GROUP BY
                    o.id,
                    o.table_id,
                    t.table_number,
                    u.full_name,
                    o.created_at
                ORDER BY o.created_at, o.id
                """;

        List<PayableOrder> orders =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Timestamp createdTimestamp =
                        resultSet.getTimestamp("created_at");

                orders.add(
                        new PayableOrder(
                                resultSet.getLong("order_id"),
                                resultSet.getLong("table_id"),
                                resultSet.getString("table_number"),
                                resultSet.getString("waiter_name"),
                                createdTimestamp.toLocalDateTime(),
                                resultSet.getBigDecimal("subtotal")
                        )
                );
            }
        }

        return orders;
    }

    public PaymentReceipt pay(
            long orderId,
            long cashierId,
            BigDecimal discountAmount,
            PaymentMethod paymentMethod
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseConnection.getConnection()
        ) {
            try {
                connection.setAutoCommit(false);

                CashierData cashier =
                        lockCashier(
                                connection,
                                cashierId
                        );

                OrderPaymentData order =
                        lockServedOrder(
                                connection,
                                orderId
                        );

                ensureNotPaid(
                        connection,
                        orderId
                );

                BigDecimal subtotal =
                        calculateSubtotal(
                                connection,
                                orderId
                        );

                validateDiscount(
                        subtotal,
                        discountAmount
                );

                BigDecimal totalAmount =
                        subtotal.subtract(discountAmount);

                long paymentId =
                        insertPayment(
                                connection,
                                orderId,
                                cashierId,
                                subtotal,
                                discountAmount,
                                totalAmount,
                                paymentMethod
                        );

                String invoiceCode =
                        createInvoiceCode(paymentId);

                insertInvoice(
                        connection,
                        paymentId,
                        invoiceCode
                );

                markOrderPaid(
                        connection,
                        orderId
                );

                completeReservation(
                        connection,
                        order.reservationId()
                );

                releaseTable(
                        connection,
                        order.tableId()
                );

                LocalDateTime paidAt =
                        findPaidAt(
                                connection,
                                paymentId
                        );

                connection.commit();

                return new PaymentReceipt(
                        paymentId,
                        invoiceCode,
                        orderId,
                        order.tableNumber(),
                        subtotal,
                        discountAmount,
                        totalAmount,
                        paymentMethod,
                        cashier.fullName(),
                        paidAt
                );

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

    private CashierData lockCashier(
            Connection connection,
            long cashierId
    ) throws SQLException {

        String sql = """
                SELECT full_name, role, active
                FROM users
                WHERE id = ?
                FOR UPDATE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, cashierId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "Nhân viên thanh toán không tồn tại"
                    );
                }

                if (!resultSet.getBoolean("active")) {
                    throw new IllegalStateException(
                            "Tài khoản nhân viên đã bị khóa"
                    );
                }

                String role =
                        resultSet.getString("role");

                if (!"MANAGER".equals(role)
                        && !"CASHIER".equals(role)) {
                    throw new IllegalStateException(
                            "Tài khoản không có quyền thanh toán"
                    );
                }

                return new CashierData(
                        resultSet.getString("full_name")
                );
            }
        }
    }

    private OrderPaymentData lockServedOrder(
            Connection connection,
            long orderId
    ) throws SQLException {

        String sql = """
                SELECT
                    o.table_id,
                    o.reservation_id,
                    o.status,
                    t.table_number
                FROM orders o
                JOIN restaurant_tables t
                    ON t.id = o.table_id
                WHERE o.id = ?
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

                if (!"SERVED".equals(status)) {
                    throw new IllegalStateException(
                            "Chỉ được thanh toán đơn đã phục vụ xong"
                    );
                }

                long reservationValue =
                        resultSet.getLong("reservation_id");

                Long reservationId =
                        resultSet.wasNull()
                                ? null
                                : reservationValue;

                return new OrderPaymentData(
                        resultSet.getLong("table_id"),
                        reservationId,
                        resultSet.getString("table_number")
                );
            }
        }
    }

    private void ensureNotPaid(
            Connection connection,
            long orderId
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM payments
                WHERE order_id = ?
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
                if (resultSet.next()) {
                    throw new IllegalStateException(
                            "Đơn gọi món đã được thanh toán"
                    );
                }
            }
        }
    }

    private BigDecimal calculateSubtotal(
            Connection connection,
            long orderId
    ) throws SQLException {

        String sql = """
                SELECT
                    COALESCE(
                        SUM(
                            CASE
                                WHEN kitchen_status <> 'CANCELLED'
                                THEN quantity * unit_price
                                ELSE 0
                            END
                        ),
                        0
                    ) AS subtotal,
                    SUM(
                        CASE
                            WHEN kitchen_status <> 'CANCELLED'
                            THEN 1
                            ELSE 0
                        END
                    ) AS active_count,
                    SUM(
                        CASE
                            WHEN kitchen_status NOT IN (
                                'SERVED',
                                'CANCELLED'
                            )
                            THEN 1
                            ELSE 0
                        END
                    ) AS unfinished_count
                FROM order_items
                WHERE order_id = ?
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
                resultSet.next();

                int activeCount =
                        resultSet.getInt("active_count");

                int unfinishedCount =
                        resultSet.getInt("unfinished_count");

                if (activeCount <= 0) {
                    throw new IllegalStateException(
                            "Đơn không có món hợp lệ để thanh toán"
                    );
                }

                if (unfinishedCount > 0) {
                    throw new IllegalStateException(
                            "Đơn vẫn còn món chưa phục vụ"
                    );
                }

                return resultSet.getBigDecimal("subtotal");
            }
        }
    }

    private void validateDiscount(
            BigDecimal subtotal,
            BigDecimal discountAmount
    ) {
        if (discountAmount == null) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá không được để trống"
            );
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá không được âm"
            );
        }

        if (discountAmount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá không được lớn hơn tạm tính"
            );
        }
    }

    private long insertPayment(
            Connection connection,
            long orderId,
            long cashierId,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            PaymentMethod paymentMethod
    ) throws SQLException {

        String sql = """
                INSERT INTO payments (
                    order_id,
                    cashier_id,
                    subtotal,
                    discount_amount,
                    total_amount,
                    payment_method
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setLong(1, orderId);
            statement.setLong(2, cashierId);
            statement.setBigDecimal(3, subtotal);
            statement.setBigDecimal(
                    4,
                    discountAmount
            );
            statement.setBigDecimal(
                    5,
                    totalAmount
            );
            statement.setString(
                    6,
                    paymentMethod.name()
            );

            statement.executeUpdate();

            try (
                    ResultSet generatedKeys =
                            statement.getGeneratedKeys()
            ) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Không lấy được mã thanh toán"
                    );
                }

                return generatedKeys.getLong(1);
            }
        }
    }

    private String createInvoiceCode(long paymentId) {
        return String.format(
                "HD%08d",
                paymentId
        );
    }

    private void insertInvoice(
            Connection connection,
            long paymentId,
            String invoiceCode
    ) throws SQLException {

        String sql = """
                INSERT INTO invoices (
                    payment_id,
                    invoice_code
                )
                VALUES (?, ?)
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, paymentId);
            statement.setString(2, invoiceCode);
            statement.executeUpdate();
        }
    }

    private void markOrderPaid(
            Connection connection,
            long orderId
    ) throws SQLException {

        String sql = """
                UPDATE orders
                SET status = 'PAID'
                WHERE id = ?
                  AND status = 'SERVED'
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, orderId);

            int updatedRows =
                    statement.executeUpdate();

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Không thể chuyển đơn sang đã thanh toán"
                );
            }
        }
    }

    private void completeReservation(
            Connection connection,
            Long reservationId
    ) throws SQLException {

        if (reservationId == null) {
            return;
        }

        String sql = """
                UPDATE reservations
                SET status = 'COMPLETED'
                WHERE id = ?
                  AND status = 'SEATED'
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, reservationId);
            statement.executeUpdate();
        }
    }

    private void releaseTable(
            Connection connection,
            long tableId
    ) throws SQLException {

        String sql = """
                UPDATE restaurant_tables
                SET status =
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM reservations r
                            WHERE r.table_id = ?
                              AND r.status = 'CONFIRMED'
                        )
                        THEN 'RESERVED'
                        ELSE 'AVAILABLE'
                    END
                WHERE id = ?
                  AND active = TRUE
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, tableId);
            statement.setLong(2, tableId);
            statement.executeUpdate();
        }
    }

    private LocalDateTime findPaidAt(
            Connection connection,
            long paymentId
    ) throws SQLException {

        String sql = """
                SELECT paid_at
                FROM payments
                WHERE id = ?
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, paymentId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Không tìm thấy giao dịch vừa tạo"
                    );
                }

                return resultSet
                        .getTimestamp("paid_at")
                        .toLocalDateTime();
            }
        }
    }

    private record CashierData(
            String fullName
    ) {
    }

    private record OrderPaymentData(
            long tableId,
            Long reservationId,
            String tableNumber
    ) {
    }
}