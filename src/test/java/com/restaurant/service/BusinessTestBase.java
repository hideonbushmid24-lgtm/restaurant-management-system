package com.restaurant.service;

import com.restaurant.dao.KitchenDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.PaymentDAO;
import com.restaurant.dao.RevenueReportDAO;
import com.restaurant.model.OrderItem;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp dùng chung cho các test nghiệp vụ (đơn hàng, bếp, thanh toán,
 * doanh thu). Mỗi test bắt đầu với dữ liệu nghiệp vụ trống.
 * Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
abstract class BusinessTestBase {

    protected OrderService orderService;
    protected KitchenService kitchenService;
    protected PaymentService paymentService;
    protected RevenueReportService reportService;

    protected long waiterId;
    protected long cashierId;
    protected long managerId;

    @BeforeEach
    void chuanBiDuLieu() throws SQLException {
        TestDatabase.requireTestDatabase();
        TestDatabase.resetBusinessData();

        orderService = new OrderService(new OrderDAO());
        kitchenService = new KitchenService(new KitchenDAO());
        paymentService = new PaymentService(new PaymentDAO());
        reportService =
                new RevenueReportService(new RevenueReportDAO());

        waiterId = userId("waiter");
        cashierId = userId("cashier");
        managerId = userId("manager");
    }

    // ---------- Tạo dữ liệu ----------

    protected long userId(String username) throws SQLException {
        return TestDatabase.queryLong(
                "SELECT id FROM users WHERE username = '" + username + "'"
        );
    }

    protected long taoBan(String soBan, int sucChua) throws SQLException {
        TestDatabase.execute(
                "INSERT INTO restaurant_tables "
                        + "(table_number, capacity, status, active) VALUES ('"
                        + soBan + "', " + sucChua + ", 'AVAILABLE', TRUE)"
        );
        return TestDatabase.queryLong(
                "SELECT id FROM restaurant_tables WHERE table_number = '"
                        + soBan + "'"
        );
    }

    protected long taoMon(String ten, String gia) throws SQLException {
        TestDatabase.execute(
                "INSERT INTO menu_items (item_name, category, price) "
                        + "VALUES ('" + ten + "', 'Test', " + gia + ")"
        );
        return TestDatabase.queryLong(
                "SELECT id FROM menu_items WHERE item_name = '" + ten + "'"
        );
    }

    protected long taoDon(long banId) throws SQLException {
        return orderService.createOrder(banId, null, waiterId, null);
    }

    protected long themMon(long donId, long monId, int soLuong)
            throws SQLException {
        return orderService.addItem(donId, monId, soLuong, null);
    }

    /** Bếp nhận làm và làm xong một món. */
    protected void bepLamXong(long itemId) throws SQLException {
        kitchenService.startPreparing(itemId);
        kitchenService.markReady(itemId);
    }

    /** Gửi bếp, bếp làm xong, phục vụ hết món: đơn thành SERVED. */
    protected void phucVuHetMon(long donId) throws SQLException {
        orderService.sendToKitchen(donId);

        for (OrderItem item : orderService.getOrderItems(donId)) {
            if (item.getKitchenStatus().name().equals("PENDING")) {
                bepLamXong(item.getId());
            }
        }

        for (OrderItem item : orderService.getOrderItems(donId)) {
            if (item.getKitchenStatus().name().equals("READY")) {
                orderService.markItemServed(item.getId());
            }
        }
    }

    /** Tạo đơn một món đã phục vụ xong, sẵn sàng thanh toán. */
    protected long donDaPhucVu(long banId, long monId, int soLuong)
            throws SQLException {
        long donId = taoDon(banId);
        themMon(donId, monId, soLuong);
        phucVuHetMon(donId);
        return donId;
    }

    // ---------- Đọc trạng thái ----------

    protected String trangThaiDon(long donId) throws SQLException {
        return TestDatabase.queryString(
                "SELECT status FROM orders WHERE id = " + donId
        );
    }

    protected String trangThaiBan(long banId) throws SQLException {
        return TestDatabase.queryString(
                "SELECT status FROM restaurant_tables WHERE id = " + banId
        );
    }

    protected String trangThaiMon(long itemId) throws SQLException {
        return TestDatabase.queryString(
                "SELECT kitchen_status FROM order_items WHERE id = "
                        + itemId
        );
    }

    protected long dem(String bang) throws SQLException {
        return TestDatabase.queryLong("SELECT COUNT(*) FROM " + bang);
    }

    // ---------- Chạy đồng thời ----------

    /**
     * Chạy các việc cùng lúc (xuất phát cùng một thời điểm).
     * @return danh sách true (thành công) / false (bị lỗi) theo thứ tự
     */
    protected List<Boolean> chayDongThoi(List<Callable<?>> jobs)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(jobs.size());
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(jobs.size());
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (Callable<?> job : jobs) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        job.call();
                        return true;
                    } catch (Exception exception) {
                        return false;
                    }
                }));
            }

            ready.await();
            go.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Kiểm tra thao tác bị từ chối và thông báo có chứa đoạn mong muốn. */
    protected static void assertLoi(String doanThongBao, Executable thaoTac) {
        Exception loi =
                assertThrows(Exception.class, thaoTac);

        assertTrue(
                loi.getMessage() != null
                        && loi.getMessage().contains(doanThongBao),
                "Thông báo lỗi không như mong đợi: " + loi.getMessage()
        );
    }
}
