package com.restaurant.service;

import com.restaurant.dao.ReservationDAO;
import com.restaurant.model.Reservation;
import com.restaurant.model.RestaurantTable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {

    private final ReservationDAO reservationDAO;

    public ReservationService(
            ReservationDAO reservationDAO
    ) {
        if (reservationDAO == null) {
            throw new IllegalArgumentException(
                    "ReservationDAO không được null"
            );
        }

        this.reservationDAO = reservationDAO;
    }

    public List<Reservation> getAllReservations()
            throws SQLException {

        return reservationDAO.findAll();
    }

    public List<RestaurantTable> getAvailableTables()
            throws SQLException {

        return reservationDAO.findAvailableTables();
    }

    /**
     * Chuẩn hóa số điện thoại: bỏ khoảng trắng và dấu gạch ngang.
     * Ví dụ "0901 234-567" -> "0901234567".
     *
     * @return chuỗi đã chuẩn hóa, hoặc null nếu đầu vào null hoặc
     *         chứa ký tự không được phép
     */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null
                || !rawPhone.trim().matches("[0-9+ -]+")) {
            return null;
        }

        return rawPhone.trim().replaceAll("[ -]", "");
    }

    public long createReservation(
            long tableId,
            String customerName,
            String customerPhone,
            LocalDateTime reservationTime,
            int guestCount,
            String note,
            long createdBy
    ) throws SQLException {

        if (tableId <= 0) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn bàn"
            );
        }

        if (customerName == null
                || customerName.isBlank()) {

            throw new IllegalArgumentException(
                    "Tên khách hàng không được để trống"
            );
        }

        if (customerName.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Tên khách hàng quá dài"
            );
        }

        /*
         * Số điện thoại là định danh của khách hàng nên phải chuẩn hóa
         * (bỏ khoảng trắng và dấu -) trước khi kiểm tra và lưu.
         */
        String normalizedPhone =
                normalizePhone(customerPhone);

        if (normalizedPhone == null
                || !normalizedPhone.matches("\\+?[0-9]{8,15}")) {

            throw new IllegalArgumentException(
                    "Số điện thoại không hợp lệ "
                            + "(8-15 chữ số, có thể bắt đầu bằng +)"
            );
        }

        if (reservationTime == null
                || reservationTime.isBefore(
                        LocalDateTime.now()
                                .minusMinutes(1)
                )) {

            throw new IllegalArgumentException(
                    "Thời gian đặt bàn không hợp lệ"
            );
        }

        if (guestCount <= 0 || guestCount > 50) {
            throw new IllegalArgumentException(
                    "Số khách phải từ 1 đến 50"
            );
        }

        if (createdBy <= 0) {
            throw new IllegalArgumentException(
                    "Nhân viên tạo đặt bàn không hợp lệ"
            );
        }

        String normalizedNote =
                note == null
                        ? null
                        : note.trim();

        if (normalizedNote != null
                && normalizedNote.length() > 500) {

            throw new IllegalArgumentException(
                    "Ghi chú không được quá 500 ký tự"
            );
        }

        return reservationDAO.createReservation(
                tableId,
                customerName.trim(),
                normalizedPhone,
                reservationTime,
                guestCount,
                normalizedNote,
                createdBy
        );
    }

    public void seatReservation(long reservationId)
            throws SQLException {

        if (reservationId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đặt bàn không hợp lệ"
            );
        }

        reservationDAO.seatReservation(
                reservationId
        );
    }

    public void cancelReservation(long reservationId)
            throws SQLException {

        if (reservationId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đặt bàn không hợp lệ"
            );
        }

        reservationDAO.cancelReservation(
                reservationId
        );
    }
}