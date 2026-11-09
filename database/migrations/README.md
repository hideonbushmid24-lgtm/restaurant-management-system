# Migration (nâng cấp cấu trúc, giữ dữ liệu)

Mỗi file là một bước nâng cấp, đặt tên `NNN_mo_ta.sql` và chạy theo thứ tự số.

Quy ước bắt buộc cho mọi migration:

1. **Không** dùng `DROP DATABASE`, `DROP TABLE` (bảng dữ liệu thật), `TRUNCATE` hay `DELETE` không điều kiện.
   Việc xóa cột chỉ được làm ở một migration **riêng**, tự kiểm tra dữ liệu đã được chuyển xong trước khi xóa.
2. Chạy lại không gây lỗi hoặc mất dữ liệu (dùng `IF NOT EXISTS`, kiểm tra trước khi chuyển dữ liệu).
3. Nếu bước nâng cấp thay đổi cấu trúc, `schema.sql` được cập nhật thành cấu trúc cuối cùng
   để cài mới không cần chạy migration.
4. **Sao lưu database trước khi chạy.**

## Danh sách

| Số | File | Nội dung |
|---|---|---|
| 001 | `001_customers.sql` | Tạo bảng `customers`, chuyển khách từ các lượt đặt bàn cũ sang, gắn `reservations.customer_id`. **Chưa xóa dữ liệu cũ.** |
| 002 | `002_drop_legacy_customer_columns.sql` | Xóa hai cột cũ `customer_name`, `customer_phone` trong `reservations`. Chỉ chạy sau khi đã kiểm tra kết quả của 001. |

## Cách chạy một migration

Đứng trong thư mục `database/`, dùng client `mariadb` (các migration có dùng
`DELIMITER` nên không chạy được trong một số công cụ giao diện):

```bash
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source migrations/001_customers.sql"
```

## Nâng cấp lên bản có bảng `customers` (database đang có dữ liệu)

1. **Tắt ứng dụng** và **sao lưu database** (xem hướng dẫn backup).
2. Chạy `001_customers.sql`. Kết quả cuối in ra số khách, số lượt đặt bàn và số lượt
   chưa gắn khách (phải bằng 0). Nếu có số điện thoại bị gộp từ nhiều tên khác nhau,
   danh sách được in ngay phía trên: xem lại để chắc tên giữ lại là đúng.
3. Cập nhật mã nguồn lên bản mới, khởi động ứng dụng và thử tạo, nhận, hủy đặt bàn.
   **Phải chạy 001 trước khi chạy ứng dụng bản mới**, vì ứng dụng bản mới đọc bảng `customers`.
4. Khi đã chắc chắn mọi thứ đúng, chạy `002_drop_legacy_customer_columns.sql`
   để dọn hai cột cũ. Sau bước này tên và số điện thoại ghi riêng trong từng lượt đặt bàn
   không còn nữa.

Quy ước khách hàng: cùng số điện thoại (đã bỏ khoảng trắng và dấu `-`) là cùng một khách;
tên khách lấy theo lần đặt bàn gần nhất.
