package com.restaurant.model;

import java.time.LocalDateTime;

public class RestaurantOrder {

    private final long id;
    private final long tableId;
    private final String tableNumber;
    private final Long reservationId;
    private final long waiterId;
    private final String waiterName;
    private final OrderStatus status;
    private final String note;
    private final LocalDateTime createdAt;

    public RestaurantOrder(
            long id,
            long tableId,
            String tableNumber,
            Long reservationId,
            long waiterId,
            String waiterName,
            OrderStatus status,
            String note,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.reservationId = reservationId;
        this.waiterId = waiterId;
        this.waiterName = waiterName;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getTableId() {
        return tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public long getWaiterId() {
        return waiterId;
    }

    public String getWaiterName() {
        return waiterName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}