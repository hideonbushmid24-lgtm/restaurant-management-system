-- =========================================================
-- MIGRATION 002: XÓA CỘT KHÁCH HÀNG CŨ TRONG reservations
-- MariaDB 10.4+  |  Chạy bằng client `mariadb` (có dùng DELIMITER)
--
-- CHỈ CHẠY SAU KHI:
--   * Đã chạy 001_customers.sql thành công.
--   * Đã kiểm tra dữ liệu khách hàng đúng (xem các tên bị gộp
--     mà 001 đã in ra). Sau bước này TÊN/SỐ ĐIỆN THOẠI CŨ TRONG
--     từng lượt đặt bàn SẼ MẤT, chỉ còn thông tin ở bảng customers.
--   * Đã SAO LƯU database.
--
-- Script tự dừng nếu còn lượt đặt bàn chưa gắn customer_id.
-- Chạy lại nhiều lần vẫn an toàn.
--   mariadb -u root -p restaurant_management < migrations/002_drop_legacy_customer_columns.sql
-- =========================================================

DELIMITER //

BEGIN NOT ATOMIC
    DECLARE has_legacy INT DEFAULT 0;
    DECLARE missing INT DEFAULT 0;

    SELECT COUNT(*) INTO has_legacy
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reservations'
      AND COLUMN_NAME IN ('customer_name', 'customer_phone');

    IF has_legacy > 0 THEN

        SELECT COUNT(*) INTO missing
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'reservations'
          AND COLUMN_NAME = 'customer_id';

        IF missing = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Chưa chạy 001_customers.sql - dừng, không xóa gì';
        END IF;

        SELECT COUNT(*) INTO missing
        FROM reservations
        WHERE customer_id IS NULL;

        IF missing > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Còn lượt đặt bàn chưa gắn khách hàng - dừng, không xóa gì';
        END IF;

        ALTER TABLE reservations
            DROP COLUMN IF EXISTS customer_name,
            DROP COLUMN IF EXISTS customer_phone;
    END IF;
END //

DELIMITER ;

SELECT 'Hoàn tất: reservations không còn cột khách hàng cũ' AS ket_qua;
