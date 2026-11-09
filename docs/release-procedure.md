# DORAI Production Release Procedure

## Overview

DORAI AI production releases are **manual-only**. A normal push or merge to `main` must never create a version bump, Git tag, or production GitHub Release.

The only way to produce a production release is to run the canonical GitHub Actions workflow:

- **Workflow file:** `.github/workflows/release.yml`
- **Workflow name:** `DORAI Production Release`
- **Trigger:** `workflow_dispatch` (manual run from the GitHub Actions tab)
- **Allowed branch:** `main` only

## Prerequisites

Before running a production release, ensure:

1. You are on the `main` branch and your local checkout is up to date with `origin/main`.
2. The repository secrets are configured:
   - `KEYSTORE_B64`
   - `KEY_ALIAS`
   - `KEYSTORE_PASSWORD`
3. For the **optional** Play Store lane (AAB → production track), additionally configure the repository **variable** `DORAI_PLAY_STORE_ENABLED=true` (the job is skipped otherwise) and repository **secrets** `PLAY_STORE_KEYSTORE_B64`, `PLAY_STORE_KEYSTORE_PASSWORD`, `PLAY_STORE_KEY_ALIAS`, `PLAY_STORE_KEY_JSON` (base64-encoded service-account JSON). The Play Store job (`play-store.yml`) re-validates the requested `ref` as an existing `vX.Y.Z` tag that is reachable from `origin/main` before building, requires `DORAI_PLAY_STORE_ENABLED=true`, and deletes the decoded credentials after the job.
4. `gradle/libs.versions.toml` contains a valid `appVersion` and `android-versionCode` that match the latest strict semantic-version Git tag (`vMAJOR.MINOR.PATCH`). If they are behind, the workflow auto-bumps them via a `chore(release)` PR + auto-merge and waits for the merge before any build runs.
5. The target version/tag does not already exist.

## How to run a production release

### Step 1 — Open GitHub Actions

1. Go to https://github.com/habibbrox2/DoraiAi/actions
2. In the left sidebar, select **DORAI Production Release**.
3. Click the **Run workflow** dropdown button on the right.

### Step 2 — Select the release type

Choose one of the following values for the `release_type` input:

| Input | Meaning | Example |
|-------|---------|---------|
| `patch` | Bug-fix / patch release | `2.1.9` → `2.1.10` |
| `minor` | Feature / minor release | `2.1.9` → `2.2.0` |
| `major` | Breaking / major release | `2.1.9` → `3.0.0` |

Default: `patch`.

### Step 3 — Run the workflow

Click **Run workflow**. The pipeline executes five phases automatically:

| Phase | Job | What happens |
|-------|-----|-------------|
| A — Preflight & Auto-Bump | `preflight` | Validates the `workflow_dispatch` event, the `main` branch, `origin/main` HEAD, the latest tag, and signing secrets; if the catalog version is behind, auto-bumps `appVersion`/`android-versionCode` via a `chore(release)` PR + auto-merge and **waits for the merge** so downstream builds read the bumped values. The decoded keystore is deleted immediately after validation. |
| B — Build All | `apk`, `android-standard`, `windows`, `macos`, `linux-deb`, `linux-rpm`, `linux-tarball`, `flatpak`, `appimage`, `play-store` | Builds all release targets: signed Android FOSS APK, Windows MSI + ZIP, macOS DMG, Linux DEB + RPM + AppImage + Flatpak + tarball. Optional Play Store AAB if `DORAI_PLAY_STORE_ENABLED=true`. The signing keystore is removed (`if: always()`) when each Android job finishes. |
| C — Signing & Metadata | (inside `apk`) | Verifies the APK signature via `apksigner`, extracts certificate SHA-256 and APK SHA-256, generates `update.json` with the full schema. |
| D — Tag & Publish Release | `release` | Runs only after the mandatory builds pass: creates and pushes the immutable `vX.Y.Z` tag on `origin/main` (never before), creates exactly one GitHub Release with verified artifacts, generates `SHA256SUMS`, and publishes/verifies the canonical root-level `update.json` on `main`. |

