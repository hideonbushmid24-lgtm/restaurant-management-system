package com.restaurant.model;

public enum ReservationStatus {

    CONFIRMED("Đã xác nhận"),
    SEATED("Khách đã vào bàn"),
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã hủy");

    private final String displayName;

    ReservationStatus(String displayName) {
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