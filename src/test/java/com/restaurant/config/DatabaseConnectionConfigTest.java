package com.restaurant.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Kiểm thử thứ tự ưu tiên cấu hình kết nối.
 * Không cần database. Test tự khôi phục lại tham số hệ thống sau khi chạy.
 */
class DatabaseConnectionConfigTest {

    @Test
    void thamSoDGhiDeLenMoiNguonKhac() {
        String key = "db.username";
        String cu = System.getProperty(key);

        try {
            System.setProperty(key, "nguoi_dung_thu_nghiem");

            assertEquals(
                    "nguoi_dung_thu_nghiem",
                    DatabaseConnection.readSetting(key)
            );
        } finally {
            khoiPhuc(key, cu);
        }
    }

    @Test
    void thamSoRongDuocXemNhuChuaDat() {
        String key = "db.username";
        String cu = System.getProperty(key);

        try {
            System.setProperty(key, "   ");

            String giaTri = DatabaseConnection.readSetting(key);

            // Bỏ qua giá trị rỗng, rơi xuống các nguồn còn lại
            assertNotNull(giaTri);
            assertFalse(giaTri.isBlank());
        } finally {
            khoiPhuc(key, cu);
        }
    }

    @Test
    void luonCoDuBaThongSoDeKetNoi() {
        assertNotNull(DatabaseConnection.readSetting("db.url"));
        assertNotNull(DatabaseConnection.readSetting("db.username"));
        assertNotNull(DatabaseConnection.readSetting("db.password"));
    }

    private static void khoiPhuc(String key, String giaTriCu) {
        if (giaTriCu == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, giaTriCu);
        }
    }
}
