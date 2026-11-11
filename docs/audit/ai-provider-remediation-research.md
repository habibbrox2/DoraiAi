# AI Provider Audit — Remediation Research (2026-11-09)

Working document for the provider-system remediation. It records what was re-validated against official sources, what was confirmed in code, and where uncertainty remains. Nothing here ships by itself — implementation evidence lives in [ai-provider-remediation-implementation.md](ai-provider-remediation-implementation.md) and test/build evidence in [ai-provider-validation-report.md](ai-provider-validation-report.md).

## Sources consulted (official)

| Source | URL | Used for |
|---|---|---|
| OWASP SSRF Prevention Cheat Sheet (current) | https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html | Deny-list design (loopback/private/link-local/ULA/CGNAT/reserved/multicast), "normalize before you compare" (IPv4-mapped IPv6, NAT64), resolved-IP validation as defense-in-depth, per-redirect re-validation |
| GitHub Actions — Secure use reference | https://docs.github.com/en/actions/reference/security/secure-use | Workflow secret hygiene: least privilege, `env:` over inline interpolation, `set -euo pipefail`, no credential echoing, runner ephemerality |
| Ktor 3.6 API (bundled) | `io.ktor.http.Url` (inspected from the shipped artifact) | Confirmed `Url.encodedUser`/`encodedPassword` exist; **no `userInfo` property**; lenient parse silently substitutes `localhost` for unparseable strings (reproduced) |
| `keytool` behavior (Oracle JDK docs, offline check of flags) | local `keytool -help` / JDK reference | Confirmed no environment-variable indirection for the storepass — `-storepass` accepts argv only |

Non-official information (forums, blogs, marketing) was not used for decisions.

Web search was unavailable in this environment (HTTP 402); all API/behavior claims above were verified against the official pages or the bundled artifacts directly.

## Key research outcomes and how they shaped the fix

 1. **SSRF is a normalization problem, not just a substring check.** OWASP guidance requires normalizing IPv4-mapped IPv6 (`::ffff:127.0.0.1`), NAT64 wrappers (`64:ff9b::/96`, `64:ff9b:1::/48`), and octal/hex/short numeric spellings before comparison. That drove a single shared classifier ([NetworkAddressGuard.kt](https://github.com/habibbrox2/DoraiAi/blob/main/composeApp/src/commonMain/kotlin/com/dorai/ai/tools/NetworkAddressGuard.kt)) used by `fetch_url` and by the OpenAI-Compatible locality classifier, so "local" and "unfetchable" stay consistent.
2. **Deny-lists must fail closed.** Unknown/malformed addresses are treated as blocked; an empty DNS answer on a platform expected to resolve (Android/Desktop) is a refusal, not a pass. On platforms with no DNS API at all (iOS, Web) the resolved-IP check is *skipped* (literal checks still apply) — this mirrors the MCP validator's documented posture and is a deliberate, documented limitation rather than a false sense of security. Note: unlike MCP (fail-closed), `fetch_url` cannot fail closed on iOS/Web without blocking all outbound requests.
3. **Redirects re-run the full validation pipeline.** `fetch_url` already used a manual redirect loop (no automatic 30x following); resolved-host validation was added *inside* `rejectUrl`, so every hop re-validates scheme, userinfo, metadata hosts, literal deny-list and resolved IPs.
4. **Ktor's parser is lenient and fails silent.** `Url("not a url")` does not throw — it yields `host='localhost'`. The first cut of the locality classifier, `localhost`-by-default for blank/garbage URLs, silently granted the Local privacy badge to anything unparseable. The fix: blank → documented default (`localhost:11434`); garbage (no `://`) → Cloud; parse failure → Cloud. Regression-tested.
5. **Signing secrets cannot be fully hidden from argv.** `keytool` has no `-storepass:env`. Accepted limitation: argv with GitHub masking + ephemeral runner + immediate deletion after the single `keytool -list` use (`release.yml` now deletes the keystore right after validation, and as a safety net after APK jobs). Documented in `docs/release-procedure.md`.
6. **verify-then-clear migration semantics.** Encrypted-destination-first with per-key removal, destination-wins on conflict, run-once via a completion flag; account-scoped keys are discovered by parsing `id` fields out of the account JSON (the common `Settings` API cannot enumerate keys), covering `email_password_<id>` and `splinterlands_posting_key_<id>`.
7. **Free-tier "verification" stamps need a real test.** The OKF bundle claimed `verified: process:desktopTest-FreeTierModels` while no such test existed. Created `FreeTierModelsTest` (17-id OpenRouter pin, service-scoped free-ness, Ollama alias normalization) so the stamp is meaningful; the bundle `log.md` now records the relationship honestly.
8. **Local vs Cloud must be an explicit, typed concept**, not string matching. The pre-existing `serviceId == "ollama" || serviceId == "litert"` could never match (local Ollama *is* `Service.OpenAICompatible`), so the privacy badge was always Cloud. Fix: `ExecutionLocation` (LOCAL/CLOUD/USER_CONFIGURED) on `Service`, propagated via `ServiceEntry.executionLocation` (resolved at load from the instance base URL), fallback answers always Cloud.

## Explicitly not verified (kept honest)

- **Live OpenRouter/Ollama pricing and catalog drift.** The free-tier bundle `stale_after` (2026-08-25) is past; refresh requires network + the environment-side `/update-free-tier-models` command. This session did **not** re-fetch pricing (no silent bumps). The runtime snapshot was aligned to documentation, not the reverse.
- **TOCTOU on DNS answers:** an address could change between validation and connect. Engine-level DNS pinning is not feasible across the Android(OkHttp)/Desktop(CIO)/iOS(Darwin)/Web split — documented as accepted risk in the fetch tool's KDoc.
- **`fetch_url` resolved-IP checks on iOS/Web** are absent (no DNS API in those source sets). Documented limitation; literal checks remain.
- **Workflow behavior on real runners** (e.g. `merge-base --is-ancestor` semantics against a real `origin/main`) was reviewed statically, not executed.
- **Play Store / keystore handling end-to-end** (actual upload to the Play track) is a production action and was not performed.
- **Arena catalog completeness** ("514 attested") was not independently reconstructed; no catalog scores were edited.
- **Network-dependent quota behavior** of the Free tier was not probed (no live requests).
