# Kịch bản demo

Kịch bản trình diễn 5 chức năng chính của hệ thống theo đúng một vòng nghiệp vụ: đặt bàn, gọi món, bếp chế biến,
thanh toán, báo cáo doanh thu. Thời lượng khoảng 12 đến 15 phút. Mọi tên nút, tên màn hình và giá tiền dưới đây lấy
từ giao diện và dữ liệu mẫu (`database/seed.sql`) của bản hiện tại.

## 1. Chuẩn bị (làm trước buổi demo)

1. Bật **MySQL** trong XAMPP Control Panel.
2. Đưa database về trạng thái dữ liệu mẫu (xóa sạch dữ liệu cũ, nên sao lưu trước nếu cần giữ):
   ```powershell
   $env:Path += ";C:\xampp\mysql\bin"
   cd database
   mysql -u root --default-character-set=utf8mb4 -e "source reset_demo.sql"
   cd ..
   ```
3. Mở ứng dụng (`mvn compile exec:java` hoặc chạy `Main.java` trong VS Code). Nên mở sẵn các cửa sổ đăng nhập để
   chuyển vai trò nhanh: đăng xuất rồi đăng nhập lại bằng tài khoản khác.

Dữ liệu mẫu có sẵn: 8 bàn `B01` đến `B08` (2, 2, 4, 4, 4, 6, 6, 8 chỗ), 8 món, 5 tài khoản.

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản lý | `manager` | `manager123` |
| Nhân viên tiếp nhận | `receptionist` | `reception123` |
| Nhân viên phục vụ | `waiter` | `waiter123` |
| Nhân viên bếp | `kitchen` | `kitchen123` |
| Thu ngân | `cashier` | `cashier123` |

Các món dùng trong demo: Cơm chiên hải sản 85.000 đ, Gà chiên nước mắm 99.000 đ, Salad rau củ 49.000 đ,
Trà đào 39.000 đ, Coca-Cola 20.000 đ.

## 2. Kịch bản 1: Đăng nhập và phân quyền (FR-01, NFR-02)

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập `receptionist` / `reception123` | Màn hình chính hiện tên và vai trò "Nhân viên tiếp nhận"; chỉ các nút Quản lý bàn, Đặt bàn, Thực đơn và gọi món sáng; Xử lý tại bếp, Thanh toán, Báo cáo doanh thu bị khóa |
| 2 | Đăng xuất, đăng nhập `kitchen` | Chỉ nút Xử lý tại bếp sáng |
| 3 | Đăng xuất, đăng nhập sai mật khẩu | Hệ thống báo lỗi, không vào được |

## 3. Kịch bản 2: Đặt bàn và khách nhận bàn (FR-03, FR-04, FR-05)

Đăng nhập `receptionist`, mở **Đặt bàn**.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Bấm **Đặt bàn mới**; chọn bàn `B03` (4 chỗ); Tên khách hàng `Nguyễn Văn An`; Số điện thoại `0901 234 567`; Thời gian: một thời điểm trong tương lai theo định dạng `yyyy-MM-dd HH:mm`; Số khách `3`; Ghi chú `Gần cửa sổ` | Lượt đặt mới ở trạng thái "Đã xác nhận" trong danh sách |
| 2 | Mở **Quản lý bàn** (hoặc xem lại danh sách bàn) | Bàn `B03` có trạng thái "Đã đặt" |
| 3 | Thử đặt bàn `B01` (2 chỗ) cho 5 khách | Hệ thống từ chối và báo lỗi sức chứa; không tạo lượt đặt, bàn `B01` vẫn "Trống" |
| 4 | Chọn lượt đặt của `B03`, bấm **Khách nhận bàn** | Lượt đặt chuyển "Khách đã vào bàn"; bàn `B03` chuyển "Đang sử dụng" |

Ghi chú: số điện thoại được chuẩn hóa (bỏ khoảng trắng và dấu `-`) và dùng làm định danh khách. Nếu đặt thêm một bàn khác
với số `0901-234-567`, hệ thống nhận ra cùng một khách (bảng `customers` vẫn chỉ có một dòng cho khách này).

## 4. Kịch bản 3: Gọi món và gửi bếp (FR-06 đến FR-09, FR-15)

Đăng xuất, đăng nhập `waiter`, mở **Thực đơn và gọi món**.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Bấm **Xem thực đơn**, thử tìm theo tên và lọc theo danh mục | Chỉ hiện các món đang bán, đúng giá |
| 2 | Chọn bàn `B03`, bấm **Tạo hoặc mở đơn** | Đơn mới ở trạng thái "Đang gọi món", gắn với lượt đặt của khách An |
| 3 | **Thêm món**: Cơm chiên hải sản x2, Gà chiên nước mắm x1, Salad rau củ x1, Trà đào x1 | Tạm tính 357.000 đ |
| 4 | Chọn Salad rau củ, bấm **Hủy món** | Món chuyển "Đã hủy", tạm tính còn 308.000 đ |
| 5 | Chọn Trà đào, bấm **Tăng số lượng** (1 lên 2) | Tạm tính 347.000 đ |
| 6 | Bấm **Gửi xuống bếp** | Đơn chuyển "Đang chế biến"; các món "Chờ chế biến" |
| 7 | Thử bấm **Hủy món** hoặc **Tăng số lượng** với món đã gửi bếp | Hệ thống từ chối: chỉ sửa được khi đơn đang mở |
| 8 | Bấm **Chọn bàn khác**, sau đó chọn lại `B03` và **Tạo hoặc mở đơn** | Mở lại đúng đơn đang hoạt động của bàn `B03` (không tạo đơn thứ hai) |

