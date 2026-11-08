package com.restaurant.service;

import com.restaurant.dao.ReservationDAO;
import com.restaurant.model.Reservation;
import com.restaurant.model.ReservationStatus;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiểm thử đặt bàn gắn với bảng khách hàng (customers).
 * Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
class ReservationServiceIntegrationTest {

    private ReservationService service;
    private long receptionistId;

    @BeforeEach
    void setUp() throws SQLException {
        TestDatabase.requireTestDatabase();
        TestDatabase.resetBusinessData();
        service = new ReservationService(new ReservationDAO());
        receptionistId = TestDatabase.queryLong(
                "SELECT id FROM users WHERE username = 'receptionist'"
        );
    }

    // ---------- Hàm hỗ trợ ----------

    private long createTable(String number, int capacity)
            throws SQLException {
        TestDatabase.execute(
                "INSERT INTO restaurant_tables "
                        + "(table_number, capacity, status, active) VALUES ('"
                        + number + "', " + capacity + ", 'AVAILABLE', TRUE)"
        );
        return TestDatabase.queryLong(
                "SELECT id FROM restaurant_tables WHERE table_number = '"
                        + number + "'"
        );
    }

    private long book(long tableId, String name, String phone, int guests)
            throws SQLException {
        return service.createReservation(
                tableId,
                name,
                phone,
                LocalDateTime.now().plusDays(1),
                guests,
                null,
                receptionistId
        );
    }

    private long countCustomers() throws SQLException {
        return TestDatabase.queryLong("SELECT COUNT(*) FROM customers");
    }

    private String tableStatus(long tableId) throws SQLException {
        for (String status : new String[]{
                "AVAILABLE", "RESERVED", "OCCUPIED"}) {
            if (TestDatabase.queryLong(
                    "SELECT COUNT(*) FROM restaurant_tables WHERE id = "
                            + tableId + " AND status = '" + status + "'"
            ) == 1) {
                return status;
            }
        }
        return "?";
    }

    // ---------- Các trường hợp kiểm thử ----------

    @Test
    void datBanTaoKhachMoiVaDocLaiDuocTenSoDienThoai() throws SQLException {
        long tableId = createTable("R01", 4);

        long reservationId = book(tableId, "Nguyen Van A", "0901234567", 2);

        assertEquals(1, countCustomers());

        List<Reservation> all = service.getAllReservations();
        assertEquals(1, all.size());
        assertEquals(reservationId, all.get(0).getId());
        assertEquals("Nguyen Van A", all.get(0).getCustomerName());
        assertEquals("0901234567", all.get(0).getCustomerPhone());
        assertEquals("RESERVED", tableStatus(tableId));
    }

    @Test
    void cungSoDienThoaiVietKhacNhauDungLaiMotKhach() throws SQLException {
        long t1 = createTable("R02", 4);
        long t2 = createTable("R03", 4);

        book(t1, "Tran B", "0901 234 567", 2);
        book(t2, "Tran B", "0901-234-567", 2);

        assertEquals(1, countCustomers());
        assertEquals(
                1,
                TestDatabase.queryLong(
                        "SELECT COUNT(DISTINCT customer_id) FROM reservations"
                )
        );
    }

    @Test
    void tenKhachCapNhatTheoLanDatGanNhat() throws SQLException {
        long t1 = createTable("R04", 4);
        long t2 = createTable("R05", 4);

        book(t1, "Le C", "0912345678", 2);
        book(t2, "Le Thi C", "0912345678", 2);

        assertEquals(1, countCustomers());
        assertEquals(
                1,
                TestDatabase.queryLong(
                        "SELECT COUNT(*) FROM customers "
                                + "WHERE full_name = 'Le Thi C'"
                )
        );
    }

    @Test
    void khacSoDienThoaiTaoKhachKhac() throws SQLException {
        long t1 = createTable("R06", 4);
        long t2 = createTable("R07", 4);

        book(t1, "Khach 1", "0911111111", 2);
        book(t2, "Khach 2", "0922222222", 2);

        assertEquals(2, countCustomers());
    }

