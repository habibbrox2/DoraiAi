# DORAI AI — Logo Asset Usage Map

Every asset in the Hexa Core pack — now version-controlled at
`docs/branding/dorai/assets/hexacore/` — and where (if
anywhere) it is consumed in this repository. Canonical copy of the pack lives at
`docs/branding/dorai/assets/hexacore/`; the tables below use paths relative to
that directory.

Legend: **used** = copied verbatim into an active resource path ·
**derived** = converted to a required format/size · **unused** = kept in the
pack for future use, nothing in the app or site consumes it.

## SVG (`svg/`)

| Asset | Purpose | Where used |
|---|---|---|
| `d-shape-gradient.svg` | Transparent primary symbol, Aurora gradient | **source** for `logo.xml` body/core colours; **derived** → Android `ic_launcher_foreground.xml` (×2); **used** → `site/img/dorai-logo.svg` (static copy); its paths also drive the animated mark in `motion/logo-animation.svg` |
| `d-shape-black.svg` | Black monochrome symbol | unused (no dark-on-light print surface in repo) |
| `d-shape-white.svg` | White monochrome symbol | unused |
| `app-icon-dark.svg` | Navy tile + gradient D | **used** → `site/img/icon.svg`, `composeApp/src/wasmJsMain/resources/icon.svg`, `composeApp/icon.svg` |
| `app-icon-light.svg` | White tile + gradient D | unused (launcher tile is navy per brand direction) |
| `app-icon-monochrome-dark.svg` | White silhouette on navy | **derived** → Android `ic_launcher_monochrome.xml` (×2) |
| `app-icon-monochrome-light.svg` | Navy silhouette on white | unused |
| `wordmark-horizontal.svg` | Horizontal DORAI AI lockup | **used** → `site/img/dorai-wordmark.svg` |
| `wordmark-vertical.svg` | Vertical lockup | unused |

## App-icon PNGs (`png/app-icons/`)

Sizes supplied: 32, 48, 64, 72, 96, 128, 144, 152, 180, 192, 256, 384, 512, 1024
for each of `dorai-dark-*`, `dorai-light-*`, `dorai-mono-dark-*`,
`dorai-mono-light-*`, plus `dorai-symbol-transparent-{16,24,32,48,64,128,256,512,1024}`.

| Asset group | Purpose | Where used |
|---|---|---|
| `dorai-dark-1024.png` | Primary opaque icon master | **used** → `composeApp/icon.png`, `iosApp/…/AppIcon.appiconset/icon2.png` (alpha flattened); **derived** → `composeApp/icon.ico` (7 sizes), `composeApp/icon.icns` (`ic07`–`ic14`) |
| `dorai-dark-180.png` | Apple touch icon | **used** → `site/img/apple-icon-180x180.png`, `composeApp/src/wasmJsMain/resources/apple-icon-180x180.png`; **derived** → `site/img/apple-icon-180x180.webp` |
| `dorai-dark-{32,48,64,128,256,512}.png` | Direct ICNS/ICO members | **derived** → `composeApp/icon.icns`, `composeApp/icon.ico` |
| `dorai-dark-512.png` | Google Play listing icon | **used** → `fastlane/metadata/android/en-US/images/icon.png` |
| `dorai-dark-{72,96,144,152,192,256,384}.png` | Android density / store slots | unused — the launcher is a vector adaptive icon, so no `mipmap-*dpi` PNGs were added |
| `dorai-light-*.png` (14 sizes) | Light-theme icons | unused |
| `dorai-mono-dark-*.png` (14 sizes) | Monochrome raster icons | unused — monochrome launcher layer is vector |
| `dorai-mono-light-*.png` (14 sizes) | Light monochrome rasters | unused |
| `dorai-symbol-transparent-1024.png` | Transparent master for launch/print | **derived** → `iosApp/…/LaunchLogo.imageset/logo_transparent.pdf`, `composeApp/icon.pdf` |
| `dorai-symbol-transparent-{16,24,32,48,64,128,256,512}.png` | Small transparent symbols | unused |

## Favicons

| Asset | Purpose | Where used |
|---|---|---|
| `png/favicon/favicon-{16,32,48}.png` | Favicon raster sources | **derived** → `site/img/favicon.ico`, `composeApp/src/wasmJsMain/resources/favicon.ico` (each 16/32/48) |
| `png/favicon/favicon-64.png` | Large favicon | unused |
| `favicon.ico` (pack root, 16²) | Pack’s own mono-size ICO | **canonical** → `docs/branding/dorai/assets/hexacore/favicon.ico` (root duplicate removed) |
| `favicon.svg` | Scalable favicon | **canonical** → `docs/branding/dorai/assets/hexacore/favicon.svg` (root duplicate removed) |
| `site.webmanifest` | PWA manifest (`#050A1C` theme, 192/512 icons) | unused — neither the site nor the Wasm shell links a manifest (pre-existing gap); sourcing it here would require shipping `png/app-icons/*` alongside, which was out of scope |

## Documentation

| Asset | Where used |
|---|---|
| `README.md` (pack) | Copied to `docs/branding/dorai/README.md` by the user’s earlier extraction; referenced from the repo `README.md:120` |

## Regenerating

```
python tools/import_hexacore_assets.py          # rewrite every derived asset
python tools/import_hexacore_assets.py --check  # verify the source pack only
```

The source of truth is the version-controlled pack directory
`docs/branding/dorai/assets/hexacore/`. The importer checks 18 sentinel files, confirms the D-mark
path data, and refuses to run if the source looks tampered with. It never touches source XML in
`composeApp`/`androidApp`, because those carry the adaptive-icon safe-zone group.

The five legacy `tools/{generate,sync}_dorai_*.py` scripts are retired behind a hard guard: they
exit non-zero unless `DORAI_ALLOW_LEGACY_BRAND_GENERATORS=1` is set, because they would overwrite
the approved assets with the previous mark.

## Motion (`motion/`)

Authored from pack parts; the pack itself ships no animation. Nothing here is a byte copy.

| Asset | Purpose | Where used |
|---|---|---|
| `logo-animation.svg` | 512² animated mark: 0.9 s left-to-right reveal, core pop-in with halo at 0.82 s, 4.2 s breathe loop; `prefers-reduced-motion` shows the static mark | **used** → `site/img/logo_animation.svg` (hero mascot, `site/index.html:101`) |
| `logo-animation.gif` | 400², 64 frames at 18 fps (3.6 s loop) on the `#050A1C` field, for surfaces that cannot animate SVG | available for README/store/social use |
