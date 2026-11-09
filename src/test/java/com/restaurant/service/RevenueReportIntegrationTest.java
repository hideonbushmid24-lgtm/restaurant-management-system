package com.restaurant.service;

import com.restaurant.model.PaymentMethod;
import com.restaurant.model.PaymentReceipt;
import com.restaurant.model.RevenueRecord;
import com.restaurant.model.RevenueSummary;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiểm thử báo cáo doanh thu: tổng tiền khớp thanh toán, khoảng ngày
 * rỗng/sai, ranh giới ngày. Chỉ chạy khi có -Ddb.url trỏ tới *_test.
 */
class RevenueReportIntegrationTest extends BusinessTestBase {

    private static void assertTien(String mongDoi, BigDecimal thucTe) {
        assertEquals(
                0,
                new BigDecimal(mongDoi).compareTo(thucTe),
                "Mong đợi " + mongDoi + " nhưng là " + thucTe
        );
    }

    /** Tạo và thanh toán một đơn, trả về phiếu thanh toán. */
    private PaymentReceipt thanhToan(
            String soBan, String giaMon, int soLuong, String giamGia
    ) throws SQLException {
        long banId = taoBan(soBan, 4);
        long monId = taoMon("Mon " + soBan, giaMon);
        long donId = donDaPhucVu(banId, monId, soLuong);

        return paymentService.pay(
                donId,
                cashierId,
                new BigDecimal(giamGia),
                PaymentMethod.CASH
        );
    }

    private void datNgayThanhToan(long paymentId, String thoiDiem)
            throws SQLException {
        TestDatabase.execute(
                "UPDATE payments SET paid_at = '" + thoiDiem
                        + "' WHERE id = " + paymentId
        );
    }

    @Test
    void tongDoanhThuKhopTungLanThanhToan() throws SQLException {
        thanhToan("R01", "50000", 2, "0");      // 100.000 - 0
        thanhToan("R02", "30000", 1, "5000");   // 30.000 - 5.000
        thanhToan("R03", "120000", 1, "20000"); // 120.000 - 20.000

        LocalDate homNay = LocalDate.now();
        RevenueSummary tong = reportService.getSummary(homNay, homNay);
        List<RevenueRecord> chiTiet =
                reportService.findRecords(homNay, homNay);

        assertEquals(3, tong.invoiceCount());
        assertTien("250000", tong.subtotal());
        assertTien("25000", tong.discountAmount());
        assertTien("225000", tong.totalAmount());

        // Tổng hợp phải bằng cộng dồn các dòng chi tiết
        assertEquals(3, chiTiet.size());
        BigDecimal cong = chiTiet.stream()
                .map(RevenueRecord::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, cong.compareTo(tong.totalAmount()));

        // Doanh thu thực nhận = tạm tính - giảm giá
        assertEquals(
                0,
                tong.subtotal().subtract(tong.discountAmount())
                        .compareTo(tong.totalAmount())
        );
    }

    @Test
    void khoangNgayKhongCoThanhToanTraVeKhong() throws SQLException {
        LocalDate ngay = LocalDate.of(2020, 1, 1);

        RevenueSummary tong = reportService.getSummary(ngay, ngay);

        assertEquals(0, tong.invoiceCount());
        assertTien("0", tong.subtotal());
        assertTien("0", tong.discountAmount());
        assertTien("0", tong.totalAmount());
        assertEquals(0, reportService.findRecords(ngay, ngay).size());
    }

    @Test
    void khoangNgaySaiHoacThieuBiTuChoi() {
        LocalDate a = LocalDate.of(2026, 11, 5);
        LocalDate b = LocalDate.of(2026, 11, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getSummary(a, b)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.findRecords(null, a)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getSummary(a, null)
        );
    }

    @Test
    void ranhGioiNgayTinhDungDauVaCuoiNgay() throws SQLException {
        PaymentReceipt p1 = thanhToan("R04", "10000", 1, "0");
        PaymentReceipt p2 = thanhToan("R05", "20000", 1, "0");
        PaymentReceipt p3 = thanhToan("R06", "40000", 1, "0");

        datNgayThanhToan(p1.getPaymentId(), "2026-11-01 23:59:59");
        datNgayThanhToan(p2.getPaymentId(), "2026-11-02 00:00:00");
        datNgayThanhToan(p3.getPaymentId(), "2026-11-02 23:59:59");

        LocalDate ngay1 = LocalDate.of(2026, 11, 1);
        LocalDate ngay2 = LocalDate.of(2026, 11, 2);

        RevenueSummary chiNgay1 = reportService.getSummary(ngay1, ngay1);
        RevenueSummary chiNgay2 = reportService.getSummary(ngay2, ngay2);
        RevenueSummary caHai = reportService.getSummary(ngay1, ngay2);

        assertEquals(1, chiNgay1.invoiceCount());
        assertTien("10000", chiNgay1.totalAmount());
        assertEquals(2, chiNgay2.invoiceCount());
        assertTien("60000", chiNgay2.totalAmount());
        assertEquals(3, caHai.invoiceCount());
        assertTien("70000", caHai.totalAmount());
    }

    @Test
    void donChuaThanhToanKhongTinhVaoDoanhThu() throws SQLException {
        thanhToan("R07", "50000", 1, "0");

        long banId = taoBan("R08", 4);
        long monId = taoMon("Mon R08", "99000");
        long donChuaTra = donDaPhucVu(banId, monId, 1);
        assertEquals("SERVED", trangThaiDon(donChuaTra));

        LocalDate homNay = LocalDate.now();
        RevenueSummary tong = reportService.getSummary(homNay, homNay);

        assertEquals(1, tong.invoiceCount());
        assertTien("50000", tong.totalAmount());
    }
}
