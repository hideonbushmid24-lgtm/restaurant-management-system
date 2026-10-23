package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.PaymentMethod;
import com.restaurant.model.RevenueRecord;
import com.restaurant.model.RevenueSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RevenueReportDAO {

    private static final String SUMMARY_SQL = """
            SELECT
                COUNT(*) AS invoice_count,
                COALESCE(SUM(p.subtotal), 0) AS subtotal,
                COALESCE(SUM(p.discount_amount), 0) AS discount_amount,
                COALESCE(SUM(p.total_amount), 0) AS total_amount
            FROM payments p
            WHERE p.paid_at >= ?
              AND p.paid_at < ?
            """;

    private static final String RECORDS_SQL = """
            SELECT
                p.id AS payment_id,
                i.invoice_code,
                p.order_id,
                t.table_number,
                u.full_name AS cashier_name,
                p.subtotal,
                p.discount_amount,
                p.total_amount,
                p.payment_method,
                p.paid_at
            FROM payments p
            JOIN invoices i
                ON i.payment_id = p.id
            JOIN orders o
                ON o.id = p.order_id
            JOIN restaurant_tables t
                ON t.id = o.table_id
            JOIN users u
                ON u.id = p.cashier_id
            WHERE p.paid_at >= ?
              AND p.paid_at < ?
            ORDER BY p.paid_at DESC, p.id DESC
            """;

    public RevenueSummary getSummary(
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate.plusDays(1).atStartOfDay();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(SUMMARY_SQL)
        ) {
            setDateRange(
                    statement,
                    startDateTime,
                    endDateTime
            );

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new RevenueSummary(
                            resultSet.getLong("invoice_count"),
                            getAmount(resultSet, "subtotal"),
                            getAmount(
                                    resultSet,
                                    "discount_amount"
                            ),
                            getAmount(resultSet, "total_amount")
                    );
                }
            }
        }

        return new RevenueSummary(
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    public List<RevenueRecord> findRecords(
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate.plusDays(1).atStartOfDay();

        List<RevenueRecord> records =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(RECORDS_SQL)
        ) {
            setDateRange(
                    statement,
                    startDateTime,
                    endDateTime
            );

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
            }
        }

        return records;
    }

    private void setDateRange(
            PreparedStatement statement,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) throws SQLException {
        statement.setTimestamp(
                1,
                Timestamp.valueOf(startDateTime)
        );

        statement.setTimestamp(
                2,
                Timestamp.valueOf(endDateTime)
        );
    }

    private RevenueRecord mapRecord(
            ResultSet resultSet
    ) throws SQLException {
        return new RevenueRecord(
                resultSet.getLong("payment_id"),
                resultSet.getString("invoice_code"),
                resultSet.getLong("order_id"),
                resultSet.getString("table_number"),
                resultSet.getString("cashier_name"),
                getAmount(resultSet, "subtotal"),
                getAmount(resultSet, "discount_amount"),
                getAmount(resultSet, "total_amount"),
                PaymentMethod.valueOf(
                        resultSet.getString("payment_method")
                ),
                resultSet
                        .getTimestamp("paid_at")
                        .toLocalDateTime()
        );
    }

    private BigDecimal getAmount(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        BigDecimal amount =
                resultSet.getBigDecimal(columnName);

        return amount == null
                ? BigDecimal.ZERO
                : amount;
    }
}