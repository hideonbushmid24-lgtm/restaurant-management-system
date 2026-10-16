package com.restaurant.model;

public class RestaurantTable {

    private final long id;
    private final String tableNumber;
    private final int capacity;
    private final TableStatus status;
    private final boolean active;

    public RestaurantTable(
            long id,
            String tableNumber,
            int capacity,
            TableStatus status,
            boolean active
    ) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.status = status;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}