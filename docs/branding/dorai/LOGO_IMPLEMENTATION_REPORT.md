# DORAI AI — Logo Implementation Report

Date: 2026-11-05
Scope: repository `G:\Web\DoraiAi-migration-worktree` (worktree; `git status` clean at start, branch `main`)

## Status summary

| Phase | Result |
|---|---|
| 1 — Locate and validate pack | ✅ Complete |
| 2 — Integrate | ✅ Complete for every active surface |
| 3 — Verify | ⚠️ Partial — Android resources + all asset formats validated; iOS/desktop **packaging** builds not runnable here |
| Backups | ✅ 26 files, timestamped |
| Commits / pushes | None (not requested) |

## 1. Pack located and validated

| Field | Value |
|---|---|
| Path (archive) | `docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip` — deleted after import; commit `94dbd8f` retains it |
| Size | 664,894 bytes |
| SHA-256 | `7821c5660df5b0221f08a418d1fb9839a4252f81efb3f5acd25251ee13d0462d` |
| Entries | 82 — 9 SVG, 72 app-icon PNG, 4 favicon PNG, `favicon.ico`, `favicon.svg`, `site.webmanifest`, `README.md` |
| Integrity | `zipfile.testzip()` clean; no absolute paths; no `..` traversal |
| Canonical copy (now source of truth) | `docs/branding/dorai/assets/hexacore/` — **82/82 files byte-identical to the archive** (SHA-256 compared per entry) |

The second archive, `DORAI_AI_Hexa_Core_Logo_Pack.zip` (5,587 bytes, 9 entries,
SVG-only), is the earlier small pack and was **rejected as the source** and left
untouched. No archive was overwritten.

The importer no longer reads an archive: it verifies 18 sentinel files inside
`docs/branding/dorai/assets/hexacore/` and derives everything from there.

## 2. Backup

The 27 superseded files were backed up to
`docs/branding/dorai/_legacy-logo-backup/20261105-030027/` before any edit, preserving relative
paths, and committed in `94dbd8f`.

**The backup tree was deleted on 2026-11-05 at the maintainer’s request**, together with the two
ZIP archives and the superseded design directions. Git history is now the recovery mechanism:

```
git checkout 94dbd8f -- docs/branding/dorai/_legacy-logo-backup
```

## 3. Changed files

### Hand-edited (vector resources)

| File | Change |
|---|---|
| `composeApp/src/commonMain/composeResources/drawable/logo.xml` | Old D paths → Hexa Core D paths; solid `#25E2F5` body + `#377CFF` forward core |
| `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` | New D + core paths, true linear gradient (`#25E2F5`→`#377CFF`→`#6F5BFF`), scaled 0.76 into the safe zone |
| `androidApp/src/main/res/drawable/ic_launcher_monochrome.xml` | New D + core silhouette, single `#000000` fill, same safe-zone group |
| `composeApp/src/androidMain/res/drawable/ic_launcher_foreground.xml` | Same as app-module foreground |
| `composeApp/src/androidMain/res/drawable/ic_launcher_monochrome.xml` | Same as app-module monochrome |
| `composeApp/src/androidMain/res/values/ic_launcher_background.xml` | `#F5F8FC` → `#050A1C` (matches app module + pack tiles) |

### Generated from pack (verbatim copies)

`site/img/{icon,dorai-logo,dorai-wordmark}.svg`,
`composeApp/src/wasmJsMain/resources/icon.svg`, `composeApp/icon.svg`,
`site/img/apple-icon-180x180.png`, `composeApp/src/wasmJsMain/resources/apple-icon-180x180.png`,
`composeApp/icon.png`, `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon2.png`,
`fastlane/metadata/android/en-US/images/icon.png` (512² Google Play listing icon)

### Generated derivatives (format/size conversion of pack sources)

| File | Derivation |
|---|---|
| `site/img/favicon.ico` | 16/32/48 from `png/favicon/favicon-*.png` |
| `composeApp/src/wasmJsMain/resources/favicon.ico` | same |
| `composeApp/icon.ico` | 16/24/32/48/64/128/256 from `dorai-dark-1024.png` |
| `composeApp/icon.icns` | PNG-based ICNS `ic07`–`ic14` from `dorai-dark-{32…1024}.png` |
| `iosApp/…/LaunchLogo.imageset/logo_transparent.pdf` | `dorai-symbol-transparent-1024.png` at 144 dpi (same filename/format, `Contents.json` unchanged) |
| `composeApp/icon.pdf` | same source |
| `site/img/apple-icon-180x180.webp` | re-encoded q92 from the new 180 px PNG |

