-- =========================================================
-- TẠO DATABASE VÀ TÀI KHOẢN ỨNG DỤNG
-- MariaDB 10.4+
--
-- An toàn khi chạy lại: không xóa gì, chỉ tạo nếu chưa có.
-- Chạy bằng tài khoản quản trị (root):
--   mariadb -u root -p < create_database.sql
--
-- Tài khoản restaurant_app chỉ có quyền đọc/ghi dữ liệu
-- (SELECT, INSERT, UPDATE, DELETE), không có quyền xóa bảng
-- hay xóa database.
-- Mật khẩu bên dưới là mật khẩu DEMO: hãy đổi khi dùng thật
-- và cập nhật tương ứng trong cấu hình kết nối.
-- =========================================================

CREATE DATABASE IF NOT EXISTS restaurant_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS
    'restaurant_app'@'localhost'
    IDENTIFIED BY 'restaurant123';

GRANT SELECT, INSERT, UPDATE, DELETE
ON restaurant_management.*
TO 'restaurant_app'@'localhost';

FLUSH PRIVILEGES;
