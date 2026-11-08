# Thư mục database

Các script được tách theo mục đích để **không vô tình xóa dữ liệu**.

| File | Mục đích | Xóa dữ liệu? |
|---|---|---|
| `create_database.sql` | Tạo database `restaurant_management` và tài khoản `restaurant_app` | Không (chạy lại an toàn) |
| `schema.sql` | Tạo bảng, chỉ mục, ràng buộc cho database **mới, trống** | Không (bảng đã có thì báo lỗi và dừng) |
| `seed.sql` | Dữ liệu mẫu để demo: 5 tài khoản, 8 bàn, 8 món | Không |
| `migrations/` | Nâng cấp cấu trúc database **đang có dữ liệu**, giữ nguyên dữ liệu | Không |
| `reset_demo.sql` | **Xóa sạch** và dựng lại database demo | **CÓ - chỉ dùng cho demo** |

## Cài mới (database trống)

Chạy trong thư mục `database/`, bằng tài khoản quản trị (root):

```bash
mariadb -u root -p < create_database.sql
mariadb -u root -p restaurant_management < schema.sql
mariadb -u root -p restaurant_management < seed.sql
```

## Reset về dữ liệu demo ban đầu (MẤT HẾT DỮ LIỆU)

```bash
cd database
mariadb -u root -p < reset_demo.sql
```

Phải đứng trong thư mục `database/` để lệnh `SOURCE` trong script tìm thấy file.

## Nâng cấp database đang có dữ liệu

Không chạy `schema.sql` hay `reset_demo.sql`. Chạy lần lượt các file trong
`migrations/` theo số thứ tự, bắt đầu từ file chưa áp dụng (xem
`migrations/README.md`). **Sao lưu trước khi chạy.**

## Database cho kiểm thử tự động

Test tích hợp chỉ chạy trên database có tên kết thúc bằng `_test`.
Tạo database riêng, **không dùng chung với database demo**:

```bash
mariadb -u root -p -e "CREATE DATABASE restaurant_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL ON restaurant_test.* TO 'restaurant_app'@'localhost'; FLUSH PRIVILEGES;"
mariadb -u root -p restaurant_test < schema.sql
mariadb -u root -p restaurant_test < seed.sql
mvn test -Ddb.url=jdbc:mariadb://localhost:3306/restaurant_test
```

Test cần quyền `GRANT ALL` trên `restaurant_test` vì phải xóa dữ liệu giữa các
lần chạy; quyền này **chỉ cấp cho database test**, không cấp cho database demo.