### Added

| File | Purpose |
|---|---|
| `docs/branding/dorai/assets/hexacore/` | Canonical validated pack copy (82 files) |
| `tools/import_hexacore_assets.py` | Reproducible importer (verifies SHA-256, regenerates every derivative) |
| `docs/branding/dorai/LOGO_REPLACEMENT_AUDIT.md` | Phase-1 audit |
| `docs/branding/dorai/LOGO_ASSET_USAGE_MAP.md` | Asset → usage map |
| `docs/branding/dorai/LOGO_IMPLEMENTATION_REPORT.md` | This report |
| `docs/branding/dorai/_legacy-logo-backup/20261105-030027/` | Backup |

### Unchanged on purpose

`androidApp/.../ic_launcher.xml` (adaptive-icon structure), both `AndroidManifest.xml`,
`iosApp` `Contents.json` files, every HTML file (all icon filenames were preserved),
`composeApp/build.gradle.kts:211-218`, `docs/branding/dorai/{favicon.ico,favicon.svg}`
(verified byte-identical to the pack).

## 4. Verification actually run

| Check | Command | Result |
|---|---|---|
| Pack integrity + path safety | `python` `zipfile.testzip` / entry scan | **PASS** — clean, no unsafe paths |
| Pack copy parity | per-entry SHA-256 vs archive | **PASS** — 82/82, 0 mismatches |
| SVG well-formedness (6 files) | `xml.etree.ElementTree` parse | **PASS** — all parse, all carry the new `M118 60 H244` geometry |
| Raster dimensions / modes | Pillow | **PASS** — 180², 180², 180², 1024² RGB (iOS, alpha flattened), 1024² RGBA |
| ICO size matrices | Pillow `info['sizes']` | **PASS** — site/wasm `16/32/48`; desktop `16/24/32/48/64/128/256` |
| ICNS structure | byte parse (magic, declared vs actual length, element walk) | **PASS** — `icns`, 166,947 B declared = actual, 8 elements, ends exactly |
| PDF headers | first bytes | **PASS** — `%PDF-`, 128,738 B (iOS) / 128,714 B (desktop) |
| Adaptive-icon safe zone | geometric transform check | **PASS** — 297.9 units max dimension vs 312.9 safe zone; centred at 256,256 |
| Stale geometry sweep | `git grep -E 'M104[ ,](72|86)'` over sources | **PASS** — no matches in `composeApp`, `androidApp`, `site`, `iosApp` (matches exist only in gitignored `build/` output) |
| Android resource processing (`aapt2`) | `./gradlew :androidApp:processFossDebugResources` | **BUILD SUCCESSFUL** (36 s, 13 tasks executed) |
| Compose resource generation | `./gradlew :composeApp:generateComposeResClass` | **BUILD SUCCESSFUL**; generated `logo.xml` contains the new path data |
| Icon legibility, dark + light surfaces | contact sheet 16→180 px | **PASS** — D and forward core readable at 16 px; gradient intact; navy tile gives contrast on white |
| Legacy generators neutralised | run each with `python` | **PASS** — all five exit 1 with the retirement message |
| New importer still functional | `python tools/import_hexacore_assets.py` | **PASS** — exit 0, regenerates all derivatives incl. the store icon |

Visual artifact: `build/tmp/icon-contact-sheet.png` (gitignored build output).

## 5. Not run / blocked

| Item | Blocker |
|---|---|
| iOS build + Xcode asset-catalog validation | Requires macOS + Xcode; not available on this Windows machine |
| Loading `composeApp/icon.icns` through macOS `iconutil` | Same blocker; validated structurally only |
| Desktop installer build (`packageDmg`/`packageMsi`) | Not run — packaging icons were replaced but no package was produced |
| Wasm distribution build | Not run in this pass; only the web icon assets + `aapt2`/Compose resource generation were exercised |
| Full `:composeApp:assembleDebug` | Not run — `processFossDebugResources` already compiles the changed drawables through `aapt2` |
| Runtime screen inspection in the running app | Not performed; in-app mark verified by vector-resource compilation + source geometry, not by launching the UI |
| Browser render of `site/index.html` | Not performed; favicon/logo assets validated as files only |
| Google Play feature graphic | `fastlane/metadata/android/en-US/images/featureGraphic.png` — replaced in this migration: 1024×500, 24-bit RGB, D-shaped mark on the `#050A1C` field with the pack gradient and the tagline “Intelligence, in every direction.” Authored only from pack parts (no new shapes or colours); vector source `docs/branding/dorai/assets/store/feature-graphic.svg`, rasterised with headless Chrome per `assets/store/README.md`. |

