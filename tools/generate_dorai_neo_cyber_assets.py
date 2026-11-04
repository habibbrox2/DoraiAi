# ---------------------------------------------------------------------------
# RETIRED 2026-11-05 -- superseded by the approved Hexa Core (D-shape) identity.
# This script regenerates a pre-Hexa-Core DORAI mark and writes it over active
# app, site, and store assets. It is kept only as project history.
#   Active pipeline : python tools/import_hexacore_assets.py
#   Pack            : docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip
# Set DORAI_ALLOW_LEGACY_BRAND_GENERATORS=1 only to reproduce an old concept,
# and only with docs/branding/ output paths -- never against live assets.
# ---------------------------------------------------------------------------
import os as _os, sys as _sys
if _os.environ.get("DORAI_ALLOW_LEGACY_BRAND_GENERATORS") != "1":
    _sys.exit(
        "retired: this script would overwrite the approved Hexa Core logo assets. "
        "Run 'python tools/import_hexacore_assets.py' instead."
    )
# ---------------------------------------------------------------------------

from pathlib import Path
import json, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/"docs/branding/dorai/neo-cyber"; OUT.mkdir(parents=True,exist_ok=True)
N=(4,12,31); N2=(7,27,61); C=(0,226,245); B=(20,104,255); W=(244,250,255); M=(151,185,220); FD=Path(r"C:\Windows\Fonts")
def font(s,k="regular"):
 names={"regular":["segoeui.ttf","arial.ttf"],"bold":["segoeuib.ttf","arialbd.ttf"],"italic":["segoeuii.ttf","ariali.ttf"],"script":["segoesc.ttf","gabriola.ttf"]}
 for n in names[k]:
  p=FD/n
  if p.exists():
   try:return ImageFont.truetype(str(p),s)
   except:pass
 return ImageFont.load_default()
def grad(sz,a=N,b=N2,horiz=False):
 w,h=sz; im=Image.new("RGBA",sz); px=im.load()
 for y in range(h):
  for x in range(w):
   t=(x/w if horiz else y/h); px[x,y]=tuple(int(a[i]*(1-t)+b[i]*t) for i in range(3))+(255,)
 return im
def glow(im,box,col=C,blur=100,alpha=100):
 l=Image.new("RGBA",im.size); ImageDraw.Draw(l).ellipse(box,fill=col+(alpha,)); im.alpha_composite(l.filter(ImageFilter.GaussianBlur(blur)))
def label(d,xy,s,z,col=W,k="regular",anchor=None):d.text(xy,s,font=font(z,k),fill=col,anchor=anchor)
def D(d,box,col=C,fill=True):
 x,y,X,Y=box; w=X-x; h=Y-y
 p=[(x,y),(x+w*.43,y),(x+w*.77,y+h*.17),(X,y+h*.5),(x+w*.77,y+h*.83),(x+w*.43,Y),(x,Y)]
 if fill:
  d.polygon(p,fill=col); d.polygon([(x+w*.22,y+h*.22),(x+w*.43,y+h*.22),(x+w*.65,y+h*.34),(x+w*.8,y+h*.5),(x+w*.65,y+h*.66),(x+w*.43,y+h*.78),(x+w*.22,y+h*.78)],fill=N2)
  d.polygon([(x+w*.39,y+h*.37),(x+w*.63,y+h*.5),(x+w*.39,y+h*.63)],fill=col)
 else:d.line(p+[p[0]],fill=col,width=12,joint="curve")
