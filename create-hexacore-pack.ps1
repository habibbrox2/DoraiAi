$ErrorActionPreference = 'Stop'
$project = 'G:\Web\DoraiAi-migration-worktree'
$target = Join-Path $project 'docs\branding\dorai\hexacore-d-shape-pack'
$svgDir = Join-Path $target 'svg'
New-Item -ItemType Directory -Force -Path $svgDir | Out-Null
$outer = 'M112 56 H250 C365 56 438 136 438 256 C438 376 365 456 250 456 H112 C96 456 84 444 84 428 V84 C84 68 96 56 112 56 Z'
$inner = 'M164 132 V380 H250 C318 380 356 332 356 256 C356 180 318 132 250 132 Z'
$play = 'M226 190 C226 178 239 171 250 178 L326 222 C342 231 342 281 326 290 L250 334 C239 341 226 334 226 322 Z'
$defs = '<defs><linearGradient id="dgrad" x1="82" y1="70" x2="438" y2="450" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset=".48" stop-color="#377CFF"/><stop offset="1" stop-color="#6F5BFF"/></linearGradient><linearGradient id="pgrad" x1="220" y1="180" x2="340" y2="330" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#25E2F5"/><stop offset="1" stop-color="#377CFF"/></linearGradient></defs>'
function SvgDoc($body, $title) {
  $head = '<?xml version="1.0" encoding="UTF-8"?>' + [Environment]::NewLine
  $open = '<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512" role="img" aria-labelledby="title desc">'
  $desc = '<desc id="desc">Geometric D-shaped DORAI AI symbol with central forward core.</desc>'
  return $head + $open + '<title id="title">' + $title + '</title>' + $desc + $body + '</svg>'
}
$shapeGradient = '<path d="' + $outer + ' ' + $inner + '" fill="url(#dgrad)" fill-rule="evenodd"/><path d="' + $play + '" fill="url(#pgrad)"/>'
$shapeBlack = '<path d="' + $outer + ' ' + $inner + '" fill="#050A1C" fill-rule="evenodd"/><path d="' + $play + '" fill="#050A1C"/>'
$shapeWhite = '<path d="' + $outer + ' ' + $inner + '" fill="#FFFFFF" fill-rule="evenodd"/><path d="' + $play + '" fill="#FFFFFF"/>'
$files = @{}
$files['d-shape-gradient.svg'] = SvgDoc ($defs + $shapeGradient) 'DORAI AI D-Shape Gradient'
$files['d-shape-black.svg'] = SvgDoc $shapeBlack 'DORAI AI D-Shape Black'
$files['d-shape-white.svg'] = SvgDoc $shapeWhite 'DORAI AI D-Shape White'
$files['app-icon-dark.svg'] = SvgDoc ('<rect width="512" height="512" rx="104" fill="#050A1C"/>' + $defs + $shapeGradient) 'DORAI AI D-Shape Dark App Icon'
$files['app-icon-light.svg'] = SvgDoc ('<rect width="512" height="512" rx="104" fill="#FFFFFF"/>' + $defs + $shapeGradient) 'DORAI AI D-Shape Light App Icon'
$files['app-icon-mono-dark.svg'] = SvgDoc ('<rect width="512" height="512" rx="104" fill="#050A1C"/>' + $shapeWhite) 'DORAI AI D-Shape White on Navy'
$files['app-icon-mono-light.svg'] = SvgDoc ('<rect width="512" height="512" rx="104" fill="#FFFFFF"/>' + $shapeBlack) 'DORAI AI D-Shape Navy on White'
foreach ($name in $files.Keys) {
  [System.IO.File]::WriteAllText((Join-Path $svgDir $name), $files[$name], [System.Text.UTF8Encoding]::new($false))
}
[System.IO.File]::WriteAllText((Join-Path $target 'favicon.svg'), $files['d-shape-gradient.svg'], [System.Text.UTF8Encoding]::new($false))
$readme = @'
# DORAI AI — Hexa Core D-Shape Logo Pack

This is the D-shaped refinement of the selected Hexa Core direction.

## Palette
- Neon Cyan: #25E2F5
- Electric Blue: #377CFF
- Violet: #6F5BFF
- Dark Navy: #050A1C
- Light: #FFFFFF

## SVG files
- svg/d-shape-gradient.svg — transparent gradient symbol
- svg/d-shape-black.svg — black monochrome symbol
- svg/d-shape-white.svg — white monochrome symbol
- svg/app-icon-dark.svg — dark app icon
- svg/app-icon-light.svg — light app icon
- svg/app-icon-mono-dark.svg — white symbol on navy
- svg/app-icon-mono-light.svg — navy symbol on white
- favicon.svg — scalable browser favicon

All SVGs use a 512x512 viewBox and remain resolution-independent.
For Android adaptive icons, split foreground/background according to the app's existing resource configuration and validate the platform safe zone before shipping.
This pack is stored separately and does not replace active application assets.
'@
[System.IO.File]::WriteAllText((Join-Path $target 'README.md'), $readme, [System.Text.UTF8Encoding]::new($false))
$zip = Join-Path (Split-Path $target -Parent) 'DORAI_AI_Hexa_Core_Logo_Pack.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $target '*') -DestinationPath $zip -CompressionLevel Optimal
Get-Item $zip | Select-Object FullName,Length,LastWriteTime | Format-List
