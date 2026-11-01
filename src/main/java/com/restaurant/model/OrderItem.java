package com.restaurant.model;

import java.math.BigDecimal;

public class OrderItem {

    private final long id;
    private final long orderId;
    private final long menuItemId;
    private final String itemName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final String note;
    private final KitchenStatus kitchenStatus;

    public OrderItem(
            long id,
            long orderId,
            long menuItemId,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            String note,
            KitchenStatus kitchenStatus
    ) {
        this.id = id;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
        this.kitchenStatus = kitchenStatus;
    }

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getNote() {
        return note;
    }

    public KitchenStatus getKitchenStatus() {
        return kitchenStatus;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(
                BigDecimal.valueOf(quantity)
        );
    }
}