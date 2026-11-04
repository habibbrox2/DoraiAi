#!/usr/bin/env python3
"""Synchronise DORAI AI platform assets from the canonical Hexa Core pack.

Canonical source of truth: docs/branding/dorai/assets/hexacore/
(version-controlled; the original ZIP archive was retired on 2026-11-05).

Every output is either a byte-for-byte copy of a pack file or a format
conversion of one. The mark is never redrawn or recoloured.

Usage:
    python tools/import_hexacore_assets.py           # rewrite derived assets
    python tools/import_hexacore_assets.py --check   # verify source only
"""

from __future__ import annotations

import struct
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PACK_DIR = ROOT / "docs/branding/dorai/assets/hexacore"
MARK_SIGNATURE = "M112 56 H250"

REQUIRED = [
    "svg/d-shape-gradient.svg",
    "svg/app-icon-dark.svg",
    "svg/app-icon-monochrome-dark.svg",
    "svg/wordmark-horizontal.svg",
    "favicon.ico",
    "favicon.svg",
    "site.webmanifest",
    "png/favicon/favicon-16.png",
    "png/favicon/favicon-32.png",
    "png/favicon/favicon-48.png",
    "png/app-icons/dorai-dark-32.png",
    "png/app-icons/dorai-dark-64.png",
    "png/app-icons/dorai-dark-128.png",
    "png/app-icons/dorai-dark-180.png",
    "png/app-icons/dorai-dark-256.png",
    "png/app-icons/dorai-dark-512.png",
    "png/app-icons/dorai-dark-1024.png",
    "png/app-icons/dorai-symbol-transparent-1024.png",
]


def verify_pack() -> None:
    missing = [name for name in REQUIRED if not (PACK_DIR / name).is_file()]
    if missing:
        sys.exit("refusing to import: missing pack files: " + ", ".join(missing))
    symbol = (PACK_DIR / "svg/d-shape-gradient.svg").read_text(encoding="utf-8")
    if MARK_SIGNATURE not in symbol:
        sys.exit("refusing to import: d-shape-gradient.svg is not the Hexa Core D mark")
    with Image.open(PACK_DIR / "png/app-icons/dorai-dark-1024.png") as icon:
        if icon.size != (1024, 1024):
            sys.exit(f"refusing to import: master icon is {icon.size}, expected 1024x1024")
    print(f"pack        : {PACK_DIR.relative_to(ROOT)} verified ({len(REQUIRED)} sentinels)")


def copy_file(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(src.read_bytes())
    print(f"copy        -> {dest.relative_to(ROOT)}")


def write_icns(dest: Path) -> None:
    """Build a PNG-based .icns (types ic07-ic14) from pack PNGs."""
    elements = [
        ("ic11", "dorai-dark-32.png"),
        ("ic12", "dorai-dark-64.png"),
        ("ic07", "dorai-dark-128.png"),
        ("ic08", "dorai-dark-256.png"),
        ("ic13", "dorai-dark-256.png"),
        ("ic09", "dorai-dark-512.png"),
        ("ic14", "dorai-dark-512.png"),
        ("ic10", "dorai-dark-1024.png"),
    ]
    body = b""
    for kind, name in elements:
        data = (PACK_DIR / "png/app-icons" / name).read_bytes()
        body += kind.encode("ascii") + struct.pack(">I", len(data) + 8) + data
    dest.write_bytes(b"icns" + struct.pack(">I", len(body) + 8) + body)
    print(f"icns        -> {dest.relative_to(ROOT)}")


def write_ico(dest: Path, sizes: list[tuple[int, int]]) -> None:
    source = Image.open(PACK_DIR / "png/app-icons/dorai-dark-1024.png").convert("RGBA")
    dest.parent.mkdir(parents=True, exist_ok=True)
    source.save(dest, format="ICO", sizes=sizes)
    print(f"ico         -> {dest.relative_to(ROOT)} ({len(sizes)} sizes)")


def write_pdf(dest: Path) -> None:
    source = Image.open(PACK_DIR / "png/app-icons/dorai-symbol-transparent-1024.png")
    dest.parent.mkdir(parents=True, exist_ok=True)
    source.save(dest, format="PDF", resolution=144.0)
    print(f"pdf         -> {dest.relative_to(ROOT)}")


def main() -> None:
    verify_pack()
    if "--check" in sys.argv:
        print("check       : source valid, no files written")
        return

    # --- Web (marketing site) -------------------------------------------------
    copy_file(PACK_DIR / "svg/app-icon-dark.svg", ROOT / "site/img/icon.svg")
    copy_file(PACK_DIR / "svg/d-shape-gradient.svg", ROOT / "site/img/dorai-logo.svg")
    copy_file(PACK_DIR / "svg/d-shape-gradient.svg", ROOT / "site/img/logo_animation.svg")
    copy_file(PACK_DIR / "svg/wordmark-horizontal.svg", ROOT / "site/img/dorai-wordmark.svg")
    copy_file(PACK_DIR / "png/app-icons/dorai-dark-180.png", ROOT / "site/img/apple-icon-180x180.png")
    write_ico(ROOT / "site/img/favicon.ico", [(16, 16), (32, 32), (48, 48)])
    Image.open(ROOT / "site/img/apple-icon-180x180.png").save(
        ROOT / "site/img/apple-icon-180x180.webp", format="WEBP", quality=92
    )
    print("webp        -> site/img/apple-icon-180x180.webp")

    # --- Web (Compose Wasm shell) --------------------------------------------
    wasm = ROOT / "composeApp/src/wasmJsMain/resources"
    copy_file(PACK_DIR / "svg/app-icon-dark.svg", wasm / "icon.svg")
    copy_file(PACK_DIR / "png/app-icons/dorai-dark-180.png", wasm / "apple-icon-180x180.png")
    write_ico(wasm / "favicon.ico", [(16, 16), (32, 32), (48, 48)])

    # --- iOS ------------------------------------------------------------------
    ios = ROOT / "iosApp/iosApp/Assets.xcassets"
    icon = Image.open(PACK_DIR / "png/app-icons/dorai-dark-1024.png").convert("RGBA")
    flat = Image.new("RGB", icon.size, (5, 10, 28))
    flat.paste(icon, mask=icon.split()[-1])
    flat.save(ios / "AppIcon.appiconset/icon2.png")
    print("png         -> iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png")
    write_pdf(ios / "LaunchLogo.imageset/logo_transparent.pdf")

    # --- Desktop packaging + store listing -----------------------------------
    copy_file(PACK_DIR / "png/app-icons/dorai-dark-1024.png", ROOT / "composeApp/icon.png")
    copy_file(PACK_DIR / "svg/app-icon-dark.svg", ROOT / "composeApp/icon.svg")
    write_ico(
        ROOT / "composeApp/icon.ico",
        [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )
    write_icns(ROOT / "composeApp/icon.icns")
    write_pdf(ROOT / "composeApp/icon.pdf")
    copy_file(
        PACK_DIR / "png/app-icons/dorai-dark-512.png",
        ROOT / "fastlane/metadata/android/en-US/images/icon.png",
    )


if __name__ == "__main__":
    main()
