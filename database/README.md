# Thư mục database

Các script được tách theo mục đích để **không vô tình xóa dữ liệu**.

| File | Mục đích | Xóa dữ liệu? |
|---|---|---|
| `create_database.sql` | Tạo database `restaurant_management` và tài khoản `restaurant_app` | Không (chạy lại an toàn) |
| `schema.sql` | Tạo 9 bảng, chỉ mục, ràng buộc cho database **mới, trống** | Không (bảng đã có thì báo lỗi và dừng) |
| `seed.sql` | Dữ liệu mẫu để demo: 5 tài khoản, 8 bàn, 8 món | Không |
| `migrations/` | Nâng cấp cấu trúc database **đang có dữ liệu**, giữ nguyên dữ liệu | Không |
| `reset_demo.sql` | **Xóa sạch** và dựng lại database demo | **CÓ - chỉ dùng cho demo** |

## Chuẩn bị chương trình dòng lệnh

Các lệnh dưới đây dùng chương trình `mysql` (đi kèm XAMPP và MariaDB).
Với XAMPP trên Windows, chương trình nằm ở `C:\xampp\mysql\bin`; nếu PowerShell
báo không nhận ra lệnh `mysql`, chạy một lần trong cửa sổ terminal đang dùng:

```powershell
$env:Path += ";C:\xampp\mysql\bin"
```

- Phải bật **MySQL** trong XAMPP Control Panel (nút Start) trước khi chạy.
- Tài khoản `root` của XAMPP mặc định **không có mật khẩu**. Nếu root của bạn có
  mật khẩu, thêm `-p` ngay sau `-u root` rồi gõ mật khẩu khi được hỏi.
- Các lệnh dùng `-e "source ..."` thay cho dấu `<` vì PowerShell không hỗ trợ
  `<`. Cách này chạy được trên cả Windows, Linux và macOS.
- Luôn đứng trong thư mục `database/` khi chạy (để tìm thấy các file `.sql`).

## Cài mới (database trống)

```powershell
cd database
mysql -u root --default-character-set=utf8mb4 -e "source create_database.sql"
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source schema.sql"
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source seed.sql"
```

Kiểm tra: `mysql -u root restaurant_management -e "SHOW TABLES"` phải hiện 9 bảng.

## Reset về dữ liệu demo ban đầu (MẤT HẾT DỮ LIỆU)

```powershell
cd database
mysql -u root --default-character-set=utf8mb4 -e "source reset_demo.sql"
```

**Sao lưu trước** nếu database có dữ liệu cần giữ (xem `docs/huong-dan-backup-restore.md`).

## Nâng cấp database đang có dữ liệu

Không chạy `schema.sql` hay `reset_demo.sql`. Chạy lần lượt các file trong
`migrations/` theo số thứ tự, bắt đầu từ file chưa áp dụng (xem
`migrations/README.md`, có hướng dẫn từng bước cho bản có bảng `customers`).
**Sao lưu trước khi chạy.**

## Database cho kiểm thử tự động

Test tích hợp chỉ chạy trên database có tên kết thúc bằng `_test`.
Tạo database riêng, **không dùng chung với database demo**:

```powershell
cd database
mysql -u root -e "CREATE DATABASE restaurant_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL ON restaurant_test.* TO 'restaurant_app'@'localhost'; FLUSH PRIVILEGES;"
mysql -u root --default-character-set=utf8mb4 restaurant_test -e "source schema.sql"
mysql -u root --default-character-set=utf8mb4 restaurant_test -e "source seed.sql"
```

Rồi chạy test ở thư mục gốc dự án:

```powershell
mvn test -Ddb.url=jdbc:mariadb://localhost:3306/restaurant_test
```

Test cần quyền `GRANT ALL` trên `restaurant_test` vì phải xóa dữ liệu giữa các
lần chạy; quyền này **chỉ cấp cho database test**, không cấp cho database demo.
