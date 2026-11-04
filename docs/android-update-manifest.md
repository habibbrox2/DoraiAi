# Android update manifest (schema v1)

The canonical `update.json` is published at the repository root on `main` and attached to the matching stable GitHub Release beside the universal FOSS APK.

## Canonical location and compatibility

- Primary discovery URL: `https://raw.githubusercontent.com/habibbrox2/DoraiAi/main/update.json`.
- The release workflow publishes the manifest only after the corresponding GitHub Release and APK asset are created.
- For older releases created before the canonical file existed, the Android client falls back to the matching stable GitHub Release's `update.json` asset only when the canonical URL returns HTTP 404.
- Other HTTP failures, malformed JSON, and invalid manifests fail closed; they must not silently fall back to stale metadata.
- Publishing is monotonic by `versionCode`: an older release workflow must never replace a newer canonical manifest.

## Contract

| Field | Meaning |
|---|---|
| `schemaVersion` | Manifest format version; currently `1`. |
| `packageName` | Must equal `com.dorai.ai`. |
| `versionName` | Human-readable release version, e.g. `2.0.75`. |
| `versionCode` | Authoritative Android upgrade ordering value. |
| `assetName` | Exact universal APK filename attached to this release. |
| `downloadUrl` | HTTPS URL for that exact APK asset. |
| `sha256` | Lowercase SHA-256 digest of the APK bytes. |
| `signerCertificateSha256` | Lowercase SHA-256 digest of the APK signing certificate. |

## Client requirements

- Compare `versionCode` with the installed package version code. Never use `versionName` for upgrade ordering.
- Reject an unexpected schema version, package name, asset name, URL origin, malformed digest, or non-increasing version code.
- Verify APK SHA-256 and signing certificate before invoking Android's package installer.
- Treat the manifest as untrusted remote input. HTTPS alone is not an integrity or signer-trust guarantee.
- The installed signing certificate remains the trust anchor; never trust a signer fingerprint solely because it is supplied by the remote manifest.
