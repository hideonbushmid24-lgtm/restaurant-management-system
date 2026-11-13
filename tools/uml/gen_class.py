import os,re,sys,glob,os
HERE=os.path.dirname(os.path.abspath(__file__)); ROOT=os.path.abspath(os.path.join(HERE,"..",".."))
sys.path.insert(0,HERE)
from diagram_lib import *
SRC=os.path.join(ROOT,"src","main","java","com","restaurant")

# ================= 1) Đọc mã Java =================
def read_model(name):
    s=open(f"{SRC}/model/{name}.java",encoding="utf-8").read()
    if re.search(r"public enum "+name,s):
        body=s[s.index("{",s.index("enum "+name))+1:]
        consts=[]
        for ln in body.split("\n"):
            if re.match(r"\s*(private|public|\})",ln): break
            m=re.match(r"\s*([A-Z][A-Z_]*)\b",ln)
            if m: consts.append(m.group(1))
        meths=sorted(set(re.findall(r"public (?:static )?[\w<>\[\]]+ (getDisplayName)\(",s)))
        return dict(kind="enum",consts=consts,meths=["+ getDisplayName(): String"] if meths else [])
    m=re.search(r"public record "+name+r"\((.*?)\)\s*\{",s,re.S)
    if m:
        comps=[" ".join(c.split()).rsplit(" ",1) for c in m.group(1).split(",")]
        return dict(kind="record",fields=[f"+ {n}: {t}" for t,n in comps],meths=[])
    fields=re.findall(r"private (?:final )?([\w<>\[\]]+) (\w+);",s)
    pubs=re.findall(r"public ([\w<>\[\]]+) (\w+)\(",s)
    getters={("get"+n[0].upper()+n[1:]) for _,n in fields}|{("is"+n[0].upper()+n[1:]) for _,n in fields}
    extra=[f"+ {n}(): {t}" for t,n in pubs if n not in getters and n!=name and n!="toString"]
    return dict(kind="class",fields=[f"- {n}: {t}" for t,n in fields],meths=extra,has_getters=bool(getters&{n for _,n in pubs}))
M={n:read_model(n) for n in [os.path.basename(f)[:-5] for f in glob.glob(f"{SRC}/model/*.java")]}
assert "CASHIER" in M["Role"]["consts"] and len(M["Role"]["consts"])==5, M["Role"]
print("model:",len(M),"lớp |","Role:",M["Role"]["consts"])

def class_box(name,x,y,fill,stroke,stereo=None,w=None,extra_small=None,dashed=False,minw=210):
    m=M.get(name)
    lines=[]
    if m and m["kind"]=="enum": stereo="<<enum>>"
    if m and m["kind"]=="record": stereo="<<record>>"
    if stereo: lines.append(("stereo",stereo))
    lines.append(("title",name)); lines.append(("hr",""))
    if m["kind"]=="enum":
        for c in m["consts"]: lines.append(("text",c))
        if m["meths"]: lines.append(("hr","")); [lines.append(("text",t)) for t in m["meths"]]
    else:
        for f in m["fields"]: lines.append(("text",f))
        ms=list(m["meths"])
        if m["kind"]=="class" and m.get("has_getters"): ms.append("+ getters()")
        if ms: lines.append(("hr","")); [lines.append(("text",t)) for t in ms]
    if extra_small: 
        lines.append(("hr",""))
        for t in extra_small: lines.append(("small",t))
    return Box("c_"+name,x,y,w or max(text_width(lines),minw),lines_height(lines),lines,fill=fill,stroke=stroke,dashed=dashed)

# ================= 2) TRANG 1: Domain model =================
d1=Diagram("Class Diagram — Domain model",1560,1020)
PAL={"user":("#e8f0fe","#4a74c9"),"table":("#e6f7ef","#2f9e6e"),"order":("#fff9db","#d4a800"),"pay":("#f3e8ff","#8a4fd0"),"enum":("#eef1f5","#8a94a6"),"cust":("#fff4e5","#e08a00"),"rep":("#fde8f1","#d6458a")}
def mk(name,pal,**kw):
    f,s=PAL[pal]; return d1.box(class_box(name,0,0,f,s,**kw))
