import os,re,sys
HERE=os.path.dirname(os.path.abspath(__file__)); ROOT=os.path.abspath(os.path.join(HERE,"..",".."))
sys.path.insert(0,HERE)
from diagram_lib import *
SCHEMA=os.path.join(ROOT,"database","schema.sql")
sch=open(SCHEMA,encoding="utf-8").read()

# ---------- 1) đọc schema ----------
tables={}; order=[]
for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\);",sch,re.S):
    name,body=m.group(1),m.group(2); order.append(name)
    depth=0; cur=""; parts=[]
    for ch in body:
        if ch=="(": depth+=1
        if ch==")": depth-=1
        if ch=="," and depth==0: parts.append(cur); cur=""
        else: cur+=ch
    parts.append(cur)
    cols=[]; fks=[]; uqs=set()
    for p in parts:
        p=" ".join(p.split())
        if not p: continue
        mm=re.match(r"CONSTRAINT \w+ FOREIGN KEY \((\w+)\) REFERENCES (\w+)\((\w+)\)",p)
        if mm: fks.append((mm.group(1),mm.group(2),mm.group(3))); continue
        mm=re.match(r"CONSTRAINT \w+ UNIQUE \(([\w, ]+)\)",p)
        if mm: uqs.update(c.strip() for c in mm.group(1).split(",") if "," not in mm.group(1)); continue
        if re.match(r"(CONSTRAINT|PRIMARY|UNIQUE|FOREIGN|CHECK|KEY|INDEX)",p): continue
        mm=re.match(r"(\w+) (.+)",p)
        if not mm: continue
        cname,defn=mm.group(1),mm.group(2)
        t=re.match(r"(ENUM|[A-Z]+(?:\(\d+(?:,\s*\d+)?\))?)",defn).group(1).replace(" ","")
        cols.append(dict(name=cname,type=t,pk="PRIMARY KEY" in defn,nn=("NOT NULL" in defn) or ("PRIMARY KEY" in defn),uq=("UNIQUE" in defn)))
    for c in cols:
        if c["name"] in uqs: c["uq"]=True
    tables[name]=dict(cols=cols,fks=fks)
fk_cols={(t,c) for t,v in tables.items() for c,_,_ in v["fks"]}
print("Bảng:",len(tables),"| Khóa ngoại:",sum(len(v['fks']) for v in tables.values()))
for t,v in tables.items():
    for c,rt,rc in v["fks"]: print(f"  {t}.{c} -> {rt}.{rc}")

# ---------- 2) hộp bảng ----------
COLORS={"users":("#e8f0fe","#4a74c9"),"customers":("#fff4e5","#e08a00"),"restaurant_tables":("#e6f7ef","#2f9e6e"),
        "reservations":("#e6f7ef","#2f9e6e"),"menu_items":("#fff9db","#d4a800"),"orders":("#fff9db","#d4a800"),
        "order_items":("#fff9db","#d4a800"),"payments":("#f3e8ff","#8a4fd0"),"invoices":("#f3e8ff","#8a4fd0")}
def build_box(name):
    v=tables[name]; fill,stroke=COLORS[name]
    lines=[("title",name),("hr","")]
    for c in v["cols"]:
        flag="PK" if c["pk"] else ("FK" if (name,c["name"]) in fk_cols else "")
        if c["uq"] and not c["pk"]: flag=(flag+" UQ").strip()
        txt=f"{flag:<7}{c['name']:<17}{c['type']}"
        lines.append(("text",txt))
    w=max(text_width(lines,mono=True),250)
    return Box("t_"+name,0,0,w,lines_height(lines),lines,fill=fill,stroke=stroke,mono=True)
B={n:build_box(n) for n in tables}

# ---------- 3) lưới bố trí ----------
GRID=[["customers","reservations","restaurant_tables","invoices"],
      ["users",None,"orders","payments"],
      [None,"menu_items","order_items",None]]
GAPX,GAPY,X0,Y0=120,100,40,90
colw=[max(B[g[c]].w for g in GRID if g[c]) for c in range(4)]
rowh=[max(B[n].h for n in row if n) for row in GRID]
cx=[X0]; 
for c in range(1,4): cx.append(cx[-1]+colw[c-1]+GAPX)
ry=[Y0]
for r in range(1,3): ry.append(ry[-1]+rowh[r-1]+GAPY)
for r,row in enumerate(GRID):
    for c,n in enumerate(row):
        if n: B[n].x,B[n].y=cx[c],ry[r]
W=cx[3]+colw[3]+X0; H=ry[2]+rowh[2]+GAPY/2+8+50
d=Diagram("ERD — Cơ sở dữ liệu quản lý nhà hàng",W,H)
for n in order: d.box(B[n])

# ---------- 4) đường nối ----------
def card(parent_nullable,child_unique):
    p="zeroone" if parent_nullable else "oneone"
    c="zeroone" if child_unique else "zeromany"
    return p,c
def fk_info(child,col):
    ccol=next(c for c in tables[child]["cols"] if c["name"]==col)
    return (not ccol["nn"]), ccol["uq"]
