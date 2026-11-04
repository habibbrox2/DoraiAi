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
ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/"docs/branding/dorai/aurora"; OUT.mkdir(parents=True,exist_ok=True)
W,H=2400,1350; NAVY=(5,10,28); INK=(8,19,43); CYAN=(37,226,245); BLUE=(55,124,255); VIOLET=(111,91,255); WHITE=(241,248,255); MUTED=(145,171,205); FONT=Path(r"C:\Windows\Fonts")
def font(n,b=False,i=False):
 names=(["segoeuiz.ttf","arialbi.ttf"] if b and i else ["segoeuib.ttf","arialbd.ttf"] if b else ["segoeuii.ttf","ariali.ttf"] if i else ["segoeui.ttf","arial.ttf"])
 for x in names:
  p=FONT/x
  if p.exists():
   try:return ImageFont.truetype(str(p),n)
   except:pass
 return ImageFont.load_default()
def text(d,xy,s,n,c=WHITE,b=False,i=False,anchor=None):d.text(xy,s,font=font(n,b,i),fill=c,anchor=anchor)
def grad(sz,a,b,horiz=False):
 w,h=sz; im=Image.new("RGBA",sz); px=im.load()
 for y in range(h):
  for x in range(w):
   t=x/(w-1) if horiz else y/(h-1); px[x,y]=tuple(round(a[k]*(1-t)+b[k]*t) for k in range(3))+(255,)
 return im
def glow(im,box,c=CYAN,blur=100,alpha=100):
 l=Image.new("RGBA",im.size); ImageDraw.Draw(l).ellipse(box,fill=c+(alpha,)); im.alpha_composite(l.filter(ImageFilter.GaussianBlur(blur)))
def mark(d,box):
 x,y,X,Y=box; w=X-x; h=Y-y
 d.rounded_rectangle((x,y,x+w*.28,Y),radius=int(w*.08),fill=CYAN)
 d.pieslice((x+w*.06,y,X,y+h),270,90,fill=BLUE)
 d.rectangle((x+w*.16,y+h*.22,x+w*.57,y+h*.78),fill=NAVY)
 d.polygon([(x+w*.44,y+h*.31),(x+w*.78,y+h*.5),(x+w*.44,y+h*.69)],fill=CYAN)
def globe(im,cx,cy,r):
 d=ImageDraw.Draw(im)
 for k in range(4):
  rr=r-k*42; d.ellipse((cx-rr,cy-rr,cx+rr,cy+rr),outline=(48,139,255,70-k*10),width=3)
 for q in [.34,.62,.84]:d.ellipse((cx-r*q,cy-r,cx+r*q,cy+r),outline=(0,204,245,65),width=2)
 d.arc((cx-r,cy-r,cx+r,cy+r),195,345,fill=(0,224,245,180),width=5); d.arc((cx-r,cy-r,cx+r,cy+r),15,165,fill=(70,123,255,160),width=4)
 for i in range(18):
  a=i*math.pi/9; x=cx+math.cos(a)*r*.9; y=cy+math.sin(a)*r*.9; d.ellipse((x-4,y-4,x+4,y+4),fill=CYAN+(200,))
