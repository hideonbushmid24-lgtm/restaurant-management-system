# CHƯƠNG 1. GIỚI THIỆU ĐỀ TÀI

## 1.1. Lý do chọn đề tài
Trong hoạt động của một nhà hàng, các công việc tiếp nhận đặt bàn, ghi nhận món ăn, chuyển yêu cầu đến bếp và thanh toán diễn ra liên tục, có sự phối hợp giữa nhiều nhân viên. Nếu thông tin được ghi chép trên giấy hoặc trao đổi trực tiếp, nhà hàng có thể gặp tình trạng nhầm bàn, thiếu món, chậm cập nhật tiến độ chế biến hoặc mất thời gian đối chiếu khi thanh toán. Những khó khăn này càng dễ xuất hiện vào thời điểm đông khách.

Xuất phát từ nhu cầu đó, nhóm lựa chọn đề tài “Hệ thống quản lý nhà hàng” để phân tích quy trình phục vụ khách ăn tại chỗ và đề xuất cách quản lý thông tin thống nhất. Hệ thống tập trung vào bốn nghiệp vụ chính: đặt bàn, gọi món, xử lý món tại bếp và thanh toán. Thông qua đề tài, nhóm có thể vận dụng kiến thức của môn Phân tích và thiết kế yêu cầu vào việc khảo sát hiện trạng, xác định và đặc tả yêu cầu, mô hình hóa hệ thống bằng UML, thiết kế cơ sở dữ liệu và xây dựng chương trình Java minh họa các chức năng quan trọng.

## 1.2. Mục tiêu của hệ thống
- **Mục tiêu chung:** Phân tích, thiết kế và xây dựng chương trình minh họa bằng Java nhằm hỗ trợ nhà hàng quản lý quy trình phục vụ khách ăn tại chỗ, từ đặt bàn, gọi món, xử lý món tại bếp đến thanh toán.
- **Mục tiêu cụ thể:**
  - Quản lý danh sách bàn và trạng thái sử dụng của từng bàn.
  - Ghi nhận thông tin đặt bàn gồm khách hàng, thời gian, số lượng khách và bàn được chọn.
  - Lập đơn gọi món theo bàn, lưu món ăn, số lượng và đơn giá.
  - Cho phép bộ phận bếp xem các món cần chế biến và cập nhật trạng thái xử lý.
  - Tính tổng tiền từ các món đã gọi, ghi nhận thanh toán và hiển thị thông tin hóa đơn.
  - Lưu trữ dữ liệu để nhân viên có thể tra cứu đặt bàn, đơn gọi món và hóa đơn khi cần.

## 1.3. Phạm vi hệ thống
Đề tài tập trung vào hoạt động phục vụ khách ăn tại một nhà hàng. Chương trình Java được xây dựng để minh họa các nghiệp vụ chính từ lúc tiếp nhận đặt bàn đến khi hoàn tất thanh toán.

**Các chức năng trong phạm vi:**
- Đăng nhập và phân quyền: Xác thực tài khoản nhân viên và mở chức năng theo vai trò.
- Quản lý bàn: Thêm, sửa, ngừng hoạt động bàn; xem danh sách và trạng thái bàn: trống, đã đặt, đang phục vụ.
- Đặt bàn: Ghi nhận khách hàng, thời gian, số lượng khách và bàn được đặt.
- Quản lý thực đơn cơ bản: Xem thông tin món ăn và giá để lập đơn gọi món.
- Gọi món: Tạo đơn theo bàn, chọn món và số lượng.
- Xử lý bếp: Xem món được gọi và cập nhật trạng thái chế biến.
- Thanh toán: Tính tổng tiền, áp dụng giảm giá, ghi nhận đã thanh toán và hiển thị hóa đơn.
- Báo cáo doanh thu: Quản lý xem số hóa đơn, giảm giá và doanh thu thực nhận theo khoảng ngày.

**Các chức năng ngoài phạm vi (Giới hạn đồ án):**
- Giao hàng: Chỉ xét khách ăn tại nhà hàng.
- Quản lý lương và chấm công: Không triển khai nghiệp vụ nhân sự.
- Kế toán chuyên sâu: Chỉ tính và ghi nhận hóa đơn của đơn hàng.
- Kết nối cổng thanh toán: Chỉ ghi nhận kết quả thanh toán, không xử lý giao dịch ngân hàng.
- Quản lý nhiều chi nhánh: Chỉ xét một nhà hàng.