#!/usr/bin/env python3
"""Sinh Communication Diagram cho 3 use case chính: UC-02 Đặt bàn, UC-03 Gọi món, UC-05 Thanh toán.

Luồng thông điệp lấy từ mã nguồn thật: Màn hình (ui) -> Service -> DAO -> Cơ sở dữ liệu.
Khác với Sequence Diagram (các đối tượng xếp hàng ngang, thời gian chạy dọc), Communication Diagram đặt các
đối tượng trên mặt phẳng, nối bằng đường liên kết; thứ tự gọi thể hiện bằng SỐ trên thông điệp
(1, 1.1, 1.1.1 ... là các lời gọi lồng nhau).

Kết quả ghi vào docs/uml/: Communication_Dat_Ban / Communication_Goi_Mon / Communication_Thanh_Toan
(.drawio và .png). Cách chạy:  python tools/uml/gen_communication.py
Ảnh PNG cần Pillow (pip install pillow); thiếu Pillow thì chỉ không có ảnh.
"""
import os
import sys
from xml.sax.saxutils import escape, quoteattr

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_lib import FONT, FONTB  # noqa: E402  (dùng chung phông chữ với các sơ đồ khác)

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:  # chỉ cần để xuất ảnh PNG
    Image = None

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "uml")

# ----------------------------------------------------------------------------------------------
# Hằng số bố cục
# ----------------------------------------------------------------------------------------------
COLX = [130, 740, 1350]      # tâm các cột
BOX_W = 250                  # bề rộng hộp đối tượng
ACTOR_W, ACTOR_H = 50, 90
FS = 12                      # cỡ chữ nhãn thông điệp
LINE_H = 17                  # chiều cao một dòng nhãn
ROW_GAP = 6                  # khoảng cách giữa các thông điệp
SIDE_PAD = 14                # từ đầu đường liên kết tới mũi tên
ARROW_LEN = 38               # chiều dài mũi tên thông điệp (đường ngang)
TEXT_X = SIDE_PAD + ARROW_LEN + 10   # từ đầu đường liên kết tới chữ (đường ngang), chừa chỗ sau mũi tên
LAYER_FILL = {"ui": "#dae8fc", "svc": "#d5e8d4", "dao": "#ffe6cc", "db": "#e1d5e7"}


def _font(bold=False, size=FS):
    return ImageFont.truetype(FONTB if bold else FONT, size)


def tw(text, bold=False, size=FS):
    """Đo bề rộng chữ (px). Thiếu Pillow thì ước lượng."""
    if Image is None:
        return len(text) * size * 0.6
    return _font(bold, size).getlength(text)


def natural(num):
    return tuple(int(p) for p in num.split("."))


def wrap(num, text, width, size=FS):
    """Ngắt dòng 'num: text' vừa bề rộng cho trước; trả (danh sách dòng, độ thụt dòng sau)."""
    prefix = num + ": "
    indent = tw(prefix, True, size)
    words = text.split(" ")
    lines, cur, cur_w = [], "", indent
    for w in words:
        add = tw((" " if cur else "") + w, False, size)
        if cur and cur_w + add > width:
            lines.append(cur)
            cur, cur_w = w, indent + tw(w, False, size)
        else:
            cur = (cur + " " + w) if cur else w
            cur_w += add
    lines.append(cur)
    return lines, indent