def android(im,cx,cy,s=1):
 glow(im,(cx-430*s,cy-470*s,cx+400*s,cy+420*s),BLUE,160*s,90); d=ImageDraw.Draw(im)
 d.arc((cx-360*s,cy-470*s,cx+320*s,cy+370*s),205,535,fill=(61,127,255,210),width=int(14*s)); d.arc((cx-320*s,cy-430*s,cx+280*s,cy+340*s),210,520,fill=(0,216,240,110),width=int(5*s))
 d.polygon([(cx-80*s,cy+175*s),(cx+160*s,cy+140*s),(cx+310*s,cy+470*s),(cx-330*s,cy+470*s)],fill=(10,27,59,255),outline=(47,138,204,255))
 pts=[(cx+90*s,cy-340*s),(cx-80*s,cy-360*s),(cx-210*s,cy-260*s),(cx-240*s,cy-130*s),(cx-320*s,cy-55*s),(cx-255*s,cy-5*s),(cx-270*s,cy+75*s),(cx-205*s,cy+170*s),(cx-35*s,cy+210*s),(cx+155*s,cy+115*s),(cx+220*s,cy-65*s),(cx+190*s,cy-250*s)]
 d.polygon(pts,fill=(14,35,70,255),outline=(83,167,229,255),width=max(2,int(5*s)))
 d.line([(cx-205*s,cy-185*s),(cx-270*s,cy-75*s),(cx-210*s,cy-38*s),(cx-240*s,cy+15*s),(cx-178*s,cy+75*s),(cx-112*s,cy+94*s)],fill=(0,222,244,210),width=max(2,int(5*s)),joint="curve")
 d.line((cx-215*s,cy-128*s,cx-285*s,cy-115*s),fill=CYAN,width=max(2,int(7*s))); d.ellipse((cx-238*s,cy-139*s,cx-222*s,cy-123*s),fill=WHITE)
 for i in range(4):
  yy=cy-300*s+i*48*s; d.line((cx-80*s,yy,cx+145*s,yy-25*s),fill=(43,104,173,220),width=max(2,int(6*s)))
 d.ellipse((cx+72*s,cy-88*s,cx+250*s,cy+92*s),fill=(5,18,42,255),outline=CYAN,width=max(3,int(8*s))); d.ellipse((cx+94*s,cy-66*s,cx+228*s,cy+70*s),outline=(63,116,255,255),width=max(2,int(4*s))); mark(d,(cx+126*s,cy-37*s,cx+206*s,cy+43*s))
 d.arc((cx+44*s,cy-125*s,cx+280*s,cy+132*s),280,75,fill=(64,153,255,255),width=max(2,int(8*s)))
 for i in range(5):
  xx=cx-175*s+i*62*s; d.line((xx,cy+260*s,xx+38*s,cy+300*s),fill=(0,190,245,150),width=max(2,int(4*s)))
def hero():
 im=grad((W,H),NAVY,INK,True); glow(im,(1150,-180,2450,1380),VIOLET,280,60); glow(im,(1530,150,2360,1120),CYAN,230,35)
 d=ImageDraw.Draw(im)
 for i in range(170):
  x=(i*173+41)%W; y=(i*293+17)%H; r=1+i%3; d.ellipse((x-r,y-r,x+r,y+r),fill=(94,169,255,65+(i%4)*25))
 globe(im,1880,630,510); android(im,1870,610,1.05); d=ImageDraw.Draw(im)
 text(d,(145,160),"DORAI",154,WHITE,True); text(d,(700,160),"AI",154,CYAN,True); d.rounded_rectangle((150,355,770,365),radius=5,fill=(0,220,245,230))
 text(d,(155,430),"Intelligence, in every direction.",53,WHITE,False,True); text(d,(158,520),"Your AI. Your World.",44,CYAN,False,True)
 text(d,(158,660),"A unified space for thinking, creating,",38,(196,216,239)); text(d,(158,715),"building and getting things done.",38,(196,216,239))
 x=165
 for num,name in [("01","CHAT"),("02","AGENTS"),("03","CODE"),("04","AUTOMATE"),("05","DISCOVER")]:
  d.rounded_rectangle((x,865,x+208,965),radius=24,fill=(8,25,54,235),outline=(43,115,177,190),width=2); text(d,(x+22,890),num,22,CYAN,True); text(d,(x+22,923),name,24,WHITE,True); x+=225
 d.rounded_rectangle((155,1055,880,1155),radius=50,outline=(0,220,245,220),width=3,fill=(8,26,57,220)); text(d,(518,1105),"LOCAL  /  CLOUD  /  HYBRID",30,WHITE,True,anchor="mm"); text(d,(158,1240),"ONE ASSISTANT. MANY POSSIBILITIES.",23,MUTED,True)
 return im.convert("RGB")
def appicon(circle=False):
 im=Image.new("RGBA",(720,720),NAVY+(255,)); glow(im,(55,55,665,665),BLUE if not circle else CYAN,130,110); d=ImageDraw.Draw(im)
 if circle:
  d.ellipse((66,66,654,654),fill=(8,24,53,255),outline=CYAN,width=7); d.ellipse((90,90,630,630),outline=(61,112,240,180),width=3)
 else:d.rounded_rectangle((34,34,686,686),radius=164,fill=(9,26,58,255),outline=(55,139,211,220),width=5); d.ellipse((112,112,608,608),outline=(36,113,200,110),width=3)
 mark(d,(190,180,535,535)); return im
