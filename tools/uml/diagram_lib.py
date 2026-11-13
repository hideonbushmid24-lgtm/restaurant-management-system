# Thư viện dựng sơ đồ (hộp, đường nối), xuất file .drawio và ảnh xem trước.
# Thư viện nhỏ: dựng sơ đồ (hộp + đường nối), xuất .drawio và ảnh xem trước, kiểm tra bố cục.
import re, math
from xml.sax.saxutils import escape, quoteattr
try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:   # chỉ cần để xuất ảnh xem trước (PNG)
    Image = None

import os
def _font(name):
    for d in ("/usr/share/fonts/truetype/dejavu/","C:/Windows/Fonts/","/Library/Fonts/","/System/Library/Fonts/Supplemental/"):
        for n in (name,{"DejaVuSans.ttf":"arial.ttf","DejaVuSans-Bold.ttf":"arialbd.ttf","DejaVuSansMono.ttf":"consola.ttf"}.get(name,name)):
            if os.path.exists(d+n): return d+n
    return name
FONT=_font("DejaVuSans.ttf"); FONTB=_font("DejaVuSans-Bold.ttf"); MONO=_font("DejaVuSansMono.ttf")

class Box:
    def __init__(s,id,x,y,w,h,lines,fill="#ffffff",stroke="#333333",mono=False,dashed=False,title_idx=0,kind="class",font_color="#111111"):
        s.id,s.x,s.y,s.w,s.h=id,x,y,w,h
        s.lines=lines      # danh sách (kiểu, văn bản) với kiểu: 'title','stereo','hr','text','small'
        s.fill,s.stroke,s.mono,s.dashed,s.kind=fill,stroke,mono,dashed,kind
        s.font_color=font_color
    @property
    def r(s): return s.x+s.w
    @property
    def b(s): return s.y+s.h
    def pt(s,side,f=0.5):
        if side=="l": return (s.x, s.y+s.h*f)
        if side=="r": return (s.r, s.y+s.h*f)
        if side=="t": return (s.x+s.w*f, s.y)
        if side=="b": return (s.x+s.w*f, s.b)

LH=16  # chiều cao dòng
def lines_height(lines, pad=8):
    h=pad
    for k,_ in lines:
        h+= {"hr":9,"title":19,"stereo":15,"text":LH,"small":14,"gap":6}[k]
    return h+pad-2

def text_width(lines, mono=False):
    cw=7.0 if mono else 6.9
    m=0
    for k,t in lines:
        if k=="hr" or k=="gap": continue
        c=cw*(1.12 if k=="title" else 1)
        m=max(m,len(t)*c)
    return int(m+26)

class Edge:
    def __init__(s,id,src,tgt,pts,style="",lab_start="",lab_end="",lab_mid="",kind="assoc",lab_mid_pos=None):
        s.id,s.src,s.tgt,s.pts=id,src,tgt,pts
        s.style,s.lab_start,s.lab_end,s.lab_mid,s.kind=style,lab_start,lab_end,lab_mid,kind
        s.lab_mid_pos=lab_mid_pos

class Note:
    def __init__(s,id,x,y,w,h,text,fill="#fff7e6",stroke="#e0b060"):
        s.id,s.x,s.y,s.w,s.h,s.text,s.fill,s.stroke=id,x,y,w,h,text,fill,stroke

class Group:
    def __init__(s,id,x,y,w,h,title,fill="#f8f9fa",stroke="#b0b7c3"):
        s.id,s.x,s.y,s.w,s.h,s.title,s.fill,s.stroke=id,x,y,w,h,title,fill,stroke
    @property
    def r(s): return s.x+s.w
    @property
    def b(s): return s.y+s.h

class Diagram:
    def __init__(s,name,w,h): s.name,s.w,s.h=name,w,h; s.boxes=[]; s.edges=[]; s.notes=[]; s.groups=[]; s.labels=[]
    def box(s,b): s.boxes.append(b); return b
    def by(s,id):
        for b in s.boxes+s.groups:
            if b.id==id: return b
        raise KeyError(id)

