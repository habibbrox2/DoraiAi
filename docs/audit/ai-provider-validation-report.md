# AI Provider Audit — Remediation Validation Report (2026-11-09)

Companion to [ai-provider-audit-2026-11-09.md](ai-provider-audit-2026-11-09.md) (findings), [ai-provider-remediation-research.md](ai-provider-remediation-research.md) (research) and [ai-provider-remediation-implementation.md](ai-provider-remediation-implementation.md) (statuses).
**No commits, pushes, tags or releases were made.** Everything below is working-tree evidence; statuses use the audit legend (FIXED / PARTIALLY_FIXED / VERIFIED_NOT_REPRODUCIBLE / NOT_APPLICABLE / BLOCKED / UNVERIFIED).

## Build & test gates (this working tree, `main` @ `decf31dd` + changes)

| Gate | Command | Result |
|---|---|---|
| Formatting | `gradlew.bat spotlessCheck` | **BUILD SUCCESSFUL** (after one `spotlessApply` pass to normalize import order) |
| Shared/Desktop tests | `gradlew.bat :composeApp:desktopTest` | **BUILD SUCCESSFUL** — full suite green incl. all new regression tests; earlier iteration found + fixed 3 assertion defects in the new tests (see "Test defects found & fixed") |
| Screenshot goldens | `gradlew.bat :screenshotTests:verifyPaparazziDebug` | **BUILD SUCCESSFUL** after re-recording the 8 goldens (intentional visual changes); baseline re-verified on a detached checkout at `decf31dd` (all 116 goldens passed before changes — proving the 8 diffs came from the remediation, not stale goldens) |
| Ktor artifact check | `javap` on `ktor-http-jvm-3.6.0.jar` | Confirmed `Url.encodedUser/encodedPassword/user/password` exist; **no `userInfo`** member — rewrote the check accordingly |

Baseline attribution: a detached worktree at `decf31dd` passed `:screenshotTests:verifyPaparazziDebug` (116/116). After remediation, exactly the 8 screens touched by provider icon/badge-color/static-branch changes differed; each golden was inspected against the intent before re-recording.

## New / extended regression tests (all passing in `desktopTest`)

| Test file | Coverage |
|---|---|
| `commonTest/tools/SsrfAddressGuardTest.kt` | Deny-list: v4 loopback/private/link-local/metadata/CGNAT/benchmark/reserved; v6 `::`, `::1`, fe80::/10, fec0::/10 (deprecated site-local), fc00::/7, ff00::/8; NAT64 well-known + local-use prefixes; IPv4-mapped IPv6; fail-closed on malformed input |
| `commonTest/tools/FetchUrlHostTest.kt` (extended) | Metadata hostnames (incl. mixed-case), `isIpLiteralHost` |
| `commonTest/network/HttpLogSanitizerTest.kt` | Credential header redaction, provider key variants, cookies, inline bearer tokens, pass-through |
| `commonTest/data/ProviderLocalityTest.kt` | LOCAL/CLOUD per service; OpenAI-Compatible by endpoint (blank→Ollama default, loopback/LAN/`.local`→LOCAL, public/malformed→CLOUD); `ServiceEntry` defaults; `fromIdOrNull`; LiteRT invariant across base URLs; **Ktor localhost-fallback regression** (`"not a url"` → CLOUD) |
| `commonTest/data/ModelCatalogTest.kt` (extended) | `openrouter/auto` resolves; full-id-first lookup; `duplicateKeys` empty; fallback targets resolve |
| `commonTest/data/FreeTierModelsTest.kt` | 17-id OpenRouter pin; service-scoped free-ness; Ollama 3 ids + alias normalization |
| `commonTest/data/SettingsLegacyMigrationTest.kt` | Verify-then-clear; destination wins + legacy still cleared; run-once; account-scoped keys discovered from JSON; null legacy is a no-op |
| `commonTest/ui/chat/ToolEventGrouperTest.kt` (extended) | Locality propagates to assistant messages; cloud when fallback answers; provider icon propagated |
| `desktopTest/tools/DnsResolvedHostGuardTest.kt` | Desktop DNS support declared; `localhost` resolves to blocked addresses (offline-safe: hosts file only) |

## Intentional screenshot goldens re-recorded (8, `recordPaparazziDebug --tests …`)