None of the above is reported as passing.

## 6. Remaining risks

1. **Five legacy generators are now hard-retired (not deleted).** `tools/generate_dorai_{aurora,brand,neo_cyber}_assets.py` and `tools/sync_dorai_{aurora,brand}_assets.py` regenerated the pre-Hexa-Core mark; `sync_dorai_aurora_assets.py` and `generate_dorai_brand_assets.py` wrote it straight into `logo.xml`, `ic_launcher_foreground.xml`, `composeApp/icon.*`, the iOS icon, site images and the fastlane store icon. Each now exits non-zero with a pointer to the new importer unless `DORAI_ALLOW_LEGACY_BRAND_GENERATORS=1` is set. Residual risk: the override still permits a regression, and the scripts themselves remain readable history.
2. **In-app mark is flat, launcher is gradient.** Compose Multiplatform documents XML vectors as Android format “except no external resource references”; `aapt:attr` gradient behaviour inside multiplatform resources is undocumented, so `logo.xml` uses two solid palette colours instead of risking a parse failure or a black glyph on Desktop/Wasm/iOS. Android launcher and monochrome keep the pack’s true gradient. Flattening the launcher to match was considered and rejected: the pack is the source of truth and the launcher is the most brand-visible surface. Visual difference is confined to the small in-app glyph.
3. **No API-level constraint on the launcher gradient.** `gradle/libs.versions.toml` sets `android-minSdk = "26"`, so the `aapt:attr` gradient in the adaptive-icon foreground renders on every supported device. An earlier revision of this report claimed an API 24 floor; that was incorrect for this project (`minSdk` 26) and `minSdk` was not changed.
4. **iOS icon catalog has a single `universal`/`ios` 1024² slot.** The pack ships 1024² only; if iPad/Mac slots are added later, matching sizes must be exported from the pack.
5. **`docs/branding/dorai/assets/` still contains the superseded Nexus Orbit direction** (with `hexacore/` nested inside it, as the brief specified). Nothing in the app or site references it, but the directory layout is now slightly confusing.
6. **Stale `build/` output** still holds pre-migration copies of `logo.xml` / `ic_launcher_foreground.xml` until the next full build; these are gitignored and regenerate.

## 7. Before/after

| Surface | Before | After |
|---|---|---|
| In-app mark | D with flat triangle core, `M104,72 …` | Hexa Core D with forward-play core, `M112,56 …` |
| Android launcher | Old D, ~70 % of canvas (outside safe zone) | New D + core, 298/313 units (inside safe zone), true gradient on `#050A1C` |
| Android monochrome | D ring only | D ring + forward core silhouette |
| iOS app icon | Old mark, RGBA | New mark, opaque RGB 1024² |
| iOS launch logo | Old mark PDF | New symbol PDF, same filename |
| Site/Wasm favicon | Old ICO (site 4,154 B mono-size set) | New mark, 16/32/48 |
| Site logo / hero / wordmark | Old D with `aurora` gradient id | Asymmetric new D + approved horizontal wordmark |
| Desktop icons | Old mark (PNG/ICO/ICNS/SVG/PDF) | New mark, all five formats |

## 8. Refinement after adoption (v2 squircle)

After the pack was integrated and committed, a refinement was requested and applied.
A four-variant render comparison (current / balanced core / squircle / chevron) was
made with headless Chrome at 512 px, and the **squircle** variant was selected.

What changed in the geometry:

| Property | v1 (imported pack) | v2 (now) |
|---|---|---|
| Straight-edge stroke | 76 / 76 / 80 | **80 uniform** |
| Bowl / counter radii | 124 / 48 (not concentric) | **196 / 116, concentric at x=244** |
| Core | 100 wide, offset right, r=19 | **94 wide, optically centred, r=22** |
| Outer left corners | R=24 | **R=34** |
| Bounding box | x 84–438, y 56–456 (354 × 400) | x 84–440, y 60–452 (356 × 392) |
| Safe-zone fit (scale 0.76) | 296 / 312.9 units | **297.9 / 312.9 units** |

Regenerated from the new numbers: 8 pack SVGs, both wordmarks, 65 theme PNGs,
9 transparent-symbol PNGs, 4 favicon PNGs, `favicon.ico`, plus every derived asset
(iOS icon and launch PDF, desktop png/ico/icns/pdf, site and Wasm icons, touch icons,
Play listing icon). Android adaptive foreground now uses `android:scaleX/Y="0.76"`
with pivot (262, 256); the shared `logo.xml` carries the same paths with solid
`#25E2F5` / `#377CFF` fills. `MARK_SIGNATURE` in the importer is now `M118 60 H244`.

