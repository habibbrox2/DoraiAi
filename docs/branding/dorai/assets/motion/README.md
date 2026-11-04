# Motion assets

Everything here is authored from pack parts only: the D mark paths, the forward-core path, the
`#25E2F5 → #377CFF → #6F5BFF` gradient, the `#050A1C` field and the copy the site already publishes.
The pack ships no animation. Do not add shapes, colours or typefaces without updating
`../../LOGO_REPLACEMENT_AUDIT.md` first.

| Asset | Size | What it is |
|---|---|---|
| `logo-animation.svg` | 512² | The mark alone: 0.9 s left-to-right reveal, core pop-in with halo, 4.2 s breathe loop |
| `logo-animation.gif` | 400², 18 fps | Raster of the above for surfaces that cannot animate SVG |
| `banner-animation.svg` | 1200×630 | Staged banner: glow, mark, lockup, rule, taglines, description, chips, pill, footer |
| `banner-animation.webp` | 1200×630, 10 fps | Raster of the banner (WebP, not GIF — see below) |
| `loader-animation.svg` | 128² | Seamless 1.8 s loop: two rings expand from the pulsing core; static single ring under reduced motion |
| `wordmark-animation.svg` | 920×240 | Mark settles, DORAI and AI rise, underline draws, then holds |
| `splash-animation.svg` | 512² | One-shot app splash on the icon tile: fade, mark draw, core landing with halo, then settles |

`site/img/logo_animation.svg` is a byte copy of `logo-animation.svg`; edit the canonical file and copy,
never the reverse. The banner is not wired into the site yet — it is available for social, docs and
store use.

## Accessibility

Both SVGs carry `@media (prefers-reduced-motion: reduce)` and render the finished static composition
when motion is suppressed. Nothing is conveyed by motion alone.

## Timing

| Beat | Starts | Duration | Effect |
|---|---|---|---|
| Field + rings | 0.00 s | 0.70 s | Fade up |
| Mark reveal | 0.25 s | 0.85 s | Mask sweeps from the left edge |
| Lockup | 0.50 s | 0.70 s | Rise 16 px, fade in |
| Rule | 0.85 s | 0.75 s | Grows from the left |
| Taglines | 1.00 / 1.12 s | 0.70 s | Rise + fade |
| Description | 1.28 s | 0.70 s | Rise + fade (both lines together) |
| Core | 1.00 s | 0.52 s | Pops 42 % → 107 % → 100 %, then breathes every 4.2 s |
| Halo | 1.00 s | 1.05 s | Cyan bloom behind the core |
| Chips | 1.50 s + 0.08 s each | 0.70 s | Rise + fade, staggered |
| Pill / footer | 1.95 / 2.08 s | 0.70 s | Rise + fade |
| Loop | 3.20 s | — | 0.2 s dissolve, then restart |

Smaller assets:

| Asset | Timeline |
|---|---|
| `loader-animation.svg` | Core breathes on 1.8 s; ring 1 from 0 s and ring 2 from 0.9 s, each scaling 0.24 → 1.0 while fading out, so the loop is seamless |
| `wordmark-animation.svg` | Mark settles 0 → 0.8 s, DORAI at 0.45 s, AI at 0.58 s, underline draws 0.95 → 1.65 s |
| `splash-animation.svg` | Tile fades 0 → 0.6 s, mark draws 0.3 → 1.15 s, core lands 0.95 s with a halo to 1.95 s, then the core breathes |

## Building the banner raster

```bash
python tools/render_banner_animation.py --frames
```

The script rasterises each element once as a transparent layer with headless Chrome and composites the
timeline above in Pillow. Do **not** try to step the CSS animation with
`--virtual-time-budget` launch per frame: Chrome's animation clock runs behind the virtual-time budget
by an inconsistent offset (observed between 0.3 s and 0.8 s), so frames land at the wrong moment, and
roughly one launch in twenty returns a blank page.

Two flags remain essential for any headless Chrome rasterisation here:

- `--user-data-dir` must be **unique per launch**; concurrent runs otherwise block on the default
  profile lock and hang until the command times out.
- `--screenshot` must be an **absolute** path; a relative path silently hangs the launch.

`build/tmp/banner-frames/` holds the PNG sequence for inspection (git-ignored).

## Why WebP instead of GIF for the banner

A 1200×630 GIF needs 128–256 colours and shows diagonal banding across the gradient (peak channel
error 181/255 at 256 colours). Floyd–Steinberg dithering hides the banding but pushes the file to
~800 KB. The animated WebP is 356 KB at quality 90 with a mean channel error of 1.06, and every current
browser, GitHub and the major chat clients display it.

If a GIF is genuinely required, render 900×473 with 256 colours and no dither, and accept the banding.

## Gotchas worth remembering

**A CSS `transform` animation overrides the SVG `transform` attribute.** Every animated group here keeps
its placement on an untransformed wrapper for this reason. Skipping it made the banner's play triangle
jump to raw 512-space coordinates and the wordmark's mark render at full size, cropped, over the text.

**The pack wordmark paints `DORAI` in `#08132B`**, which is invisible on a dark surface. The animated
lockup uses `#F5F8FC` instead; the pack file itself is unchanged.

**Paint servers that do not resolve render as a dark band, not as nothing.** The splash originally had a
rotated light sweep whose gradient silently failed, leaving a black stripe across the tile; it was
removed rather than debugged, because the reveal and core landing already carry the animation.
