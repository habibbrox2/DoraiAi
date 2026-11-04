> **Canonical pack:** `docs/branding/dorai/assets/hexacore/`. Platform assets are derived from
> that directory by `tools/import_hexacore_assets.py`; loose copies at this level were removed.

# DORAI AI — D-Shape Logo Pack

The selected Hexa Core direction has been refined into a clearer capital-D silhouette with a forward/play core.

## Colors
- Neon Cyan #25E2F5
- Electric Blue #377CFF
- Violet #6F5BFF
- Dark Navy #050A1C
- Light #FFFFFF

## Main SVGs
- `svg/d-shape-gradient.svg` — transparent gradient symbol
- `svg/d-shape-black.svg` — black monochrome symbol
- `svg/d-shape-white.svg` — white monochrome symbol
- `svg/app-icon-dark.svg` and `svg/app-icon-light.svg`
- `svg/app-icon-monochrome-dark.svg` and `svg/app-icon-monochrome-light.svg`
- Optional wordmarks: `svg/wordmark-horizontal.svg`, `svg/wordmark-vertical.svg`

## Raster
- `png/app-icons/` — theme and monochrome PNGs from 32px through 1024px
- `png/favicon/` — favicon PNGs
- `favicon.ico` and `favicon.svg`
- `site.webmanifest`

## Integration caution
This is a logo asset pack, not an automatic project migration. Inspect the target app's Android adaptive icon configuration and desktop packaging before integrating. Android adaptive icons may require separately generated foreground/background layers and platform safe-zone validation.
