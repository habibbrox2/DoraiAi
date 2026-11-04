# P0-A Implementation Plan — DORAI AI

## Overview
P0-A addresses critical security findings identified in the read-only audit.
All changes are security hardening — no feature changes, no branding changes.

## Phase 1: Backup (Completed)
- Backup directory: `backup-pre-p0-20261019-014057/`
- Recovery branch: `backup/pre-p0-20261019-014057`
- Working tree was clean before backup; no file modifications to preserve

## Phase 2: Implementation (Completed)

### 1. Android Network Security Config
**File:** `androidApp/src/main/res/xml/network_security_config.xml`
**Change:** Restricted cleartext HTTP to localhost only (`localhost`, `127.0.0.1`, `10.0.2.2`). Removed global cleartext allowance and user certificate trust.
**Impact:** Cloud provider traffic (all HTTPS) unaffected. Self-hosted servers on LAN IPs require HTTPS or tunnel.
**Risk:** Medium — users with LAN self-hosted servers may need to reconfigure.

### 2. Android Backup Disable
**File:** `androidApp/src/main/AndroidManifest.xml`
**Change:** `allowBackup="true"` → `allowBackup="false"`
**Impact:** No ADB/cloud backup of app data. In-app export/import provides migration.
**Risk:** Low — export/import already tested by `AppSettingsExportImportTest`.

### 3. Backup Rules Tightening
**Files:** `backup_rules.xml`, `data_extraction_rules.xml`
**Change:** Added exclusions for conversation DB, WAL/journal/SHM files, and encrypted settings files.
**Impact:** Even if backup is re-enabled, sensitive data is excluded.
**Risk:** Low.

### 4. MCP SSRF Protection
**Files:** `McpUrlValidator.kt` (new), `McpClient.kt`, `McpServerManager.kt`
**Changes:**
- New `McpUrlValidator` object with scheme validation, IP-range filtering, header sanitization, DNS resolution
- `McpClient` constructor validates URL and headers (fail fast)
- `McpClient.sendRequest()` re-validates URL before each HTTP call (connection-time)
- `followRedirects = false` — manual redirect handling with per-hop validation (max 3)
- `isLenient = true` → `isLenient = false` (JSON parsing hardening)
- `McpServerManager.addServer()` validates URL/headers before adding
- `McpServerManager.connectAndDiscoverTools()` validates URL before connecting
- Platform `resolveHostAddresses()` expect/actual: JVM (Android + Desktop) uses `InetAddress`; iOS/Web return empty list (fail closed)

**Limitations (documented, not guarantees):**
- DNS rebinding TOCTOU window exists
- Proxy bypass possible
- No connection-time IP pinning (Ktor limitation)
- iOS/Web fail closed (all URLs rejected)

### 5. Release Artifact Checksums
**File:** `.github/workflows/release.yml`
**Change:** Added `Generate SHA-256 checksums` and `Verify checksums` steps. Updated release files to include `SHA256SUMS`.
**Impact:** Published releases now include SHA256SUMS for integrity verification. Unsigned (integrity only, not authentication).
**Risk:** Low — fail-safe with `set -euo pipefail` and explicit artifact count check.

### 6. Security Documentation
**File:** `SECURITY.md`
**Change:** Added "Protection Boundaries" section documenting actual security posture, limitations, and planned improvements.

### 7. Unit Tests (Proposal)
**File:** `McpUrlValidatorTest.kt` (new)
**Change:** 28 test cases covering scheme, credential, encoding, IP range, DNS, redirect, header, and platform edge cases.

## Phase 3: Static Verification (Completed)

- ✅ XML syntax verified (network_security_config.xml, backup_rules.xml, data_extraction_rules.xml)
- ✅ Kotlin syntax verified (McpUrlValidator.kt, McpClient.kt, McpServerManager.kt)
- ✅ Platform expect/actual consistency verified (Android, Desktop, iOS, Web)
- ✅ YAML syntax verified (release.yml checksum steps)
- ✅ No JVM-only APIs in commonMain (DNS resolution abstracted via expect/actual)
- ✅ McpUrlValidatorTest.kt compiles (pure Kotlin, no I/O, no network)

## Files Changed

### Modified
1. `androidApp/src/main/res/xml/network_security_config.xml`
2. `androidApp/src/main/AndroidManifest.xml`
3. `androidApp/src/main/res/xml/backup_rules.xml`
4. `androidApp/src/main/res/xml/data_extraction_rules.xml`
5. `composeApp/src/commonMain/kotlin/com/dorai/ai/mcp/McpClient.kt`
6. `composeApp/src/commonMain/kotlin/com/dorai/ai/mcp/McpServerManager.kt`
7. `composeApp/src/androidMain/kotlin/com/dorai/ai/Platform.android.kt`
8. `composeApp/src/desktopMain/kotlin/com/dorai/ai/Platform.jvm.kt`
9. `composeApp/src/iosMain/kotlin/com/dorai/ai/Platform.ios.kt`
10. `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/Platform.wasmJs.kt`
11. `.github/workflows/release.yml`
12. `SECURITY.md`

### Created
13. `composeApp/src/commonMain/kotlin/com/dorai/ai/mcp/McpUrlValidator.kt`
14. `composeApp/src/commonTest/kotlin/com/dorai/ai/mcp/McpUrlValidatorTest.kt`

## Not In Scope (P0-B, Separate Approval)
- `ShellCommandTool.kt` — shell hardening (fail-closed allowlist) deferred

## Unresolved Limitations
1. DNS rebinding TOCTOU window (mitigated, not eliminated)
2. Proxy bypass possible
3. No connection-time IP pinning (Ktor limitation)
4. Unsigned checksums (integrity only, not authentication)
5. No dependency vulnerability scanning (P5)