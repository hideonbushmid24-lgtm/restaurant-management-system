package com.restaurant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiểm thử cách đọc dữ liệu người dùng nhập (không cần database):
 * thời gian đặt bàn và tiền giảm giá.
 */
class InputParsingTest {

    // ---------- Thời gian đặt bàn ----------

    @Test
    void docDuocThoiGianHopLe() {
        assertEquals(
                LocalDateTime.of(2026, 11, 12, 19, 30),
                ReservationService.parseReservationTime(" 2026-11-12 19:30 ")
        );
    }

    @Test
    void chapNhanNgayCuoiThangVaNgayNhuan() {
        assertEquals(
                LocalDateTime.of(2028, 2, 29, 8, 0),
                ReservationService.parseReservationTime("2028-02-29 08:00")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-02-30 19:00",   // không có ngày 30/02 (trước đây bị đổi thành 28/02)
            "2026-02-29 19:00",   // 2026 không phải năm nhuận
            "2026-11-31 19:00",   // tháng 11 chỉ có 30 ngày
            "2026-04-31 19:00",
            "2026-11-12 24:00",   // trước đây bị đẩy sang 00:00 hôm sau
            "2026-11-12 25:00",
            "2026-13-01 19:00",
            "2026-11-12",
            "12/11/2026 19:00",
            "abc"
    })
    void tuChoiThoiGianKhongTonTaiHoacSaiDinhDang(String input) {
        IllegalArgumentException loi = assertThrows(
                IllegalArgumentException.class,
                () -> ReservationService.parseReservationTime(input)
        );

        assertEquals(true, loi.getMessage().contains("yyyy-MM-dd HH:mm"));
    }

    @Test
    void tuChoiThoiGianTrong() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReservationService.parseReservationTime("   ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ReservationService.parseReservationTime(null)
        );
    }

    // ---------- Tiền giảm giá ----------

    @Test
    void giamGiaTrongNghiaLaKhong() {
        assertEquals(
                BigDecimal.ZERO,
                PaymentService.parseDiscountInput("")
        );
        assertEquals(
                BigDecimal.ZERO,
                PaymentService.parseDiscountInput(null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"50000", "50.000", "50,000", " 50 000 "})
    void docDuocSoNguyenCoHoacKhongCoPhanCachHangNghin(String input) {
        assertEquals(
                new BigDecimal("50000"),
                PaymentService.parseDiscountInput(input)
        );
    }

    @Test
    void docDuocSoLonNhieuNhomPhanCach() {
        assertEquals(
                new BigDecimal("1250000"),
                PaymentService.parseDiscountInput("1.250.000")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1,5",       // trước đây thành 15
            "50.5",      // trước đây thành 505
            "100.50",    // trước đây thành 10050
            "50.000,50", // trước đây thành 5000050
            "1.00",
            "12.34.567",
            "abc",
            "5 000 đ"
    })
    void tuChoiSoKhongPhaiSoNguyenVnd(String input) {
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentService.parseDiscountInput(input)
        );
    }

    @Test
    void soAmVanDuocNhanDeBaoLoiKhongDuocAmOBuocSau() {
        assertEquals(
                new BigDecimal("-1000"),
                PaymentService.parseDiscountInput("-1000")
        );
    }
}