cl=[("stereo","<<entity>> bảng customers"),("title","Customer"),("hr",""),
    ("text","- id: long"),("text","- fullName: String"),("text","- phone: String  {unique}"),("text","- email: String"),
    ("text","- createdAt: LocalDateTime"),("text","- updatedAt: LocalDateTime"),("hr",""),
    ("small","Chưa có lớp Java riêng: tên và số"),("small","điện thoại được nạp vào Reservation"),("small","qua JOIN khi đọc danh sách đặt bàn.")]
cust=d1.box(Box("c_Customer",0,0,text_width(cl),lines_height(cl),cl,fill=PAL["cust"][0],stroke=PAL["cust"][1],dashed=True))
res=mk("Reservation","table"); ordr=mk("RestaurantOrder","order"); oitem=mk("OrderItem","order"); mitem=mk("MenuItem","order")
tbl=mk("RestaurantTable","table"); tstat=mk("TableStatus","enum"); user=mk("User","user"); role=mk("Role","enum")
rstat=mk("ReservationStatus","enum"); ostat=mk("OrderStatus","enum"); kstat=mk("KitchenStatus","enum")
pord=mk("PayableOrder","pay",stereo="<<DTO>>"); prec=mk("PaymentReceipt","pay",stereo="<<DTO>>")
pmeth=mk("PaymentMethod","enum"); rrec=mk("RevenueRecord","rep"); kitem=mk("KitchenItem","order",stereo="<<DTO>>"); rsum=mk("RevenueSummary","rep")
# cột: các hộp cùng cột có cùng độ rộng để thẳng hàng
COLS=[[cust,pord],[res,rstat,prec],[ordr,ostat,pmeth],[oitem,rrec,rsum],[mitem,kstat,kitem]]
GAPX=118; x=40; colx=[]
for col in COLS:
    w=max(max(b.w for b in col),215)
    for b in col: b.w=w
    colx.append(x)
    for b in col: b.x=x
    x+=w+GAPX
# hàng A: bảng nằm vắt qua khoảng giữa cột 1 và 2; User phía trên bên phải RestaurantOrder
tbl.w=max(tbl.w,250); tbl.x=res.r-70
tstat.w=210; tstat.x=tbl.x-70-tstat.w
user.w=max(user.w,215); user.x=tbl.r+45
role.w=210; role.x=user.r+70
YA=104
for b in (tbl,tstat,user,role): b.y=YA
YB=max(b.b for b in (tbl,tstat,user,role))+85
for b in (cust,res,ordr,oitem,mitem): b.y=YB
YC=max(b.b for b in (cust,res,ordr,oitem,mitem))+70
for b in (rstat,ostat,kstat): b.y=YC
YD=max(b.b for b in (rstat,ostat,kstat))+70
for b in (pord,prec,pmeth,rrec,kitem): b.y=YD
rsum.y=max(b.b for b in (pord,prec,pmeth,rrec,kitem))+60
d1.w=int(max(b.r for b in d1.boxes)+40); d1.h=int(rsum.b+50)

def E(id,src,tgt,pts,style,ls="",le="",lm=""):
    e=Edge(id,src,tgt,pts,style,lab_start=ls,lab_end=le,lab_mid=lm); d1.edges.append(e); return e
