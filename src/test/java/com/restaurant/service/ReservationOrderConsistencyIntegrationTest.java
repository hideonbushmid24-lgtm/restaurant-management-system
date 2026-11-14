package com.restaurant.service;

import com.restaurant.dao.ReservationDAO;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm thử tính nhất quán giữa đặt bàn, trạng thái bàn và đơn gọi món.
 * Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
class ReservationOrderConsistencyIntegrationTest extends BusinessTestBase {

    private ReservationService reservationService;
    private long receptionistId;

    @BeforeEach
    void chuanBiDatBan() throws SQLException {
        reservationService = new ReservationService(new ReservationDAO());
        receptionistId = userId("receptionist");
    }

    private long datBan(long banId) throws SQLException {
        return reservationService.createReservation(
                banId,
                "Nguyễn Văn An",
                "0901234567",
                LocalDateTime.now().plusHours(2),
                2,
                null,
                receptionistId
        );
    }

    private String trangThaiDatBan(long datBanId) throws SQLException {
        return TestDatabase.queryString(
                "SELECT status FROM reservations WHERE id = " + datBanId
        );
    }

    private void epTrangThaiBan(long banId, String trangThai)
            throws SQLException {
        TestDatabase.execute(
                "UPDATE restaurant_tables SET status = '" + trangThai
                        + "' WHERE id = " + banId
        );
    }

    @Test
    void taoDonVangLaiTrenBanDaDatBiTuChoi() throws SQLException {
        long ban = taoBan("B01", 4);
        long datBan = datBan(ban);

        assertLoi(
                "Khách chưa nhận bàn",
                () -> orderService.createOrder(ban, null, waiterId, null)
        );

        assertEquals("RESERVED", trangThaiBan(ban));
        assertEquals("CONFIRMED", trangThaiDatBan(datBan));
        assertEquals(0, dem("orders"));
    }

    @Test
    void sauNhanBanVanGoiMonVaThanhToanBinhThuong() throws SQLException {
        long ban = taoBan("B01", 4);
        long mon = taoMon("Mì xào", "79000");
        long datBan = datBan(ban);

        reservationService.seatReservation(datBan);
        assertEquals("OCCUPIED", trangThaiBan(ban));
        assertEquals("SEATED", trangThaiDatBan(datBan));

        long don = orderService.createOrder(ban, datBan, waiterId, null);
        themMon(don, mon, 1);
        phucVuHetMon(don);

        paymentService.pay(
                don,
                cashierId,
                java.math.BigDecimal.ZERO,
                com.restaurant.model.PaymentMethod.CASH
        );

        assertEquals("AVAILABLE", trangThaiBan(ban));
        assertEquals("COMPLETED", trangThaiDatBan(datBan));
    }

    @Test
    void nhanBanKhiBanDangDuocDungBiTuChoiVaKhongDoiTrangThai()
            throws SQLException {
        long ban = taoBan("B01", 4);
        long datBan = datBan(ban);
        epTrangThaiBan(ban, "OCCUPIED");

        assertLoi(
                "chưa thể nhận bàn",
                () -> reservationService.seatReservation(datBan)
        );

        assertEquals("OCCUPIED", trangThaiBan(ban));
        assertEquals("CONFIRMED", trangThaiDatBan(datBan));
    }

    @Test
    void huyDatBanKhongTraBanDangDuocDungVeTrong() throws SQLException {
        long ban = taoBan("B01", 4);
        long datBan = datBan(ban);
        epTrangThaiBan(ban, "OCCUPIED");

        reservationService.cancelReservation(datBan);

        assertEquals("CANCELLED", trangThaiDatBan(datBan));
        assertEquals("OCCUPIED", trangThaiBan(ban));
    }

    @Test
    void huyDatBanBinhThuongVanTraBanVeTrong() throws SQLException {
        long ban = taoBan("B01", 4);
        long datBan = datBan(ban);

        reservationService.cancelReservation(datBan);

        assertEquals("CANCELLED", trangThaiDatBan(datBan));
        assertEquals("AVAILABLE", trangThaiBan(ban));
    }
}
