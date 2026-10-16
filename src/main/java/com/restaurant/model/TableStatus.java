package com.restaurant.model;

public enum TableStatus {

    AVAILABLE("Trống"),
    RESERVED("Đã đặt"),
    OCCUPIED("Đang sử dụng"),
    INACTIVE("Ngừng hoạt động");

    private final String displayName;

    TableStatus(String displayName) {
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