# DORAI Identity Migration

## Canonical application identity

- Product name: DORAI AI
- Android application ID: `com.dorai.ai`
- Android application namespace: `com.dorai.ai`
- Shared Kotlin namespace: `com.dorai.ai.shared`
- iOS bundle ID: `com.dorai.ai`
- iOS shared framework bundle ID: `com.dorai.ai.composeapp`
- Gradle root project: `dorai`
- SQLDelight database class: `DoraIDatabase`
- Local SQL database file: `dorai.db`
- Desktop distribution name: `DORAI`
- Flatpak app ID: `com.dorai.ai`

## New-app installation model

DORAI is a separate application, not an in-place rename of the legacy Kai installation. Android and iOS use new bundle/application identifiers. The old app can remain installed independently. No silent transfer of private app data is attempted. If user data import is added later, it must be an explicit, user-initiated, validated export/import flow.

## Updater safety

The DORAI updater accepts only releases carrying DORAI-prefixed artifacts and a manifest whose package name is `com.dorai.ai`. Legacy releases without a DORAI APK are ignored. The stale pre-migration root `update.json` was removed because its checksum and signer fingerprint described a different application artifact. The first signed DORAI release must generate and publish a fresh manifest from the built APK; never reuse legacy APK checksums or signing fingerprints.

## External dependencies that require separate provisioning

The GitHub repository has been renamed to `habibbrox2/DoraiAi`; the local working folder remains `G:\Web\broxkai2.0` until a separate local rename.

New Google Play and App Store listings must be registered for `com.dorai.ai`; legacy Kai store URLs are upstream references only. WinGet publishing is disabled until a DORAI package ID is registered. The legacy `kai-bin` AUR package and auto-publisher were removed; create a new `dorai-bin` package only after the first DORAI release provides a verifiable checksum. Do not publish DORAI installers under legacy package-manager identifiers.

The legacy service endpoint `api.kai9000.com` and website hostname `kai9000.com` are external infrastructure references. They must not be replaced with guessed DORAI domains or endpoints. Before retiring them, provision and verify DORAI-owned DNS, TLS, API routing, website hosting, and release hosting, then update the app and website configuration together.

Upstream attribution, copyright notices, and license references must remain intact. They identify the original upstream project and are not DORAI product identity.

## Release gate

Do not publish a DORAI release until:
- Android APK package name is verified as `com.dorai.ai`.
- Release signing credentials and signer fingerprint are provisioned for the new app.
- The generated update manifest matches the APK package, version, version code, asset name, SHA-256, and signer certificate.
- Android, Desktop and iOS builds pass their applicable checks. Web/Wasm remains excluded from distribution until its compatibility backlog is resolved.
- Website and DNS ownership for any new DORAI domain is verified before changing production endpoints.

## GitHub release-history and first-release gate

The renamed GitHub repository still retains legacy Kai release history. The latest published release is `Brox Kai v2.1.3`; legacy tags `v2.1.4` and `v2.1.5` also already exist. No DORAI release has been published. Do not direct users to the Releases page as a DORAI download source until a DORAI release exists; the current public copy explicitly marks the first release as pending.

The current source metadata still says `appVersion = 2.1.4` and `android-versionCode = 216`. The `v2.1.4` tag is already occupied by a legacy release, so it cannot be reused for DORAI. Before release, select a new unused semantic version and matching version code, then update `gradle/libs.versions.toml`. The release workflow now rejects a tag that does not match `appVersion`, and it requires the tag commit to already be merged into `main` before it can publish `update.json` there.

Google Play publishing is disabled unless the repository variable `DORAI_PLAY_STORE_ENABLED=true` is deliberately configured after the DORAI listing and credentials are ready. GitHub Pages deployment is manual-only and requires `DORAI_SITE_DOMAIN` to match `site/CNAME`, plus `DORAI_WEB_WASM_ENABLED=true` after Web/Wasm support is release-ready. No release, merge, push, or deployment is authorized by this migration work.
