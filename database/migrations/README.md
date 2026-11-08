# Migration (nâng cấp cấu trúc, giữ dữ liệu)

Mỗi file là một bước nâng cấp, đặt tên `NNN_mo_ta.sql` và chạy theo thứ tự số.

Quy ước bắt buộc cho mọi migration:

1. **Không** dùng `DROP DATABASE`, `DROP TABLE`, `TRUNCATE` hay `DELETE` không điều kiện.
2. Chạy lại không gây lỗi hoặc mất dữ liệu (dùng `IF NOT EXISTS`, kiểm tra trước khi chuyển dữ liệu).
3. Nếu bước nâng cấp thay đổi cấu trúc, `schema.sql` được cập nhật thành cấu trúc cuối cùng
   để cài mới không cần chạy migration.
4. **Sao lưu database trước khi chạy.**

## Danh sách

| Số | File | Nội dung |
|---|---|---|
| - | (chưa có) | |

## Cách chạy một migration

```bash
mariadb -u root -p restaurant_management < migrations/001_ten_migration.sql
```
