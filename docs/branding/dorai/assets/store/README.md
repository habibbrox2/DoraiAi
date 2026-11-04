# Store artwork sources

The logo pack (`../hexacore/`) ships app icons, favicons and wordmarks, but no store banners.
Artwork in this folder is authored from pack parts only: the D mark paths, the `#050A1C` field,
the `#25E2F5 → #377CFF → #6F5BFF` gradient and the existing taglines. Do not introduce new
shapes, colours or typefaces without updating `../../LOGO_REPLACEMENT_AUDIT.md` first.

## feature-graphic.svg → `fastlane/metadata/android/en-US/images/featureGraphic.png`

Google Play requires exactly 1024×500, 24-bit PNG, no alpha. The SVG is rasterised with headless
Chrome so text uses installed system fonts (Segoe UI first, then Inter / Helvetica Neue / Arial):

```bash
CHROME="/c/Program Files/Google/Chrome/Application/chrome.exe"
SRC=$(cygpath -m "$PWD/docs/branding/dorai/assets/store/feature-graphic.svg")
"$CHROME" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --default-background-color=00000000 --window-size=1024,500 \
  --screenshot="build/tmp/feature-graphic-raw.png" "file:///$SRC"
```

Then flatten to 24-bit RGB (`Image.open(...).convert("RGB")`) and write it over
`fastlane/metadata/android/en-US/images/featureGraphic.png`. Keep all content inside
x 56–960 and y 40–460: Play crops the graphic on some surfaces.
