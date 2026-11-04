# DORAI AI — Hexa Core Logo Replacement Agent Prompt

## Mission
Implement the approved DORAI AI Hexa Core identity throughout this repository. Replace active references to outdated logos with the approved D-shaped Hexa Core assets, preserve recoverable backups, and verify the result across supported platforms.

## Project boundary
- Repository: G:\Web\DoraiAi-migration-worktree
- Branding directory: docs\branding\dorai
- The approved full logo ZIP has been downloaded to this Office PC by the user. **Locate it first**; check the project branding directory and the current Windows user's Downloads folder. Search for likely names such as DORAI_HexaCore_Full_LogoPack.zip and DORAI_AI_Hexa_Core_Logo_Pack.zip.
- Do not assume a ZIP path or overwrite an existing archive. Inspect candidates and identify the full pack by its contents (SVG, PNG, favicon/ICO, manifest/readme as available). The older small SVG-only ZIP is not the full pack.
- If the full pack cannot be found, STOP before replacing assets and report the exact locations checked.

## Approved brand direction
- Mark: Concept 4 — Hexa Core, refined into a clearly recognizable D-shaped mark.
- Tagline: “Your AI. Your World.”
- Aurora colors: Neon Cyan #25E2F5, Electric Blue #377CFF, Violet #6F5BFF.
- Dark surfaces: #050A1C, #08132B, #0D1C38.
- Light surfaces: #F5F9FF, #FFFFFF.
- Use supplied pack assets as the source of truth; do not redraw or approximate the logo unless a required variant is genuinely missing.## Mandatory safety rules
1. Inspect repository instructions (AGENTS.md, README, package/build docs) and current git/worktree state before changes. The .git metadata may point to an external worktree; if git commands fail, report that and do not attempt repair.
2. Audit all logo files, app icons, favicons, splash assets, metadata, and code references before editing.
3. Create a timestamped backup under docs\branding\dorai\_legacy-logo-backup\<timestamp>\, preserving original relative paths. Back up every asset before replacing or removing it.
4. Never use broad destructive commands (for example, recursive deletion of image files). Do not delete unrelated illustrations, user uploads, screenshots, or third-party logos.
5. Do not touch secrets, .env files, credentials, signing keys, certificates, or unrelated application logic.
6. Do not commit, push, publish, or deploy. Do not claim a build/test passed unless its command completed successfully.
7. If asset intent or platform mapping is uncertain, preserve the file and document the ambiguity.

## Phase 1 — Locate and validate the pack
- Locate the full ZIP as described above; record its exact path, size, and SHA-256.
- Inspect the archive listing before extraction. Reject corrupt, suspicious, or unexpected archive paths (path traversal).
- Extract to a temporary staging directory, not directly over live assets.
- Inventory every supplied file and variant. Validate SVG XML and image dimensions/formats; identify dark/light/mono variants and app icon/favicons.
- Copy the validated pack into docs\branding\dorai\assets\hexacore\ (preserve filenames and structure). Keep the original ZIP in docs\branding\dorai\ as a source archive only if it is not already there; never replace a different archive without backup.
- Produce docs\branding\dorai\LOGO_REPLACEMENT_AUDIT.md with old assets, references, proposed replacements, and platform mapping before making replacements.## Phase 2 — Integrate
- Inspect the actual project structure and determine which platforms are present: shared Compose Multiplatform, Android, iOS, Desktop, Web, and any marketing/site frontend.
- Map supplied assets to each platform's correct resource conventions and density/size requirements. Use vector assets where supported; generate raster derivatives only from the approved source and only where required.
- Update every active logo reference (app branding, navigation/header, login/onboarding, splash screen, launcher icon, web favicon, metadata, manifests, and documentation examples where appropriate).
- Select dark/light/monochrome variants according to the actual background and contrast requirements. Preserve clear space, aspect ratio, and legibility; do not stretch, recolor arbitrarily, or add effects that alter the mark.
- Search again for old logo filenames, old brand marks, and references. Move superseded assets out of active resource paths only after confirming no active references remain and a backup exists.
- Do not remove assets whose purpose is uncertain. Record them in the audit report.

## Phase 3 — Verify
- Run available format/XML validation and platform-specific resource checks.
- Run relevant build, lint, and tests for each platform that can be built in the current environment.
- Inspect key screens and icon previews at practical sizes and on both dark and light backgrounds where supported.
- Check for missing resources, broken paths, duplicate/obsolete active logos, incorrect scaling, clipping, and poor contrast.
- If a platform cannot be built because of missing SDKs, signing configuration, network access, or environment limitations, state the exact blocker; do not fabricate success.

## Required deliverables
1. Updated logo assets and references across supported platforms.
2. Timestamped backup of every superseded asset.
3. docs\branding\dorai\LOGO_REPLACEMENT_AUDIT.md
4. docs\branding\dorai\LOGO_IMPLEMENTATION_REPORT.md — list changed files, old-to-new mapping, backup location, commands run, build/test outcomes, and any remaining risks.
5. docs\branding\dorai\LOGO_ASSET_USAGE_MAP.md — each supplied asset, its purpose, and where it is used.

## Completion criteria
Declare completion only when the full pack was located and validated, backups exist, supported platform references were updated, verification was run, and reports are written. Clearly separate completed work from blocked or unverified work.