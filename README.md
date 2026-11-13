# Hệ Thống Quản Lý Nhà Hàng (Restaurant Management System)

## 1. Thông tin môn học
- **Học phần:** Phân tích và thiết kế yêu cầu (Mã môn: 502050)
- **Nhóm thực hiện:** Nhóm 03

## 2. Giới thiệu

Ứng dụng desktop quản lý hoạt động chính của một nhà hàng: đặt bàn, gọi món, chế biến tại bếp,
thanh toán và báo cáo doanh thu. Mỗi nhân viên đăng nhập bằng tài khoản riêng và chỉ thấy các
chức năng thuộc vai trò của mình.

| Chức năng | Vai trò sử dụng |
|---|---|
| Quản lý bàn (thêm, sửa, xóa mềm bàn) | Quản lý, Nhân viên tiếp nhận |
| Đặt bàn, nhận bàn, hủy đặt bàn, lưu khách hàng | Quản lý, Nhân viên tiếp nhận |
| Tra cứu thực đơn và gọi món (tạo đơn, thêm/sửa/hủy món, gửi bếp, xác nhận phục vụ) | Quản lý, Nhân viên tiếp nhận, Nhân viên phục vụ |
| Xử lý tại bếp (nhận làm, báo hoàn thành món) | Quản lý, Nhân viên bếp |
| Thanh toán và hóa đơn | Quản lý, Thu ngân |
| Báo cáo doanh thu theo khoảng ngày | Quản lý |

## 3. Công nghệ

- **Ngôn ngữ:** Java 21
- **Giao diện:** Java Swing
- **Hệ quản trị CSDL:** MariaDB 10.4 trở lên (bản đi kèm XAMPP dùng được)
- **Kết nối CSDL:** JDBC (`mariadb-java-client`)
- **Quản lý build:** Maven
- **Kiểm thử:** JUnit 5
- **Quản lý phiên bản:** Git và GitHub

## 4. Yêu cầu cài đặt

1. **JDK 21** (không phải 17: `pom.xml` biên dịch với `release 21`).
2. **Maven** (hoặc dùng Extension Pack for Java của VS Code, có sẵn Maven).
3. **MariaDB 10.4+**. Trên Windows có thể dùng XAMPP; phải bật **MySQL** trong XAMPP Control Panel.

## 5. Cấu trúc thư mục

```
restaurant-management-system/
├── pom.xml                      Cấu hình Maven (Java 21, JDBC, JUnit 5)
├── config/                      Mẫu cấu hình kết nối riêng từng máy
├── database/                    Script database: schema, seed, migrations, reset, hướng dẫn
├── docs/                        Tài liệu phân tích: phạm vi, khảo sát, ma trận đối chiếu, kịch bản demo, UML, báo cáo, báo cáo tuần
├── tools/uml/                   Công cụ sinh Class Diagram và ERD từ mã nguồn và schema.sql
└── src/
    ├── main/java/com/restaurant/
    │   ├── Main.java            Điểm khởi động ứng dụng
    │   ├── config/              Kết nối database
    │   ├── model/               Các lớp dữ liệu (đơn, món, bàn, đặt bàn, ...)
    │   ├── dao/                 Truy cập database (SQL)
    │   ├── service/             Quy tắc nghiệp vụ và kiểm tra dữ liệu
    │   ├── ui/                  Các màn hình Swing
    │   └── util/                Tiện ích (băm mật khẩu)
    ├── main/resources/          application.properties (cấu hình mặc định cho demo)
    └── test/java/               Test tự động
```

## 6. Cài đặt và chạy (Windows + XAMPP)

### 6.1. Lấy mã nguồn
```powershell
git clone https://github.com/hideonbushmid24-lgtm/restaurant-management-system.git
cd restaurant-management-system
```

### 6.2. Tạo database
Bật MySQL trong XAMPP Control Panel, rồi chạy trong PowerShell:
```powershell
$env:Path += ";C:\xampp\mysql\bin"
cd database
mysql -u root --default-character-set=utf8mb4 -e "source create_database.sql"
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source schema.sql"
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "source seed.sql"
cd ..
```
Root của XAMPP mặc định không có mật khẩu; nếu có thì thêm `-p`. Chi tiết, cách reset và nâng cấp
database cũ: xem [`database/README.md`](database/README.md).

### 6.3. Cấu hình kết nối (thường không cần làm)
Mặc định ứng dụng kết nối `localhost:3306`, database `restaurant_management`, tài khoản
`restaurant_app` (tạo bởi `create_database.sql`). Nếu máy bạn khác (ví dụ MySQL dùng cổng khác):

1. Sao chép `config/local.properties.example` thành `config/local.properties`.
2. Sửa giá trị trong file mới. File này **không** bị đưa lên GitHub.

