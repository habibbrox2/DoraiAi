# DORAI AI — Logo Replacement Audit

Date: 2026-11-05
Brand direction: **Concept 4 — Hexa Core**, refined to the D-shaped mark
Tagline: “Your AI. Your World.”

## 1. Source pack

| Field | Value |
|---|---|
| Original archive | `docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip` — **deleted 2026-11-05** on the maintainer’s request after import; recoverable from commit `94dbd8f` |
| Size | 664,894 bytes |
| SHA-256 | `7821c5660df5b0221f08a418d1fb9839a4252f81efb3f5acd25251ee13d0462d` |
| Entries | 82 (SVG, PNG app icons 32–1024, favicon PNG/ICO/SVG, `site.webmanifest`, README) |
| Integrity | `testzip()` clean; no absolute paths, no `..` traversal |
| Verdict | **Full pack — approved source of truth** |

Rejected candidate: `docs/branding/dorai/DORAI_AI_Hexa_Core_Logo_Pack.zip`
(5,587 bytes, 9 entries, 7 SVGs + favicon + README). This is the earlier
small SVG-only pack, not the full pack. Deleted in the same cleanup; commit `94dbd8f` retains it.

Validated pack copy: `docs/branding/dorai/assets/hexacore/`.
Extraction staging: `build/tmp/hexacore-import/` (never over live assets).

## 2. Logo surfaces found

### Shared Compose Multiplatform
| Surface | File | State found |
|---|---|---|
| In-app mark | `composeApp/src/commonMain/composeResources/drawable/logo.xml` | Old D geometry (`M104,72 …`, flat triangle core) |

Consumers (unchanged, still resolve to the same resource name): `AgentHud.kt`
(×2), `BotMessage.kt`, `PremiumChatBackdrop.kt`, `DashboardScreen.kt`,
`ServicesSettings.kt`, `desktopMain/main.kt` (window icon).

### Android
| Surface | File | State found |
|---|---|---|
| Adaptive icon | `androidApp/src/main/res/drawable/ic_launcher.xml` | Structure only — **unchanged** |
| Foreground | `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` | Old D geometry |
| Monochrome | `androidApp/src/main/res/drawable/ic_launcher_monochrome.xml` | Old D silhouette, no forward core |
| Background | `androidApp/src/main/res/values/ic_launcher_background.xml` | `#050A1C` (already correct) |
| Mirror set | `composeApp/src/androidMain/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` | Old geometry |
| Mirror background | `composeApp/src/androidMain/res/values/ic_launcher_background.xml` | `#F5F8FC` — **conflicted** with the app module’s `#050A1C` |

Manifest reference `androidApp/src/main/AndroidManifest.xml:36` → `@drawable/ic_launcher`. Unchanged.

### iOS
| Surface | File | State found |
|---|---|---|
| App icon | `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png` (1024²) | Old mark, single universal slot (no size matrix) |
| Launch logo | `iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/logo_transparent.pdf` | Old mark, vector PDF |
| `Contents.json` (both sets) | — | **unchanged** (filenames kept stable) |

### Web — marketing site
| Surface | File | State found |
|---|---|---|
| Favicon (ICO) | `site/img/favicon.ico` | Old mark, multi-size |
| Favicon (SVG) | `site/img/icon.svg` | Old D on `#08132B` tile |
| Logo / README | `site/img/dorai-logo.svg` (used by `README.md:5`) | Old D, `aurora` gradient id |
| Hero mascot | `site/img/logo_animation.svg` (used by `site/index.html:101`) | Old D duplicate of the logo |
| Wordmark | `site/img/dorai-wordmark.svg` | Old D + navy plate + tagline |
| Apple touch icon | `site/img/apple-icon-180x180.png` / `.webp` | Old mark |

HTML wiring (unchanged; filenames preserved): `site/index.html:27-29,101`,
`site/404.html:9-11`, `site/docs/index.html:11-13`, `site/latex-math/index.html:29-31`,
`site/run-gemma-locally/index.html:27-29`.

