package com.restaurant.model;

import java.time.LocalDateTime;

public class Reservation {

    private final long id;
    private final long tableId;
    private final String tableNumber;
    private final String customerName;
    private final String customerPhone;
    private final LocalDateTime reservationTime;
    private final int guestCount;
    private final String note;
    private final ReservationStatus status;

    public Reservation(
            long id,
            long tableId,
            String tableNumber,
            String customerName,
            String customerPhone,
            LocalDateTime reservationTime,
            int guestCount,
            String note,
            ReservationStatus status
    ) {
        this.id = id;
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.reservationTime = reservationTime;
        this.guestCount = guestCount;
        this.note = note;
        this.status = status;
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

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public String getNote() {
        return note;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}