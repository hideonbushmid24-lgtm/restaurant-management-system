-- =========================================================
-- MIGRATION 001: TÁCH KHÁCH HÀNG RA BẢNG customers
-- MariaDB 10.4+  |  Chạy bằng client `mariadb` (có dùng DELIMITER)
--
-- Việc làm:
--   1. Tạo bảng customers.
--   2. Thêm reservations.customer_id.
--   3. Chuyển khách từ các lượt đặt bàn cũ sang customers
--      (gộp theo số điện thoại đã chuẩn hóa: bỏ khoảng trắng và dấu -).
--      Nếu một số điện thoại có nhiều tên khác nhau thì lấy tên
--      của lượt đặt GẦN NHẤT (danh sách các trường hợp này được in ra).
--   4. Gắn customer_id cho mọi lượt đặt bàn, tạo khóa ngoại.
--
-- KHÔNG XÓA DỮ LIỆU: hai cột cũ customer_name, customer_phone vẫn
-- được giữ lại (chuyển thành cho phép NULL) để có thể đối chiếu.
-- Sau khi kiểm tra xong mới chạy 002_drop_legacy_customer_columns.sql.
--
-- Chạy lại nhiều lần vẫn an toàn.
-- Nếu còn lượt đặt bàn không gắn được khách, script DỪNG với lỗi.
--
-- !!! SAO LƯU DATABASE TRƯỚC KHI CHẠY !!!
--   mariadb -u root -p restaurant_management < migrations/001_customers.sql
-- =========================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_customer_phone
        UNIQUE (phone)
);

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS customer_id BIGINT NULL AFTER table_id;

DELIMITER //

-- Bước chuyển dữ liệu: chỉ chạy khi còn cột cũ customer_phone
BEGIN NOT ATOMIC
    DECLARE has_legacy INT DEFAULT 0;

    SELECT COUNT(*) INTO has_legacy
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reservations'
      AND COLUMN_NAME = 'customer_phone';

    IF has_legacy > 0 THEN

        CREATE TEMPORARY TABLE tmp_res_phone AS
        SELECT
            id,
            created_at,
            customer_name,
            IF(
                REGEXP_REPLACE(TRIM(customer_phone), '[ -]', '') = '',
                CONCAT('LEGACY-', id),
                REGEXP_REPLACE(TRIM(customer_phone), '[ -]', '')
            ) AS phone_key
        FROM reservations
        WHERE customer_id IS NULL;

        -- Báo cáo: số điện thoại đang có nhiều tên khác nhau
        -- (COLLATE utf8mb4_bin để phân biệt cả dấu và hoa/thường)
        SELECT
            phone_key AS so_dien_thoai,
            COUNT(DISTINCT customer_name COLLATE utf8mb4_bin)
                AS so_ten_khac_nhau,
            GROUP_CONCAT(
                DISTINCT customer_name COLLATE utf8mb4_bin
                SEPARATOR ' | '
            ) AS cac_ten
        FROM tmp_res_phone
        GROUP BY phone_key
        HAVING COUNT(DISTINCT customer_name COLLATE utf8mb4_bin) > 1;

        -- Tạo khách mới (tên lấy từ lượt đặt gần nhất)
        INSERT INTO customers (full_name, phone, created_at)
        SELECT r.customer_name, k.phone_key, k.first_created
        FROM (
            SELECT
                phone_key,
                MAX(id) AS latest_id,
                MIN(created_at) AS first_created
            FROM tmp_res_phone
            GROUP BY phone_key
        ) k
        JOIN reservations r ON r.id = k.latest_id
        WHERE NOT EXISTS (
            SELECT 1 FROM customers c WHERE c.phone = k.phone_key
        );

        -- Gắn customer_id cho các lượt đặt bàn
        UPDATE reservations r
        JOIN tmp_res_phone t ON t.id = r.id
        JOIN customers c ON c.phone = t.phone_key
        SET r.customer_id = c.id;

        DROP TEMPORARY TABLE tmp_res_phone;

        -- Cột cũ không còn bắt buộc (ứng dụng mới không ghi vào đó)
        ALTER TABLE reservations
            MODIFY customer_name VARCHAR(150) NULL,
            MODIFY customer_phone VARCHAR(20) NULL;
    END IF;
END //

-- Chặn tiếp tục nếu còn lượt đặt bàn chưa có khách
BEGIN NOT ATOMIC
    DECLARE missing INT DEFAULT 0;

    SELECT COUNT(*) INTO missing
    FROM reservations
    WHERE customer_id IS NULL;

    IF missing > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Còn lượt đặt bàn chưa gắn được khách hàng - dừng migration';
    END IF;
END //

-- Tạo khóa ngoại nếu chưa có
BEGIN NOT ATOMIC
    DECLARE has_fk INT DEFAULT 0;

    SELECT COUNT(*) INTO has_fk
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reservations'
      AND CONSTRAINT_NAME = 'fk_reservation_customer';

    ALTER TABLE reservations
        MODIFY customer_id BIGINT NOT NULL;

    IF has_fk = 0 THEN
        ALTER TABLE reservations
            ADD CONSTRAINT fk_reservation_customer
            FOREIGN KEY (customer_id)
            REFERENCES customers(id);
    END IF;
END //

DELIMITER ;

-- Kết quả để đối chiếu bằng mắt
SELECT
    (SELECT COUNT(*) FROM customers) AS so_khach_hang,
    (SELECT COUNT(*) FROM reservations) AS so_luot_dat_ban,
    (SELECT COUNT(*) FROM reservations
      WHERE customer_id IS NULL) AS luot_dat_chua_gan_khach;
