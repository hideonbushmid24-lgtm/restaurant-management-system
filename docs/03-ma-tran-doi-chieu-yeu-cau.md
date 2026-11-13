# Ma trận đối chiếu yêu cầu

Tài liệu này đối chiếu từng yêu cầu trong báo cáo (mục 3.1 và 3.2) với use case, màn hình, mã nguồn, bảng dữ liệu
và kiểm thử tự động. Mục đích: chỉ ra yêu cầu nào đã được hiện thực và kiểm chứng, yêu cầu nào còn thiếu,
để báo cáo không khẳng định nhiều hơn những gì hệ thống thực sự có.

Nguồn đối chiếu: báo cáo `docs/report/Bao_cao_Quan_ly_nha_hang.docx`, mã nguồn `src/main/java/com/restaurant`,
câu lệnh SQL trong các lớp DAO và các lớp kiểm thử trong `src/test/java`.

## 1. Yêu cầu chức năng

| Mã | Yêu cầu | Ưu tiên | Use case | Màn hình | Service (phương thức chính) | Bảng dữ liệu | Kiểm thử tự động |
|---|---|---|---|---|---|---|---|
| FR-01 | Đăng nhập | Must | UC-01 | LoginFrame, DashboardFrame | AuthService.login | users | AuthServiceTest, PasswordUtilTest |
| FR-02 | Quản lý bàn (thêm, sửa, ngừng, kích hoạt lại) | Must | Chỉ có trong Use Case tổng quát | TableManagementFrame | TableService: addTable, updateTable, deleteTable | restaurant_tables, orders, reservations | TableServiceIntegrationTest |
| FR-03 | Xem bàn hiện có, lọc theo số bàn và trạng thái | Should | UC-02 | TableManagementFrame, ReservationFrame | TableService.search, ReservationService.getAvailableTables | restaurant_tables | Một phần (TableServiceIntegrationTest dùng `search`) |
| FR-04 | Đặt bàn (bàn, khách, số điện thoại, thời gian, số khách) | Must | UC-02 | ReservationFrame | ReservationService.createReservation | reservations, customers, restaurant_tables | ReservationServiceIntegrationTest |
| FR-05 | Cập nhật đặt bàn (khách nhận bàn, hủy) | Should | UC-02 | ReservationFrame | ReservationService: seatReservation, cancelReservation | reservations, restaurant_tables | ReservationServiceIntegrationTest, PaymentIntegrationTest |
| FR-06 | Xem thực đơn | Must | UC-03 | MenuFrame | MenuService: searchAvailableItems, getAvailableCategories | menu_items | **Chưa có** |
| FR-07 | Tìm món theo tên, danh mục, sắp xếp theo giá | Should | UC-03 | MenuFrame | MenuService.searchAvailableItems | menu_items | **Chưa có** |
| FR-08 | Tạo đơn gọi món | Must | UC-03 | OrderFrame | OrderService: createOrder, addItem | orders, order_items, restaurant_tables, reservations | OrderFlowIntegrationTest |
| FR-09 | Cập nhật đơn (tăng, giảm, hủy, gọi thêm, xác nhận phục vụ) | Must | UC-03 | OrderFrame | OrderService: updateItemQuantity, cancelItem, addItem, markItemServed | order_items, orders | OrderFlowIntegrationTest |
| FR-10 | Xem món tại bếp | Must | UC-04 | KitchenFrame | KitchenService.getActiveItems | order_items, orders, restaurant_tables | **Chưa có** |
| FR-11 | Cập nhật trạng thái món (chế biến, hoàn thành) | Must | UC-04 | KitchenFrame | KitchenService: startPreparing, markReady | order_items, orders | OrderFlowIntegrationTest |
| FR-12 | Tính tiền từ các món không bị hủy | Must | UC-05 | PaymentFrame | PaymentService.getPayableOrders, OrderService.getOrderTotal | order_items, orders | OrderFlowIntegrationTest, PaymentIntegrationTest |
| FR-13 | Ghi nhận thanh toán (giảm giá, phương thức, thu ngân) | Must | UC-05 | PaymentFrame | PaymentService.pay | payments, orders, restaurant_tables, reservations | PaymentIntegrationTest |
| FR-14 | Hiển thị hóa đơn | Must | UC-05 | PaymentFrame | PaymentService.pay (phiếu thanh toán) | invoices, payments | PaymentIntegrationTest |
| FR-15 | Mở lại đơn đang hoạt động theo bàn | Should | UC-03 | OrderFrame | OrderService: getActiveOrderByTable, getSeatedReservationIdByTable | orders, order_items, reservations | **Chưa có** |
| FR-16 | Báo cáo doanh thu theo khoảng ngày | Should | UC-06 | RevenueReportFrame | RevenueReportService: getSummary, findRecords | payments, invoices, orders | RevenueReportIntegrationTest |

Thống kê: 11/16 yêu cầu có kiểm thử trực tiếp, 1 yêu cầu có kiểm thử một phần (FR-03), 4 yêu cầu chưa có kiểm thử tự động
riêng (FR-06, FR-07, FR-10, FR-15). Các yêu cầu này vẫn được kiểm tra bằng tay qua [kịch bản demo](04-kich-ban-demo.md).

