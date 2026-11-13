# Sơ đồ UML

Mở các file `.drawio` bằng [diagrams.net](https://app.diagrams.net) hoặc extension **Draw.io Integration** trong VS Code.

| File | Nội dung | Nguồn |
|---|---|---|
| `UseCase_TongQuat_QuanLyNhaHang_HoanChinh.drawio` | Use Case tổng quát, 5 tác nhân | Vẽ tay |
| `Sequence_Dat_Ban.drawio` | Tuần tự: đặt bàn | Vẽ tay |
| `Sequence_Goi_Mon.drawio` | Tuần tự: gọi món | Vẽ tay |
| `Sequence_Thanh_Toan.drawio` | Tuần tự: thanh toán | Vẽ tay |
| `ClassDiagram_QuanLyNhaHang.drawio` | Class Diagram, 2 trang: domain model và kiến trúc phân tầng (UI, Service, DAO) | **Sinh từ mã nguồn** bằng `tools/uml/gen_class.py` |
| `ERD_CoSoDuLieu_QuanLyNhaHang.drawio` | ERD 9 bảng, 11 khóa ngoại | **Sinh từ `database/schema.sql`** bằng `tools/uml/gen_erd.py` |
| `ClassDiagram_DomainModel.png`, `ClassDiagram_KienTrucPhanTang.png`, `ERD_CoSoDuLieu_QuanLyNhaHang.png` | Ảnh xem nhanh của hai sơ đồ trên (độ phân giải cao, phóng to được) | Sinh cùng lúc |

Ghi chú:
- Class Diagram và ERD được sinh tự động nên luôn khớp mã nguồn và database. Nếu sửa tay trong diagrams.net rồi
  chạy lại công cụ, bản sinh sẽ ghi đè phần sửa tay. Cách chạy xem [`tools/uml/README.md`](../../tools/uml/README.md).
- Class Diagram: `Customer` là bảng `customers` nhưng chưa có lớp Java riêng (vẽ nét đứt); tên và số điện thoại khách
  được nạp vào `Reservation` qua JOIN.
- Ảnh PNG do công cụ tự vẽ để xem nhanh. Muốn ảnh theo kiểu giao diện diagrams.net, mở file `.drawio` rồi
  chọn File, Export as, PNG.