def icon(sz=640,bg=N2):
 im=Image.new("RGBA",(sz,sz),bg+(255,)); glow(im,(sz*.08,sz*.08,sz*.92,sz*.92),B,sz*.1,85); d=ImageDraw.Draw(im)
 d.rounded_rectangle((24,24,sz-24,sz-24),radius=int(sz*.22),fill=N2,outline=(20,94,165),width=max(2,sz//100))
 D(d,(sz*.21,sz*.19,sz*.80,sz*.80))
 return im
def circuit(im,box,step=80):
 x0,y0,x1,y1=box; d=ImageDraw.Draw(im)
 for x in range(x0,x1,step):d.line((x,y0,x,y1),fill=(26,94,163,40),width=2)
 for y in range(y0,y1,step):d.line((x0,y,x1,y),fill=(26,94,163,34),width=2)
 for i in range(14):
  x=x0+(i*139)%(x1-x0); y=y0+(i*211)%(y1-y0); d.line((x,y,x+45,y),fill=(0,170,245,100),width=3); d.ellipse((x+40,y-5,x+50,y+5),fill=C+(200,))
def cyborg(im,cx,cy,s=1):
 glow(im,(cx-310*s,cy-360*s,cx+350*s,cy+360*s),C,120*s,90); d=ImageDraw.Draw(im)
 d.polygon([(cx-140*s,cy+140*s),(cx+120*s,cy+145*s),(cx+220*s,cy+350*s),(cx-240*s,cy+350*s)],fill=(13,44,87,255),outline=(37,155,232,255))
 head=[(cx-170*s,cy-230*s),(cx-65*s,cy-330*s),(cx+105*s,cy-300*s),(cx+195*s,cy-195*s),(cx+185*s,cy-105*s),(cx+245*s,cy-40*s),(cx+194*s,cy+4*s),(cx+204*s,cy+84*s),(cx+135*s,cy+157*s),(cx-42*s,cy+168*s),(cx-170*s,cy+68*s)]
 d.polygon(head,fill=(15,39,77,255),outline=(77,192,255,255),width=5)
 d.line([(cx+92*s,cy-174*s),(cx+163*s,cy-110*s),(cx+205*s,cy-40*s),(cx+155*s,cy-12*s),(cx+174*s,cy+40*s),(cx+122*s,cy+94*s)],fill=C,width=5,joint="curve")
 d.line((cx+105*s,cy-110*s,cx+172*s,cy-102*s),fill=C,width=8); d.ellipse((cx+128*s,cy-120*s,cx+147*s,cy-101*s),fill=W)
 d.arc((cx-205*s,cy-350*s,cx+220*s,cy+110*s),180,310,fill=(40,130,230,255),width=12)
 d.ellipse((cx-125*s,cy-90*s,cx+38*s,cy+73*s),fill=(6,24,55,255),outline=C,width=8); d.ellipse((cx-95*s,cy-60*s,cx+8*s,cy+43*s),outline=B,width=5); D(d,(cx-72*s,cy-42*s,cx-2*s,cy+28*s),C)
 d.arc((cx-170*s,cy-135*s,cx+80*s,cy+115*s),70,280,fill=(54,165,255,255),width=8)
def feature(d,cx,cy,k):
 d.ellipse((cx-28,cy-28,cx+28,cy+28),outline=C,width=4)
 if k=="Chat":
  d.rounded_rectangle((cx-14,cy-11,cx+14,cy+10),radius=5,outline=W,width=3); d.line((cx-5,cy+10,cx-11,cy+18),fill=W,width=3)
 elif k=="Agent":
  d.rounded_rectangle((cx-15,cy-13,cx+15,cy+13),radius=6,outline=W,width=3); d.ellipse((cx-8,cy-4,cx-2,cy+2),fill=C); d.ellipse((cx+2,cy-4,cx+8,cy+2),fill=C)
 elif k=="Coding":
  d.line((cx-6,cy-12,cx-17,cy,cx-6,cy+12),fill=W,width=4); d.line((cx+6,cy-12,cx+17,cy,cx+6,cy+12),fill=W,width=4)
 elif k=="Automation":
  d.ellipse((cx-9,cy-9,cx+9,cy+9),outline=W,width=3)
  for a in range(8):
   q=a*math.pi/4; xx=cx+math.cos(q)*16; yy=cy+math.sin(q)*16; d.ellipse((xx-4,yy-4,xx+4,yy+4),fill=W)
 else:
  d.ellipse((cx-13,cy-13,cx+7,cy+7),outline=W,width=3); d.line((cx+5,cy+6,cx+17,cy+18),fill=W,width=4)
def hero():
 im=grad((2400,1260)); glow(im,(1350,-100,2450,1150),B,260,85); glow(im,(1640,120,2380,1050),C,220,45); circuit(im,(1200,0,2400,1260),92)
 d=ImageDraw.Draw(im)
 for b in [(1510,115,2370,975),(1620,225,2260,865),(1735,340,2145,750)]:d.ellipse(b,outline=(0,184,255,85),width=4)
 cyborg(im,1900,580,1.18); d=ImageDraw.Draw(im)
 label(d,(145,220),"DORAI",146,W,"bold"); label(d,(725,220),"AI",146,C,"bold")
 label(d,(154,395),"Your AI.",60,W,"italic"); label(d,(430,395),"Your World.",60,C,"italic")
 label(d,(155,520),"Hybrid AI Assistant & Agent Platform",43,W)
 x=120
 for k in ["Chat","Agent","Coding","Automation","Research"]:
  feature(d,x,680,k); label(d,(x+47,662),k,28,W,"bold"); x+=225
 d.rounded_rectangle((155,790,950,890),radius=50,outline=C,width=4,fill=(7,28,62,190)); label(d,(552,840),"LOCAL   \u2022   CLOUD   \u2022   HYBRID",34,W,"bold","mm")
 label(d,(155,1060),"INTELLIGENCE IN MOTION",25,M,"bold")
 return im.convert("RGB")
def appicon():
 im=icon(700); d=ImageDraw.Draw(im); label(d,(350,590),"DORAI AI",54,W,"bold","mm"); return im
def badge():
 im=Image.new("RGBA",(700,700),N+(255,)); glow(im,(40,40,660,660),C,100,130); d=ImageDraw.Draw(im); d.ellipse((65,65,635,635),fill=N2,outline=C,width=8); d.ellipse((90,90,610,610),outline=B,width=3); D(d,(195,155,505,450)); label(d,(350,535),"DORAI AI",48,W,"bold","mm"); return im
def lockup(light):
 bg=(250,253,255,255) if light else (5,17,41,255); fg=(8,34,72,255) if light else W
 im=Image.new("RGBA",(1500,460),bg); d=ImageDraw.Draw(im); D(d,(90,90,390,370),B if light else C); d.line((465,85,465,375),fill=(40,119,181),width=4)
 label(d,(545,180),"DORAI",105,fg,"bold"); label(d,(965,180),"AI",105,C,"bold"); label(d,(552,270),"Your AI. Your World.",42,(45,103,155) if light else C,"italic"); return im
def darklock():
 im=Image.new("RGBA",(1500,460),(5,17,41,255)); glow(im,(160,250,1340,530),C,90,80); d=ImageDraw.Draw(im)
 label(d,(750,170),"DORAI",128,W,"bold","mm"); label(d,(1115,170),"AI",128,C,"bold","mm")
 label(d,(750,265),"Your AI. Your World.",42,W,"italic","mm"); d.arc((250,260,1260,590),195,345,fill=C,width=10)
 return im
def promo():
 im=grad((1500,620),N,(9,37,83)); glow(im,(50,440,1500,760),C,80,80); d=ImageDraw.Draw(im); label(d,(80,110),"DORAI AI",82,W,"bold"); label(d,(85,205),"Smarter. Faster. More Powerful.",36,C,"italic")
 for i,s in enumerate(["Multi-Model Support","AI Agent & MCP","Secure & Private","Cross-Platform"]):
  y=300+i*66; d.ellipse((825,y,865,y+40),outline=C,width=4); d.line((835,y+20,846,y+31,858,y+10),fill=C,width=5); label(d,(895,y-2),s,34,W)
 d.arc((30,450,1480,850),190,350,fill=C,width=8); return im
def portal():
 im=grad((1500,620),(3,10,28),(5,29,67)); circuit(im,(0,0,1500,620),60); glow(im,(250,50,980,650),C,140,125); d=ImageDraw.Draw(im)
 for i in range(13):d.line((0,350+i*i*2,1500,350+i*i*2),fill=(0,133,222,70),width=2)
 for x in range(-600,2100,110):d.line((750,290,x,620),fill=(0,133,222,65),width=2)
 d.rounded_rectangle((330,85,820,560),radius=220,outline=C,width=18); d.rounded_rectangle((380,135,770,560),radius=190,outline=B,width=8); d.polygon([(475,560),(475,285),(575,190),(675,285),(675,560)],fill=(0,91,185,55),outline=C)
 D(d,(1000,180,1160,340),C); label(d,(1210,230),"DORAI AI",66,W,"bold","mm"); label(d,(1210,305),"Your AI. Your World.",29,C,"italic","mm"); return im
def signature():
 im=grad((1500,620),(7,48,112),(15,118,210),True); glow(im,(120,40,1400,600),C,130,45); d=ImageDraw.Draw(im); label(d,(750,260),"DORAI AI",150,W,"script","mm"); d.arc((300,270,1200,520),190,350,fill=C,width=8); label(d,(750,455),"Your AI. Your World.",43,W,"regular","mm"); return im
def main():
 items={"main-banner.png":hero(),"app-icon.png":appicon(),"circle-badge.png":badge(),"light-print-lockup.png":lockup(True),"dark-lockup.png":darklock(),"promo-features.png":promo(),"promo-cyber-portal.png":portal(),"promo-signature.png":signature()}
 for n,im in items.items():im.save(OUT/n,optimize=True)
 icon(1024).save(OUT/"app-icon-1024.png",optimize=True); icon(512).save(OUT/"app-icon-512.png",optimize=True)
 svg='''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="DORAI AI forward D symbol"><defs><linearGradient id="d" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#00E2F5"/><stop offset=".55" stop-color="#1468FF"/><stop offset="1" stop-color="#0C3796"/></linearGradient></defs><rect width="512" height="512" rx="112" fill="#071B3D"/><path d="M104 88 H270 L395 169 L466 256 L395 343 L270 424 H104 Z" fill="url(#d)"/><path d="M190 174 H270 L342 213 L385 256 L342 299 L270 338 H190 Z" fill="#071B3D"/><path d="M260 211 L334 256 L260 301 Z" fill="url(#d)"/></svg>'''
 (OUT/"symbol.svg").write_text(svg,encoding="utf-8")
 (OUT/"wordmark.svg").write_text('''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 300"><defs><linearGradient id="d" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#00E2F5"/><stop offset="1" stop-color="#0C3796"/></linearGradient></defs><rect width="1200" height="300" rx="28" fill="#071B3D"/><g transform="translate(25 25) scale(.49)"><path d="M104 88 H270 L395 169 L466 256 L395 343 L270 424 H104 Z" fill="url(#d)"/><path d="M190 174 H270 L342 213 L385 256 L342 299 L270 338 H190 Z" fill="#071B3D"/><path d="M260 211 L334 256 L260 301 Z" fill="url(#d)"/></g><text x="320" y="145" font-family="Segoe UI,Arial" font-size="104" font-weight="700" fill="#F4FAFF">DORAI</text><text x="780" y="145" font-family="Segoe UI,Arial" font-size="104" font-weight="700" fill="#00E2F5">AI</text><text x="328" y="215" font-family="Segoe UI,Arial" font-size="37" font-style="italic" fill="#00E2F5">Your AI. Your World.</text></svg>''',encoding="utf-8")
 board=Image.new("RGB",(2400,2200),(3,10,25)); board.paste(items["main-banner.png"].resize((2400,1260)),(0,0)); d=ImageDraw.Draw(board); d.rectangle((0,1260,2400,2200),fill=(5,17,40)); label(d,(90,1320),"IDENTITY SYSTEM",35,C,"bold")
 board.paste(items["app-icon.png"].resize((300,300)),(90,1400)); board.paste(items["circle-badge.png"].resize((300,300)),(440,1400)); board.paste(items["light-print-lockup.png"].resize((650,200)),(800,1450)); board.paste(items["dark-lockup.png"].resize((500,180)),(1570,1460)); label(d,(90,1770),"PROMO ASSETS",35,C,"bold")
 x=90
 for k in ["promo-features.png","promo-cyber-portal.png","promo-signature.png"]:board.paste(items[k].resize((700,290)),(x,1850)); x+=760
 board.save(OUT/"brand-board.png",optimize=True)
 (OUT/"README.md").write_text("""# DORAI AI â€” Neo Cyber Identity

New independent local identity direction based on the supplied brief.

- Clear uppercase D silhouette with a forward / play-like right edge
- Cyan â†’ electric blue â†’ deep blue gradient
- Deep navy background, white and cyan typography
- Main banner with stylized cybernetic AI profile, glowing D earpiece and digital globe/circuit atmosphere
- App icon, circle badge, light/print lockup, dark lockup
- Three promo cards: feature list, cyber portal, signature wordmark
- Brand board: complete presentation sheet

Palette: #040C1F, #071B3D, #00E2F5, #1468FF, #0C3796, #F4FAFF
Tagline: Your AI. Your World.

These concept assets are isolated here; existing application and website assets remain unchanged pending visual approval.
""",encoding="utf-8")
 (OUT/"manifest.json").write_text(json.dumps({"brand":"DORAI AI","direction":"neo-cyber-forward-D","tagline":"Your AI. Your World.","integrated_into_app":False,"files":sorted(p.name for p in OUT.iterdir())},indent=2),encoding="utf-8")
 print("OUTPUT",OUT)
 for p in sorted(OUT.iterdir()):
  if p.is_file():print(p.name,p.stat().st_size)
if __name__=="__main__":main()




