package com.restaurant.service;

import com.restaurant.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử luồng đơn hàng: tạo đơn, gọi món, gửi bếp, bếp chế biến,
 * phục vụ, gọi thêm. Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
class OrderFlowIntegrationTest extends BusinessTestBase {

    // ---------- Tạo đơn ----------

    @Test
    void taoDonChoBanTrongThiBanChuyenSangDangDung() throws SQLException {
        long banId = taoBan("D01", 4);

        long donId = taoDon(banId);

        assertEquals("OPEN", trangThaiDon(donId));
        assertEquals("OCCUPIED", trangThaiBan(banId));
    }

    @Test
    void khongTaoHaiDonHoatDongCungMotBan() throws SQLException {
        long banId = taoBan("D02", 4);
        taoDon(banId);

        assertLoi(
                "đang có một đơn chưa hoàn tất",
                () -> taoDon(banId)
        );

        assertEquals(1, dem("orders"));
    }

    @Test
    void haiNguoiCungTaoDonChoMotBanChiThanhCongMot() throws Exception {
        long banId = taoBan("D03", 4);

        List<Callable<?>> jobs = List.of(
                () -> taoDon(banId),
                () -> taoDon(banId)
        );

        List<Boolean> ketQua = chayDongThoi(jobs);

        assertEquals(
                1,
                ketQua.stream().filter(ok -> ok).count(),
                "Chỉ một người được tạo đơn cho bàn"
        );
        assertEquals(1, dem("orders"));
    }

    @Test
    void khongTaoDonChoBanKhongTonTai() {
        assertLoi("Bàn không tồn tại", () -> taoDon(999999));
        assertLoi("Mã bàn không hợp lệ", () -> taoDon(0));
    }

    // ---------- Gọi món ----------

    @Test
    void soLuongMonKhongHopLeBiTuChoiVaKhongGhiGi() throws SQLException {
        long banId = taoBan("D04", 4);
        long monId = taoMon("Mon D04", "50000");
        long donId = taoDon(banId);

        for (int soLuong : new int[]{0, -1, 101}) {
            assertLoi(
                    "Số lượng",
                    () -> themMon(donId, monId, soLuong)
            );
        }

        assertEquals(0, dem("order_items"));
    }

    @Test
    void giaMonDuocChotTaiThoiDiemGoiKhiThucDonDoiGia()
            throws SQLException {
        long banId = taoBan("D05", 4);
        long monId = taoMon("Mon D05", "50000");
        long donId = taoDon(banId);
        themMon(donId, monId, 2);

        // Quản lý đổi giá món sau khi khách đã gọi
        com.restaurant.support.TestDatabase.execute(
                "UPDATE menu_items SET price = 99000 WHERE id = " + monId
        );

        assertEquals(
                0,
                new BigDecimal("100000.00")
                        .compareTo(orderService.getOrderTotal(donId)),
                "Tổng tiền phải theo giá lúc gọi (50.000 x 2)"
        );
    }

    @Test
    void monNgungBanBiTuChoiVaDonDaPhucVuKhongBiMoLai()
            throws SQLException {
        long banId = taoBan("D06", 4);
        long monId = taoMon("Mon D06", "40000");
        long monNgung = taoMon("Mon ngung D06", "30000");
        com.restaurant.support.TestDatabase.execute(
                "UPDATE menu_items SET available = FALSE WHERE id = "
                        + monNgung
        );

        long donId = donDaPhucVu(banId, monId, 1);
        assertEquals("SERVED", trangThaiDon(donId));

        assertLoi(
                "không tồn tại hoặc đã ngừng bán",
                () -> themMon(donId, monNgung, 1)
        );

        // Lỗi giữa chừng phải hoàn tác: đơn vẫn SERVED, không có món mới
        assertEquals("SERVED", trangThaiDon(donId));
        assertEquals(1, dem("order_items"));
    }

    @Test
    void suaVaHuyMonChiKhiDonMoVaMonDangChoCheBien()
            throws SQLException {
        long banId = taoBan("D07", 4);
        long monId = taoMon("Mon D07", "20000");
        long donId = taoDon(banId);
        long itemId = themMon(donId, monId, 1);

        orderService.updateItemQuantity(itemId, 3);
        assertEquals(
                0,
                new BigDecimal("60000.00")
                        .compareTo(orderService.getOrderTotal(donId))
        );

        orderService.sendToKitchen(donId);

        // Đã gửi bếp thì không được sửa hay hủy món
        assertLoi(
                "Chỉ được sửa món khi đơn đang mở",
                () -> orderService.updateItemQuantity(itemId, 5)
        );
        assertLoi(
                "Chỉ được sửa món khi đơn đang mở",
                () -> orderService.cancelItem(itemId)
        );
        assertEquals("PENDING", trangThaiMon(itemId));
    }

    @Test
    void tongTienKhongTinhMonDaHuy() throws SQLException {
        long banId = taoBan("D08", 4);
        long mon1 = taoMon("Mon D08a", "30000");
        long mon2 = taoMon("Mon D08b", "45000");
        long donId = taoDon(banId);
        themMon(donId, mon1, 2);
        long item2 = themMon(donId, mon2, 1);

        orderService.cancelItem(item2);

        assertEquals(
                0,
                new BigDecimal("60000.00")
                        .compareTo(orderService.getOrderTotal(donId))
        );
        assertEquals("CANCELLED", trangThaiMon(item2));
    }

    // ---------- Gửi bếp và bếp chế biến ----------

