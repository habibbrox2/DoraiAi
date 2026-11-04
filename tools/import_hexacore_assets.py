#!/usr/bin/env python3
"""Import the approved DORAI AI Hexa Core (D-shape) logo pack.

Source of truth: docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip
Every output below is either a byte-for-byte copy of a pack file or a format
conversion of one (SVG -> VectorDrawable handled in the XML resources,
PNG -> ICO/ICNS/WEBP/PDF). The mark is never redrawn or recoloured.

Usage:  python tools/import_hexacore_assets.py
"""

from __future__ import annotations

import hashlib
import shutil
import struct
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip"
EXPECTED_SHA256 = "7821c5660df5b0221f08a418d1fb9839a4252f81efb3f5acd25251ee13d0462d"
PACK_DIR = ROOT / "docs/branding/dorai/assets/hexacore"
STAGING = ROOT / "build/tmp/hexacore-import"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def extract_pack() -> Path:
    actual = sha256(ARCHIVE)
    if actual != EXPECTED_SHA256:
        sys.exit(f"refusing to import: sha256 {actual} != {EXPECTED_SHA256}")
    if STAGING.exists():
        shutil.rmtree(STAGING)
    STAGING.mkdir(parents=True)
    with zipfile.ZipFile(ARCHIVE) as archive:
        bad = archive.testzip()
        if bad:
            sys.exit(f"refusing to import: corrupt entry {bad}")
        for name in archive.namelist():
            parts = name.replace("\\", "/").split("/")
            if name.startswith("/") or ".." in parts:
                sys.exit(f"refusing to import: unsafe path {name}")
        archive.extractall(STAGING)
    return STAGING


def copy_pack(src: Path) -> None:
    if PACK_DIR.exists():
        shutil.rmtree(PACK_DIR)
    shutil.copytree(src, PACK_DIR)
    print(f"pack        -> {PACK_DIR.relative_to(ROOT)}")


def copy_file(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dest)
    print(f"copy        -> {dest.relative_to(ROOT)}")


def write_icns(pack: Path, dest: Path) -> None:
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
        data = (pack / "png/app-icons" / name).read_bytes()
        body += kind.encode("ascii") + struct.pack(">I", len(data) + 8) + data
    dest.write_bytes(b"icns" + struct.pack(">I", len(body) + 8) + body)
    print(f"icns        -> {dest.relative_to(ROOT)}")


def write_ico(pack: Path, dest: Path, sizes: list[tuple[int, int]]) -> None:
    source = Image.open(pack / "png/app-icons/dorai-dark-1024.png").convert("RGBA")
    dest.parent.mkdir(parents=True, exist_ok=True)
    source.save(dest, format="ICO", sizes=sizes)
    print(f"ico         -> {dest.relative_to(ROOT)} ({len(sizes)} sizes)")


def write_pdf(pack: Path, dest: Path) -> None:
    source = Image.open(pack / "png/app-icons/dorai-symbol-transparent-1024.png")
    dest.parent.mkdir(parents=True, exist_ok=True)
    source.save(dest, format="PDF", resolution=144.0)
    print(f"pdf         -> {dest.relative_to(ROOT)}")


def main() -> None:
    pack = extract_pack()
    copy_pack(pack)

    # --- Web (marketing site) -------------------------------------------------
    copy_file(pack / "svg/app-icon-dark.svg", ROOT / "site/img/icon.svg")
    copy_file(pack / "svg/d-shape-gradient.svg", ROOT / "site/img/dorai-logo.svg")
    copy_file(pack / "svg/d-shape-gradient.svg", ROOT / "site/img/logo_animation.svg")
    copy_file(pack / "svg/wordmark-horizontal.svg", ROOT / "site/img/dorai-wordmark.svg")
    copy_file(pack / "png/app-icons/dorai-dark-180.png", ROOT / "site/img/apple-icon-180x180.png")
    write_ico(pack, ROOT / "site/img/favicon.ico", [(16, 16), (32, 32), (48, 48)])
    Image.open(ROOT / "site/img/apple-icon-180x180.png").save(
        ROOT / "site/img/apple-icon-180x180.webp", format="WEBP", quality=92
    )
    print("webp        -> site/img/apple-icon-180x180.webp")

    # --- Web (Compose Wasm shell) --------------------------------------------
    wasm = ROOT / "composeApp/src/wasmJsMain/resources"
    copy_file(pack / "svg/app-icon-dark.svg", wasm / "icon.svg")
    copy_file(pack / "png/app-icons/dorai-dark-180.png", wasm / "apple-icon-180x180.png")
    write_ico(pack, wasm / "favicon.ico", [(16, 16), (32, 32), (48, 48)])

    # --- iOS ------------------------------------------------------------------
    ios = ROOT / "iosApp/iosApp/Assets.xcassets"
    # App Store icons must not carry an alpha channel.
    icon = Image.open(pack / "png/app-icons/dorai-dark-1024.png").convert("RGBA")
    flat = Image.new("RGB", icon.size, (5, 10, 28))
    flat.paste(icon, mask=icon.split()[-1])
    flat.save(ios / "AppIcon.appiconset/icon2.png")
    print("png         -> iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png")
    write_pdf(pack, ios / "LaunchLogo.imageset/logo_transparent.pdf")

    # --- Desktop packaging ----------------------------------------------------
    copy_file(pack / "png/app-icons/dorai-dark-1024.png", ROOT / "composeApp/icon.png")
    # Google Play store listing icon
    copy_file(
        pack / "png/app-icons/dorai-dark-512.png",
        ROOT / "fastlane/metadata/android/en-US/images/icon.png",
    )
    copy_file(pack / "svg/app-icon-dark.svg", ROOT / "composeApp/icon.svg")
    write_ico(
        pack,
        ROOT / "composeApp/icon.ico",
        [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )
    write_icns(pack, ROOT / "composeApp/icon.icns")
    write_pdf(pack, ROOT / "composeApp/icon.pdf")

    # --- Branding root: confirm the loose favicons match the pack -------------
    for name in ("favicon.ico", "favicon.svg"):
        loose = ROOT / "docs/branding/dorai" / name
        if sha256(loose) != sha256(pack / name):
            copy_file(pack / name, loose)
        else:
            print(f"unchanged   -> {loose.relative_to(ROOT)} (identical to pack)")


if __name__ == "__main__":
    main()
