# AI Provider Audit — 2026-11-09

**Scope:** provider availability / free-tier / catalog, display/UX, deploy/security for AI providers in DORAI AI (Kotlin Multiplatform, UI v2).
**Method:** read-only audit (three parallel explorations: display/UX, catalog/free-tier, release/security). Findings are file:line-cited; behavioral gaps marked as assumptions where applicable.
**Verdict:** no provider secret leaks in the repo or release pipeline; multiple medium correctness/UX defects and process gaps. Not release-blocking for the Android/desktop production pipeline, but App Store/iOS deploy is not possible as-is (see iOS section).

Severity legend: **High** = must fix before shipping; **Medium** = fix soon; **Low** = polish; **Info** = observation / no action implied.

## A. Findings (severity-descending)

### Medium

1. **Local/Cloud badge is broken everywhere** (`App.kt:212`, `DoraiChatScreenConnected.kt:162`): the check `serviceId == "ollama" || "litert"` matches nothing — local Ollama is `Service.OpenAICompatible` (`Service.kt:633`). All `ChatMessageItem`s are created with `isModelLocal = false` (`ToolEventGrouper.kt:204,229`), so `DoraiProviderBadge` always renders "Cloud". Affects message rows, top-bar pill, home card privacy copy.
2. **`DoraiSettingsScreen.kt:647-744` hardcoded bilingual mock Providers screen** (`ProvidersSettingsContent`): fictional "Ollama Local" / "LiteRT" / "Gemini" rows with `hasKey=true`, `activeProviderId` local-only, embedded secret fragment `"••••1a2b"`. Shipped in the binary; referenced by screenshot test `settings_providers_compact_ogshine`. Latent when `uiState != null` (production passes real state via `DoraiSettingsScreenConnected`).
3. **Free-tier bundle stale** (`docs/knowledge/free-tier/openrouter.md`, `ollama-cloud.md`, `docs/knowledge/model-catalog/arena-scores.md`): `stale_after: 2026-08-25` / `2026-10-16` — both past the refresh playbook trigger date as of 2026-11-09. Refresh was not performed as part of this audit (invariant: no silent bumps).
4. **Bundle falsely claims verification** (`openrouter.md:10`, `ollama-cloud.md:10`): `verified: process:desktopTest-FreeTierModels`, but no file matching `*FreeTierModels*Test*.kt` exists in the repo; the refresh playbook's `desktopTest --tests '*FreeTierModels*'` would select zero tests. `ModelCatalogTest.kt` does exist (analogous stamp plausible).
5. **ModelCatalog duplicate key `auto`** (`ModelCatalog.kt:39` vs `:1214`): HashMap rebuild (`:2036-2041`) silently keeps the later entry — `lookup("auto")` loses the 128k-context window and releaseDate; falls back to `DEFAULT_CONTEXT_WINDOW_TOKENS` (100k). Context-window validation mis-budgets Free tier.
6. **`openrouter/auto` dead key** (`ModelCatalog.kt:38`): `lookup()` strips everything before the last `/` (`substringAfterLast('/')`, `:2055`), so any slash-key is unreachable.
7. **Legacy plaintext settings migration never clears the old store** (`Platform.android.kt:220-224`, `Platform.ios.kt:132`, `AppSettingsMigrations.kt:23-52`): settings containing API keys are copied into the encrypted store but the old plaintext store is not deleted — plaintext keys persist indefinitely on migrated installations.
8. **`play-store.yml` accepts arbitrary `ref`** (`play-store.yml:4-14,42-44`): no validation that `ref` is a `v*` tag or ancestor of `origin/main`; anyone with dispatch rights + `DORAI_PLAY_STORE_ENABLED=true` can ship an arbitrary commit to the production track. Also: Play Store service-account key is inlined in `run:` steps (should use `env:`), decode step lacks `set -euo pipefail`, decoded key file is left in the workspace with no cleanup.
9. **`FetchUrlTool` DNS-resolution SSRF hole** (`FetchUrlTool.kt:159-164`): hostnames that *resolve* to private addresses are not blocked (no DNS lookup in common code); a metadata-IP-resolving public hostname is reachable by the agent tool.
10. **Free-tier bundle vs runtime mismatch** (`FreeTierModels.kt:34-36` vs `docs/knowledge/free-tier/openrouter.md:48-50`): runtime ships 17 ids (`openrouter/auto`, `auto`); bundle lists 15; `log.md:6` claims "15 → 15".
11. **Built-in Free provider is a dead-config wrapper** (`Service.kt:117-123`): `Service.Free.chatUrl = https://api.kai9000.com/chat/completions` is never read (Free routes to `KeylessFreeTierFallback`). Misleading residue; no user harm, but should be deleted or commented. Docs (`docs/features/multi-service.md:26-33`) describe the actual behavior accurately.