# ----------------------------------------------------------------------------------------------
# Mô tả 3 sơ đồ. Mỗi thông điệp: (số, nội dung, nguồn, đích, loại) với loại: call | return
# ----------------------------------------------------------------------------------------------
DIAGRAMS = [
    {
        "file": "Communication_Dat_Ban",
        "page": "Communication Đặt bàn",
        "title": "COMMUNICATION DIAGRAM — UC-02 ĐẶT BÀN",
        "req": "Liên kết yêu cầu: FR-03 Xem bàn trống, FR-04 Đặt bàn, FR-05 Cập nhật đặt bàn",
        "objects": [
            {"id": "actor", "kind": "actor", "label": "Nhân viên tiếp nhận", "col": 0, "row": 0},
            {"id": "ui", "layer": "ui", "name": ":ReservationFrame", "desc": "Màn hình đặt bàn", "col": 1, "row": 0},
            {"id": "svc", "layer": "svc", "name": ":ReservationService", "desc": "Nghiệp vụ đặt bàn", "col": 2, "row": 0,
             "selfs": [("2.2.1", "kiểm tra tên khách, số điện thoại (chuẩn hóa), số khách, thời gian, ghi chú")]},
            {"id": "dao", "layer": "dao", "name": ":ReservationDAO", "desc": "Truy cập dữ liệu", "col": 2, "row": 1},
            {"id": "db", "layer": "db", "name": ":Cơ sở dữ liệu", "desc": "MariaDB: restaurant_management", "col": 1, "row": 1},
        ],
        "messages": [
            ("1", 'Chọn "Đặt bàn mới"', "actor", "ui", "call"),
            ("1.1", "getAvailableTables()", "ui", "svc", "call"),
            ("1.1.1", "findAvailableTables()", "svc", "dao", "call"),
            ("1.1.1.1", "SELECT bàn đang hoạt động, trạng thái Trống", "dao", "db", "call"),
            ("1.2", "Hiển thị biểu mẫu và danh sách bàn trống", "ui", "actor", "return"),
            ("2", 'Chọn bàn, nhập tên khách, số điện thoại, thời gian, số khách, ghi chú; bấm "Đặt bàn"', "actor", "ui", "call"),
            ("2.1", "parseReservationTime(văn bản thời gian)", "ui", "svc", "call"),
            ("2.2", "createReservation(tableId, tên, SĐT, thời gian, số khách, ghi chú, userId)", "ui", "svc", "call"),
            ("2.2.2", "createReservation(cùng tham số, SĐT đã chuẩn hóa)", "svc", "dao", "call"),
            ("2.2.2.1", "BEGIN; SELECT bàn FOR UPDATE; kiểm tra: đang hoạt động, còn Trống, đủ sức chứa", "dao", "db", "call"),
            ("2.2.2.2", "Tìm khách theo SĐT; chưa có thì INSERT customers", "dao", "db", "call"),
            ("2.2.2.3", "INSERT reservations (Đã xác nhận)", "dao", "db", "call"),
            ("2.2.2.4", "UPDATE bàn = Đã đặt; COMMIT", "dao", "db", "call"),
            ("2.3", 'Hiển thị "Đặt bàn thành công" và nạp lại danh sách đặt bàn', "ui", "actor", "return"),
            ("2.4", "[có lỗi: bàn không còn trống, vượt sức chứa, dữ liệu sai] Hiển thị thông báo lỗi; ROLLBACK, không tạo lượt đặt",
             "ui", "actor", "return"),
        ],
        "note": ["Khi khách đến, nhân viên chọn lượt đặt và bấm \"Khách nhận bàn\":",
                 "seatReservation() cập nhật lượt đặt = Khách đã vào bàn, bàn = Đang dùng (cùng luồng ui, service, dao, csdl)."],
    },
    {
        "file": "Communication_Goi_Mon",
        "page": "Communication Gọi món",
        "title": "COMMUNICATION DIAGRAM — UC-03 GỌI MÓN",
        "req": "Liên kết yêu cầu: FR-06 Xem thực đơn, FR-07 Tìm món, FR-08 Tạo đơn, FR-09 Cập nhật đơn, FR-15 Mở lại đơn theo bàn",
        "objects": [
            {"id": "actor", "kind": "actor", "label": "Nhân viên phục vụ", "col": 0, "row": 0},
            {"id": "ui", "layer": "ui", "name": ":OrderFrame", "desc": "Màn hình gọi món", "col": 1, "row": 0},
            {"id": "svc", "layer": "svc", "name": ":OrderService", "desc": "Nghiệp vụ gọi món", "col": 2, "row": 0},
            {"id": "dao", "layer": "dao", "name": ":OrderDAO", "desc": "Truy cập dữ liệu", "col": 2, "row": 1},
            {"id": "db", "layer": "db", "name": ":Cơ sở dữ liệu", "desc": "MariaDB: restaurant_management", "col": 1, "row": 1},
        ],
        "messages": [
            ("1", 'Chọn bàn, bấm "Tạo hoặc mở đơn"', "actor", "ui", "call"),
            ("1.1", "getActiveOrderByTable(tableId)", "ui", "svc", "call"),
            ("1.1.1", "findActiveByTableId(tableId)", "svc", "dao", "call"),
            ("1.1.1.1", "SELECT đơn đang hoạt động của bàn", "dao", "db", "call"),
            ("1.2", "[chưa có đơn] createOrder(tableId, reservationId, waiterId, ghi chú)", "ui", "svc", "call"),
            ("1.2.1", "create(tableId, reservationId, waiterId, ghi chú)", "svc", "dao", "call"),
            ("1.2.1.1", "BEGIN; khóa bàn FOR UPDATE; kiểm tra bàn, đơn đang mở, lượt đặt, nhân viên", "dao", "db", "call"),
            ("1.2.1.2", "INSERT orders (Đang gọi món)", "dao", "db", "call"),
            ("1.2.1.3", "UPDATE bàn = Đang dùng, lượt đặt = Khách đã vào bàn (nếu có); COMMIT", "dao", "db", "call"),
            ("1.3", "Hiển thị đơn của bàn và tạm tính", "ui", "actor", "return"),
            ("2", 'Chọn món, số lượng, ghi chú; bấm "Thêm món"', "actor", "ui", "call"),
            ("2.1", "addItem(orderId, menuItemId, số lượng, ghi chú)", "ui", "svc", "call"),
            ("2.1.1", "addItem(orderId, menuItemId, số lượng, ghi chú)", "svc", "dao", "call"),
            ("2.1.1.1", "Kiểm tra đơn đang mở hoặc đã phục vụ; SELECT giá món đang bán", "dao", "db", "call"),
            ("2.1.1.2", "INSERT order_items (Chờ chế biến, giá tại thời điểm gọi); COMMIT", "dao", "db", "call"),
            ("2.2", "Cập nhật danh sách món và tạm tính", "ui", "actor", "return"),
            ("3", 'Bấm "Gửi xuống bếp"', "actor", "ui", "call"),
            ("3.1", "sendToKitchen(orderId)", "ui", "svc", "call"),
            ("3.1.1", "sendToKitchen(orderId)", "svc", "dao", "call"),
            ("3.1.1.1", "BEGIN; khóa đơn FOR UPDATE; kiểm tra đơn đang mở và có món chờ chế biến", "dao", "db", "call"),
            ("3.1.1.2", "UPDATE orders = Đang chế biến; COMMIT", "dao", "db", "call"),
            ("3.2", 'Hiển thị "Đã gửi đơn xuống bếp thành công"; khóa sửa món đã gửi', "ui", "actor", "return"),
            ("3.3", "[có lỗi] Hiển thị thông báo lỗi (ví dụ: bàn chưa nhận khách, bàn đã có đơn)", "ui", "actor", "return"),
        ],
        "note": ["Danh sách bàn và thực đơn được nạp khi mở màn hình (TableService, MenuService);",
                 "không vẽ để sơ đồ gọn. Bếp xử lý món ở UC-04 (KitchenFrame, KitchenService, KitchenDAO)."],
    },
    {
        "file": "Communication_Thanh_Toan",
        "page": "Communication Thanh toán",
        "title": "COMMUNICATION DIAGRAM — UC-05 THANH TOÁN HÓA ĐƠN",
        "req": "Liên kết yêu cầu: FR-12 Tính tiền, FR-13 Ghi nhận thanh toán, FR-14 Hiển thị hóa đơn",
        "objects": [
            {"id": "actor", "kind": "actor", "label": "Thu ngân", "col": 0, "row": 0},
            {"id": "ui", "layer": "ui", "name": ":PaymentFrame", "desc": "Màn hình thanh toán", "col": 1, "row": 0},
            {"id": "svc", "layer": "svc", "name": ":PaymentService", "desc": "Nghiệp vụ thanh toán", "col": 2, "row": 0,
             "selfs": [("3.1.1", "kiểm tra mã đơn, mã thu ngân, phương thức thanh toán")]},
            {"id": "dao", "layer": "dao", "name": ":PaymentDAO", "desc": "Truy cập dữ liệu (một giao dịch)", "col": 2, "row": 1},
            {"id": "db", "layer": "db", "name": ":Cơ sở dữ liệu", "desc": "MariaDB: restaurant_management", "col": 1, "row": 1},
        ],
        "messages": [
            ("1", 'Mở "Thanh toán và hóa đơn"', "actor", "ui", "call"),
            ("1.1", "getPayableOrders()", "ui", "svc", "call"),
            ("1.1.1", "findPayableOrders()", "svc", "dao", "call"),
            ("1.1.1.1", "SELECT đơn Đã phục vụ, bàn, nhân viên phục vụ, tạm tính", "dao", "db", "call"),
            ("1.2", "Hiển thị danh sách đơn cần thanh toán", "ui", "actor", "return"),
            ("2", "Chọn đơn, nhập giảm giá, chọn phương thức", "actor", "ui", "call"),
            ("2.1", "parseDiscountInput(văn bản giảm giá)", "ui", "svc", "call"),
            ("2.2", "Hiển thị tạm tính và thành tiền (hoặc \"Không hợp lệ\")", "ui", "actor", "return"),
            ("3", 'Bấm "Thanh toán", xác nhận', "actor", "ui", "call"),
            ("3.1", "pay(orderId, cashierId, giảm giá, phương thức)", "ui", "svc", "call"),
            ("3.1.2", "pay(orderId, cashierId, giảm giá, phương thức)", "svc", "dao", "call"),
            ("3.1.2.1", "BEGIN; khóa và kiểm tra thu ngân (vai trò Quản lý hoặc Thu ngân)", "dao", "db", "call"),
            ("3.1.2.2", "Khóa đơn FOR UPDATE; kiểm tra đã phục vụ xong, chưa thanh toán", "dao", "db", "call"),
            ("3.1.2.3", "SELECT tạm tính các món chưa hủy; kiểm tra giảm giá không vượt tạm tính", "dao", "db", "call"),
            ("3.1.2.4", "INSERT payments; INSERT invoices (mã HDxxxxxxxx)", "dao", "db", "call"),
            ("3.1.2.5", "UPDATE đơn = Đã thanh toán", "dao", "db", "call"),
            ("3.1.2.6", "UPDATE lượt đặt = Hoàn tất; bàn = Trống (hoặc Đã đặt nếu còn lượt khác); COMMIT", "dao", "db", "call"),
            ("3.2", "Hiển thị hóa đơn vừa tạo; nạp lại danh sách đơn", "ui", "actor", "return"),
            ("3.3", "[có lỗi: đơn chưa phục vụ xong, giảm giá sai, không đủ quyền] Hiển thị thông báo lỗi; ROLLBACK",
             "ui", "actor", "return"),
        ],
        "note": ["Hóa đơn chỉ được tạo trong cùng giao dịch với việc ghi nhận thanh toán;",
                 "mỗi đơn chỉ thanh toán một lần (kiểm tra ở bước 3.1.2.2)."],
    },
]