ASSOC=dict(color="#444444"); DEP=dict(color="#6b7280",dashed=True,end="open"); COMP=dict(color="#444444",start="diamond")
y=YB+60;  E("k1","c_Customer","c_Reservation",[(cust.r,y),(res.x,y)],ASSOC,"1","0..*")
xa=res.r-40; assert tbl.x<xa<tbl.r
E("k2","c_RestaurantTable","c_Reservation",[(xa,tbl.b),(xa,res.y)],ASSOC,"1","0..*")
xb=ordr.x+30; assert tbl.x<xb<tbl.r and ordr.x<xb<ordr.r
E("k3","c_RestaurantTable","c_RestaurantOrder",[(xb,tbl.b),(xb,ordr.y)],ASSOC,"1","0..*")
xc=ordr.r-35; assert user.x<xc<user.r,(user.x,xc,user.r)
E("k4","c_User","c_RestaurantOrder",[(xc,user.b),(xc,ordr.y)],ASSOC,"1","0..*","phục vụ")
y=YB+110; E("k5","c_Reservation","c_RestaurantOrder",[(res.r,y),(ordr.x,y)],ASSOC,"0..1","0..1")
y=YB+60;  E("k6","c_RestaurantOrder","c_OrderItem",[(ordr.r,y),(oitem.x,y)],COMP,"1","1..*")
y=YB+60;  E("k7","c_OrderItem","c_MenuItem",[(oitem.r,y),(mitem.x,y)],ASSOC,"0..*","1")
E("s1","c_RestaurantTable","c_TableStatus",[(tbl.x,tbl.y+40),(tstat.r,tbl.y+40)],DEP)
E("s2","c_User","c_Role",[(user.r,user.y+40),(role.x,user.y+40)],DEP)
E("s3","c_Reservation","c_ReservationStatus",[(res.x+res.w/2,res.b),(res.x+res.w/2,rstat.y)],DEP)
E("s4","c_RestaurantOrder","c_OrderStatus",[(ordr.x+ordr.w/2,ordr.b),(ordr.x+ordr.w/2,ostat.y)],DEP)
ys=kstat.y+kstat.h*0.5; xo=oitem.x+oitem.w/2
E("s5","c_OrderItem","c_KitchenStatus",[(xo,oitem.b),(xo,ys),(kstat.x,ys)],DEP)
E("s6","c_KitchenItem","c_KitchenStatus",[(kitem.x+kitem.w/2,kitem.y),(kitem.x+kitem.w/2,kstat.b)],DEP)
ypm=pmeth.y+50
E("s7","c_PaymentReceipt","c_PaymentMethod",[(prec.r,ypm),(pmeth.x,ypm)],DEP)
E("s8","c_RevenueRecord","c_PaymentMethod",[(rrec.x,ypm),(pmeth.r,ypm)],DEP)
E("s9","c_RevenueSummary","c_RevenueRecord",[(rrec.x+rrec.w/2,rsum.y),(rrec.x+rrec.w/2,rrec.b)],DEP,"","","tổng hợp từ")
d1.notes.append(Note("n1",40,16,d1.w-80,70,
 "CLASS DIAGRAM — DOMAIN MODEL HỆ THỐNG QUẢN LÝ NHÀ HÀNG. Sinh từ mã nguồn trong src/main/java/com/restaurant/model (thuộc tính, kiểu dữ liệu, giá trị enum lấy trực tiếp từ mã).\n"
 "Ký hiệu: - private, + public; 1 đúng một, 0..1 tùy chọn, 0..* nhiều; hình thoi đặc = hợp thành; mũi tên nét đứt = thuộc tính kiểu enum / phụ thuộc.\n"
 "Customer là bảng customers (đã có trong database) nhưng chưa có lớp Java riêng (nét đứt). PayableOrder, PaymentReceipt, KitchenItem, RevenueRecord, RevenueSummary là lớp dữ liệu tạo từ truy vấn để hiển thị."))
err1=check(d1)+check_labels(d1)
print("TRANG 1 lỗi bố cục:",err1 or "không","| kích thước",d1.w,"x",d1.h)
render(d1,os.path.join(ROOT,"docs","uml","ClassDiagram_DomainModel.png"),scale=1.0)

# ================= 3) TRANG 2: Kiến trúc phân tầng =================
def pubs(path,cls):
    s=open(path,encoding="utf-8").read()
    names=[]
    if re.search(r"public interface "+cls,s): names=re.findall(r"\b(\w+)\([^)]*\)\s*(?:throws [\w, ]+)?;",s)
    else: names=[n for n in re.findall(r"public (?:static )?[\w<>\[\], ]+? (\w+)\(",s) if n!=cls]
    out=[]; [out.append(n) for n in names if n not in out]; return out