### Low

12. **Debug builds log full HTTP bodies** (`Requests.kt:156-172`): `LogLevel.BODY` Ktor logging in debug prints request/response headers incl. `Authorization`. Release-unaffected. Desktop requires `-Ddorai.debug=true`; Android uses `FLAG_DEBUGGABLE`; iOS `isDebugBinary`.
13. **Local release builds silently produce debug-signed APKs** (`androidApp/build.gradle.kts:79-93`) without `ENFORCE_RELEASE_SIGNING=true` and without the keystore env vars — release integrity rests on CI setting the flag.
14. **keytool password on argv** (`release.yml:127,254,362`): `-storepass "$KEYSTORE_PASSWORD"` visible to sibling processes on the runner; should be `-storepass:env`.
15. **Release keystore left in workspace** (`release.yml:43`): decoded `dorai-release.jks` is not deleted in-job (not artifact-referenced, lingers until job end).
16. **`release-procedure.md` stale**: describes the old phase/`bump`-job design; actual workflow tags *after* builds pass and bumps metadata via auto-merged PR; `DORAI_PLAY_STORE_ENABLED` has no operator guidance.
17. **Status badge indistinguishable by color** (`OperationalComponents.kt:100-108`): `Connected` vs `Disabled` vs `Ready` vs `Active` share near-identical gray; only text differs. `Connected`/`Disabled` give no visual signal.
18. **TalkBack violations on provider surfaces**: `FreeProviderSuggestionsPanel.kt:123-131` retry IconButton unlabeled; `ServiceSelector.kt:166-171` chevron duplicates name (double announcement); configured-card expand header `clickable` + `liveRegion=Polite` without `Role.Button` and no expand/collapse state; `ProviderRowItem` clickables without role; `DoraiProviderBadge` non-iconOnly mode double-announces.
19. **Dozens of hardcoded English strings** in provider surfaces (`ServicesSettings.kt`, `ServiceSelector.kt`, `DoraiSettingsScreen.kt`, `DoraiHomeScreen.kt`, `App.kt`) — violate the project's `Res.string` convention.
20. **U+FFFD literal in user-visible string** (`ServiceSelector.kt:157`): renders as "�". Latent (no `showLabel=true` caller).
21. **Free fallback toggle unreachable** (`ServicesSettings.kt:194-235, :702`): single call site passes `showFallbackToggle = false`; dead Res.string.
22. **FreeMode FAST/EXPERT dead** (`Service.kt:43-49`): `nameRes`/icons/`AppSettings` keys unused; `RemoteDataRepository.kt:266-267` reads `modelId` into a path the fallback ignores.
23. **`Service.fromId` silent fallback** (`Service.kt:672`): unknown/corrupt persisted serviceId renders as "Free" everywhere.
24. **Configured service cards show no provider icon** (`ServicesSettings.kt:1008-1042`); `AssistantWhoRow` always renders generic logo (`DoraiChatScreen.kt:2224-2235`) — provider identity invisible per-message.
25. **Docs mismatch** (`README.md:91-97` claims no official release; `update.json:4-9` pins v2.1.22; `docs/features/encryption.md` says `~/.kai/*`, code uses `~/.dorai/*` with one-time copy migration).
26. **No numeric quotas documented** for OpenRouter/Ollama Cloud anywhere (only qualitative "Low usage"); free-tier bundle policy can't notify of quota drift.
27. **OpenCode/Kilo free models missing from ModelCatalog**: silently get 100k default context; `kilo-auto/free` strips to `free` → wrong Arena Elo attribution to "OpenRouter Auto (Free)".
28. **`multi-service.md` count off by one**: "32 LLM providers" vs 31 actually visible (Free/Kilo/OpenCode filtered).
29. **1min.AI adapter cap not enforced in code** (`docs/features/multi-service.md:122` documents 200 msg / 100k chars cap; no matching logic found).
30. **Arena bundle "514 attested" not reproducible** from the file as written (~519/520 mapped ids); code encodes no attested/estimated distinction.