# ----------------------------------------------------------------------------------------------
# Dựng bố cục
# ----------------------------------------------------------------------------------------------
class Layout:
    def __init__(self, spec):
        self.spec = spec
        self.objs = {}        # id -> dict(x, y, w, h, ...)
        self.links = []       # (id, (x1,y1), (x2,y2), a_id, b_id)
        self.arrows = []      # (id, (x1,y1), (x2,y2), dashed)
        self.texts = []       # dict(id, x, y, w, h, lines, num, indent, align, kind)
        self.rects = []       # (tên, x, y, w, h) để kiểm tra chồng nhau
        self.errors = []

    # --- kích thước hộp đối tượng
    def build(self):
        spec = self.spec
        for o in spec["objects"]:
            if o.get("kind") == "actor":
                o["w"], o["h"] = ACTOR_W, ACTOR_H
                label_w = tw(o["label"], False, 12) + 12
                o["occ_w"] = max(ACTOR_W, label_w)
                o["occ_h"] = ACTOR_H + 24
            else:
                selfs = o.get("selfs", [])
                self_lines = []
                for num, txt in selfs:
                    ls, ind = wrap(num, txt, BOX_W - 36, 11)
                    self_lines.append((num, ls, ind))
                o["self_lines"] = self_lines
                n_lines = sum(len(ls) for _, ls, _ in self_lines)
                o["w"] = BOX_W
                o["h"] = 62 + (14 + n_lines * 15 if n_lines else 0)
                o["occ_w"], o["occ_h"] = o["w"], o["h"]
            self.objs[o["id"]] = o

        # nhóm thông điệp theo đường liên kết
        groups = {}
        for num, text, src, dst, kind in spec["messages"]:
            key = frozenset((src, dst))
            groups.setdefault(key, []).append((num, text, src, dst, kind))
        for key in groups:
            groups[key].sort(key=lambda m: natural(m[0]))
        self.groups = groups

        # chiều cao khối nhãn của từng đường liên kết (để tính khoảng cách các hàng)
        self.pre = {}
        need_above = 0
        need_vertical = 0
        for key, msgs in groups.items():
            a, b = [self.objs[i] for i in sorted(key, key=lambda i: (self.objs[i]["col"], self.objs[i]["row"]))]
            horizontal = a["row"] == b["row"]
            if horizontal:
                xl = a["cx_est"] if "cx_est" in a else COLX[a["col"]] + a["occ_w"] / 2
                xr = COLX[b["col"]] - b["occ_w"] / 2
                width = xr - xl - SIDE_PAD - TEXT_X - 10
                up, down = [], []
                for m in msgs:
                    ls, ind = wrap(m[0], m[1], width)
                    h = len(ls) * LINE_H + ROW_GAP
                    (up if m[2] == a["id"] else down).append((m, ls, ind, h))
                self.pre[key] = ("h", a, b, up, down)
                if a["row"] == 0:
                    need_above = max(need_above, sum(x[3] for x in up))
            else:
                width = 300
                dn, upl = [], []
                for m in msgs:
                    ls, ind = wrap(m[0], m[1], width)
                    h = len(ls) * LINE_H + ROW_GAP
                    (dn if m[2] == a["id"] else upl).append((m, ls, ind, h))
                self.pre[key] = ("v", a, b, dn, upl)
                need_vertical = max(need_vertical, sum(x[3] for x in dn) + sum(x[3] for x in upl))

        # vị trí hàng
        row_h0 = max(o["h"] for o in spec["objects"] if o["row"] == 0)
        row_h1 = max(o["h"] for o in spec["objects"] if o["row"] == 1)
        title_h = 110
        cy0 = title_h + max(need_above, 60) + 50
        cy1 = cy0 + row_h0 / 2 + max(need_vertical + 70, 230) + row_h1 / 2
        self.CY = [cy0, cy1]
        for o in spec["objects"]:
            o["cx"], o["cy"] = COLX[o["col"]], self.CY[o["row"]]
            o["x"], o["y"] = o["cx"] - o["w"] / 2, o["cy"] - o["h"] / 2
            self.rects.append((o["id"], o["cx"] - o["occ_w"] / 2, o["cy"] - o["h"] / 2, o["occ_w"], o["occ_h"]))

        # đặt thông điệp
        for key, pre in self.pre.items():
            if pre[0] == "h":
                self.place_h(key, *pre[1:])
            else:
                self.place_v(key, *pre[1:])

        # chiều cao toàn khung; chú thích đặt dưới cùng
        bottom = max([r[2] + r[4] for r in self.rects] + [t["y"] + t["h"] for t in self.texts])
        self.W = int(COLX[-1] + BOX_W / 2 + 70)
        self.legend(bottom + 30)
        self.H = int(self.note["y"] + self.note["h"] + 30)

    def place_h(self, key, a, b, up, down):
        y = a["cy"]
        xl = a["cx"] + a["occ_w"] / 2
        xr = b["cx"] - b["occ_w"] / 2
        lx1 = a["cx"] + (a["w"] / 2 if a.get("kind") == "actor" else a["w"] / 2)
        lx2 = b["cx"] - b["w"] / 2
        self.links.append(("link_%s_%s" % (a["id"], b["id"]), (lx1, y), (lx2, y), a["id"], b["id"]))
        # nhãn nằm trên đường liên kết (a -> b), dưới đường liên kết (b -> a)
        tot_up = sum(x[3] for x in up)
        top = y - 10 - tot_up
        for (m, ls, ind, h) in up:
            self.add_row_h(m, ls, ind, xl, xr, top, h, direction="r")
            top += h
        top = y + 10
        for (m, ls, ind, h) in down:
            self.add_row_h(m, ls, ind, xl, xr, top, h, direction="l")
            top += h

    def add_row_h(self, m, ls, ind, xl, xr, top, h, direction):
        num, text, src, dst, kind = m
        ax1, ax2 = xl + SIDE_PAD, xl + SIDE_PAD + ARROW_LEN
        ay = top + LINE_H / 2
        if direction == "l":
            ax1, ax2 = ax2, ax1
        self.arrows.append(("arr_" + num.replace(".", "_"), (ax1, ay), (ax2, ay), kind == "return"))
        tx = xl + TEXT_X
        wmax = max([tw(l) for l in ls]) + ind
        self.texts.append({"id": "lab_msg_" + num.replace(".", "_"), "x": tx, "y": top, "w": wmax + 6,
                           "h": len(ls) * LINE_H, "lines": ls, "num": num, "indent": ind, "align": "left", "kind": kind,
                           "limit": (tx, xr - 8)})

    def place_v(self, key, a, b, dn, upl):
        x = a["cx"]
        y1, y2 = a["cy"] + a["h"] / 2, b["cy"] - b["h"] / 2
        self.links.append(("link_%s_%s" % (a["id"], b["id"]), (x, y1), (x, y2), a["id"], b["id"]))
        top = y1 + 30
        for (m, ls, ind, h) in dn:
            self.add_row_v(m, ls, ind, x, top, h, down=True)
            top += h
        for (m, ls, ind, h) in upl:
            self.add_row_v(m, ls, ind, x, top, h, down=False)
            top += h

    def add_row_v(self, m, ls, ind, x, top, h, down):
        """Nhãn của đường liên kết dọc nằm bên trái đường (cột ngoài cùng không còn chỗ bên phải)."""
        num, text, src, dst, kind = m
        ay = top + LINE_H / 2
        wmax = max([tw(l) for l in ls]) + ind
        ax = x - 14
        if down:
            self.arrows.append(("arr_" + num.replace(".", "_"), (ax, ay - 8), (ax, ay + 8), kind == "return"))
        else:
            self.arrows.append(("arr_" + num.replace(".", "_"), (ax, ay + 8), (ax, ay - 8), kind == "return"))
        tx = x - 28 - (wmax + 6)
        align = "left"
        self.texts.append({"id": "lab_msg_" + num.replace(".", "_"), "x": tx, "y": top, "w": wmax + 6,
                           "h": len(ls) * LINE_H, "lines": ls, "num": num, "indent": ind, "align": align, "kind": kind,
                           "limit": None})

    def legend(self, y):
        lines = ["Chú thích",
                 "n: thông điệp, đánh số theo thứ tự gọi; 1.1 là lời gọi lồng trong 1",
                 "──▶  lời gọi          - - ▷  kết quả trả về, thông báo cho người dùng",
                 "[điều kiện]  chỉ thực hiện khi điều kiện đúng",
                 "↺  thông điệp tự gọi, ghi trong hộp đối tượng"]
        lines += [""] + self.spec["note"]
        w = self.W - 60
        h = 16 + len(lines) * 16
        x = 30
        self.note = {"x": x, "y": y, "w": w, "h": h, "lines": lines}
        self.rects.append(("legend", x, y, w, h))

    # --- kiểm tra bố cục
    def check(self):
        errs = []
        for t in self.texts:
            if t["limit"]:
                lo, hi = t["limit"]
                if t["x"] + t["w"] > hi + 0.5:
                    errs.append("nhãn %s vượt khoảng trống giữa hai đối tượng (%.0f > %.0f)" % (t["id"], t["x"] + t["w"], hi))
        items = [("obj:" + r[0], r[1], r[2], r[3], r[4]) for r in self.rects]
        items += [("txt:" + t["id"], t["x"], t["y"], t["w"], t["h"]) for t in self.texts]
        for i, a in enumerate(items):
            if a[1] < 0 or a[2] < 0 or a[1] + a[3] > self.W or a[2] + a[4] > self.H + 1:
                errs.append("%s nằm ngoài khung" % a[0])
            for b in items[i + 1:]:
                if a[1] < b[1] + b[3] and b[1] < a[1] + a[3] and a[2] < b[2] + b[4] and b[2] < a[2] + a[4]:
                    errs.append("%s chồng %s" % (a[0], b[0]))
        # mũi tên không được chạm hộp đối tượng
        for (aid, p, q, dashed) in self.arrows:
            x1, x2 = sorted((p[0], q[0]))
            y1, y2 = sorted((p[1], q[1]))
            for r in self.rects:
                if x1 < r[1] + r[3] and r[1] < x2 and y1 < r[2] + r[4] and r[2] < y2:
                    errs.append("mũi tên %s chạm %s" % (aid, r[0]))
        return errs


