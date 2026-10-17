package com.restaurant.service;

import com.restaurant.dao.OrderDAO;
import com.restaurant.model.OrderItem;
import com.restaurant.model.RestaurantOrder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class OrderService {

    private final OrderDAO orderDAO;

    public OrderService(OrderDAO orderDAO) {
        if (orderDAO == null) {
            throw new IllegalArgumentException(
                    "OrderDAO không được null"
            );
        }

        this.orderDAO = orderDAO;
    }

    public long createOrder(
            long tableId,
            Long reservationId,
            long waiterId,
            String note
    ) throws SQLException {

        if (tableId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bàn không hợp lệ"
            );
        }

        if (waiterId <= 0) {
            throw new IllegalArgumentException(
                    "Mã nhân viên không hợp lệ"
            );
        }

        if (reservationId != null
                && reservationId <= 0) {

            throw new IllegalArgumentException(
                    "Mã đặt bàn không hợp lệ"
            );
        }

        String normalizedNote =
                normalizeNote(note);

        return orderDAO.create(
                tableId,
                reservationId,
                waiterId,
                normalizedNote
        );
    }

    public long addItem(
            long orderId,
            long menuItemId,
            int quantity,
            String note
    ) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đơn gọi món không hợp lệ"
            );
        }

        if (menuItemId <= 0) {
            throw new IllegalArgumentException(
                    "Mã món ăn không hợp lệ"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng món phải lớn hơn 0"
            );
        }

        if (quantity > 100) {
            throw new IllegalArgumentException(
                    "Số lượng một món không được vượt quá 100"
            );
        }

        String normalizedNote =
                normalizeNote(note);

        return orderDAO.addItem(
                orderId,
                menuItemId,
                quantity,
                normalizedNote
        );
    }

    public RestaurantOrder getActiveOrderByTable(
            long tableId
    ) throws SQLException {

        if (tableId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bàn không hợp lệ"
            );
        }

        return orderDAO.findActiveByTableId(
                tableId
        );
    }

    public List<OrderItem> getOrderItems(
            long orderId
    ) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đơn gọi món không hợp lệ"
            );
        }

        return orderDAO.findItemsByOrderId(
                orderId
        );
    }

    public BigDecimal getOrderTotal(
            long orderId
    ) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đơn gọi món không hợp lệ"
            );
        }

        return orderDAO.calculateTotal(
                orderId
        );
    }
    public void updateItemQuantity(
        long orderItemId,
        int quantity
) throws SQLException {

    if (orderItemId <= 0) {
        throw new IllegalArgumentException(
                "Mã món trong đơn không hợp lệ"
        );
    }

    if (quantity <= 0) {
        throw new IllegalArgumentException(
                "Số lượng món phải lớn hơn 0"
        );
    }

    if (quantity > 100) {
        throw new IllegalArgumentException(
                "Số lượng một món không được vượt quá 100"
        );
    }

    orderDAO.updateItemQuantity(
            orderItemId,
            quantity
    );
}

public void cancelItem(
        long orderItemId
) throws SQLException {

    if (orderItemId <= 0) {
        throw new IllegalArgumentException(
                "Mã món trong đơn không hợp lệ"
        );
    }

    orderDAO.cancelItem(orderItemId);
}
public Long getSeatedReservationIdByTable(
        long tableId
) throws SQLException {

    if (tableId <= 0) {
        throw new IllegalArgumentException(
                "Mã bàn không hợp lệ"
        );
    }

    return orderDAO
            .findSeatedReservationIdByTableId(
                    tableId
            );
}
    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }

        String normalized = note.trim();

        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "Ghi chú không được vượt quá 500 ký tự"
            );
        }

        return normalized;
    }
}