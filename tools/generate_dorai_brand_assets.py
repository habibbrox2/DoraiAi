
from __future__ import annotations
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
import json, math, shutil
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"docs/branding/dorai/assets"
ASSETS.mkdir(parents=True,exist_ok=True)
NAVY=(5,15,34); PANEL=(10,31,62); CYAN=(36,224,224); BLUE=(70,132,255); ICE=(239,248,255); MUTED=(150,177,207)
FONT_DIR=Path(r"C:\Windows\Fonts")

def font(size,bold=False):
    p=FONT_DIR/("segoeuib.ttf" if bold else "segoeui.ttf")
    return ImageFont.truetype(str(p if p.exists() else FONT_DIR/"arial.ttf"),size)


def draw_orbit(d,cx,cy,size,width):
    # Strong D silhouette: straight left stem + broad curved bowl.
    x=cx-size*.29; top=cy-size*.43; bottom=cy+size*.43
    d.line((x,top,x,bottom),fill=CYAN,width=width)
    d.arc((x-size*.02,top,cx+size*.43,bottom),270,450,fill=CYAN,width=width)
    # Inner intelligence core and restrained network orbit.
    d.ellipse((cx-size*.18,cy-size*.18,cx+size*.18,cy+size*.18),outline=BLUE,width=max(1,width//2))
    r=size*.105
    pts=[(cx,cy-r*1.5),(cx+r*.45,cy-r*.45),(cx+r*1.5,cy),(cx+r*.45,cy+r*.45),(cx,cy+r*1.5),(cx-r*.45,cy+r*.45),(cx-r*1.5,cy),(cx-r*.45,cy-r*.45)]
    d.polygon(pts,fill=CYAN)
    d.ellipse((cx-r*.38,cy-r*.38,cx+r*.38,cy+r*.38),fill=ICE)
    for nx,ny,col in [(cx+size*.26,cy-size*.25,BLUE),(cx+size*.26,cy+size*.25,ICE)]:
        nr=size*.035
        d.ellipse((nx-nr,ny-nr,nx+nr,ny+nr),fill=col,outline=NAVY,width=max(1,width//2))

def icon(size):
    im=Image.new("RGBA",(size,size),NAVY+(255,))
    glow=Image.new("RGBA",(size,size),(0,0,0,0)); ImageDraw.Draw(glow).ellipse((size*.15,size*.15,size*.85,size*.85),fill=CYAN+(52,))
    im=Image.alpha_composite(im,glow.filter(ImageFilter.GaussianBlur(max(2,size*.1))))
    d=ImageDraw.Draw(im); p=size*.08
    d.rounded_rectangle((p,p,size-p,size-p),radius=size*.23,fill=PANEL,outline=(24,67,111,255),width=max(1,int(size*.009)))
    draw_orbit(d,size/2,size/2,size*.78,max(1,int(size*.018)))
    return im

def banner(w,h):
    im=Image.new("RGB",(w,h)); px=im.load()
    for y in range(h):
        for x in range(w):
            t=min(1,(x/w)*.55+(y/h)*.45)
            px[x,y]=tuple(int(NAVY[i]*(1-t)+PANEL[i]*t) for i in range(3))
    im=im.convert("RGBA"); gl=Image.new("RGBA",(w,h),(0,0,0,0))
    ImageDraw.Draw(gl).ellipse((w*.62,-h*.48,w*1.14,h*1.35),fill=CYAN+(48,))
    im=Image.alpha_composite(im,gl.filter(ImageFilter.GaussianBlur(h*.15))); d=ImageDraw.Draw(im)
    for x in range(int(w*.54),w,52): d.line((x,0,x,h),fill=(55,103,155,34))
    for y in range(0,h,52): d.line((w*.52,y,w,y),fill=(55,103,155,34))
    pad=int(w*.075); d.text((pad,int(h*.23)),"DORAI AI",font=font(int(h*.18),True),fill=ICE)
    d.rounded_rectangle((pad,int(h*.48),pad+int(w*.11),int(h*.495)),radius=8,fill=CYAN)
    d.text((pad,int(h*.56)),"YOUR AI. YOUR WORLD.",font=font(int(h*.062),True),fill=CYAN)
    d.text((pad,int(h*.68)),"A hybrid AI assistant & agent platform",font=font(int(h*.043)),fill=MUTED)
    cx,cy=int(w*.80),int(h*.51); d.ellipse((cx-h*.43,cy-h*.43,cx+h*.43,cy+h*.43),outline=(36,224,224,130),width=max(2,int(h*.006)))
    draw_orbit(d,cx,cy,int(h*.68),max(2,int(h*.009)))
    return im.convert("RGB")


def symbol():
 return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="DORAI AI D Orbit symbol">
<rect width="512" height="512" rx="112" fill="#050F22"/><rect x="40" y="40" width="432" height="432" rx="104" fill="#0A1F3E" stroke="#18436F" stroke-width="5"/>
<path d="M150 82 V430 Q150 470 198 470 H256 C380 470 448 375 448 256 C448 137 380 42 256 42 H198 Q150 42 150 82 Z" fill="none" stroke="#24E0E0" stroke-width="27" stroke-linecap="round" stroke-linejoin="round"/>
<ellipse cx="286" cy="256" rx="112" ry="78" fill="none" stroke="#4684FF" stroke-width="10" transform="rotate(-22 286 256)"/>
<path d="M286 194 L305 238 L349 256 L305 274 L286 318 L267 274 L223 256 L267 238 Z" fill="#24E0E0"/><circle cx="286" cy="256" r="18" fill="#EFF8FF"/>
<circle cx="383" cy="173" r="13" fill="#EFF8FF" stroke="#050F22" stroke-width="6"/><circle cx="380" cy="339" r="14" fill="#4684FF" stroke="#050F22" stroke-width="6"/></svg>"""


def wordmark():
 return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1120 280" role="img" aria-label="DORAI AI — Your AI. Your World.">
<rect width="1120" height="280" rx="30" fill="#050F22"/><g transform="translate(8 8) scale(.5)">
<rect width="512" height="512" rx="112" fill="#050F22"/><rect x="40" y="40" width="432" height="432" rx="104" fill="#0A1F3E" stroke="#18436F" stroke-width="5"/>
<path d="M150 82 V430 Q150 470 198 470 H256 C380 470 448 375 448 256 C448 137 380 42 256 42 H198 Q150 42 150 82 Z" fill="none" stroke="#24E0E0" stroke-width="27" stroke-linecap="round" stroke-linejoin="round"/>
<ellipse cx="286" cy="256" rx="112" ry="78" fill="none" stroke="#4684FF" stroke-width="10" transform="rotate(-22 286 256)"/>
<path d="M286 194 L305 238 L349 256 L305 274 L286 318 L267 274 L223 256 L267 238 Z" fill="#24E0E0"/><circle cx="286" cy="256" r="18" fill="#EFF8FF"/></g>
<text x="292" y="130" fill="#EFF8FF" font-family="Segoe UI,Arial,sans-serif" font-size="94" font-weight="700" letter-spacing="2">DORAI AI</text><text x="298" y="194" fill="#24E0E0" font-family="Segoe UI,Arial,sans-serif" font-size="30" font-weight="600" letter-spacing="7">YOUR AI. YOUR WORLD.</text></svg>"""


def svg_banner():
 return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 630" role="img" aria-label="DORAI AI D Orbit social banner">
<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#050F22"/><stop offset="1" stop-color="#0A1F3E"/></linearGradient></defs><rect width="1200" height="630" fill="url(#bg)"/>
<circle cx="970" cy="320" r="246" fill="none" stroke="#24E0E0" stroke-opacity=".35" stroke-width="3"/>
<g transform="translate(766 116) scale(.8)"><rect width="512" height="512" rx="112" fill="#050F22"/><rect x="40" y="40" width="432" height="432" rx="104" fill="#0A1F3E" stroke="#18436F" stroke-width="5"/>
<path d="M150 82 V430 Q150 470 198 470 H256 C380 470 448 375 448 256 C448 137 380 42 256 42 H198 Q150 42 150 82 Z" fill="none" stroke="#24E0E0" stroke-width="27" stroke-linecap="round" stroke-linejoin="round"/>
<ellipse cx="286" cy="256" rx="112" ry="78" fill="none" stroke="#4684FF" stroke-width="10" transform="rotate(-22 286 256)"/>
<path d="M286 194 L305 238 L349 256 L305 274 L286 318 L267 274 L223 256 L267 238 Z" fill="#24E0E0"/><circle cx="286" cy="256" r="18" fill="#EFF8FF"/></g>
<text x="86" y="266" fill="#EFF8FF" font-family="Segoe UI,Arial,sans-serif" font-size="112" font-weight="700">DORAI AI</text><rect x="88" y="308" width="132" height="9" rx="4" fill="#24E0E0"/>
<text x="86" y="403" fill="#24E0E0" font-family="Segoe UI,Arial,sans-serif" font-size="40" font-weight="700">YOUR AI. YOUR WORLD.</text><text x="86" y="468" fill="#96B1CF" font-family="Segoe UI,Arial,sans-serif" font-size="29">A hybrid AI assistant &amp; agent platform</text></svg>"""

def main():
    (ASSETS/"symbol.svg").write_text(symbol(),encoding="utf-8")
    (ASSETS/"wordmark.svg").write_text(wordmark(),encoding="utf-8")
    (ASSETS/"banner-social.svg").write_text(svg_banner(),encoding="utf-8")
    for s in (16,32,48,72,96,144,180,192,256,512,1024): icon(s).save(ASSETS/f"icon-{s}.png")
    social=banner(1200,630); wide=banner(1920,640)
    social.save(ASSETS/"banner-social.png"); wide.save(ASSETS/"banner-wide.png")
    def cp(src, targets):
        for rel in targets:
            dst=ROOT/rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)
    cp(ASSETS/"icon-1024.png",["composeApp/icon.png","iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png"])
    cp(ASSETS/"icon-512.png",["fastlane/metadata/android/en-US/images/icon.png"])
    cp(ASSETS/"icon-180.png",["composeApp/src/wasmJsMain/resources/apple-icon-180x180.png","site/img/apple-icon-180x180.png"])
    cp(ASSETS/"banner-social.png",["site/img/preview-banner.png","site/img/og-preview.png","screenshots/og-preview.png"])
    cp(ASSETS/"icon-512.png",["site/img/preview.png"])
    icon(256).save(ROOT/"composeApp/icon.ico",format="ICO",sizes=[(16,16),(32,32),(48,48),(256,256)])
    icon(256).save(ROOT/"site/img/favicon.ico",format="ICO",sizes=[(16,16),(32,32),(48,48),(256,256)])
    Image.open(ASSETS/"icon-1024.png").convert("RGB").save(ROOT/"composeApp/icon.pdf","PDF",resolution=144.0)
    icon(1024).save(ROOT/"composeApp/icon.icns",format="ICNS")
    android="""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="512" android:viewportHeight="512">
<path android:fillColor="#00000000" android:strokeColor="#24E0E0" android:strokeWidth="27" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M150,82 L150,430 C150,452 170,470 198,470 L256,470 C380,470 448,375 448,256 C448,137 380,42 256,42 L198,42 C170,42 150,60 150,82"/>
<path android:fillColor="#00000000" android:strokeColor="#4684FF" android:strokeWidth="10" android:pathData="M390,218 C368,154 282,142 220,196 C165,244 204,326 280,334 C340,340 397,286 390,218"/>
<path android:fillColor="#24E0E0" android:pathData="M286,194 L305,238 L349,256 L305,274 L286,318 L267,274 L223,256 L267,238 Z"/>
<path android:fillColor="#EFF8FF" android:pathData="M304,256 A18,18 0,1 1,268 256 A18,18 0,1 1,304 256"/>
</vector>"""

    (ROOT/"androidApp/src/main/res/drawable/ic_launcher_foreground.xml").write_text(android,encoding="utf-8")
    (ROOT/"composeApp/src/commonMain/composeResources/drawable/logo.xml").write_text(android.replace("108dp","90dp"),encoding="utf-8")
    for rel in ["composeApp/icon.svg","composeApp/src/wasmJsMain/resources/icon.svg","site/img/icon.svg","site/img/dorai-logo.svg","site/img/logo_animation.svg"]:
        (ROOT/rel).write_text(symbol(),encoding="utf-8")
    (ROOT/"site/img/dorai-wordmark.svg").write_text(wordmark(),encoding="utf-8")
    (ASSETS/"README.md").write_text("# DORAI AI Brand Assets — Nexus Orbit\n\nA new identity direction: an orbital network around a luminous intelligence core. It replaces the previous geometric-D letterform monogram.\n\n- symbol.svg: master Nexus Orbit mark\n- wordmark.svg: horizontal brand lockup\n- banner-social.svg/png: 1200x630 social banner\n- banner-wide.png: 1920x640 wide banner\n- icon-*.png: multi-size app icon exports\n- Palette: #050F22, #0A1F3E, #24E0E0, #4684FF, #EFF8FF\n- Tagline: Your AI. Your World.\n",encoding="utf-8")
    (ASSETS/"manifest.json").write_text(json.dumps({"brand":"DORAI AI","tagline":"Your AI. Your World.","design_direction":"nexus-orbit","symbol":"orbital-network-intelligence-core","letterform_monogram":False,"icon_sizes":[16,32,48,72,96,144,180,192,256,512,1024],"generated_locally":True},indent=2),encoding="utf-8")
    print("Generated DORAI AI Nexus Orbit brand pack.")
if __name__=="__main__": main()