SV=sorted(os.path.basename(f)[:-5] for f in glob.glob(f"{SRC}/service/*.java"))
DA=sorted(os.path.basename(f)[:-5] for f in glob.glob(f"{SRC}/dao/*.java"))
UI=sorted(os.path.basename(f)[:-5] for f in glob.glob(f"{SRC}/ui/*.java"))
ROWS=[("LoginFrame","AuthService","UserRepository"),("DashboardFrame",None,"UserDAO"),("TableManagementFrame","TableService","TableDAO"),
      ("ReservationFrame","ReservationService","ReservationDAO"),("MenuFrame","MenuService","MenuDAO"),("OrderFrame","OrderService","OrderDAO"),
      ("KitchenFrame","KitchenService","KitchenDAO"),("PaymentFrame","PaymentService","PaymentDAO"),("RevenueReportFrame","RevenueReportService","RevenueReportDAO")]
assert sorted(r[0] for r in ROWS)==UI and sorted(r[1] for r in ROWS if r[1])==SV and sorted(r[2] for r in ROWS)==DA
CAP={"LoginFrame":["Đăng nhập"],"DashboardFrame":["Màn hình chính, phân quyền theo vai trò;","mở các màn hình chức năng,","khởi tạo Service và DAO"],
     "TableManagementFrame":["Quản lý bàn"],"ReservationFrame":["Đặt bàn, nhận bàn, hủy đặt bàn"],"MenuFrame":["Tra cứu thực đơn"],
     "OrderFrame":["Tạo đơn gọi món, gửi bếp,","xác nhận phục vụ"],"KitchenFrame":["Xử lý món tại bếp"],
     "PaymentFrame":["Thanh toán và hóa đơn"],"RevenueReportFrame":["Báo cáo doanh thu"]}
# --- kiểm chứng quan hệ với mã nguồn ---
exp_ui={"LoginFrame":{"AuthService"},"DashboardFrame":None,"TableManagementFrame":{"TableService"},"ReservationFrame":{"ReservationService"},
        "MenuFrame":{"MenuService"},"OrderFrame":{"MenuService","OrderService","TableService"},"KitchenFrame":{"KitchenService"},
        "PaymentFrame":{"PaymentService"},"RevenueReportFrame":{"RevenueReportService"}}
for u,exp in exp_ui.items():
    if exp is None: continue
    s=open(f"{SRC}/ui/{u}.java",encoding="utf-8").read()
    got={x for x in SV if re.search(r"\b"+x+r"\b",s)}
    assert got==exp,(u,got,exp)
exp_sv={"AuthService":"UserRepository","TableService":"TableDAO","ReservationService":"ReservationDAO","MenuService":"MenuDAO","OrderService":"OrderDAO",
        "KitchenService":"KitchenDAO","PaymentService":"PaymentDAO","RevenueReportService":"RevenueReportDAO"}
for v,dd in exp_sv.items():
    s=open(f"{SRC}/service/{v}.java",encoding="utf-8").read()
    got={x for x in DA if re.search(r"\b"+x+r"\b",s)}
    assert got=={dd},(v,got)
assert "implements UserRepository" in open(f"{SRC}/dao/UserDAO.java",encoding="utf-8").read()
print("quan hệ UI→Service, Service→DAO, UserDAO⊳UserRepository: khớp mã nguồn")

d2=Diagram("Class Diagram — Kiến trúc phân tầng",1700,1200)
C_UI=("#e8f0fe","#4a74c9"); C_SV=("#fff9db","#d4a800"); C_DA=("#e6f7ef","#2f9e6e"); C_IN=("#f3e8ff","#8a4fd0")
def cbox(id,name,stereo,body,pal,small=None):
    lines=[]
    if stereo: lines.append(("stereo",stereo))
    lines+=[("title",name),("hr","")]
    lines+=[("text",t) for t in body]
    if small:
        lines+=[("hr","")]+[("small",t) for t in small]
    return Box(id,0,0,text_width(lines),lines_height(lines),lines,fill=pal[0],stroke=pal[1])
