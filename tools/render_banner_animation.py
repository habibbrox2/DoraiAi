#!/usr/bin/env python3
"""Render the animated DORAI AI banner to an animated WebP.

Chrome's --virtual-time-budget is unreliable for stepping a CSS animation frame by
frame: launch timing jitter shifts the effective time and the capture occasionally
returns a blank page. Instead every element is rasterised once as a transparent
layer, and the timeline below composites them deterministically.

Usage:
    python tools/render_banner_animation.py            # writes the WebP
    python tools/render_banner_animation.py --frames    # also dump PNG frames
"""

from __future__ import annotations

import argparse
import math
import shutil
import subprocess
import tempfile
from pathlib import Path

from PIL import Image

CHROME_CANDIDATES = [
    r"C:/Program Files/Google/Chrome/Application/chrome.exe",
    r"C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "/usr/bin/google-chrome",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
]

W, H = 1200, 630
OUT = Path("docs/branding/dorai/assets/motion/banner-animation.webp")

DEFS = """
<defs>
  <linearGradient id="dgrad" x1="84" y1="60" x2="440" y2="452" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset=".48" stop-color="#377CFF"/><stop offset="1" stop-color="#6F5BFF"/></linearGradient>
  <linearGradient id="pgrad" x1="214" y1="200" x2="308" y2="312" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset="1" stop-color="#377CFF"/></linearGradient>
  <linearGradient id="aigrad" x1="350" y1="120" x2="470" y2="200" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset="1" stop-color="#377CFF"/></linearGradient>
  <linearGradient id="rule" x1="72" y1="0" x2="440" y2="0" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset="1" stop-color="#377CFF" stop-opacity=".15"/></linearGradient>
  <radialGradient id="glowA" cx="966" cy="300" r="400" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#377CFF" stop-opacity=".34"/><stop offset=".55" stop-color="#377CFF" stop-opacity=".10"/><stop offset="1" stop-color="#377CFF" stop-opacity="0"/></radialGradient>
  <radialGradient id="glowB" cx="120" cy="40" r="360" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5" stop-opacity=".13"/><stop offset="1" stop-color="#25E2F5" stop-opacity="0"/></radialGradient>
  <radialGradient id="coreGlow" cx="966" cy="300" r="150" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5" stop-opacity=".5"/><stop offset=".6" stop-color="#377CFF" stop-opacity=".18"/><stop offset="1" stop-color="#377CFF" stop-opacity="0"/></radialGradient>
</defs>
"""

FONT = "Segoe UI, Inter, Helvetica Neue, Arial, sans-serif"
MARK_T = "translate(730.2,69.6) scale(.9)"
D_PATH = ("M118 60 H244 C352.25 60 440 147.75 440 256 C440 364.25 352.25 452 244 452 H118 "
          "C99 452 84 437 84 418 V94 C84 79 103 60 118 60 Z M164 140.0 V372.0 H244 "
          "C308.07 372.0 360.0 320.07 360.0 256 C360.0 191.93 308.07 140.0 244 140.0 Z")
CORE_PATH = ("M214.0 223.0 Q214 201 233.0 212.1 L289.0 244.9 Q308 256 289.0 267.1 "
             "L233.0 299.9 Q214 311 214.0 289.0 Z")

CHIPS = [(72, "01", "CHAT"), (192, "02", "AGENTS"), (312, "03", "CODE"),
         (432, "04", "AUTOMATE"), (552, "05", "DISCOVER")]