    @Test
    void khongGuiBepDonRongVaKhongGuiHaiLan() throws SQLException {
        long banId = taoBan("D09", 4);
        long monId = taoMon("Mon D09", "20000");
        long donId = taoDon(banId);

        assertLoi(
                "chưa có món",
                () -> orderService.sendToKitchen(donId)
        );

        themMon(donId, monId, 1);
        orderService.sendToKitchen(donId);
        assertEquals("IN_KITCHEN", trangThaiDon(donId));

        assertLoi(
                "Chỉ đơn đang mở",
                () -> orderService.sendToKitchen(donId)
        );
    }

    @Test
    void bepPhaiDiDungThuTuTrangThai() throws SQLException {
        long banId = taoBan("D10", 4);
        long monId = taoMon("Mon D10", "20000");
        long donId = taoDon(banId);
        long itemId = themMon(donId, monId, 1);
        orderService.sendToKitchen(donId);

        // Không được nhảy cóc từ chờ chế biến sang hoàn thành
        assertLoi(
                "Chuyển trạng thái món không hợp lệ",
                () -> kitchenService.markReady(itemId)
        );
        // Món chưa xong thì chưa được xác nhận phục vụ
        assertLoi(
                "Chỉ món đã hoàn thành",
                () -> orderService.markItemServed(itemId)
        );

        kitchenService.startPreparing(itemId);
        kitchenService.markReady(itemId);

        // Không được quay ngược trạng thái
        assertLoi(
                "Chuyển trạng thái món không hợp lệ",
                () -> kitchenService.startPreparing(itemId)
        );
        assertEquals("READY", trangThaiMon(itemId));
    }

    @Test
    void donChiThanhDaPhucVuKhiPhucVuHetCacMon() throws SQLException {
        long banId = taoBan("D11", 4);
        long mon1 = taoMon("Mon D11a", "20000");
        long mon2 = taoMon("Mon D11b", "25000");
        long donId = taoDon(banId);
        long item1 = themMon(donId, mon1, 1);
        long item2 = themMon(donId, mon2, 1);
        orderService.sendToKitchen(donId);

        bepLamXong(item1);
        orderService.markItemServed(item1);
        assertTrue(
                !"SERVED".equals(trangThaiDon(donId)),
                "Còn món chưa xong thì đơn chưa được SERVED"
        );

        bepLamXong(item2);
        orderService.markItemServed(item2);
        assertEquals("SERVED", trangThaiDon(donId));
    }

    // ---------- Gọi thêm ----------

    @Test
    void goiThemKhiDonDaPhucVuThiMonCuGiuNguyenTrangThai()
            throws SQLException {
        long banId = taoBan("D12", 4);
        long monCu = taoMon("Mon D12a", "30000");
        long monMoi = taoMon("Mon D12b", "15000");
        long donId = donDaPhucVu(banId, monCu, 1);
        long itemCu =
                orderService.getOrderItems(donId).get(0).getId();

        long itemMoi = themMon(donId, monMoi, 2);

        assertEquals("OPEN", trangThaiDon(donId));
        assertEquals("SERVED", trangThaiMon(itemCu));
        assertEquals("PENDING", trangThaiMon(itemMoi));

        // Gửi bếp, chế biến, phục vụ phần gọi thêm
        orderService.sendToKitchen(donId);
        bepLamXong(itemMoi);
        orderService.markItemServed(itemMoi);

        assertEquals("SERVED", trangThaiDon(donId));
        assertEquals(
                0,
                new BigDecimal("60000.00")
                        .compareTo(orderService.getOrderTotal(donId))
        );
    }

    @Test
    void goiThemRoiHuyMonMoiThiDonPhaiThanhToanDuoc()
            throws SQLException {
        long banId = taoBan("D13", 4);
        long monCu = taoMon("Mon D13a", "30000");
        long monMoi = taoMon("Mon D13b", "15000");
        long donId = donDaPhucVu(banId, monCu, 1);

        long itemMoi = themMon(donId, monMoi, 1);
        orderService.cancelItem(itemMoi);

        // Không còn món nào chờ chế biến, các món còn lại đã phục vụ:
        // đơn phải trở lại SERVED để thu ngân thanh toán được.
        assertEquals(
                "SERVED",
                trangThaiDon(donId),
                "Đơn bị kẹt ở trạng thái mở sau khi hủy món gọi thêm"
        );

        paymentService.pay(
                donId,
                cashierId,
                BigDecimal.ZERO,
                com.restaurant.model.PaymentMethod.CASH
        );
        assertEquals("PAID", trangThaiDon(donId));
    }

    @Test
    void layDuocDanhSachMonCuaDon() throws SQLException {
        long banId = taoBan("D14", 4);
        long monId = taoMon("Mon D14", "10000");
        long donId = taoDon(banId);
        themMon(donId, monId, 2);

        List<OrderItem> items = orderService.getOrderItems(donId);

        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals("Mon D14", items.get(0).getItemName());
    }

    @Test
    void huyMonGoiThemKhiConMonGoiThemKhacChoCheBienThiDonVanMo()
            throws SQLException {
        long banId = taoBan("D15", 4);
        long monCu = taoMon("Mon D15a", "30000");
        long monMoi = taoMon("Mon D15b", "15000");
        long donId = donDaPhucVu(banId, monCu, 1);

        long itemHuy = themMon(donId, monMoi, 1);
        themMon(donId, monMoi, 2); // vẫn còn một món đang chờ
        orderService.cancelItem(itemHuy);

        assertEquals("OPEN", trangThaiDon(donId));
    }

    @Test
    void donMoiHuyHetMonThiVanMoVaChuaTungPhucVu() throws SQLException {
        long banId = taoBan("D16", 4);
        long monId = taoMon("Mon D16", "30000");
        long donId = taoDon(banId);
        long itemId = themMon(donId, monId, 1);

        orderService.cancelItem(itemId);

        // Chưa từng có món nào được phục vụ nên không được coi là SERVED
        assertEquals("OPEN", trangThaiDon(donId));
    }
}
