"""Kiểm tra file .drawio độc lập với trình tạo: đọc lại tọa độ từ XML.
Kiểm tra: id không trùng, đường nối trỏ tới hộp có thật, hộp không chồng nhau, đường nối không xuyên qua hộp.
Cách dùng:  python tools/uml/verify_drawio.py docs/uml/ERD_CoSoDuLieu_QuanLyNhaHang.drawio ...
"""
import sys
import xml.etree.ElementTree as ET


def verify(path):
    root = ET.parse(path).getroot()
    ok = True
    for page in root.iter("diagram"):
        cells = list(page.iter("mxCell"))
        ids = [c.get("id") for c in cells]
        assert len(ids) == len(set(ids)), "id bị trùng"
        vertices = {}
        for c in cells:
            if c.get("vertex") == "1":
                g = c.find("mxGeometry")
                vertices[c.get("id")] = tuple(float(g.get(k)) for k in ("x", "y", "width", "height"))
        edges = [c for c in cells if c.get("edge") == "1"]
        broken = [(e.get("id"), k) for e in edges for k in ("source", "target")
                  if e.get(k) and e.get(k) not in vertices]
        # chỉ xét các hộp lớp/bảng (bỏ nhãn lab_, ghi chú n..., nhóm g_)
        boxes = {i: g for i, g in vertices.items() if not i.startswith(("lab_", "n", "g_", "legend"))}
        overlap = [(a, b) for a in boxes for b in boxes if a < b
                   and boxes[a][0] < boxes[b][0] + boxes[b][2] and boxes[b][0] < boxes[a][0] + boxes[a][2]
                   and boxes[a][1] < boxes[b][1] + boxes[b][3] and boxes[b][1] < boxes[a][1] + boxes[a][3]]
        pierce = []
        for e in edges:
            g = e.find("mxGeometry")
            sp = g.find("mxPoint[@as='sourcePoint']")
            tp = g.find("mxPoint[@as='targetPoint']")
            pts = [(float(sp.get("x")), float(sp.get("y")))]
            pts += [(float(p.get("x")), float(p.get("y"))) for p in g.findall("Array/mxPoint")]
            pts += [(float(tp.get("x")), float(tp.get("y")))]
            for p, q in zip(pts, pts[1:]):
                for i, (x, y, w, h) in boxes.items():
                    if abs(p[1] - q[1]) < .01:
                        hit = y + 2 < p[1] < y + h - 2 and max(p[0], q[0]) > x + 2 and min(p[0], q[0]) < x + w - 2
                    else:
                        hit = x + 2 < p[0] < x + w - 2 and max(p[1], q[1]) > y + 2 and min(p[1], q[1]) < y + h - 2
                    if hit:
                        pierce.append((e.get("id"), i))
        print(f"  trang '{page.get('name')}': {len(vertices)} hộp/nhãn, {len(edges)} đường nối"
              f" | tham chiếu hỏng: {broken or 0} | hộp chồng: {overlap or 0} | đường xuyên hộp: {pierce or 0}")
        ok = ok and not broken and not overlap and not pierce
    return ok


if __name__ == "__main__":
    result = True
    for f in sys.argv[1:]:
        print(f)
        result = verify(f) and result
    sys.exit(0 if result else 1)