LAYERS: dict[str, str] = {
    "glow": f'<rect width="{W}" height="{H}" fill="url(#glowA)"/><rect width="{W}" height="{H}" fill="url(#glowB)"/>',
    "rings": ('<g fill="none" stroke="#377CFF" stroke-width="1.6" stroke-opacity=".16">'
              '<circle cx="966" cy="300" r="196"/><circle cx="966" cy="300" r="232" stroke-opacity=".10"/></g>'),
    "halo": '<circle cx="966" cy="300" r="150" fill="url(#coreGlow)"/>',
    "mark": f'<g transform="{MARK_T}"><path d="{D_PATH}" fill="url(#dgrad)" fill-rule="evenodd"/></g>',
    "core": f'<g transform="{MARK_T}"><path d="{CORE_PATH}" fill="url(#pgrad)"/></g>',
    "name": (f'<g font-family="{FONT}"><text x="72" y="188" font-size="84" font-weight="800" letter-spacing="-2.5" fill="#F5F8FC">DORAI</text>'
             f'<text x="350" y="188" font-size="84" font-weight="800" letter-spacing="-2.5" fill="url(#aigrad)">AI</text></g>'),
    "rule": '<rect x="72" y="210" width="368" height="5" rx="2.5" fill="url(#rule)"/>',
    "tag1": f'<text x="74" y="270" font-family="{FONT}" font-size="29" fill="#DCE7F7">Intelligence, in every direction.</text>',
    "tag2": f'<text x="74" y="314" font-family="{FONT}" font-size="27" font-style="italic" fill="#25E2F5">Your AI. Your World.</text>',
    "body": (f'<g font-family="{FONT}" font-size="23" fill="#9FB3CF">'
             f'<text x="74" y="372">A unified space for thinking, creating,</text>'
             f'<text x="74" y="404">building and getting things done.</text></g>'),
    "chips": "".join(
        f'<g font-family="{FONT}"><rect x="{x}" y="446" width="108" height="58" rx="13" fill="#0C1730" stroke="#377CFF" stroke-opacity=".35"/>'
        f'<text x="{x+13}" y="470" font-size="12.5" font-weight="600" letter-spacing="1" fill="#5E7FA8">{n}</text>'
        f'<text x="{x+13}" y="492" font-size="13.5" font-weight="600" letter-spacing=".5" fill="#D8E6F8">{label}</text></g>'
        for x, n, label in CHIPS),
    "pill": (f'<g font-family="{FONT}"><rect x="72" y="536" width="396" height="56" rx="28" fill="none" stroke="#25E2F5" stroke-opacity=".55"/>'
             f'<text x="270" y="571" text-anchor="middle" font-size="18" font-weight="600" letter-spacing="2.6" fill="#8FE6F7">LOCAL / CLOUD / HYBRID</text></g>'),
    "foot": (f'<text x="72" y="612" font-family="{FONT}" font-size="12.5" font-weight="600" letter-spacing="2.6" '
             f'fill="#4E6B93">ONE ASSISTANT. MANY POSSIBILITIES.</text>'),
}

FPS = 10
DURATION = 3.2          # seconds of story
FADE = 0.2              # trailing dissolve so the loop does not flash
BG = (5, 10, 28)


def find_chrome() -> str:
    for c in CHROME_CANDIDATES:
        if Path(c).exists() or shutil.which(c):
            return c
    raise SystemExit("Chrome not found")


def render(chrome: str, name: str, body: str, workdir: Path) -> Image.Image:
    svg = workdir / f"{name}.svg"
    png = workdir / f"{name}.png"
    svg.write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
        f"{DEFS}{body}</svg>", encoding="utf-8")
    profile = Path(tempfile.mkdtemp())
    subprocess.run(
        [chrome, "--headless", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
         "--no-first-run", "--no-default-browser-check", "--disable-extensions", "--disable-sync",
         f"--user-data-dir={profile.as_posix()}", "--default-background-color=00000000",
         f"--window-size={W},{H}", f"--screenshot={png.as_posix()}",
         "file:///" + svg.as_posix()],
        capture_output=True, timeout=90, check=True)
    shutil.rmtree(profile, ignore_errors=True)
    if not png.exists():
        raise SystemExit(f"chrome produced no screenshot for layer {name}")
    return Image.open(png).convert("RGBA")


def ease_out(p: float) -> float:
    return 1 - (1 - p) ** 3


def ramp(t: float, start: float, dur: float) -> float:
    return max(0.0, min(1.0, (t - start) / dur))


def pop(t: float, start: float) -> float:
    """Opacity + scale of the play core."""
    c = ramp(t, start, 0.52)
    if c >= 1.0:
        s = 1.0
    elif c < 0.62:
        s = 0.42 + (1.07 - 0.42) * (1 - (1 - c / 0.62) ** 2)
    else:
        s = 1.07 - 0.07 * ((c - 0.62) / 0.38)
    if t >= 1.9:
        s *= 1 + 0.05 * (0.5 - 0.5 * math.cos(2 * math.pi * (t - 1.9) / 4.2))
    return s, min(1.0, c / 0.25)


