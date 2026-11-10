# CHƯƠNG 2. KHẢO SÁT VÀ MÔ TẢ HIỆN TRẠNG

## 2.1. Các đối tượng liên quan
Các đối tượng liên quan đến hoạt động nhà hàng gồm khách hàng, nhân viên tiếp nhận, nhân viên phục vụ, nhân viên bếp, thu ngân và quản lý. Mỗi đối tượng tham gia một phần của quy trình, từ tiếp nhận nhu cầu của khách đến hoàn tất đơn hàng.

- **Khách hàng (Stakeholder):** Yêu cầu đặt bàn, gọi món, nhận món và thanh toán. (Không tương tác trực tiếp với hệ thống, nhân viên nhập thông tin thay khách).
- **Nhân viên tiếp nhận (Actor):** Tiếp nhận đặt bàn, sắp xếp bàn, xác nhận khách nhận bàn hoặc hủy lượt đặt.
- **Nhân viên phục vụ (Actor):** Đón khách, ghi nhận món khách gọi, gửi món xuống bếp và xác nhận món đã phục vụ.
- **Nhân viên bếp (Actor):** Nhận danh sách món cần làm, cập nhật tiến độ chế biến.
- **Thu ngân (Actor):** Kiểm tra đơn hàng, tính tiền, xác nhận thanh toán.
- **Quản lý nhà hàng (Actor):** Theo dõi hoạt động, quản lý bàn, tra cứu thực đơn và xem báo cáo doanh thu.

## 2.2. Quy trình phục vụ hiện tại
Trong mô hình nhà hàng được nhóm sử dụng để phân tích, quy trình diễn ra như sau:
1. **Liên hệ / Đặt bàn:** Khách hàng liên hệ đặt bàn trước hoặc đến nhà hàng. Nhân viên tiếp nhận kiểm tra bàn và ghi yêu cầu đặt bàn vào sổ nếu khách đặt trước.
2. **Đón khách:** Nhân viên phục vụ đón khách, kiểm tra đặt bàn hoặc tìm bàn trống và hướng dẫn khách vào bàn.
3. **Gọi món:** Nhân viên phục vụ nhận yêu cầu gọi món và ghi món, số lượng vào phiếu.
4. **Xử lý bếp:** Nhân viên bếp nhận phiếu gọi món, chế biến và thông báo khi món hoàn thành.
5. **Phục vụ:** Nhân viên phục vụ mang món ra bàn; ghi thêm phiếu nếu khách gọi bổ sung.
6. **Thanh toán:** Thu ngân nhận các phiếu gọi món, tính tổng tiền, thu tiền và đưa hóa đơn.

## 2.3. Các hạn chế của quy trình hiện tại
- **Kiểm tra và ghi đặt bàn:** Thông tin bàn trống không được cập nhật kịp thời; có nguy cơ ghi trùng bàn. Khách phải chờ hoặc nhân viên phải sắp xếp lại bàn.
- **Ghi món bằng phiếu giấy:** Chữ khó đọc, ghi thiếu món hoặc sai số lượng. Bếp chế biến sai, nhân viên phải xác nhận lại.
- **Chuyển phiếu cho bếp:** Khó theo dõi món nào đang chờ, đang chế biến hoặc đã hoàn thành. Nhân viên phục vụ khó thông báo tiến độ cho khách.
- **Ghi nhận món gọi thêm:** Phiếu gọi thêm có thể tách khỏi phiếu ban đầu. Dễ bỏ sót món khi phục vụ hoặc tính tiền.
- **Tính tiền từ nhiều phiếu:** Thu ngân phải đối chiếu và cộng thủ công. Mất thời gian, có nguy cơ tính thiếu hoặc tính sai.