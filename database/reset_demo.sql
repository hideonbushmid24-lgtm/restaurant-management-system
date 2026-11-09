-- =========================================================
-- !!! CẢNH BÁO: SCRIPT NÀY XÓA TOÀN BỘ DỮ LIỆU !!!
-- RESET DATABASE DEMO VỀ TRẠNG THÁI BAN ĐẦU
--
-- Chỉ dùng cho database demo/phát triển. KHÔNG chạy trên
-- database có dữ liệu cần giữ. Hãy sao lưu trước (xem
-- docs/huong-dan-backup-restore.md nếu đã có).
--
-- Thực hiện: xóa database -> tạo lại -> nạp schema.sql
--            -> nạp seed.sql.
--
-- Phải chạy từ thư mục database/ để các lệnh SOURCE tìm thấy file:
--   cd database
--   mysql -u root --default-character-set=utf8mb4 -e "source reset_demo.sql"
-- =========================================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS restaurant_management;

CREATE DATABASE restaurant_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_management;

SOURCE schema.sql;
SOURCE seed.sql;

-- Cấp lại quyền cho tài khoản ứng dụng (nếu đã tạo bằng
-- create_database.sql thì lệnh dưới không gây lỗi).
CREATE USER IF NOT EXISTS
    'restaurant_app'@'localhost'
    IDENTIFIED BY 'restaurant123';

GRANT SELECT, INSERT, UPDATE, DELETE
ON restaurant_management.*
TO 'restaurant_app'@'localhost';

FLUSH PRIVILEGES;