## 5. Kịch bản 4: Bếp chế biến và phục vụ (FR-10, FR-11, FR-09)

Đăng nhập `kitchen`, mở **Xử lý tại bếp**.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Màn hình liệt kê 3 món của bàn `B03` ở "Chờ chế biến" | Đúng tên món, số lượng, bàn |
| 2 | Với từng món: **Bắt đầu chế biến**, rồi **Hoàn thành món** | Món lần lượt "Đang chế biến" rồi "Đã hoàn thành"; không thể nhảy thẳng sang hoàn thành |

Đăng nhập lại `waiter`, mở đơn bàn `B03`:

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 3 | Chọn từng món đã hoàn thành, bấm **Xác nhận đã phục vụ** | Khi cả 3 món đã phục vụ, đơn chuyển "Đã phục vụ" |
| 4 | **Thêm món** Coca-Cola x2 (gọi thêm) | Đơn mở lại "Đang gọi món"; ba món cũ vẫn "Đã phục vụ"; tạm tính 387.000 đ |
| 5 | **Gửi xuống bếp**; bếp hoàn thành món mới; phục vụ xác nhận | Đơn trở lại "Đã phục vụ" |

## 6. Kịch bản 5: Thanh toán và báo cáo doanh thu (FR-12 đến FR-14, FR-16)

Đăng nhập `cashier`, mở **Thanh toán và hóa đơn**.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Danh sách chỉ có các đơn "Đã phục vụ"; chọn đơn bàn `B03` | Tạm tính 387.000 đ |
| 2 | Ở ô **Giảm giá (VNĐ)** nhập `500000` rồi bấm **Thanh toán** | Hệ thống từ chối vì giảm giá vượt tạm tính |
| 3 | Nhập giảm giá `37000`, phương thức **Tiền mặt**, bấm **Thanh toán** | Thanh toán thành công; thực trả 350.000 đ; sinh mã hóa đơn |
| 4 | Bấm **Xem/In hóa đơn vừa tạo** | Hóa đơn gồm mã hóa đơn, bàn, tổng tiền, giảm giá, thực trả |
| 5 | Kiểm tra bàn `B03` (tài khoản `receptionist` hoặc `manager`) | Bàn "Trống"; lượt đặt của khách An "Hoàn tất" |

Đăng nhập `manager`, mở **Báo cáo doanh thu**:

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 6 | Giữ khoảng ngày mặc định (hôm nay, định dạng `dd/MM/yyyy`), bấm **Xem báo cáo** | 1 hóa đơn; giảm giá 37.000 đ; doanh thu thực nhận 350.000 đ |
| 7 | Nhập khoảng ngày chưa có giao dịch, ví dụ từ `01/01/2020` đến `01/01/2020` | Kết quả bằng 0, không báo lỗi |

## 7. Kịch bản bổ sung: các tình huống hệ thống từ chối (NFR-03, NFR-04)

Dùng khi còn thời gian, để chứng minh các ràng buộc nghiệp vụ.

| Tình huống | Cách thực hiện | Kết quả |
|---|---|---|
| Hai đơn hoạt động cùng một bàn | Bàn `B04`: tạo đơn, thử tạo đơn thứ hai cho cùng bàn (hoặc từ hai cửa sổ) | Chỉ một đơn được tạo; lần sau báo bàn đang có đơn chưa hoàn tất |
| Thanh toán đơn chưa phục vụ xong | Cashier chọn đơn vừa gửi bếp (không có trong danh sách thanh toán) | Không thể thanh toán đơn chưa phục vụ xong |
| Phân quyền | Đăng nhập `waiter`, thử mở **Thanh toán và hóa đơn** | Nút bị khóa |
| Đổi trạng thái bàn đang có khách | Bàn đang "Đang sử dụng", sửa bàn rồi đổi trạng thái về "Trống" | Hệ thống từ chối vì bàn còn đơn hoặc lượt đặt đang hiệu lực |

## 8. Kiểm tra dữ liệu sau demo (tùy chọn)

```powershell
mysql -u root --default-character-set=utf8mb4 restaurant_management -e "SELECT status FROM orders; SELECT COUNT(*) AS so_khach FROM customers; SELECT invoice_code, issued_at FROM invoices"
```

## 9. Lưu ý khi trình diễn

- Đặt bàn dựa trên trạng thái bàn, chưa có lịch theo khung giờ: nên nói rõ đây là giới hạn của phiên bản đồ án.
- Hóa đơn là dạng tóm tắt (tổng tiền, giảm giá, thực trả), chưa liệt kê từng món.
- Thực đơn chỉ tra cứu, không thêm hoặc sửa món trong ứng dụng.
- Sau buổi demo, chạy lại `reset_demo.sql` để về dữ liệu mẫu cho lần demo sau.