# ---------------- kiểm tra bố cục ----------------
def seg_hits_rect(p,q,b,margin=1):
    # đoạn thẳng p-q (ngang/dọc) cắt phần trong của hộp b?
    x1,y1=p; x2,y2=q
    bx1,by1,bx2,by2=b.x+margin,b.y+margin,b.r-margin,b.b-margin
    if abs(y1-y2)<0.01:
        y=y1; lo,hi=sorted((x1,x2))
        return by1<y<by2 and hi>bx1 and lo<bx2
    if abs(x1-x2)<0.01:
        x=x1; lo,hi=sorted((y1,y2))
        return bx1<x<bx2 and hi>by1 and lo<by2
    raise ValueError("đoạn không ngang/dọc: %s-%s"%(p,q))

def check(d):
    errs=[]
    for i,a in enumerate(d.boxes):
        for b in d.boxes[i+1:]:
            if a.x<b.r and b.x<a.r and a.y<b.b and b.y<a.b: errs.append(f"hộp chồng nhau: {a.id} / {b.id}")
    for e in d.edges:
        for p,q in zip(e.pts,e.pts[1:]):
            for b in d.boxes:
                if b.id in (e.src,e.tgt):
                    # cho phép chạm biên hộp của chính nó; chỉ báo nếu đi xuyên phần trong
                    pass
                if seg_hits_rect(p,q,b,margin=2): errs.append(f"đường {e.id} xuyên hộp {b.id}")
        # điểm đầu/cuối phải nằm trên biên hộp
        for pt,bid in ((e.pts[0],e.src),(e.pts[-1],e.tgt)):
            if bid is None: continue
            b=d.by(bid); x,y=pt
            on=(abs(x-b.x)<0.6 or abs(x-b.r)<0.6) and b.y-0.6<=y<=b.b+0.6 or (abs(y-b.y)<0.6 or abs(y-b.b)<0.6) and b.x-0.6<=x<=b.r+0.6
            if not on: errs.append(f"đầu mút của {e.id} không nằm trên biên {bid}: {pt}")
    return errs

def label_rects(d):
    out=[]
    for e in d.edges:
        for pos,txt in (("s",e.lab_start),("e",e.lab_end)):
            if not txt: continue
            out.append((e.id+pos,)+label_pos(e,pos,txt))
        if e.lab_mid:
            out.append((e.id+"m",)+mid_pos(e))
    return out

def label_pos(e,which,txt):
    pts=e.pts
    if which=="s": p,q=pts[0],pts[1]
    else: p,q=pts[-1],pts[-2]
    dx,dy=q[0]-p[0],q[1]-p[1]; L=math.hypot(dx,dy) or 1
    ux,uy=dx/L,dy/L; nx,ny=-uy,ux
    w=max(18,len(txt)*7+6); h=16
    cx=p[0]+ux*(10+w/2*abs(ux))+nx*13*(1 if True else 1)
    cy=p[1]+uy*(10+h/2*abs(uy))+ny*13
    # đặt nhãn lệch về một phía của đường
    return (cx-w/2,cy-h/2,w,h,txt)

def mid_pos(e):
    best=max(zip(e.pts,e.pts[1:]),key=lambda s:abs(s[0][0]-s[1][0])+abs(s[0][1]-s[1][1]))
    (x1,y1),(x2,y2)=best
    x=(x1+x2)/2; y=(y1+y2)/2
    if e.lab_mid_pos: x,y=e.lab_mid_pos
    w=len(e.lab_mid)*6.6+8; h=16
    if abs(x1-x2)<0.01:      # đường dọc: nhãn nằm bên phải đường
        return (x+6,y-h/2,w,h,e.lab_mid)
    return (x-w/2,y-h-4,w,h,e.lab_mid)   # đường ngang: nhãn phía trên đường