If any mandatory job fails, no partial production release is published.

## What gets published

For each successful release, the following assets are attached to the GitHub Release:

| Asset | Pattern |
|-------|---------|
| Android FOSS APK | `DORAI-AI-vX.Y.Z-Android.apk` |
| Windows MSI | `DORAI-AI-vX.Y.Z-windows.msi` |
| Windows ZIP | `DORAI-AI-vX.Y.Z-Windows-x64.zip` |
| macOS DMG | `DORAI-AI-vX.Y.Z-macos.dmg` |
| Linux DEB | `DORAI-AI-vX.Y.Z-linux.deb` |
| Linux RPM | `DORAI-AI-vX.Y.Z-linux.rpm` |
| Linux AppImage | `DORAI-AI-vX.Y.Z-linux.AppImage` |
| Linux Flatpak | `DORAI-AI-vX.Y.Z-linux.flatpak` |
| Linux tarball | `DORAI-AI-vX.Y.Z-linux-x86_64.tar.gz` |
| Checksums | `SHA256SUMS` |
| Update manifest | `update.json` (also published to root of `main`) |

## Version and Android versionCode generation

- **Semantic version:** Calculated from the latest `vMAJOR.MINOR.PATCH` tag using the selected `release_type` (patch/minor/major), confirmed by the `bump` step in the preflight job.
- **Version metadata bump:** If the catalog is behind the computed target, `appVersion`/`android-versionCode` are bumped through an automated `chore(release)` PR that is auto-merged (protected `main`); the workflow waits for the auto-merge before downstream builds run.
- **Android `versionCode`:** Incremented by exactly one from the current value in `gradle/libs.versions.toml`.
- **Tag:** The immutable `vX.Y.Z` tag is created on `origin/main` **only after** the mandatory builds pass (in the `release` job), not before builds.

## Investigating failed release jobs

1. Open the failed workflow run at https://github.com/habibbrox2/DoraiAi/actions
2. Click the failed job name to view its logs.
3. Common failure points:
   - **Preflight:** Missing signing secrets, `appVersion` mismatch, existing tag, or non-`main` branch.
   - **Build:** Gradle compilation or packaging failure for a specific platform.
   - **Signing:** `apksigner` verification failure or invalid keystore/alias.
   - **Publish:** Artifact missing, checksum mismatch, or `update.json` validation failure.

## Verifying the Android update manifest

After a successful release:

1. Fetch the canonical manifest: `https://raw.githubusercontent.com/habibbrox2/DoraiAi/main/update.json`
2. Verify these fields match the released APK:
   - `schemaVersion` == `1`
   - `packageName` == `com.dorai.ai`
   - `versionName` == released version (e.g. `2.1.10`)
   - `versionCode` == released Android versionCode
   - `assetName` == `DORAI-AI-vX.Y.Z-Android.apk`
   - `downloadUrl` points to the GitHub Release asset URL
   - `sha256` matches the APK SHA-256
   - `signerCertificateSha256` matches the APK signing certificate digest

## Credential hygiene on the runner

- The decoded release keystore is deleted immediately after preflight validation and again (as a safety net, `if: always()`) after the `apk` and `android-standard` jobs finish — a production signing key should not linger on the runner for the remaining jobs.
- The Play Store job (`play-store.yml`) validates the `ref` input as a strict `vX.Y.Z` tag that exists and is reachable from `origin/main` before building, decodes the Play keystore and service-account key into temporary files, and removes both on an `if: always()` cleanup step — secrets are passed between steps via `env:`, never inlined into shell.
- Known limitation: `keytool` requires the keystore password via `-storepass` argv (it has no environment-variable or stdin option); the value is GitHub-masked in logs and the runner is ephemeral.

## What must NOT happen

- Do not push or merge to `main` expecting an automatic release — it will not happen.
- Do not create or push Git tags manually for production releases — the workflow handles tagging.
- Do not force-update or overwrite existing tags.
- Do not publish a GitHub Release outside this workflow.
