-- =========================================================
-- HỆ THỐNG QUẢN LÝ NHÀ HÀNG - CẤU TRÚC DATABASE (SCHEMA)
-- MariaDB 10.4+
--
-- File này CHỈ tạo bảng, chỉ mục, ràng buộc.
--   * KHÔNG xóa database, KHÔNG xóa dữ liệu.
--   * KHÔNG chứa dữ liệu mẫu (xem seed.sql).
--   * Dùng cho database MỚI, TRỐNG. Nếu bảng đã tồn tại,
--     lệnh sẽ báo lỗi và dừng - đó là chủ ý để bảo vệ dữ liệu.
--   * Database đang có dữ liệu: dùng các file trong migrations/.
--
-- Cách chạy (database phải được tạo trước bằng create_database.sql):
--   mariadb -u root -p restaurant_management < schema.sql
-- =========================================================

SET NAMES utf8mb4;

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
-- 2. KHÁCH HÀNG
-- Số điện thoại là định danh duy nhất của khách (đã chuẩn hóa:
-- chỉ gồm chữ số, có thể bắt đầu bằng dấu +).
-- =========================================================

CREATE TABLE customers (
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

-- =========================================================
-- 3. BÀN NHÀ HÀNG
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
-- 4. ĐẶT BÀN
-- =========================================================

CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
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

    CONSTRAINT fk_reservation_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_reservation_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE INDEX idx_reservation_time
    ON reservations(reservation_time);

CREATE INDEX idx_reservation_table_status
    ON reservations(table_id, status);

-- =========================================================
-- 5. THỰC ĐƠN
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
-- 6. ĐƠN GỌI MÓN
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
-- 7. CHI TIẾT ĐƠN GỌI MÓN
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
-- 8. THANH TOÁN
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
-- 9. HÓA ĐƠN
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
