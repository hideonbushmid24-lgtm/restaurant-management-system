package com.restaurant.service;

import com.restaurant.model.PaymentMethod;
import com.restaurant.model.PaymentReceipt;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử thanh toán: giảm giá, trạng thái đơn, thanh toán hai lần,
 * đồng thời, phân quyền, hoàn tác khi lỗi giữa chừng.
 * Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
class PaymentIntegrationTest extends BusinessTestBase {

    private static BigDecimal tien(String so) {
        return new BigDecimal(so);
    }

    private static void assertTien(String mongDoi, BigDecimal thucTe) {
        assertEquals(
                0,
                tien(mongDoi).compareTo(thucTe),
                "Mong đợi " + mongDoi + " nhưng là " + thucTe
        );
    }

    // ---------- Thanh toán đúng ----------

    @Test
    void thanhToanDonDaPhucVuTaoPhieuHoaDonVaTraBan() throws SQLException {
        long banId = taoBan("P01", 4);
        long monId = taoMon("Mon P01", "50000");
        long donId = donDaPhucVu(banId, monId, 3);

        PaymentReceipt receipt = paymentService.pay(
                donId, cashierId, tien("20000"), PaymentMethod.CASH
        );

        assertTien("150000", receipt.getSubtotal());
        assertTien("20000", receipt.getDiscountAmount());
        assertTien("130000", receipt.getTotalAmount());
        assertNotNull(receipt.getInvoiceCode());

        assertEquals("PAID", trangThaiDon(donId));
        assertEquals("AVAILABLE", trangThaiBan(banId));
        assertEquals(1, dem("payments"));
        assertEquals(1, dem("invoices"));
    }

    @Test
    void thanhToanKhongTinhMonDaHuy() throws SQLException {
        long banId = taoBan("P02", 4);
        long mon1 = taoMon("Mon P02a", "30000");
        long mon2 = taoMon("Mon P02b", "70000");
        long donId = taoDon(banId);
        themMon(donId, mon1, 1);
        long itemHuy = themMon(donId, mon2, 1);
        orderService.cancelItem(itemHuy);
        phucVuHetMon(donId);

        PaymentReceipt receipt = paymentService.pay(
                donId, cashierId, BigDecimal.ZERO, PaymentMethod.CARD
        );

        assertTien("30000", receipt.getSubtotal());
        assertTien("30000", receipt.getTotalAmount());
    }

    // ---------- Giảm giá ----------