# ----------------------------------------------------------------------------------------------
# Xuất .drawio
# ----------------------------------------------------------------------------------------------
def html_row(t):
    first = "<b>%s:</b> %s" % (escape(t["num"]), escape(t["lines"][0]))
    rest = "".join("<br/>" + escape(l) for l in t["lines"][1:])
    color = "#7a2e2e" if t["kind"] == "return" else "#1f1f1f"
    return '<font color="%s">%s%s</font>' % (color, first, rest)


def obj_html(o):
    parts = ["<b><u>%s</u></b><br/><font style=\"font-size:11px\">%s</font>" % (escape(o["name"]), escape(o["desc"]))]
    if o["self_lines"]:
        parts.append('<hr size="1" style="margin:4px 0"/>')
        for num, ls, ind in o["self_lines"]:
            parts.append('<div style="text-align:left;font-size:11px">↺ <b>%s:</b> %s</div>' % (escape(num), escape(" ".join(ls))))
    return "".join(parts)


def to_drawio(L, path):
    out = ['<mxfile host="app.diagrams.net" agent="Claude" version="24.7.17" type="device">']
    out.append('<diagram id="d0" name=%s>' % quoteattr(L.spec["page"]))
    out.append('<mxGraphModel dx="%d" dy="%d" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
               'fold="1" page="1" pageScale="1" pageWidth="%d" pageHeight="%d" math="0" shadow="0"><root>'
               '<mxCell id="0"/><mxCell id="1" parent="0"/>' % (L.W, L.H, L.W, L.H))

    def vertex(i, value, style, x, y, w, h):
        out.append('<mxCell id="%s" value=%s style=%s vertex="1" parent="1"><mxGeometry x="%.1f" y="%.1f" width="%.1f" height="%.1f" as="geometry"/></mxCell>'
                   % (i, quoteattr(value), quoteattr(style), x, y, w, h))

    vertex("lab_title", L.spec["title"], "text;html=1;align=left;verticalAlign=middle;fontSize=18;fontStyle=1;", 30, 20, 1100, 34)
    vertex("lab_req", L.spec["req"], "text;html=1;align=left;verticalAlign=middle;fontSize=12;fontColor=#555555;", 30, 56, 1300, 24)

    for o in L.spec["objects"]:
        if o.get("kind") == "actor":
            vertex("obj_" + o["id"], o["label"],
                   "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fontSize=12;"
                   "strokeWidth=2;strokeColor=#333333;fillColor=#ffffff;", o["x"], o["y"], o["w"], o["h"])
        else:
            vertex("obj_" + o["id"], obj_html(o),
                   "rounded=0;whiteSpace=wrap;html=1;align=center;verticalAlign=top;spacingTop=4;fontSize=12;strokeWidth=2;"
                   "strokeColor=#333333;fillColor=%s;" % LAYER_FILL[o["layer"]], o["x"], o["y"], o["w"], o["h"])

    for (lid, p, q, a, b) in L.links:
        out.append('<mxCell id="%s" value="" style="html=1;edgeStyle=none;endArrow=none;startArrow=none;strokeColor=#333333;strokeWidth=2;" '
                   'edge="1" parent="1" source="obj_%s" target="obj_%s"><mxGeometry relative="1" as="geometry">'
                   '<mxPoint x="%.1f" y="%.1f" as="sourcePoint"/><mxPoint x="%.1f" y="%.1f" as="targetPoint"/></mxGeometry></mxCell>'
                   % (lid, a, b, p[0], p[1], q[0], q[1]))

    for (aid, p, q, dashed) in L.arrows:
        style = "html=1;edgeStyle=none;strokeColor=#333333;strokeWidth=1.5;endSize=7;"
        style += "endArrow=open;endFill=0;dashed=1;" if dashed else "endArrow=block;endFill=1;"
        out.append('<mxCell id="%s" value="" style="%s" edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
                   '<mxPoint x="%.1f" y="%.1f" as="sourcePoint"/><mxPoint x="%.1f" y="%.1f" as="targetPoint"/></mxGeometry></mxCell>'
                   % (aid, style, p[0], p[1], q[0], q[1]))

    for t in L.texts:
        vertex(t["id"], html_row(t), "text;html=1;whiteSpace=nowrap;align=left;verticalAlign=top;fontSize=12;spacing=0;"
               "spacingLeft=0;spacingTop=0;", t["x"], t["y"], t["w"], t["h"])

    n = L.note
    body = "<b>%s</b>" % escape(n["lines"][0]) + "".join("<br/>" + escape(l) for l in n["lines"][1:])
    vertex("legend_note", body, "rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=8;spacingTop=6;fontSize=11;"
           "fillColor=#fff7e6;strokeColor=#e0b060;", n["x"], n["y"], n["w"], n["h"])
    out.append("</root></mxGraphModel></diagram></mxfile>")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out))


