# Công cụ sinh sơ đồ UML

Class Diagram và ERD trong `docs/uml/` được sinh trực tiếp từ mã nguồn và cấu trúc database, để sơ đồ luôn khớp
với hệ thống thật. Khi sửa mô hình dữ liệu hoặc các lớp, chạy lại để cập nhật.

| File | Việc làm |
|---|---|
| `gen_erd.py` | Đọc `database/schema.sql`, vẽ ERD 9 bảng, mọi khóa ngoại tự có đường nối |
| `gen_class.py` | Đọc các lớp trong `src/main/java/com/restaurant`, vẽ 2 trang: domain model và kiến trúc phân tầng. Dừng và báo lỗi nếu quan hệ UI, Service, DAO trong mã khác với sơ đồ |
| `verify_drawio.py` | Kiểm tra độc lập file `.drawio`: tham chiếu hợp lệ, hộp không chồng nhau, đường nối không xuyên hộp |
| `diagram_lib.py` | Thư viện dùng chung |

## Cách chạy

Cần Python 3. Ảnh PNG xem trước cần thêm thư viện Pillow (không bắt buộc, thiếu thì chỉ không có ảnh):

```powershell
pip install pillow
python tools/uml/gen_erd.py
python tools/uml/gen_class.py
python tools/uml/verify_drawio.py docs/uml/ERD_CoSoDuLieu_QuanLyNhaHang.drawio docs/uml/ClassDiagram_QuanLyNhaHang.drawio
```

Kết quả ghi vào `docs/uml/`. Mở file `.drawio` bằng [diagrams.net](https://app.diagrams.net) hoặc extension
Draw.io Integration trong VS Code để chỉnh sửa; có thể xuất lại PNG hoặc PDF từ đó.