    @Test
    void giamGiaVuotTamTinhBiTuChoiVaKhongGhiGi() throws SQLException {
        long banId = taoBan("P03", 4);
        long monId = taoMon("Mon P03", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        assertLoi(
                "",
                () -> paymentService.pay(
                        donId, cashierId, tien("50000.01"),
                        PaymentMethod.CASH
                )
        );

        assertEquals(0, dem("payments"));
        assertEquals("SERVED", trangThaiDon(donId));
        assertEquals("OCCUPIED", trangThaiBan(banId));
    }

    @Test
    void giamGiaBangTamTinhChoPhepTongTienBangKhong() throws SQLException {
        long banId = taoBan("P04", 4);
        long monId = taoMon("Mon P04", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        PaymentReceipt receipt = paymentService.pay(
                donId, cashierId, tien("50000"), PaymentMethod.CASH
        );

        assertTien("0", receipt.getTotalAmount());
    }

    @Test
    void giamGiaAmHoacQuaHaiChuSoThapPhanBiTuChoi() throws SQLException {
        long banId = taoBan("P05", 4);
        long monId = taoMon("Mon P05", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        assertLoi(
                "không được âm",
                () -> paymentService.pay(
                        donId, cashierId, tien("-1"), PaymentMethod.CASH
                )
        );
        assertLoi(
                "tối đa 2 chữ số thập phân",
                () -> paymentService.pay(
                        donId, cashierId, tien("10.123"), PaymentMethod.CASH
                )
        );
        assertEquals(0, dem("payments"));
    }

    // ---------- Trạng thái đơn ----------

    @Test
    void donChuaPhucVuXongKhongThanhToanDuoc() throws SQLException {
        long banId = taoBan("P06", 4);
        long monId = taoMon("Mon P06", "50000");
        long donId = taoDon(banId);
        themMon(donId, monId, 1);

        // Đơn đang mở
        assertLoi(
                "phục vụ xong",
                () -> paymentService.pay(
                        donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
                )
        );

        // Đơn đã gửi bếp
        orderService.sendToKitchen(donId);
        assertLoi(
                "phục vụ xong",
                () -> paymentService.pay(
                        donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
                )
        );

        assertEquals(0, dem("payments"));
    }

    @Test
    void thanhToanHaiLanChiGhiMotLan() throws SQLException {
        long banId = taoBan("P07", 4);
        long monId = taoMon("Mon P07", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        paymentService.pay(
                donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
        );

        assertLoi(
                "",
                () -> paymentService.pay(
                        donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
                )
        );

        assertEquals(1, dem("payments"));
        assertEquals(1, dem("invoices"));
    }

    @Test
    void haiThuNganCungThanhToanMotDonChiThanhCongMot() throws Exception {
        long banId = taoBan("P08", 4);
        long monId = taoMon("Mon P08", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        List<Callable<?>> jobs = List.of(
                () -> paymentService.pay(
                        donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
                ),
                () -> paymentService.pay(
                        donId, managerId, BigDecimal.ZERO, PaymentMethod.CARD
                )
        );

        List<Boolean> ketQua = chayDongThoi(jobs);

        assertEquals(
                1,
                ketQua.stream().filter(ok -> ok).count(),
                "Chỉ một lần thanh toán được ghi nhận"
        );
        assertEquals(1, dem("payments"));
        assertEquals(1, dem("invoices"));
        assertEquals("PAID", trangThaiDon(donId));
    }

    // ---------- Phân quyền ----------

    @Test
    void chiQuanLyVaThuNganDuocThanhToan() throws SQLException {
        long banId = taoBan("P09", 4);
        long monId = taoMon("Mon P09", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        assertLoi(
                "không có quyền thanh toán",
                () -> paymentService.pay(
                        donId, waiterId, BigDecimal.ZERO, PaymentMethod.CASH
                )
        );
        assertEquals(0, dem("payments"));

        PaymentReceipt receipt = paymentService.pay(
                donId, managerId, BigDecimal.ZERO, PaymentMethod.CASH
        );
        assertNotNull(receipt.getInvoiceCode());
    }

    // ---------- Đặt bàn liên quan ----------

    private long taoLuotDat(long banId, String trangThai, int stt)
            throws SQLException {
        long khachId = TestDatabase.createCustomer(
                "Khach P " + stt, "09100000" + (10 + stt)
        );
        TestDatabase.execute(
                "INSERT INTO reservations (table_id, customer_id, "
                        + "reservation_time, guest_count, status, "
                        + "created_by) VALUES (" + banId + ", " + khachId
                        + ", NOW(), 2, '" + trangThai + "', "
                        + "(SELECT id FROM users "
                        + "WHERE username = 'receptionist'))"
        );
        return TestDatabase.queryLong(
                "SELECT MAX(id) FROM reservations"
        );
    }

    @Test
    void thanhToanHoanTatLuotDatVaTraBanVeTrong() throws SQLException {
        long banId = taoBan("P10", 4);
        TestDatabase.execute(
                "UPDATE restaurant_tables SET status = 'RESERVED' "
                        + "WHERE id = " + banId
        );
        long datBanId = taoLuotDat(banId, "CONFIRMED", 1);
        long monId = taoMon("Mon P10", "50000");

        long donId = orderService.createOrder(
                banId, datBanId, waiterId, null
        );
        themMon(donId, monId, 1);
        phucVuHetMon(donId);

        assertEquals(
                "SEATED",
                TestDatabase.queryString(
                        "SELECT status FROM reservations WHERE id = "
                                + datBanId
                )
        );

        paymentService.pay(
                donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
        );

        assertEquals(
                "COMPLETED",
                TestDatabase.queryString(
                        "SELECT status FROM reservations WHERE id = "
                                + datBanId
                )
        );
        assertEquals("AVAILABLE", trangThaiBan(banId));
    }

    @Test
    void thanhToanGiuBanLaDaDatKhiConLuotDatKhacDangHieuLuc()
            throws SQLException {
        long banId = taoBan("P11", 4);
        TestDatabase.execute(
                "UPDATE restaurant_tables SET status = 'RESERVED' "
                        + "WHERE id = " + banId
        );
        long datBanId = taoLuotDat(banId, "CONFIRMED", 2);
        taoLuotDat(banId, "CONFIRMED", 3); // lượt đặt khác cùng bàn
        long monId = taoMon("Mon P11", "50000");

        long donId = orderService.createOrder(
                banId, datBanId, waiterId, null
        );
        themMon(donId, monId, 1);
        phucVuHetMon(donId);

        paymentService.pay(
                donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
        );

        assertEquals("RESERVED", trangThaiBan(banId));
    }

    // ---------- Transaction ----------

    @Test
    void loiGiuaChungPhaiHoanTacToanBoThanhToan() throws SQLException {
        long banId = taoBan("P12", 4);
        long monId = taoMon("Mon P12", "50000");
        long donId = donDaPhucVu(banId, monId, 1);

        /*
         * Giả lập lỗi ở bước cuối (trả bàn): tạo trigger làm mọi lệnh
         * UPDATE bảng restaurant_tables đều bị lỗi. Khi đó phiếu thanh
         * toán, hóa đơn và việc đổi trạng thái đơn đã làm trước đó
         * đều phải được hoàn tác.
         */
        TestDatabase.execute("DROP TRIGGER IF EXISTS trg_test_loi_ban");
        TestDatabase.execute(
                "CREATE TRIGGER trg_test_loi_ban "
                        + "BEFORE UPDATE ON restaurant_tables "
                        + "FOR EACH ROW SIGNAL SQLSTATE '45000' "
                        + "SET MESSAGE_TEXT = 'Loi gia lap'"
        );

        try {
            assertLoi(
                    "",
                    () -> paymentService.pay(
                            donId, cashierId, BigDecimal.ZERO,
                            PaymentMethod.CASH
                    )
            );
        } finally {
            TestDatabase.execute(
                    "DROP TRIGGER IF EXISTS trg_test_loi_ban"
            );
        }

        assertEquals(0, dem("payments"), "Phiếu thanh toán phải bị hoàn tác");
        assertEquals(0, dem("invoices"), "Hóa đơn phải bị hoàn tác");
        assertEquals("SERVED", trangThaiDon(donId));
        assertEquals("OCCUPIED", trangThaiBan(banId));

        // Sau khi sự cố hết, thanh toán lại bình thường
        paymentService.pay(
                donId, cashierId, BigDecimal.ZERO, PaymentMethod.CASH
        );
        assertEquals("PAID", trangThaiDon(donId));
    }

    @Test
    void danhSachCanThanhToanChiGomDonDaPhucVu() throws SQLException {
        long ban1 = taoBan("P13", 4);
        long ban2 = taoBan("P14", 4);
        long monId = taoMon("Mon P13", "50000");
        long donXong = donDaPhucVu(ban1, monId, 1);
        long donMo = taoDon(ban2);
        themMon(donMo, monId, 1);

        var danhSach = paymentService.getPayableOrders();

        assertTrue(
                danhSach.stream().anyMatch(o -> o.getOrderId() == donXong)
        );
        assertTrue(
                danhSach.stream().noneMatch(o -> o.getOrderId() == donMo),
                "Đơn đang mở không được xuất hiện trong danh sách thanh toán"
        );
    }
}