### Web — Compose Wasm shell
| Surface | File | State found |
|---|---|---|
| Favicon / icon / touch icon | `composeApp/src/wasmJsMain/resources/{favicon.ico,icon.svg,apple-icon-180x180.png}` | Old mark |
| `index.html:8-10` | — | **unchanged** (same filenames) |
| PWA manifest | none | No manifest is linked or shipped — pre-existing gap, not introduced here |

### Desktop packaging
| Surface | File | State found |
|---|---|---|
| Window icon | `composeApp/src/commonMain/…/logo.xml` via `Res.drawable.logo` | Old geometry |
| Installer icon | `composeApp/icon.png` (1024²), `composeApp/icon.ico`, `composeApp/icon.icns`, `composeApp/icon.svg`, `composeApp/icon.pdf` | Old mark |
| Gradle wiring | `composeApp/build.gradle.kts:211-218` (`icns`/`ico`/`png`) | **unchanged** |

### Google Play store listing
| Surface | File | State found |
|---|---|---|
| Store icon | `fastlane/metadata/android/en-US/images/icon.png` (512²) | Old mark — **missed in the first audit pass**, found by tracing the legacy generator’s output paths |
| Feature graphic | `fastlane/metadata/android/en-US/images/featureGraphic.png` (1024×500, 24-bit RGB) | **Authored 2026-11-05** — replaced the pre-migration placeholder (two overlapping purple circles on white, no DORAI mark, no pack palette). The pack ships no banner, so it is built from pack parts only: the D-shaped mark, the `#050A1C` field, the pack gradient and the existing tagline. Vector source `assets/store/feature-graphic.svg`; regression recipe in `assets/store/README.md`. |
| Store screenshots | `fastlane/metadata/android/*/images/**` | Contain the old in-app UI; out of scope for asset replacement (they are captured screen state, not logo files) |

## 3. Proposed and applied mapping

| Platform target | Pack source | Operation |
|---|---|---|
| `logo.xml` (commonMain) | `svg/d-shape-gradient.svg` | Path data → VectorDrawable; gradient → solid `#25E2F5` body + `#377CFF` core. JetBrains documents CMP XML vectors as “the same format as Android, except no external resource references” and says nothing about `aapt:attr` gradients, so the shared resource deliberately avoids them rather than risk a runtime parse failure. |
| Android foreground ×2 | `svg/d-shape-gradient.svg` | Path data → VectorDrawable **with** true linear gradient, scaled 0.76 into the 66dp safe zone |
| Android monochrome ×2 | `svg/app-icon-monochrome-dark.svg` | Silhouette (D + forward core) → `#000000`, same safe-zone group |
| Android background ×2 | `site.webmanifest` / icon tiles | `#050A1C` |
| iOS `icon2.png` | `png/app-icons/dorai-dark-1024.png` | Opaque RGB flatten (App Store forbids alpha), 1024² |
| iOS `logo_transparent.pdf` | `png/app-icons/dorai-symbol-transparent-1024.png` | PDF at 144 dpi, same filename/format so `Contents.json` stays valid |
| `site/img/icon.svg`, `wasm icon.svg`, `composeApp/icon.svg` | `svg/app-icon-dark.svg` | Byte copy |
| `site/img/dorai-logo.svg`, `logo_animation.svg` | `svg/d-shape-gradient.svg` | Byte copy (transparent symbol) |
| `site/img/dorai-wordmark.svg` | `svg/wordmark-horizontal.svg` | Byte copy |
| `favicon.ico` (site + wasm) | `png/favicon/favicon-16/32/48.png` | Pack has 16² only; rebuilt multi-size ICO 16/32/48 from pack PNGs |
| `apple-icon-180x180.png` ×2, `.webp` | `dorai-dark-180.png` | Byte copy; WebP re-encoded q92 |
| `composeApp/icon.png` | `dorai-dark-1024.png` | Byte copy |
| `composeApp/icon.ico` | `dorai-dark-1024.png` | 7-size ICO 16→256 |
| `composeApp/icon.icns` | `dorai-dark-32…1024.png` | PNG-based ICNS (`ic07`–`ic14`) |
| `composeApp/icon.pdf` | `dorai-symbol-transparent-1024.png` | PDF at 144 dpi |
| `docs/branding/dorai/{favicon.ico,favicon.svg}` | Root duplicates of the pack favicons | Removed in the 2026-11-05 cleanup; canonical copies live in `assets/hexacore/` |
| fastlane store icon | `png/app-icons/dorai-dark-512.png` | Byte copy, 512² |