Ghi chú:
- "Must/Should" lấy từ bảng ưu tiên ở mục 3.3 của báo cáo.
- Cột "Bảng dữ liệu" lấy từ các câu lệnh SQL trong DAO tương ứng.
- Các test tích hợp chỉ chạy khi cấu hình `-Ddb.url` trỏ tới database `*_test` (xem `database/README.md`).

## 2. Yêu cầu phi chức năng

| Mã | Yêu cầu | Cách hệ thống đáp ứng | Kiểm chứng | Giới hạn |
|---|---|---|---|---|
| NFR-01 | Hiệu năng (khoảng 3 giây với dữ liệu mẫu) | Truy vấn đơn giản trên dữ liệu nhỏ | **Chưa đo** | Chưa có phép đo thời gian trên máy demo |
| NFR-02 | Bảo mật, phân quyền theo vai trò | Đăng nhập bằng tài khoản; `DashboardFrame` khóa nút theo vai trò; `PaymentDAO` kiểm tra vai trò khi thanh toán | AuthServiceTest; PaymentIntegrationTest (thu ngân và quản lý được thanh toán, phục vụ thì không) | Các chức năng khác chủ yếu chặn ở giao diện, chưa kiểm tra ở Service; mật khẩu băm SHA-256 không muối |
| NFR-03 | Toàn vẹn dữ liệu | Transaction và khóa dòng (`FOR UPDATE`); một bàn chỉ có một đơn hoạt động; món đã gửi bếp không sửa; sửa bàn không làm lệch trạng thái | OrderFlowIntegrationTest (có test đồng thời); PaymentIntegrationTest (hoàn tác khi lỗi, thanh toán đồng thời); TableServiceIntegrationTest | |
| NFR-04 | Khả dụng: báo lỗi dễ hiểu, không đóng chương trình | Service kiểm tra dữ liệu và ném lỗi có thông báo tiếng Việt; giao diện hiển thị hộp thoại | Các test kiểm tra thông báo từ chối (`assertLoi`) | Giao diện chưa có test tự động |
| NFR-05 | Lưu trữ lâu dài | MariaDB; mọi thay đổi ghi trong transaction | Tất cả test tích hợp đọc lại dữ liệu từ database | |
| NFR-06 | Dễ sử dụng, tiếng Việt thống nhất | Nhãn, thông báo, tên trạng thái bằng tiếng Việt | Kiểm tra bằng tay qua kịch bản demo | Chưa có đánh giá người dùng |

## 3. Use case, tác nhân và sơ đồ

| Use case | Tác nhân chính | Yêu cầu liên quan | Sơ đồ |
|---|---|---|---|
| UC-01 Đăng nhập | Tất cả vai trò | FR-01 | `UseCase_TongQuat_QuanLyNhaHang_HoanChinh.drawio` |
| UC-02 Đặt bàn | Nhân viên tiếp nhận | FR-03, FR-04, FR-05 | `Sequence_Dat_Ban.drawio` |
| UC-03 Gọi món | Nhân viên phục vụ | FR-06, FR-07, FR-08, FR-09, FR-15 | `Sequence_Goi_Mon.drawio` |
| UC-04 Xử lý món tại bếp | Nhân viên bếp | FR-10, FR-11 | (trong Use Case tổng quát) |
| UC-05 Thanh toán hóa đơn | Thu ngân | FR-12, FR-13, FR-14 | `Sequence_Thanh_Toan.drawio` |
| UC-06 Báo cáo doanh thu | Quản lý | FR-16 | (trong Use Case tổng quát) |
| (không có đặc tả riêng) Quản lý bàn | Quản lý, Nhân viên tiếp nhận | FR-02 | `UseCase_TongQuat_QuanLyNhaHang_HoanChinh.drawio` |

Các sơ đồ nằm trong [`uml/`](uml). Class Diagram và ERD được sinh từ mã nguồn và `schema.sql`
(xem [`uml/README.md`](uml/README.md)).

## 4. Khoảng trống đã phát hiện

1. **Liên kết use case:** trước đây các mục "Yêu cầu liên quan" ở chương 4 của báo cáo chưa phủ FR-02, FR-05, FR-06 và FR-15.
   Bản báo cáo kèm theo đã bổ sung FR-05 vào UC-02, FR-06 và FR-15 vào UC-03. FR-02 (quản lý bàn) chỉ có trong
   Use Case tổng quát, chưa có đặc tả chi tiết.
2. **Kiểm thử:** FR-06, FR-07, FR-10, FR-15 chưa có test tự động riêng; NFR-01 chưa được đo.
3. **Phân quyền:** mới được ép ở tầng dữ liệu cho thanh toán; các thao tác khác chỉ bị khóa ở giao diện.
4. **Giới hạn đã biết của hệ thống:** đặt bàn theo trạng thái bàn (chưa có lịch theo khung giờ, chưa chống trùng giờ),
   hóa đơn dạng tóm tắt, thực đơn chỉ tra cứu. Chi tiết trong mục "Giới hạn hiện tại" của `README.md`.
