# Hướng dẫn sao lưu và phục hồi database

Dành cho database `restaurant_management` chạy trên XAMPP (MariaDB) trên Windows.
Các lệnh chạy trong PowerShell. Cách sao lưu, phục hồi và thử phục hồi dưới đây
đã được kiểm tra trên dữ liệu đặt bàn có tiếng Việt: bản sao lưu phục hồi ra đủ số dòng
và giữ nguyên dấu.

> **Nguyên tắc:** sao lưu bằng file `.sql` (lệnh `mysqldump`), **không** chỉ sao chép
> thư mục `data` của XAMPP. Và một bản sao lưu chưa từng thử phục hồi thì chưa chắc dùng được.

## Khi nào phải sao lưu

- Trước khi chạy bất kỳ file nào trong `database/migrations/`.
- Trước khi chạy `reset_demo.sql` (script này xóa sạch dữ liệu).
- Trước buổi demo hoặc buổi báo cáo, và sau khi nhập xong dữ liệu mẫu quan trọng.
- Trước khi cập nhật XAMPP hoặc cài lại máy.

## 0. Chuẩn bị

1. Mở XAMPP Control Panel, bấm **Start** ở dòng **MySQL**.
2. Mở terminal, cho PowerShell biết chương trình nằm ở đâu (chỉ có tác dụng trong cửa sổ này):
   ```powershell
   $env:Path += ";C:\xampp\mysql\bin"
   mysqldump --version
   ```
3. Tạo thư mục lưu bản sao lưu **ngoài thư mục dự án**:
   ```powershell
   mkdir C:\Backup -Force
   ```
   File sao lưu chứa toàn bộ dữ liệu và mật khẩu đã băm của tài khoản, **không được
   đưa lên GitHub**. (`.gitignore` của dự án đã chặn các file `backup*.sql`, nhưng đặt ngoài
   dự án vẫn an toàn hơn.)

XAMPP mặc định tài khoản `root` không có mật khẩu. Nếu root của bạn có mật khẩu, thêm `-p`
sau `-u root` trong mọi lệnh bên dưới.

## 1. Sao lưu

```powershell
mysqldump -u root --default-character-set=utf8mb4 --single-transaction --result-file=C:\Backup\backup_restaurant.sql restaurant_management
```

- Dùng `--result-file=`, **không** dùng dấu `>`. PowerShell ghi file bằng bảng mã khác
  khi dùng `>`, làm hỏng tiếng Việt trong bản sao lưu.
- Muốn lưu nhiều bản theo ngày, đổi tên file, ví dụ `backup_restaurant_2026-11-09.sql`.

## 2. Kiểm tra file sao lưu

```powershell
Get-Item C:\Backup\backup_restaurant.sql | Select-Object Name, Length
Get-Content C:\Backup\backup_restaurant.sql -Tail 2
```

Dung lượng phải lớn hơn 0 và dòng cuối có chữ `Dump completed`.

## 3. Thử phục hồi vào database tạm (nên làm)

Bước này không động vào database thật:

```powershell
mysql -u root -e "CREATE DATABASE kiem_tra_backup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root --default-character-set=utf8mb4 kiem_tra_backup -e "source C:/Backup/backup_restaurant.sql"
mysql -u root --default-character-set=utf8mb4 -e "SELECT (SELECT COUNT(*) FROM restaurant_management.reservations) AS that, (SELECT COUNT(*) FROM kiem_tra_backup.reservations) AS ban_sao"
mysql -u root -e "DROP DATABASE kiem_tra_backup"
```

Hai số `that` và `ban_sao` phải bằng nhau. Lệnh cuối chỉ xóa database tạm
`kiem_tra_backup`. Trong lệnh `source`, đường dẫn dùng dấu gạch chéo `/`.

## 4. Phục hồi thật (khi mất hoặc hỏng dữ liệu)

**Lệnh này xóa database hiện tại và thay bằng nội dung bản sao lưu.** Nếu database hiện tại còn
đọc được, hãy sao lưu nó sang một file khác trước.

```powershell
mysql -u root -e "DROP DATABASE IF EXISTS restaurant_management; CREATE DATABASE restaurant_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source C:/Backup/backup_restaurant.sql"
```

Quyền của tài khoản `restaurant_app` được lưu riêng và vẫn còn sau khi tạo lại database.
Phục hồi xong, đăng nhập ứng dụng và kiểm tra số lượng đặt bàn, đơn hàng.

Nếu cần cấu trúc rỗng cùng dữ liệu demo thay vì phục hồi, xem `database/README.md` (mục reset).

## Khi XAMPP/MySQL không khởi động được

1. **Không xóa** thư mục `C:\xampp\mysql\data` và không cài lại XAMPP khi chưa sao chép
   thư mục đó sang nơi khác (ví dụ `C:\Backup\data_copy`).
2. Mở file nhật ký lỗi `C:\xampp\mysql\data\mysql_error.log` (hoặc bấm **Logs** ở dòng MySQL
   trong XAMPP Control Panel) và xem những dòng cuối.
3. Nếu có bản `.sql` gần đây trong `C:\Backup`, bạn có thể cài lại MySQL rồi phục hồi theo mục 4.
   Đây là lý do cần sao lưu bằng file `.sql` đều đặn.

## Lỗi thường gặp

| Thông báo | Cách xử lý |
|---|---|
| `Can't connect to MySQL server on 'localhost' (10061)` | MySQL chưa Start trong XAMPP |
| `Access denied for user 'root'@'localhost' (using password: NO)` | Root có mật khẩu, thêm `-p` |
| `mysqldump is not recognized` | Chưa chạy dòng `$env:Path` ở mục 0 trong cửa sổ này |
| `Unknown database 'restaurant_management'` | Sai tên hoặc chưa tạo; xem `SHOW DATABASES` |
| Tiếng Việt thành `?` hoặc ký tự lạ | Thiếu `--default-character-set=utf8mb4`, hoặc sao lưu bằng dấu `>` |