### Info / good

31. **No secrets in release pipeline**: signing env vars + `GITHUB_TOKEN` only; no `set -x`, no `echo $SECRET`; update.json contains only digests/versions; SHA256SUMS nothing sensitive.
32. **Storage hygiene correct**: Android EncryptedSharedPreferences (AES256 SIV/GCM, Keystore MasterKey); iOS Keychain (`service="com.dorai.ai"`); Desktop AES-256-GCM file with owner-only perms; Web plaintext localStorage (documented, not release-ready). No tracked `.jks`/`*.key`/`.env*`; `fastlane/play-store-key.json` gitignored.
33. **No committed provider test keys**; integration test reads keys from env, does not print them.
34. **MCP validator is stronger than provider validation** (`McpUrlValidator.kt` fail-closed, restricted schemes) — provider endpoints get strictly weaker treatment (baseUrl not validated, cleartext allowed for local-Ollama LAN use; intentional but undocumented).
35. **iOS release lane missing**: `iosApp/Config.xcconfig: TEAM_ID=` empty, `APP_VERSION=2.1.18` vs catalog 2.1.23. Not a security issue; nothing leaks; deploy is simply not possible.
36. **`.grok/skills/update-*` paths in knowledge bundles are fictitious** in this repo; `/update-free-tier-models` and `/update-model-catalog` are environment-side agent commands only.
37. **Kilo/OpenCode are intentionally hidden** from user-selectable providers (`SettingsViewModel.kt:345`); `"Auto"` in the picker is the keyless free fallback entry.
38. **`ic_service_*` drawables all resolve**; free-suggestions signup URLs all non-null; OllamaCloud correctly "hosted".

## B. Priority fix order

1. D1 badge fix (Local/Cloud detection) + D6 status colors + D7 TalkBack labels.
2. D2 `ProvidersSettingsContent`: mock section should be deleted or wired to real state; screenshot test must be updated to the new surface instead of referencing mock.
3. D5 refresh free-tier + model-catalog OKF bundles; create `FreeTierModelsTest.kt` so `verified:` stamps become true.
4. D5 `ModelCatalog` `auto` key dedupe; decide 128k vs `OpenRouter Auto` (or both with unique keys).
5. C8 `play-store.yml` ref validation (`^v[0-9]+\.[0-9]+\.[0-9]+$` + ancestry of `origin/main`); move Play Store key decode to `env:`; add `set -euo pipefail`.
6. C7 clear legacy plaintext settings store after migration (Android + iOS).
7. C9 FetchUrlTool: DNS-resolve + re-check on redirects (or block until resolvedSafe).
8. D8/D9/D10/D16 hardcoded strings + icon presence on configured cards & who-row; D17/D19 doc refresh.

## C. Out of scope / not verified

- iOS archive, signing, App Store Connect upload — no lane exists.
- Runtime quota verification of free providers against live endpoints.
- Bundle-vs-code counts are file-inspection based; no Gradle builds were run.
- `site/`, `screenshots/`, `fastlane/metadata/**` — binary assets can't be fully verified here.