def check_labels(d):
    errs=[]
    rects=label_rects(d)
    for (lid,x,y,w,h,t) in rects:
        for b in d.boxes:
            if x<b.r and b.x<x+w and y<b.b and b.y<y+h: errs.append(f"nhãn '{t}' ({lid}) chạm hộp {b.id}")
    for i,a in enumerate(rects):
        for b in rects[i+1:]:
            if a[1]<b[1]+b[3] and b[1]<a[1]+a[3] and a[2]<b[2]+b[4] and b[2]<a[2]+a[4]: errs.append(f"nhãn chồng nhau: '{a[5]}' / '{b[5]}'")
    return errs

# ---------------- xem trước (PNG) ----------------
def arrow_head(dr,p,q,kind,color="#333"):
    # vẽ đầu mũi tại p theo hướng từ q tới p
    dx,dy=p[0]-q[0],p[1]-q[1]; L=math.hypot(dx,dy) or 1; ux,uy=dx/L,dy/L; nx,ny=-uy,ux
    if kind in("open","block"):
        a=(p[0]-ux*11+nx*5,p[1]-uy*11+ny*5); b=(p[0]-ux*11-nx*5,p[1]-uy*11-ny*5)
        if kind=="block": dr.polygon([p,a,b],outline=color,fill="white")
        else: dr.line([a,p,b],fill=color,width=2)
    elif kind=="diamond":
        a=(p[0]-ux*8+nx*5,p[1]-uy*8+ny*5); c=(p[0]-ux*16,p[1]-uy*16); b=(p[0]-ux*8-nx*5,p[1]-uy*8-ny*5)
        dr.polygon([p,a,c,b],fill=color,outline=color)
    elif kind=="oneone": arrow_head(dr,p,q,"one",color)
    elif kind=="zeroone": arrow_head(dr,p,q,"one",color); arrow_head(dr,p,q,"zero",color)
    elif kind=="zeromany": arrow_head(dr,p,q,"many",color); arrow_head(dr,p,q,"zero",color)
    elif kind=="many":   # chân chim
        t=(p[0]-ux*14,p[1]-uy*14)
        dr.line([t,(p[0]+nx*7,p[1]+ny*7)],fill=color,width=1); dr.line([t,(p[0]-nx*7,p[1]-ny*7)],fill=color,width=1); dr.line([t,p],fill=color,width=1)
    elif kind=="one":
        for k in (8,12):
            c=(p[0]-ux*k,p[1]-uy*k); dr.line([(c[0]+nx*6,c[1]+ny*6),(c[0]-nx*6,c[1]-ny*6)],fill=color,width=2)
    elif kind=="zero":
        c=(p[0]-ux*24,p[1]-uy*24); dr.ellipse([c[0]-5,c[1]-5,c[0]+5,c[1]+5],fill="white",outline=color)