ui_b={};sv_b={};da_b={}
for u,v,dd in ROWS:
    ui_b[u]=cbox("u_"+u,u,"<<JFrame>>",[],C_UI,small=CAP[u]) if False else cbox("u_"+u,u,"<<JFrame>>",CAP[u],C_UI)
    if v:
        extra=["dùng PasswordUtil để băm","và kiểm tra mật khẩu"] if v=="AuthService" else None
        body=[f"+ {m}()" for m in pubs(f"{SRC}/service/{v}.java",v)]
        sv_b[v]=cbox("s_"+v,v,None,body,C_SV,small=extra)
    dd_path=f"{SRC}/dao/{dd}.java"
    da_b[dd]=cbox("d_"+dd,dd,"<<interface>>" if dd=="UserRepository" else None,[f"+ {m}()" for m in pubs(dd_path,dd)],C_DA)
for b in list(ui_b.values())+list(sv_b.values())+list(da_b.values()): d2.box(b)
wu=max(b.w for b in ui_b.values()); ws=max(b.w for b in sv_b.values()); wd=max(b.w for b in da_b.values())
for b in ui_b.values(): b.w=max(wu,230)
for b in sv_b.values(): b.w=max(ws,250)
for b in da_b.values(): b.w=max(wd,250)
PADG=20; TOPG=34; GAPG=130; ROWGAP=40
gx_ui=30; ui_x=gx_ui+PADG
gx_sv=ui_x+max(wu,230)+PADG+GAPG; sv_x=gx_sv+PADG
gx_da=sv_x+max(ws,250)+PADG+GAPG; da_x=gx_da+PADG
gx_in=da_x+max(wd,250)+PADG+GAPG
y=110+TOPG; rowy=[]
for u,v,dd in ROWS:
    hh=max(ui_b[u].h,sv_b[v].h if v else 0,da_b[dd].h)
    rowy.append(y); ui_b[u].x=ui_x; ui_b[u].y=y; da_b[dd].x=da_x; da_b[dd].y=y
    if v: sv_b[v].x=sv_x; sv_b[v].y=y
    y+=hh+ROWGAP
yend=y-ROWGAP+PADG
def grp(id,x,w,title,pal):
    g=Group(id,x,110,w,yend-110,title,fill=pal[0]+"",stroke=pal[1]); d2.groups.append(g); return g
g_ui=grp("g_ui",gx_ui,max(wu,230)+2*PADG,"Tầng giao diện (Java Swing)","#f5f8ff" and C_UI)
g_ui.fill="#f5f8ff"
g_sv=grp("g_sv",gx_sv,max(ws,250)+2*PADG,"Tầng nghiệp vụ (Service): kiểm tra dữ liệu, quy tắc",C_SV); g_sv.fill="#fffdf2"
g_da=grp("g_da",gx_da,max(wd,250)+2*PADG,"Tầng truy cập dữ liệu (DAO, JDBC, transaction)",C_DA); g_da.fill="#f4fbf7"
# hạ tầng
dbc=cbox("i_DatabaseConnection","DatabaseConnection","<<config>>",["+ getConnection()"],C_IN,small=["đọc cấu hình: -D, biến môi trường,","config/local.properties,","application.properties"])
maria=Box("i_MariaDB",0,0,200,64,[("title","MariaDB"),("hr",""),("text","restaurant_management")],fill="#2d3748",stroke="#1a202c",font_color="#ffffff")
dbc.w=max(dbc.w,250); maria.w=dbc.w
dbc.x=gx_in; maria.x=gx_in
ymid=110+(yend-110)/2-60
dbc.y=ymid; maria.y=dbc.b+70
d2.box(dbc); d2.box(maria)
d2.w=int(dbc.r+40); d2.h=int(max(yend,maria.b)+50)
def D(id,src,tgt,pts,style,lm=""):
    e=Edge(id,src,tgt,pts,style,lab_mid=lm); d2.edges.append(e); return e