## Motion assets

`site/img/logo_animation.svg` was named for animation but shipped as a static byte copy of the pack
symbol. It is now a real animation, authored only from pack parts (mark path, core path, pack gradient):

| Beat | Timing | What moves |
|---|---|---|
| Reveal | 0–0.9 s, ease-out | A mask sweeps the mark in from its left edge |
| Core landing | 0.82–1.34 s, cubic-bezier(.34,1.56,.64,1) | The forward core pops from 42 % to 100 % with a small overshoot |
| Halo | 0.8–1.95 s | A cyan radial glow blooms and fades behind the core |
| Breathe | 1.6 s onward, 4.2 s loop | The core scales 1.00 → 1.045 and back |

`prefers-reduced-motion: reduce` disables every animation and leaves the static mark. Canonical
source: `docs/branding/dorai/assets/motion/logo-animation.svg` (copied to `site/img/logo_animation.svg`);
`assets/motion/logo-animation.gif` is a 400², 64-frame, 18 fps raster built from the same layers for
surfaces that cannot animate SVG. Verified in headless Chrome: 14.5 % canvas coverage at 150 ms versus
33.7 % settled, and the settled frame matches the static pack symbol at IoU 0.9977.

## Social card

`site/img/preview-banner.png` (and its byte-identical twin `site/img/og-preview.png`) still showed the
pre-migration mark, the retired hero illustration and the old purple accents, while `site/index.html`
advertises it as `og:image`, `twitter:image` and the JSON-LD `image`. Authoring it again from the new
mark also removes the last old-logo artwork from the deployed site.

The card keeps every string the site already publishes — the lockup, “Intelligence, in every
direction.”, “Your AI. Your World.”, the two-line description, the 01–05 feature chips, the
LOCAL / CLOUD / HYBRID pill and “ONE ASSISTANT. MANY POSSIBILITIES.” — and swaps only the artwork:
the old hero illustration and hexagon mark are replaced by the D mark on `#050A1C` with two faint
concentric rings and a soft radial glow.

Verified after rendering at 1200×630: the mark and the outer ring stay inside the canvas (right edge
1198), every chip label fits its box with at least 8 px of padding, the navy field has no white
pixels, and both output files are 24-bit RGB and byte-identical.

## Animated banner

`assets/motion/banner-animation.svg` is a 1200×630 staged animation of the social card: the field and
rings fade up, the mark sweeps in, the lockup and rule follow, then both taglines, the description, the
five chips (80 ms apart), the platform pill and the footer rise into place while the core pops and
starts breathing. `prefers-reduced-motion` shows the finished composition immediately.

| Beat | Start | Beat | Start |
|---|---|---|---|
| Field + rings | 0.00 s | Core pop + halo | 1.00 s |
| Mark reveal | 0.25 s | Chips (staggered) | 1.50 s |
| Lockup | 0.50 s | Pill | 1.95 s |
| Rule | 0.85 s | Footer | 2.08 s |
| Taglines | 1.00 / 1.12 s | Loop length | 3.20 s |

The settled frame was compared against the static social card: mean channel difference 0.94, with the
remaining delta coming from the breathing core and halo. `tools/render_banner_animation.py` rebuilds the
WebP from per-element layers; the motion README records why frame-stepping with `--virtual-time-budget`
does not work (Chrome's animation clock lags the budget by an inconsistent 0.3–0.8 s and occasionally
captures a blank page) and why WebP replaced GIF for the banner.

Known gap: nothing links the banner into the site yet, so its only consumers today are people who open
the files directly.

## Additional motion assets

Three more animations complete the set, all built from pack parts and all guarded by
`prefers-reduced-motion`:

| Asset | Size | Behaviour |
|---|---|---|
| `loader-animation.svg` | 128² | Seamless 1.8 s loop — the core breathes while two rings expand outward and fade |
| `wordmark-animation.svg` | 920×240 | Mark settles, DORAI and AI rise, underline draws, then holds |
| `splash-animation.svg` | 512² | One-shot app splash: tile fade, mark draw, core landing with halo, then settle |

The splash settles to an exact match with the static icon (`svg/app-icon-dark.svg`, mask IoU 1.0000),
so a launch screen ends on precisely the shipped artwork. Two defects were found and fixed while
building them: an animated group can lose its placement because a CSS `transform` overrides the SVG
`transform` attribute (this bit the wordmark mark), and the pack wordmark's `#08132B` DORAI is
invisible on dark surfaces, so the animated lockup uses `#F5F8FC`.