def lockup(light=False):
 bg=(248,251,255,255) if light else (6,17,40,255); fg=(9,31,66,255) if light else WHITE; im=Image.new("RGBA",(1600,470),bg); d=ImageDraw.Draw(im)
 mark(d,(90,80,390,380)); d.line((470,80,470,390),fill=(63,137,190,210),width=3)
 text(d,(560,165),"DORAI",110,fg,True); text(d,(1000,165),"AI",110,CYAN,True); text(d,(570,270),"Your AI. Your World.",43,(48,105,159) if light else (0,216,240),False,True); return im
def darklock():
 im=Image.new("RGBA",(1600,500),(5,15,36,255)); glow(im,(100,320,1500,590),CYAN,100,85); d=ImageDraw.Draw(im); text(d,(800,175),"DORAI",145,WHITE,True,anchor="mm"); text(d,(1195,175),"AI",145,CYAN,True,anchor="mm"); text(d,(800,280),"Your AI. Your World.",46,(203,226,247),False,True,anchor="mm"); d.arc((280,260,1330,650),195,345,fill=(0,220,245,230),width=8); return im
def promo_features():
 im=grad((1500,650),(5,13,34),(11,39,83),True); glow(im,(0,440,1500,760),BLUE,120,90); d=ImageDraw.Draw(im); text(d,(85,105),"DORAI AI",86,WHITE,True); text(d,(90,215),"Think bigger. Move faster.",42,CYAN,False,True); text(d,(90,300),"Your workspace for modern intelligence.",27,(192,214,239))
 for i,s in enumerate(["Multi-model intelligence","Agent workflows + MCP","Privacy-first control","One experience, every device"]):
  y=160+i*92; d.ellipse((850,y,900,y+50),outline=CYAN,width=4); d.line((862,y+25,875,y+38,891,y+13),fill=CYAN,width=5); text(d,(930,y+5),s,34,WHITE)
 d.arc((35,475,1460,850),190,350,fill=CYAN,width=7); return im
def promo_portal():
 im=grad((1500,650),(3,10,26),(8,29,66),True); glow(im,(180,20,1020,690),CYAN,140,115); d=ImageDraw.Draw(im)
 for i in range(12):d.line((0,330+i*i*2,1500,330+i*i*2),fill=(28,115,220,75),width=2)
 for x in range(-500,2050,100):d.line((720,260,x,650),fill=(28,115,220,70),width=2)
 d.rounded_rectangle((300,80,820,570),radius=230,outline=(0,222,245,230),width=18); d.rounded_rectangle((350,130,770,570),radius=200,outline=(64,111,255,210),width=7); d.polygon([(445,570),(445,300),(560,185),(675,300),(675,570)],fill=(0,130,230,50),outline=CYAN); mark(d,(1010,175,1175,340)); text(d,(1250,225),"DORAI AI",62,WHITE,True,anchor="mm"); text(d,(1250,300),"Step into what's next.",28,CYAN,False,True,anchor="mm"); return im
def promo_signature():
 im=grad((1500,650),(6,25,65),(20,105,185),True); glow(im,(100,50,1400,630),CYAN,150,50); d=ImageDraw.Draw(im); text(d,(750,260),"DORAI AI",154,WHITE,False,True,anchor="mm"); d.arc((280,270,1220,560),190,350,fill=CYAN,width=9); text(d,(750,470),"Your AI. Your World.",43,WHITE,False,anchor="mm"); return im