DEP2=dict(color="#6b7280",dashed=True,end="open"); REAL=dict(color="#6b7280",dashed=True,end="block")
for i,(u,v,dd) in enumerate(ROWS):
    ub=ui_b[u]; yy=rowy[i]+34
    if v:
        sb=sv_b[v]
        if u!="OrderFrame": D("a_"+u,ub.id,sb.id,[(ub.r,yy),(sb.x,yy)],DEP2)
        else: D("a_"+u,ub.id,sb.id,[(ub.r,rowy[i]+46),(sb.x,rowy[i]+46)],DEP2)
        db=da_b[dd]; D("b_"+v,sb.id,db.id,[(sb.r,yy),(db.x,yy)],DEP2)
# OrderFrame -> TableService (làn gần UI) và -> MenuService (làn xa hơn)
of=ui_b["OrderFrame"]; ro=ROWS.index(next(r for r in ROWS if r[0]=="OrderFrame"))
lane_a=g_ui.x+g_ui.w+34; lane_b=g_ui.x+g_ui.w+66
ts=sv_b["TableService"]; ms=sv_b["MenuService"]
yta=ts.y+min(ts.h-14,78); ymb=ms.y+min(ms.h-14,66)
D("a_Order_Table","u_OrderFrame",ts.id,[(of.r,rowy[ro]+26),(lane_a,rowy[ro]+26),(lane_a,yta),(ts.x,yta)],DEP2)
D("a_Order_Menu","u_OrderFrame",ms.id,[(of.r,rowy[ro]+66),(lane_b,rowy[ro]+66),(lane_b,ymb),(ms.x,ymb)],DEP2)
# Login -> Dashboard (mở)
lg=ui_b["LoginFrame"]; dsh=ui_b["DashboardFrame"]; xm=lg.x+lg.w/2
D("a_Login_Dash",lg.id,dsh.id,[(xm,lg.b),(xm,dsh.y)],DEP2,"mở")
# UserDAO hiện thực UserRepository
ur=da_b["UserRepository"]; ud=da_b["UserDAO"]; xu=ur.x+ur.w/2
D("r_UserDAO",ud.id,ur.id,[(xu,ud.y),(xu,ur.b)],REAL,"implements")
# DAO -> DatabaseConnection -> MariaDB
yg=dbc.y+30
D("i1","g_da",dbc.id,[(g_da.x+g_da.w,yg),(dbc.x,yg)],DEP2,"mọi DAO")
xd=dbc.x+dbc.w/2
D("i2",dbc.id,maria.id,[(xd,dbc.b),(xd,maria.y)],DEP2,"JDBC")
d2.notes.append(Note("n2",30,16,d2.w-60,64,
 "CLASS DIAGRAM — KIẾN TRÚC PHÂN TẦNG (UI → Service → DAO → database). Danh sách phương thức public và các phụ thuộc đã đối chiếu với mã nguồn trong src/main/java/com/restaurant.\n"
 "Mũi tên nét đứt = phụ thuộc (gọi); mũi tên rỗng nét đứt = implements. Các lớp dữ liệu (model) xem ở trang Domain model. PasswordUtil (băm mật khẩu) được AuthService sử dụng.\n"
 "Quy tắc nghiệp vụ nằm ở Service và DAO (transaction, khóa dòng), giao diện chỉ nhập liệu và hiển thị; phân quyền theo vai trò nằm chủ yếu ở DashboardFrame."))
err2=check(d2)+check_labels(d2)
print("TRANG 2 lỗi bố cục:",err2 or "không","| kích thước",d2.w,"x",d2.h)
render(d2,os.path.join(ROOT,"docs","uml","ClassDiagram_KienTrucPhanTang.png"),scale=1.0)
to_drawio([d1,d2],os.path.join(ROOT,"docs","uml","ClassDiagram_QuanLyNhaHang.drawio"))
print("đã xuất ClassDiagram_QuanLyNhaHang.drawio (2 trang)")