Failing→re-recorded set: `settings_providers_compact_ogshine` (static Providers screen → localized empty-state Services screen; contains no fake provider rows or key fragments), `settings_providers_custom_gateway_expanded_ogshine` (provider identity icon on configured card + primary-colored Connected badge), `gemmaLocal_download_dark`, `gemmaLocal_modelCard_dark`, `gemmaLocal_contextSlider_dark`, `gemmaLocal_select_dark`, `gemmaLocal_fixedContextModel_dark` (same icon + badge changes on the LiteRT card), `settingsFree_dark` (same on the configured Free card). `gemmaLocal_settings_dark`, `gemmaLocal_chat_dark`, `buildStatusPills_light`, `settings_account_compact_ogshine` all passed without re-recording. No other goldens re-recorded; `verifyPaparazziDebug` is green afterwards.

## Test defects found & fixed during the run (honesty log)

1. `ProviderLocalityTest` asserted `"not a url"` → CLOUD; actual LOCAL — root-caused to Ktor's lenient `Url("not a url")` returning `host='localhost'`. **Fix in `Service.isOpenAiCompatibleLocalEndpoint`** (require `://`; blank→Ollama default; parse failure→CLOUD) + assertion kept. No production behavior was tuned to pass a test.
2. `FreeTierModelsTest` asserted `gemma4:cloud` is free — it is not (the curated API-id is `gemma4:31b`). Assertion corrected to the true contract (`gemma4:31b-cloud` → `gemma4:31b`).
3. `SsrfAddressGuardTest` asserted `fec0::1` blocked — `fe80::/10` does not cover it; classifier extended to `fec0::/10` (deprecated site-local, still internal).
4. Compile errors: missing `role`/`stateDescription` semantics imports; Ktor `Url.userInfo` does not exist → `encodedUser`/`encodedPassword` (verified in the artifact); `HttpLogSanitizer` Regex `options` needed `setOf`; resource accessors require explicit imports per key; `action_retry` import removed as unused after refactor.
5. Golden diffs were anticipated for Connected/Ready/Active color + provider icons; `buildStatusPills_light` and `settings_account_compact_ogshine` were **not** failing after the final implementation — initial fear of over-broad recolor was dispelled by the baseline comparison, not by relaxing the gate.

## Verified end-to-end

- New locality classification is source-sealed: every platform (`android/desktop/ios/wasmJs`) actualized `supportsDnsResolution`; common code compiles for all targets in `desktopTest` scope (commonMain compiled to JVM bytecode).
- `settings.aes` path corrected in docs to `~/.dorai` (with `~/.kai` one-time copy documented).
- OKF free-tier bundle ↔ runtime literal parity (17 ids) enforced by `FreeTierModelsTest` from now on.

## Open items carried forward (with owners)

| Item | Status | Needed to close |
|---|---|---|
| Live free-tier pricing refresh (`stale_after` 2026-08-25 past) | UNVERIFIED | Run `/update-free-tier-models` in the agent environment with network + human approval |
| `README.md` "first DORAI AI release is not published yet" vs tags v2.1.18–v2.1.22 and root `update.json` pinning v2.1.22/code 234 (catalog at 2.1.23/235) | PARTIALLY_FIXED, not edited | Release-owner reconciliation when the 2.1.23 workflow runs |
| iOS lane (`TEAM_ID=`, `APP_VERSION`) | BLOCKED | Apple Developer account + deliberate decision |
| Numeric free-tier quotas | UNVERIFIED | Live probing or provider docs + product decision |
| 1min.AI 200 msg/100k chars cap | UNVERIFIED | Implement or drop the doc claim |
| Arena "514 attested" | UNVERIFIED | Reconstruct from source bundle |
| `Enforce DNS pinning at engine layer` | NOT_APPLICABLE (split engines) | Accept documented TOCTOU risk |

## Deliverables

- [ai-provider-remediation-research.md](ai-provider-remediation-research.md) — official-source research and decision rationale
- [ai-provider-remediation-implementation.md](ai-provider-remediation-implementation.md) — per-finding status (all 38) and verification notes
- [CHANGELOG.md](../CHANGELOG.md) — unreleased section drafted (not committed)
- `docs/features/chat.md`, `docs/features/multi-service.md`, `docs/features/encryption.md`, `docs/release-procedure.md` — updated with “Last verified” 2026-11-09
- `git status` — see final report; **no commit/push/tag/release performed**

_Gate re-run status (post-remediation, final): `desktopTest` **BUILD SUCCESSFUL** (~1375 tests, full suite green — initial rerun caught and fixed one compile regression (`Service?` vs `Service` in `getOrderedFallbackEntries` after `fromIdOrNull`)); `spotlessCheck` **BUILD SUCCESSFUL**; `verifyPaparazziDebug` **BUILD SUCCESSFUL** after the 8 intentional goldens were re-recorded. Screenshot verification re-run on demand confirms green at HEAD of this working tree._
