-- =========================================================
-- HỆ THỐNG QUẢN LÝ NHÀ HÀNG
-- MariaDB 10.4+
-- Lưu ý: chạy file này sẽ tạo lại toàn bộ cơ sở dữ liệu.
-- =========================================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS restaurant_management;

CREATE DATABASE restaurant_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_management;

-- =========================================================
-- 1. NGƯỜI DÙNG
-- =========================================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,
    full_name VARCHAR(150) NOT NULL,

    role ENUM(
        'MANAGER',
        'RECEPTIONIST',
        'WAITER',
        'KITCHEN',
        'CASHIER'
    ) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 2. BÀN NHÀ HÀNG
-- =========================================================

CREATE TABLE restaurant_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_number VARCHAR(20) NOT NULL UNIQUE,
    capacity INT NOT NULL,

    status ENUM(
        'AVAILABLE',
        'RESERVED',
        'OCCUPIED',
        'INACTIVE'
    ) NOT NULL DEFAULT 'AVAILABLE',

    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_table_capacity
        CHECK (capacity > 0)
);

-- =========================================================
-- 3. ĐẶT BÀN
-- =========================================================

CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    reservation_time DATETIME NOT NULL,
    guest_count INT NOT NULL,
    note VARCHAR(500),

    status ENUM(
        'CONFIRMED',
        'SEATED',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'CONFIRMED',

    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reservation_guest_count
        CHECK (guest_count > 0),

    CONSTRAINT fk_reservation_table
        FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id),

    CONSTRAINT fk_reservation_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE INDEX idx_reservation_time
    ON reservations(reservation_time);

CREATE INDEX idx_reservation_table_status
    ON reservations(table_id, status);

-- =========================================================
-- 4. THỰC ĐƠN
-- =========================================================

CREATE TABLE menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_menu_item_price
        CHECK (price >= 0)
);

CREATE INDEX idx_menu_item_name
    ON menu_items(item_name);

CREATE INDEX idx_menu_item_category
    ON menu_items(category);

-- =========================================================
-- 5. ĐƠN GỌI MÓN
-- =========================================================

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    reservation_id BIGINT,
    waiter_id BIGINT NOT NULL,

    status ENUM(
        'OPEN',
        'IN_KITCHEN',
        'READY',
        'SERVED',
        'PAID',
        'CANCELLED'
    ) NOT NULL DEFAULT 'OPEN',

    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_table
        FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id),

    CONSTRAINT fk_order_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_order_waiter
        FOREIGN KEY (waiter_id)
        REFERENCES users(id)
);

CREATE INDEX idx_order_table_status
    ON orders(table_id, status);

-- =========================================================
-- 6. CHI TIẾT ĐƠN GỌI MÓN
-- =========================================================

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    note VARCHAR(500),

    kitchen_status ENUM(
        'PENDING',
        'PREPARING',
        'READY',
        'SERVED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_price
        CHECK (unit_price >= 0),

    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_order_item_menu
        FOREIGN KEY (menu_item_id)
        REFERENCES menu_items(id)
);

CREATE INDEX idx_order_item_order
    ON order_items(order_id);

CREATE INDEX idx_order_item_kitchen_status
    ON order_items(kitchen_status);

-- =========================================================
-- 7. THANH TOÁN
-- =========================================================

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    cashier_id BIGINT NOT NULL,

    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,

    payment_method ENUM(
        'CASH',
        'CARD',
        'TRANSFER'
    ) NOT NULL,

    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT chk_payment_discount
        CHECK (discount_amount >= 0),

    CONSTRAINT chk_payment_total
        CHECK (total_amount >= 0),

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_payment_cashier
        FOREIGN KEY (cashier_id)
        REFERENCES users(id)
);

-- =========================================================
-- 8. HÓA ĐƠN
-- =========================================================

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
);

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

-- =========================================================
-- 12. TÀI KHOẢN CHUYÊN DỤNG CHO ỨNG DỤNG
-- =========================================================

CREATE USER IF NOT EXISTS
    'restaurant_app'@'localhost'
    IDENTIFIED BY 'restaurant123';

GRANT SELECT, INSERT, UPDATE, DELETE
ON restaurant_management.*
TO 'restaurant_app'@'localhost';

FLUSH PRIVILEGES;