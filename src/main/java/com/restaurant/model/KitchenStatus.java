package com.restaurant.model;

public enum KitchenStatus {

    PENDING("Chờ chế biến"),
    PREPARING("Đang chế biến"),
    READY("Đã hoàn thành"),
    SERVED("Đã phục vụ"),
    CANCELLED("Đã hủy");

    private final String displayName;

    KitchenStatus(String displayName) {
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