Nothing was redrawn, recoloured, stretched, or given effects. Every raster is
either a pack file verbatim or a re-encode of one at a required format/size.

## 4. Ambiguities and items deliberately preserved

| Item | Why preserved |
|---|---|
| `iosApp/…/AppIcon.appiconset/Contents.json` | Pack ships one 1024² icon. The catalog still uses a single `universal`/`ios` slot, so no size matrix was invented. If iPad/Mac slots are added later, the pack’s `dorai-dark-{20,29,40,60,76,83.5,1024}` equivalents must be generated. |
| `site/img/logo_animation.svg` | The name promised animation but the file was static (byte copy of `svg/d-shape-gradient.svg`). **Now genuinely animated (2026-11-05)**, built from pack parts only — the mark sweeps in left to right, the forward core lands with a soft halo, then the mark breathes. `prefers-reduced-motion` falls back to the static mark. Canonical source `assets/motion/logo-animation.svg`; a 400² GIF covers surfaces that cannot animate SVG. The name finally matches the content, so `site/index.html:101` is untouched. |
| `docs/branding/dorai/{assets,aurora,neo-cyber,png}/**` | Superseded design directions. They live only under `docs/branding/` and are referenced by no app or site code. Left intact as history rather than deleted. `assets/` still holds the *Nexus Orbit* direction; the new pack sits at `assets/hexacore/` as instructed. |
| `tools/generate_dorai_{aurora,brand,neo_cyber}_assets.py`, `tools/sync_dorai_{aurora,brand}_assets.py` | Legacy generators that re-emit the **old** mark; two of them write directly into `logo.xml`, `ic_launcher_foreground.xml`, desktop/iOS icons, site images and the fastlane store icon. Retired in place behind a hard guard (non-zero exit with an override env var), never deleted. |
| `site/img/apple-icon-180x180.webp` | No HTML references it, but it is a brand asset and was refreshed to match. |
| `docs/branding/dorai/README.md` | Exists (copied pack README). Pre-existing link from `README.md:120` still resolves. |
| `README.md:120` → `docs/branding/dorai/README.md` | Valid on disk. |
| `create-hexacore-pack.ps1` | Authoring script for the *previous* 9-file pack; not re-run (would overwrite the small archive). |

## 5. Backup

**Retired at the maintainer’s request on 2026-11-05.** The 27 superseded files were backed up
to `docs/branding/dorai/_legacy-logo-backup/20261105-030027/` before any edit and committed in
`94dbd8f`; that tree and the superseded design directions were then deleted to declutter the
repository.

Recovery: `git checkout 94dbd8f -- docs/branding/dorai/_legacy-logo-backup` (and the same trick
for `docs/branding/dorai/{aurora,neo-cyber,png}`). The canonical Hexa Core pack itself is kept
in the working tree at `docs/branding/dorai/assets/hexacore/`.

## 6. Verification plan

1. XML well-formedness for every touched vector/SVG — see implementation report.
2. Raster dimension / colour-mode / ICO size-matrix / ICNS element checks.
3. Safe-zone geometry check for the adaptive icon.
4. Sweep for residual old-mark path signatures in source paths.
5. Android resource processing (`aapt2`) for the modified drawables.

## 7. Refinement applied after import (2026-11-05)

The pack geometry was superseded by a refined **v2 squircle** mark, adopted by the
maintainer after a four-variant comparison:

- uniform 80-unit stroke on all three straight edges (was 78/88, visibly uneven)
- bowl and counter are concentric: outer R=196, inner R=116, both centred at x=244
- core moved to the counter's optical centre, 94 units wide with a 22-unit corner radius
- outer left corners opened to R=34

Resulting bbox: x 84–440, y 60–452 (356 × 392), centre (262, 256). All pack SVGs,
the PNG matrix, both wordmarks, the Android vector resources and every derived
platform asset were regenerated from those numbers; `MARK_SIGNATURE` in the
importer now tracks `M118 60 H244`.