def main():
 assets={"hero-banner.png":hero(),"app-icon.png":appicon(),"circle-badge.png":appicon(True),"light-print-lockup.png":lockup(True),"dark-lockup.png":darklock(),"promo-features.png":promo_features(),"promo-portal.png":promo_portal(),"promo-signature.png":promo_signature()}
 for n,im in assets.items():im.save(OUT/n,optimize=True)
 appicon().resize((1024,1024),Image.Resampling.LANCZOS).save(OUT/"app-icon-1024.png",optimize=True); appicon().resize((512,512),Image.Resampling.LANCZOS).save(OUT/"app-icon-512.png",optimize=True)
 sym='''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="DORAI AI D mark"><defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#25E2F5"/><stop offset=".55" stop-color="#377CFF"/><stop offset="1" stop-color="#6F5BFF"/></linearGradient></defs><rect width="512" height="512" rx="112" fill="#08132B"/><path d="M104 86h112c132 0 212 72 212 170S348 426 216 426H104z" fill="url(#a)"/><path d="M184 166h35c83 0 132 33 132 90s-49 90-132 90h-35z" fill="#08132B"/><path d="M230 207l94 49-94 49z" fill="url(#a)"/></svg>'''
 word='''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 320"><defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#25E2F5"/><stop offset=".55" stop-color="#377CFF"/><stop offset="1" stop-color="#6F5BFF"/></linearGradient></defs><rect width="1200" height="320" rx="30" fill="#08132B"/><g transform="translate(35 35) scale(.48)"><path d="M104 86h112c132 0 212 72 212 170S348 426 216 426H104z" fill="url(#a)"/><path d="M184 166h35c83 0 132 33 132 90s-49 90-132 90h-35z" fill="#08132B"/><path d="M230 207l94 49-94 49z" fill="url(#a)"/></g><text x="330" y="155" font-family="Segoe UI,Arial" font-size="112" font-weight="700" fill="#F1F8FF">DORAI</text><text x="800" y="155" font-family="Segoe UI,Arial" font-size="112" font-weight="700" fill="#25E2F5">AI</text><text x="338" y="235" font-family="Segoe UI,Arial" font-size="38" font-style="italic" fill="#25E2F5">Your AI. Your World.</text></svg>'''
 (OUT/"symbol.svg").write_text(sym,encoding="utf-8"); (OUT/"wordmark.svg").write_text(word,encoding="utf-8")
 board=Image.new("RGB",(2400,2320),(4,10,25)); board.paste(assets["hero-banner.png"].resize((2400,1350)),(0,0)); d=ImageDraw.Draw(board); d.rectangle((0,1350,2400,2320),fill=(5,15,36)); text(d,(90,1415),"DORAI / VISUAL IDENTITY",34,CYAN,True)
 board.paste(assets["app-icon.png"].resize((290,290)),(95,1480)); board.paste(assets["circle-badge.png"].resize((290,290)),(430,1480)); board.paste(assets["light-print-lockup.png"].resize((690,203)),(800,1520)); board.paste(assets["dark-lockup.png"].resize((560,175)),(1580,1535)); text(d,(90,1840),"CAMPAIGN VARIATIONS",32,CYAN,True)
 for x,k in zip((90,865,1640),("promo-features.png","promo-portal.png","promo-signature.png")):board.paste(assets[k].resize((680,295)),(x,1900))
 board.save(OUT/"brand-board.png",optimize=True)
 (OUT/"README.md").write_text("""# DORAI AI â€” AURORA Identity

A fresh premium direction: luminous intelligence, restrained cyber aesthetics, and an unmistakable D mark.

- Original D glyph with a forward-pointing negative-space/play cue
- Cyan â†’ electric blue â†’ violet gradient
- Obsidian navy canvas, restrained light, editorial typography
- Illustrated android profile with a luminous D ear module and orbital digital globe
- Campaign language: â€œIntelligence, in every direction.â€ / â€œYour AI. Your World.â€

Deliverables: brand-board.png, hero-banner.png, symbol.svg, wordmark.svg, app-icon.png, app-icon-512.png, app-icon-1024.png, circle-badge.png, light-print-lockup.png, dark-lockup.png, promo-features.png, promo-portal.png, promo-signature.png.

Palette: #050A1C, #08132B, #25E2F5, #377CFF, #6F5BFF, #F1F8FF.

All assets are isolated concept work. Existing production app/site assets are untouched pending approval.
""",encoding="utf-8")
 (OUT/"manifest.json").write_text(json.dumps({"brand":"DORAI AI","direction":"AURORA","tagline":"Your AI. Your World.","integrated_into_app":False,"files":sorted(p.name for p in OUT.iterdir())},indent=2),encoding="utf-8")
 print("OUTPUT="+str(OUT))
 for p in sorted(OUT.iterdir()):
  if p.is_file():print(p.name,p.stat().st_size)
if __name__=="__main__":main()