def render(d,path,scale=1.0):
    if Image is None:
        print('  (bỏ qua ảnh PNG: chưa cài Pillow, chạy: pip install pillow)'); return
    img=Image.new("RGB",(int(d.w*scale),int(d.h*scale)),"white"); dr=ImageDraw.Draw(img)
    f=lambda sz,b=False,m=False: ImageFont.truetype(MONO if m else (FONTB if b else FONT),int(sz*scale))
    for g in d.groups:
        dr.rounded_rectangle([g.x*scale,g.y*scale,(g.x+g.w)*scale,(g.y+g.h)*scale],8,fill=g.fill,outline=g.stroke,width=2)
        dr.text(((g.x+10)*scale,(g.y+6)*scale),g.title,font=f(12,True),fill="#444")
    for e in d.edges:
        pts=[(x*scale,y*scale) for x,y in e.pts]
        col=e.style.get("color","#333") if isinstance(e.style,dict) else "#333"
        dashed=isinstance(e.style,dict) and e.style.get("dashed")
        if dashed:
            for p,q in zip(pts,pts[1:]):
                L=math.hypot(q[0]-p[0],q[1]-p[1]); n=int(L//8)
                for i in range(0,n,2):
                    a=(p[0]+(q[0]-p[0])*i/n,p[1]+(q[1]-p[1])*i/n); b=(p[0]+(q[0]-p[0])*(i+1)/n,p[1]+(q[1]-p[1])*(i+1)/n); dr.line([a,b],fill=col,width=1)
        else: dr.line(pts,fill=col,width=int(max(1,1.5*scale)))
        st=e.style if isinstance(e.style,dict) else {}
        if st.get("start"): arrow_head(dr,pts[0],pts[1],st["start"],col)
        if st.get("end"): arrow_head(dr,pts[-1],pts[-2],st["end"],col)
    for b in d.boxes:
        dr.rectangle([b.x*scale,b.y*scale,b.r*scale,b.b*scale],fill=b.fill,outline=b.stroke,width=2)
        y=b.y+8
        for k,t in b.lines:
            if k=="hr": dr.line([b.x*scale,(y+3)*scale,b.r*scale,(y+3)*scale],fill=b.stroke,width=1); y+=9
            elif k=="gap": y+=6
            elif k=="title": dr.text(((b.x+b.w/2)*scale,y*scale),t,font=f(13,True),fill=b.font_color,anchor="ma"); y+=19
            elif k=="stereo": dr.text(((b.x+b.w/2)*scale,y*scale),t,font=f(11),fill="#333",anchor="ma"); y+=15
            elif k=="small": dr.text(((b.x+8)*scale,y*scale),t,font=f(10),fill="#555"); y+=14
            else: dr.text(((b.x+8)*scale,y*scale),t,font=f(11,False,b.mono),fill=b.font_color); y+=LH
    for n in d.notes:
        dr.rectangle([n.x*scale,n.y*scale,(n.x+n.w)*scale,(n.y+n.h)*scale],fill=n.fill,outline=n.stroke,width=1)
        yy=n.y+6
        for ln in n.text.split("\n"): dr.text(((n.x+8)*scale,yy*scale),ln,font=f(11),fill="#533"); yy+=15
    for (lid,x,y,w,h,t) in label_rects(d):
        dr.text(((x+w/2)*scale,(y+h/2)*scale),t,font=f(11,True),fill="#a33",anchor="mm")
    img.save(path)

# ---------------- xuất .drawio ----------------
def box_style(b):
    base="rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=6;spacingTop=4;fontSize=12;"
    base+=f"fillColor={b.fill};strokeColor={b.stroke};strokeWidth=2;fontColor={b.font_color};"
    if b.mono: base+="fontFamily=Courier New;fontSize=11;"
    if b.dashed: base+="dashed=1;"
    return base

def box_html(b):
    out=[]
    for k,t in b.lines:
        t=escape(t).replace(" ","&nbsp;") if b.mono and k=="text" else escape(t)
        if k=="title": out.append(f'<div style="text-align:center"><b>{t}</b></div>')
        elif k=="stereo": out.append(f'<div style="text-align:center"><i>{t}</i></div>')
        elif k=="hr": out.append('<hr size="1" style="margin:3px 0"/>')
        elif k=="gap": out.append('<div style="line-height:4px">&nbsp;</div>')
        elif k=="small": out.append(f'<div style="font-size:10px;color:#555">{t}</div>')
        else: out.append(f'<div style="white-space:nowrap">{t}</div>')
    return "".join(out)

ARROW={"open":("open",0),"block":("block",0),"diamond":("diamondThin",1),"many":("ERmany",0),"one":("ERmandOne",0),"zero":("ERzeroToOne",0),
       "zeromany":("ERzeroToMany",0),"onemany":("ERoneToMany",0),"zeroone":("ERzeroToOne",0),"oneone":("ERmandOne",0)}

def edge_style(e):
    st=e.style if isinstance(e.style,dict) else {}
    s="html=1;rounded=0;edgeStyle=none;jettySize=auto;orthogonalLoop=1;fontSize=11;"
    s+=f"strokeColor={st.get('color','#333333')};strokeWidth=1.5;"
    if st.get("dashed"): s+="dashed=1;"
    for key,pre in (("end","end"),("start","start")):
        a=st.get(key)
        if a:
            name,fill=ARROW.get(a,(a,0))
            s+=f"{pre}Arrow={name};{pre}Fill={fill};{pre}Size=12;"
        else: s+=f"{pre}Arrow=none;"
    # điểm neo
    def anchor(pt,b,pre):
        x=(pt[0]-b.x)/b.w; y=(pt[1]-b.y)/b.h
        return f"{pre}X={x:.4f};{pre}Y={y:.4f};{pre}Dx=0;{pre}Dy=0;"
    return s,anchor

def to_drawio(diagrams,path,agent="Claude"):
    out=['<mxfile host="app.diagrams.net" agent="%s" version="24.7.17" type="device">'%agent]
    for di,d in enumerate(diagrams):
        out.append(f'<diagram id="d{di}" name={quoteattr(d.name)}>')
        out.append(f'<mxGraphModel dx="{d.w}" dy="{d.h}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="{d.w}" pageHeight="{d.h}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>')
        for g in d.groups:
            out.append(f'<mxCell id="{g.id}" value={quoteattr(g.title)} style="rounded=1;whiteSpace=wrap;html=1;verticalAlign=top;align=left;spacingLeft=10;fontStyle=1;fontSize=12;fillColor={g.fill};strokeColor={g.stroke};strokeWidth=2;arcSize=3;" vertex="1" parent="1"><mxGeometry x="{g.x}" y="{g.y}" width="{g.w}" height="{g.h}" as="geometry"/></mxCell>')
        for b in d.boxes:
            out.append(f'<mxCell id="{b.id}" value={quoteattr(box_html(b))} style={quoteattr(box_style(b))} vertex="1" parent="1"><mxGeometry x="{b.x}" y="{b.y}" width="{b.w}" height="{b.h}" as="geometry"/></mxCell>')
        for n in d.notes:
            val=escape(n.text).replace("\n","<br/>")
            out.append(f'<mxCell id="{n.id}" value={quoteattr(val)} style="rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=6;spacingTop=4;fontSize=11;fillColor={n.fill};strokeColor={n.stroke};" vertex="1" parent="1"><mxGeometry x="{n.x}" y="{n.y}" width="{n.w}" height="{n.h}" as="geometry"/></mxCell>')
        for e in d.edges:
            s,anchor=edge_style(e)
            sb=d.by(e.src) if e.src else None; tb=d.by(e.tgt) if e.tgt else None
            if sb: s+=anchor(e.pts[0],sb,"exit")
            if tb: s+=anchor(e.pts[-1],tb,"entry")
            attrs=f'id="{e.id}" value="" style={quoteattr(s)} edge="1" parent="1"'
            if e.src: attrs+=f' source="{e.src}"'
            if e.tgt: attrs+=f' target="{e.tgt}"'
            mid="".join(f'<mxPoint x="{x:.1f}" y="{y:.1f}"/>' for x,y in e.pts[1:-1])
            geo=f'<mxGeometry relative="1" as="geometry"><mxPoint x="{e.pts[0][0]:.1f}" y="{e.pts[0][1]:.1f}" as="sourcePoint"/><mxPoint x="{e.pts[-1][0]:.1f}" y="{e.pts[-1][1]:.1f}" as="targetPoint"/>'
            if mid: geo+=f'<Array as="points">{mid}</Array>'
            geo+='</mxGeometry>'
            out.append(f'<mxCell {attrs}>{geo}</mxCell>')
        k=0
        for (lid,x,y,w,h,t) in label_rects(d):
            k+=1
            out.append(f'<mxCell id="lab_{k}" value={quoteattr(escape(t))} style="text;html=1;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=#a33333;" vertex="1" parent="1"><mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/></mxCell>')
        out.append('</root></mxGraphModel></diagram>')
    out.append('</mxfile>')
    open(path,"w",encoding="utf-8").write("\n".join(out))