# ----------------------------------------------------------------------------------------------
# Xuất PNG (xem nhanh)
# ----------------------------------------------------------------------------------------------
def render_png(L, path, scale=2):
    if Image is None:
        print("  (bỏ qua ảnh PNG: chưa cài Pillow, chạy: pip install pillow)")
        return
    S = scale
    img = Image.new("RGB", (int(L.W * S), int(L.H * S)), "white")
    d = ImageDraw.Draw(img)
    f = lambda size, bold=False: ImageFont.truetype(FONTB if bold else FONT, int(size * S))

    d.text((30 * S, 22 * S), L.spec["title"], font=f(18, True), fill="#111")
    d.text((30 * S, 58 * S), L.spec["req"], font=f(12), fill="#555")

    # đường liên kết
    for (lid, p, q, a, b) in L.links:
        d.line([(p[0] * S, p[1] * S), (q[0] * S, q[1] * S)], fill="#333", width=int(2 * S))

    # đối tượng
    for o in L.spec["objects"]:
        if o.get("kind") == "actor":
            cx, top = o["cx"], o["y"]
            d.ellipse([(cx - 9) * S, (top + 2) * S, (cx + 9) * S, (top + 20) * S], outline="#333", width=int(2 * S), fill="white")
            d.line([(cx * S, (top + 20) * S), (cx * S, (top + 54) * S)], fill="#333", width=int(2 * S))
            d.line([((cx - 22) * S, (top + 32) * S), ((cx + 22) * S, (top + 32) * S)], fill="#333", width=int(2 * S))
            d.line([(cx * S, (top + 54) * S), ((cx - 18) * S, (top + 86) * S)], fill="#333", width=int(2 * S))
            d.line([(cx * S, (top + 54) * S), ((cx + 18) * S, (top + 86) * S)], fill="#333", width=int(2 * S))
            d.text((cx * S, (top + 92) * S), o["label"], font=f(12), fill="#111", anchor="ma")
        else:
            x, y, w, h = o["x"], o["y"], o["w"], o["h"]
            d.rectangle([x * S, y * S, (x + w) * S, (y + h) * S], fill=LAYER_FILL[o["layer"]], outline="#333", width=int(2 * S))
            name_font = f(13, True)
            d.text((o["cx"] * S, (y + 9) * S), o["name"], font=name_font, fill="#111", anchor="ma")
            ww = name_font.getlength(o["name"])
            d.line([(o["cx"] * S - ww / 2, (y + 27) * S), (o["cx"] * S + ww / 2, (y + 27) * S)], fill="#111", width=int(1 * S))
            d.text((o["cx"] * S, (y + 33) * S), o["desc"], font=f(11), fill="#333", anchor="ma")
            if o["self_lines"]:
                yy = y + 56
                d.line([(x * S, yy * S), ((x + w) * S, yy * S)], fill="#333", width=int(1 * S))
                yy += 6
                for num, ls, ind in o["self_lines"]:
                    for k, l in enumerate(ls):
                        if k == 0:
                            d.text(((x + 8) * S, yy * S), "↺ " + num + ":", font=f(11, True), fill="#111")
                            off = tw("↺ " + num + ": ", True, 11)
                            d.text(((x + 8) * S + off * S, yy * S), l, font=f(11), fill="#111")
                        else:
                            d.text(((x + 8 + tw("↺ " + num + ": ", True, 11)) * S, yy * S), l, font=f(11), fill="#111")
                        yy += 15

    # mũi tên thông điệp
    import math
    for (aid, p, q, dashed) in L.arrows:
        x1, y1, x2, y2 = p[0] * S, p[1] * S, q[0] * S, q[1] * S
        if dashed:
            n = max(1, int(math.hypot(x2 - x1, y2 - y1) // (6 * S)))
            for i in range(0, n, 2):
                a = (x1 + (x2 - x1) * i / n, y1 + (y2 - y1) * i / n)
                b = (x1 + (x2 - x1) * min(i + 1, n) / n, y1 + (y2 - y1) * min(i + 1, n) / n)
                d.line([a, b], fill="#333", width=int(1.5 * S))
        else:
            d.line([(x1, y1), (x2, y2)], fill="#333", width=int(1.5 * S))
        ang = math.atan2(y2 - y1, x2 - x1)
        L_, W_ = 10 * S, 4.5 * S
        bx, by = x2 - L_ * math.cos(ang), y2 - L_ * math.sin(ang)
        left = (bx + W_ * math.sin(ang), by - W_ * math.cos(ang))
        right = (bx - W_ * math.sin(ang), by + W_ * math.cos(ang))
        if dashed:
            d.line([left, (x2, y2), right], fill="#333", width=int(1.5 * S))
        else:
            d.polygon([(x2, y2), left, right], fill="#333")

    # nhãn thông điệp
    for t in L.texts:
        color = "#7a2e2e" if t["kind"] == "return" else "#1f1f1f"
        yy = t["y"]
        for k, line in enumerate(t["lines"]):
            if k == 0:
                num = t["num"] + ":"
                d.text((t["x"] * S, yy * S), num, font=f(FS, True), fill=color)
                d.text(((t["x"] + t["indent"]) * S, yy * S), line, font=f(FS), fill=color)
            else:
                d.text(((t["x"] + t["indent"]) * S, yy * S), line, font=f(FS), fill=color)
            yy += LINE_H

    # chú thích
    n = L.note
    d.rectangle([n["x"] * S, n["y"] * S, (n["x"] + n["w"]) * S, (n["y"] + n["h"]) * S], fill="#fff7e6", outline="#e0b060", width=int(1 * S))
    yy = n["y"] + 8
    for k, line in enumerate(n["lines"]):
        d.text(((n["x"] + 10) * S, yy * S), line, font=f(11, k == 0), fill="#533")
        yy += 16
    img.save(path)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    failed = False
    for spec in DIAGRAMS:
        L = Layout(spec)
        L.build()
        errs = L.check()
        print("%s: %d đối tượng, %d thông điệp, khung %dx%d" % (spec["file"], len(spec["objects"]), len(spec["messages"]), L.W, L.H))
        if errs:
            failed = True
            for e in errs:
                print("  LỖI BỐ CỤC:", e)
            continue
        base = os.path.join(OUT_DIR, spec["file"])
        to_drawio(L, base + ".drawio")
        render_png(L, base + ".png")
        print("  đã ghi", os.path.relpath(base + ".drawio"), "và .png")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
