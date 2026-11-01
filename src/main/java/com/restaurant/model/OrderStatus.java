package com.restaurant.model;

public enum OrderStatus {

    OPEN("Đang gọi món"),
    IN_KITCHEN("Đang chế biến"),
    READY("Sẵn sàng phục vụ"),
    SERVED("Đã phục vụ"),
    PAID("Đã thanh toán"),
    CANCELLED("Đã hủy");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}