E=[]
def add(eid,child,col,parent,pts,labmid=None):
    pn,cu=fk_info(child,col); p,c=card(pn,cu)
    st=dict(start=p,end=c,color="#444444")
    # start = phía bảng cha, end = phía bảng con
    e=Edge(eid,"t_"+parent,"t_"+child,pts,st,lab_mid=col)
    e.fk=(child,col,parent)
    d.edges.append(e); return e
b=B
# customers -> reservations (customer_id)  [ngang]
y=b["reservations"].y+58
add("e1","reservations","customer_id","customers",[(b["customers"].r,y),(b["reservations"].x,y)])
# reservations -> restaurant_tables (table_id)  [ngang]; cha là restaurant_tables
y2=b["reservations"].y+58
pn,cu=fk_info("reservations","table_id"); p,c=card(pn,cu)
e2=Edge("e2","t_restaurant_tables","t_reservations",[(b["restaurant_tables"].x,y2),(b["reservations"].r,y2)],dict(start=p,end=c,color="#444444"),lab_mid="table_id"); e2.fk=("reservations","table_id","restaurant_tables"); d.edges.append(e2)
# restaurant_tables -> orders (table_id) [dọc]
x3=min(b["orders"].x+0.78*b["orders"].w,b["restaurant_tables"].r-18)
add("e3","orders","table_id","restaurant_tables",[(x3,b["restaurant_tables"].b),(x3,b["orders"].y)],labmid="orders.table_id")
# reservations -> orders (reservation_id) [chữ L trong hành lang giữa hai hàng]
ycor=ry[0]+rowh[0]+GAPY/2
xa=b["reservations"].x+b["reservations"].w*0.5; xb=b["orders"].x+b["orders"].w*0.22
add("e4","orders","reservation_id","reservations",[(xa,b["reservations"].b),(xa,ycor),(xb,ycor),(xb,b["orders"].y)],labmid="orders.reservation_id")
# users -> reservations (created_by)
yu1=b["users"].y+b["users"].h*0.32; lane=(b["customers"].r+b["reservations"].x)/2+22; yr=b["reservations"].y+b["reservations"].h*0.8
add("e5","reservations","created_by","users",[(b["users"].r,yu1),(lane,yu1),(lane,yr),(b["reservations"].x,yr)],labmid="reservations.created_by")
# users -> orders (waiter_id)  [ngang qua ô trống]
yu2=b["users"].y+b["users"].h*0.72
assert b["orders"].y<yu2<b["orders"].b, (yu2,b["orders"].y,b["orders"].b)
add("e6","orders","waiter_id","users",[(b["users"].r,yu2),(b["orders"].x,yu2)],labmid="orders.waiter_id")
# orders -> payments (order_id)
y7=b["orders"].y+60
add("e7","payments","order_id","orders",[(b["orders"].r,y7),(b["payments"].x,y7)],labmid="payments.order_id")
# payments -> invoices (payment_id) [dọc]
x8=b["payments"].x+b["payments"].w*0.5
add("e8","invoices","payment_id","payments",[(x8,b["payments"].y),(x8,b["invoices"].b)],labmid="invoices.payment_id")
# users -> payments (cashier_id) [đi vòng dưới]
ybot=ry[2]+rowh[2]+GAPY/2+8; xu=b["users"].x+b["users"].w*0.5; xp=b["payments"].x+b["payments"].w*0.5
add("e9","payments","cashier_id","users",[(xu,b["users"].b),(xu,ybot),(xp,ybot),(xp,b["payments"].b)],labmid="payments.cashier_id")
# orders -> order_items (order_id) [dọc]
x10=b["orders"].x+b["orders"].w*0.5
add("e10","order_items","order_id","orders",[(x10,b["orders"].b),(x10,b["order_items"].y)],labmid="order_items.order_id")
# menu_items -> order_items (menu_item_id) [ngang]
y11=b["order_items"].y+58
add("e11","order_items","menu_item_id","menu_items",[(b["menu_items"].r,y11),(b["order_items"].x,y11)],labmid="order_items.menu_item_id")
# kiểm tra: mỗi khóa ngoại trong schema đều có đúng một đường nối
want={(t,c,rt) for t,v in tables.items() for c,rt,_ in v["fks"]}
got={e.fk for e in d.edges}
assert want==got,(want^got)

# ---------- 5) chú thích ----------
d.notes.append(Note("legend",X0,16,W-2*X0,62,
 "ERD CƠ SỞ DỮ LIỆU — HỆ THỐNG QUẢN LÝ NHÀ HÀNG (9 bảng, MariaDB). Sinh từ database/schema.sql.\n"
 "Ký hiệu: PK khóa chính · FK khóa ngoại · UQ giá trị duy nhất. Chân chim = nhiều (0..*), vạch đôi = đúng một (1), vòng tròn + vạch = không hoặc một (0..1).\n"
 "Đường nối ghi tên cột khóa ngoại. orders.reservation_id cho phép rỗng (khách vãng lai không đặt bàn)."))
errs=check(d)+check_labels(d)
print("LỖI BỐ CỤC:",errs or "không")
render(d,os.path.join(ROOT,"docs","uml","ERD_CoSoDuLieu_QuanLyNhaHang.png"),scale=1.0)
to_drawio([d],os.path.join(ROOT,"docs","uml","ERD_CoSoDuLieu_QuanLyNhaHang.drawio"))
print("kích thước:",W,"x",H)