Các cách khác, theo thứ tự ưu tiên từ cao xuống thấp: tham số `-Ddb.url=...`, biến môi trường
`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`, file `config/local.properties`, rồi file mặc định
`src/main/resources/application.properties` (mật khẩu demo công khai, không dùng cho dữ liệu thật).
Hãy chạy ứng dụng từ thư mục gốc dự án để tìm thấy `config/local.properties`.

### 6.4. Chạy ứng dụng
```powershell
mvn compile exec:java
```
Hoặc mở `src/main/java/com/restaurant/Main.java` trong VS Code và chọn **Run**. Cửa sổ đăng nhập
sẽ hiện ra.

## 7. Tài khoản demo và phân quyền

Các tài khoản này do `database/seed.sql` tạo, mật khẩu **công khai trong repository**,
chỉ dùng để trình diễn:

| Vai trò | Tên đăng nhập | Mật khẩu | Các chức năng được dùng |
|---|---|---|---|
| Quản lý | `manager` | `manager123` | Tất cả |
| Nhân viên tiếp nhận | `receptionist` | `reception123` | Quản lý bàn, đặt bàn, thực đơn và gọi món |
| Nhân viên phục vụ | `waiter` | `waiter123` | Thực đơn và gọi món |
| Nhân viên bếp | `kitchen` | `kitchen123` | Xử lý tại bếp |
| Thu ngân | `cashier` | `cashier123` | Thanh toán và hóa đơn |

Luồng demo gợi ý: `receptionist` đặt bàn rồi nhận bàn, `waiter` tạo đơn và gửi bếp, `kitchen` nhận làm
và báo hoàn thành, `waiter` xác nhận phục vụ, `cashier` thanh toán, `manager` xem báo cáo doanh thu.

## 8. Chạy kiểm thử

Chạy ở thư mục gốc dự án:
```powershell
mvn test
```
Các test không cần database (đăng nhập, băm mật khẩu, cấu hình) luôn chạy. Các test nghiệp vụ dùng
database thật sẽ **tự bỏ qua** nếu chưa cấu hình, để không bao giờ xóa nhầm dữ liệu demo.

Để chạy đủ, tạo một database riêng có tên kết thúc bằng `_test` (hướng dẫn trong
[`database/README.md`](database/README.md)) rồi chạy:
```powershell
mvn test -Ddb.url=jdbc:mariadb://localhost:3306/restaurant_test
```

## 9. Sao lưu và phục hồi

Xem [`docs/huong-dan-backup-restore.md`](docs/huong-dan-backup-restore.md): cách sao lưu bằng
`mysqldump`, thử phục hồi vào database tạm và phục hồi thật.

## 10. Giới hạn hiện tại

Những điểm sau là giới hạn đã biết của bản hiện tại, không phải chức năng đã hoàn thiện:

- **Đặt bàn** dựa trên trạng thái bàn (trống / đã đặt / đang dùng), chưa có lịch theo khung giờ
  và chưa kiểm tra trùng thời gian đặt bàn.
- **Hóa đơn** hiển thị thông tin tóm tắt và tổng tiền; chưa liệt kê chi tiết từng món và chưa có
  chức năng tra cứu, in lại hóa đơn.
- **Phân quyền** chủ yếu thực hiện ở giao diện (ẩn/khóa nút); riêng thanh toán có kiểm tra vai trò ở
  tầng dữ liệu.
- **Mật khẩu** được băm SHA-256 không có muối (salt), chỉ phù hợp để trình diễn.
- **Giao diện** gọi truy vấn trực tiếp; nếu database phản hồi chậm, cửa sổ có thể bị đứng tạm thời.
- **Thực đơn** chỉ có màn hình tra cứu; chưa có chức năng thêm, sửa hoặc ngừng bán món trong ứng dụng
  (thực đơn được nạp từ `seed.sql` hoặc sửa trực tiếp trong database).
- Chưa có màn hình quản lý tài khoản người dùng và chưa có chức năng hủy hẳn một đơn gọi món đã tạo.

## 11. Tài liệu

- [`docs/01-pham-vi.md`](docs/01-pham-vi.md): phạm vi dự án
- [`docs/02-khao-sat-hien-trang.md`](docs/02-khao-sat-hien-trang.md): khảo sát hiện trạng
- [`docs/03-ma-tran-doi-chieu-yeu-cau.md`](docs/03-ma-tran-doi-chieu-yeu-cau.md): đối chiếu yêu cầu, use case, mã nguồn, bảng dữ liệu và kiểm thử
- [`docs/04-kich-ban-demo.md`](docs/04-kich-ban-demo.md): kịch bản trình diễn 5 chức năng, kèm tài khoản và dữ liệu mẫu
- [`docs/uml/`](docs/uml): sơ đồ Use Case, sơ đồ tuần tự, Class Diagram và ERD (file `.drawio` và ảnh PNG)
- [`docs/report/`](docs/report): báo cáo Word
- [`docs/weekly/`](docs/weekly): báo cáo tiến độ theo tuần
- [`docs/huong-dan-backup-restore.md`](docs/huong-dan-backup-restore.md): sao lưu và phục hồi
