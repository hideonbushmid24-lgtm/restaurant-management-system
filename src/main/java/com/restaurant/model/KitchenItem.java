package com.restaurant.model;

import java.time.LocalDateTime;

public class KitchenItem {

    private final long id;
    private final long orderId;
    private final String tableNumber;
    private final String itemName;
    private final int quantity;
    private final String note;
    private final KitchenStatus status;
    private final LocalDateTime createdAt;

    public KitchenItem(
            long id,
            long orderId,
            String tableNumber,
            String itemName,
            int quantity,
            String note,
            KitchenStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.itemName = itemName;
        this.quantity = quantity;
        this.note = note;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public KitchenStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}