    @Test
    void chapNhanSoDienThoaiQuocTeCoDauCong() throws SQLException {
        long t1 = createTable("R08", 4);

        book(t1, "Khach QT", "+84 912 345 678", 2);

        assertEquals(
                1,
                TestDatabase.queryLong(
                        "SELECT COUNT(*) FROM customers "
                                + "WHERE phone = '+84912345678'"
                )
        );
    }

    @Test
    void tuChoiSoDienThoaiKhongHopLeVaKhongTaoGiCa() throws SQLException {
        long tableId = createTable("R09", 4);

        for (String badPhone : new String[]{
                "--------", "abcdefghij", "12345", "0901 abc 567",
                "0901234567890123456", null}) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> book(tableId, "Khach Xau", badPhone, 2),
                    "Phải từ chối số điện thoại: " + badPhone
            );
        }

        assertEquals(0, countCustomers());
        assertEquals("AVAILABLE", tableStatus(tableId));
    }

    @Test
    void datBanLoiPhaiHoanTacKhongTaoKhachMoi() throws SQLException {
        long tableId = createTable("R10", 2);

        // 5 khách vượt sức chứa 2 -> lỗi nghiệp vụ trong transaction
        assertThrows(
                IllegalArgumentException.class,
                () -> book(tableId, "Khach Dong", "0933333333", 5)
        );

        assertEquals(0, countCustomers());
        assertEquals(
                0,
                TestDatabase.queryLong("SELECT COUNT(*) FROM reservations")
        );
        assertEquals("AVAILABLE", tableStatus(tableId));
    }

    @Test
    void nhanBanDoiTrangThaiDungVaHuyTraBanVeTrong() throws SQLException {
        long t1 = createTable("R11", 4);
        long t2 = createTable("R12", 4);

        long seatId = book(t1, "Khach Nhan", "0944444444", 2);
        long cancelId = book(t2, "Khach Huy", "0955555555", 2);

        service.seatReservation(seatId);
        service.cancelReservation(cancelId);

        assertEquals("OCCUPIED", tableStatus(t1));
        assertEquals("AVAILABLE", tableStatus(t2));

        for (Reservation r : service.getAllReservations()) {
            if (r.getId() == seatId) {
                assertEquals(ReservationStatus.SEATED, r.getStatus());
            } else {
                assertEquals(ReservationStatus.CANCELLED, r.getStatus());
            }
        }

        // Khách vẫn còn trong danh bạ sau khi hủy
        assertEquals(2, countCustomers());
    }

    @Test
    void haiNguoiCungDatBangSoDienThoaiMoiChiTaoMotKhach()
            throws Exception {
        long t1 = createTable("R13", 4);
        long t2 = createTable("R14", 4);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Callable<Long> job1 = () -> {
                ready.countDown();
                go.await();
                return book(t1, "Song Song", "0966666666", 2);
            };
            Callable<Long> job2 = () -> {
                ready.countDown();
                go.await();
                return book(t2, "Song Song", "0966666666", 2);
            };

            Future<Long> f1 = pool.submit(job1);
            Future<Long> f2 = pool.submit(job2);

            ready.await();
            go.countDown();

            // Cả hai đều phải thành công (không lỗi trùng khóa)
            f1.get();
            f2.get();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, countCustomers());
        assertEquals(
                1,
                TestDatabase.queryLong(
                        "SELECT COUNT(DISTINCT customer_id) FROM reservations"
                )
        );
        assertEquals(
                2,
                TestDatabase.queryLong("SELECT COUNT(*) FROM reservations")
        );
    }

    @Test
    void chuanHoaSoDienThoai() {
        assertEquals("0901234567",
                ReservationService.normalizePhone(" 0901 234-567 "));
        assertEquals("+84912345678",
                ReservationService.normalizePhone("+84 912-345 678"));
        assertNull(ReservationService.normalizePhone("09a1"));
        assertNull(ReservationService.normalizePhone(null));
    }
}
