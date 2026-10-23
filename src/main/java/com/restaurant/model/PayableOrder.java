package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayableOrder {

    private final long orderId;
    private final long tableId;
    private final String tableNumber;
    private final String waiterName;
    private final LocalDateTime createdAt;
    private final BigDecimal subtotal;

    public PayableOrder(
            long orderId,
            long tableId,
            String tableNumber,
            String waiterName,
            LocalDateTime createdAt,
            BigDecimal subtotal
    ) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.waiterName = waiterName;
        this.createdAt = createdAt;
        this.subtotal = subtotal;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getTableId() {
        return tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getWaiterName() {
        return waiterName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}