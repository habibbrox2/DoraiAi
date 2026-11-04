from pathlib import Path
from PIL import Image, ImageOps

root = Path(__file__).resolve().parents[1]
aurora = root / "docs/branding/dorai/aurora"
master = Image.open(aurora / "app-icon.png").convert("RGBA")
master1024 = master.resize((1024, 1024), Image.Resampling.LANCZOS)

def save_png(rel, size):
    target = root / rel
    target.parent.mkdir(parents=True, exist_ok=True)
    master1024.resize((size, size), Image.Resampling.LANCZOS).save(target, optimize=True)

save_png("composeApp/icon.png", 1024)
save_png("composeApp/src/wasmJsMain/resources/apple-icon-180x180.png", 180)
master1024.save(root / "composeApp/icon.ico", format="ICO", sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])
master1024.save(root / "composeApp/icon.icns", format="ICNS")
save_png("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png", 1024)
save_png("fastlane/metadata/android/en-US/images/icon.png", 512)
save_png("site/img/apple-icon-180x180.png", 180)
master1024.resize((32,32), Image.Resampling.LANCZOS).save(root / "site/img/favicon.ico", format="ICO")

hero = Image.open(aurora / "hero-banner.png").convert("RGB")
for rel in ["site/img/og-preview.png", "site/img/preview-banner.png", "site/img/preview.png"]:
    ImageOps.fit(hero, (1200,630), method=Image.Resampling.LANCZOS).save(root / rel, optimize=True)

symbol = (aurora / "symbol.svg").read_text(encoding="utf-8")
symbol_light = symbol.replace('fill="#08132B"', 'fill="#F8FBFF"')
(aurora / "symbol-light.svg").write_text(symbol_light, encoding="utf-8")
for rel in ["composeApp/icon.svg", "composeApp/src/wasmJsMain/resources/icon.svg", "site/img/icon.svg"]:
    (root / rel).write_text(symbol, encoding="utf-8")

logo = (root / "site/img/logo_animation.svg").read_text(encoding="utf-8")
(root / "site/img/dorai-logo.svg").write_text(logo, encoding="utf-8")

wordmark = (aurora / "wordmark.svg").read_text(encoding="utf-8")
light = wordmark.replace('fill="#08132B"', 'fill="#F8FBFF"')
light = light.replace('fill="#F1F8FF">DORAI', 'fill="#10213D">DORAI')
light = light.replace('fill="#25E2F5">AI', 'fill="#176BC5">AI')
light = light.replace('fill="#25E2F5">Your AI.', 'fill="#176BC5">Your AI.')
(aurora / "wordmark-light.svg").write_text(light, encoding="utf-8")
(aurora / "wordmark-dark.svg").write_text(wordmark, encoding="utf-8")
(root / "site/img/dorai-wordmark.svg").write_text(wordmark, encoding="utf-8")
print("AURORA platform and theme variants synchronized.")
