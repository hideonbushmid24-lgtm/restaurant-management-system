-- =========================================================
-- HỆ THỐNG QUẢN LÝ NHÀ HÀNG - DỮ LIỆU MẪU (SEED) CHO DEMO
-- MariaDB 10.4+
--
-- Chỉ chạy sau schema.sql, trên database demo mới.
-- Tài khoản bên dưới là TÀI KHOẢN DEMO với mật khẩu công khai
-- trong repository: không dùng cho dữ liệu thật.
--
-- Cách chạy:
--   mariadb -u root -p restaurant_management < seed.sql
-- =========================================================

SET NAMES utf8mb4;

-- =========================================================
-- 9. DỮ LIỆU NGƯỜI DÙNG MẪU
-- Mật khẩu được mã hóa bằng SHA-256.
-- =========================================================

INSERT INTO users (
    username,
    password_hash,
    full_name,
    role
)
VALUES
(
    'manager',
    SHA2('manager123', 256),
    'Quản lý nhà hàng',
    'MANAGER'
),
(
    'receptionist',
    SHA2('reception123', 256),
    'Nhân viên tiếp nhận',
    'RECEPTIONIST'
),
(
    'waiter',
    SHA2('waiter123', 256),
    'Nhân viên phục vụ',
    'WAITER'
),
(
    'kitchen',
    SHA2('kitchen123', 256),
    'Nhân viên bếp',
    'KITCHEN'
),
(
    'cashier',
    SHA2('cashier123', 256),
    'Nhân viên thu ngân',
    'CASHIER'
);

-- =========================================================
-- 10. DỮ LIỆU BÀN MẪU
-- =========================================================

INSERT INTO restaurant_tables (
    table_number,
    capacity,
    status
)
VALUES
('B01', 2, 'AVAILABLE'),
('B02', 2, 'AVAILABLE'),
('B03', 4, 'AVAILABLE'),
('B04', 4, 'AVAILABLE'),
('B05', 4, 'AVAILABLE'),
('B06', 6, 'AVAILABLE'),
('B07', 6, 'AVAILABLE'),
('B08', 8, 'AVAILABLE');

-- =========================================================
-- 11. DỮ LIỆU THỰC ĐƠN MẪU
-- =========================================================

INSERT INTO menu_items (
    item_name,
    category,
    description,
    price
)
VALUES
(
    'Cơm chiên hải sản',
    'Món chính',
    'Cơm chiên với tôm, mực và rau củ',
    85000
),
(
    'Mì xào bò',
    'Món chính',
    'Mì xào với thịt bò và rau cải',
    79000
),
(
    'Gà chiên nước mắm',
    'Món chính',
    'Cánh gà chiên giòn sốt nước mắm',
    99000
),
(
    'Salad rau củ',
    'Khai vị',
    'Salad rau củ tươi',
    49000
),
(
    'Khoai tây chiên',
    'Khai vị',
    'Khoai tây chiên giòn',
    45000
),
(
    'Coca-Cola',
    'Đồ uống',
    'Nước ngọt có ga',
    20000
),
(
    'Nước cam',
    'Đồ uống',
    'Nước cam tươi',
    35000
),
(
    'Trà đào',
    'Đồ uống',
    'Trà đào cam sả',
    39000
);