def place(img: Image.Image, op: float, dy: float = 0.0) -> Image.Image:
    if op >= 1.0 and dy == 0:
        return img
    out = img.copy()
    out.putalpha(out.split()[3].point(lambda v: int(v * op)))
    return out if dy == 0 else out.transform(
        out.size, Image.Transform.AFFINE, (1, 0, 0, 0, 1, -dy), resample=Image.Resampling.BICUBIC)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--frames", action="store_true", help="also write PNG frames")
    args = ap.parse_args()

    chrome = find_chrome()
    work = Path(tempfile.mkdtemp(prefix="dorai-banner-"))
    L = {name: render(chrome, name, body, work) for name, body in LAYERS.items()}
    print(f"  rendered {len(L)} layers")

    n = int(FPS * DURATION)
    frames: list[Image.Image] = []
    for i in range(n):
        t = i / FPS
        fr = Image.new("RGBA", (W, H), (0, 0, 0, 0))

        bg_op = ease_out(ramp(t, 0.0, 0.7))
        fr.alpha_composite(place(L["glow"], bg_op))
        fr.alpha_composite(place(L["rings"], bg_op))

        if 1.0 <= t <= 2.05:
            h = (t - 1.0) / 1.05
            op = h / 0.3 if h < 0.3 else max(0.0, 1 - (h - 0.3) / 0.7)
            s = 0.6 + 0.85 * h
            box = L["halo"].getbbox()                 # the glow circle only
            halo = L["halo"].crop(box)
            hw, hh = max(1, int(halo.width * s)), max(1, int(halo.height * s))
            halo = place(halo.resize((hw, hh), Image.Resampling.LANCZOS), op)
            fr.alpha_composite(halo, (966 - hw // 2, 300 - hh // 2))

        e = ease_out(ramp(t, 0.25, 0.85))
        w = int(round(330 * e))
        if w > 0:
            fr.alpha_composite(L["mark"].crop((800, 60, 800 + w, 480)), (800, 60))

        if t >= 1.0:
            s, op = pop(t, 1.0)
            core = L["core"].crop(L["core"].getbbox())
            cw, ch = max(1, int(core.width * s)), max(1, int(core.height * s))
            core = core.resize((cw, ch), Image.Resampling.LANCZOS)
            core = place(core, op)
            fr.alpha_composite(core, (int(966 - cw / 2), int(300 - ch / 2)))

        def rise(layer: str, start: float, dur: float = 0.7) -> None:
            p = ramp(t, start, dur)
            if p > 0:
                fr.alpha_composite(place(L[layer], ease_out(p), 16 * (1 - ease_out(p))))

        rise("name", 0.5)
        p = ease_out(ramp(t, 0.85, 0.75))
        if p > 0:
            fr.alpha_composite(L["rule"].crop((72, 208, 72 + max(1, int(368 * p)), 217)), (72, 208))
        rise("tag1", 1.0)
        rise("tag2", 1.12)
        rise("body", 1.28)
        for k, (x, _, _) in enumerate(CHIPS):
            chip = L["chips"].crop((x - 2, 444, x + 110, 508))
            cp = ramp(t, 1.5 + 0.08 * k, 0.7)
            if cp > 0:
                fr.alpha_composite(place(chip, ease_out(cp), 16 * (1 - ease_out(cp))), (x - 2, 444))
        rise("pill", 1.95)
        rise("foot", 2.08)

        frame = Image.alpha_composite(Image.new("RGBA", (W, H), BG + (255,)), fr).convert("RGB")
        fade_from = DURATION - FADE
        if t >= fade_from:
            frame = Image.blend(Image.new("RGB", (W, H), BG), frame,
                                max(0.0, (DURATION - t) / FADE))
        frames.append(frame)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(OUT, save_all=True, append_images=frames[1:], duration=int(1000 / FPS),
                   loop=0, format="WEBP", quality=90, method=6)
    print(f"  wrote {OUT} ({OUT.stat().st_size // 1024} KB, {len(frames)} frames, {W}x{H})")

    if args.frames:
        dump = Path("build/tmp/banner-frames"); dump.mkdir(parents=True, exist_ok=True)
        for i, f in enumerate(frames):
            f.save(dump / f"f{i:03d}.png")
        print(f"  frames dumped to {dump}")
    shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
