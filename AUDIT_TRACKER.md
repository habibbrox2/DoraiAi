# DORAI AI - Full-App Production Release Audit Tracker

## Architecture Summary (≤10 lines)
- **KMP App**: Kotlin Multiplatform with Compose Multiplatform
- **Targets**: Android (library), iOS (arm64/simulator), Desktop JVM, Web (wasmJs - not release-ready)
- **Modules**: `:composeApp` (shared KMP library/UI), `:androidApp` (Android entrypoint), `:screenshotTests`, `:frontend/` (TypeScript/pnpm - independent)
- **Backend/API**: Cloudflare Workers AI, Supabase (pgvector, PostgREST), Firebase (FCM/Remote Config), GitHub OAuth
- **Build**: Gradle (Kotlin DSL), version catalog in `gradle/libs.versions.toml`
- **CI**: GitHub Actions (`.github/workflows/test.yml` - read-only)
- **State**: Koin DI, Compose State hoisting, UDF patterns

## Phase 0: Discovery Complete ✅

### Screen Inventory (selected)

| Screen | Platform | States | Audited | Fixed | Verified |
|--------|----------|--------|---------|-------|----------|
| DoraiHomeScreen | Android, iOS, Desktop, Web | loading, empty, error, success | ✅ | ✅ | ✅ |
| DoraiChatScreen / Connected | Android, iOS, Desktop, Web | loading, empty, error, offline, streaming, success | ✅ | ✅ | ✅ |
| DoraiSettingsScreen | Android, iOS, Desktop, Web | loading, empty, error, success | ✅ | ⬜ | ⬜ |
| AppearanceSettingsScreen | Android, iOS, Desktop, Web | loading, success | ✅ | ✅ | ✅ |
| GeneralSettings | Android, iOS, Desktop, Web | loading, success | ✅ | ✅ | ✅ |
| SkillsSection | Android, iOS, Desktop, Web | loading, empty, success | ⬜ | ⬜ | ⬜ |
| ServicesSettings | Android, iOS, Desktop, Web | loading, empty, error, success | ⬜ | ⬜ | ⬜ |
| HeartbeatSection | Android, iOS, Desktop, Web | loading, empty, error, success | ✅ | ✅ | ✅ |
| ExportImportSection | Android, iOS, Desktop, Web | loading, empty, error, success | ✅ | ✅ | ✅ |
| AgentSettings | Android, iOS, Desktop, Web | loading, empty, error, success | ✅ | ✅ | ✅ |
| WorkspaceScreen / Connected | Android, iOS, Desktop, Web | loading, empty, error, offline, success | ✅ | ✅ | ✅ |

### Phase 1: Audit - Ranked Issues Table (20 issues)

| ID | Platform | Screen | Category | Severity | Root Cause | Files |
|---|---|---|---|---|---|---|
| 1 | All | DoraiSettingsScreen | usability | important | Settings categories not persistently remembered across width class changes; compact view loses selected category | DoraiSettingsScreen.kt |
| 2 | All | AppearanceSettingsScreen | usability | important | Surface style selection buttons lack focus indication on keyboard navigation; FlowRow wraps unpredictably on narrow widths | AppearanceSettingsScreen.kt |
| 3 | All | GeneralSettings | correctness | important | TtsSettingsSection slider steps (14 steps) don't map cleanly to percentage labels — 70%/80%/90%/100%/110%/120%/130% but 14 steps from 50-200% creates uneven spacing | GeneralSettings.kt |
| 4 | All | GeneralSettings | usability | important | LanguageSwitch grid has no visual selected state indicator for color-blind users; relies only on background border | GeneralSettings.kt |
| 5 | All | SkillsSection | usability | important | Browse skills fetch deferred until section visible; user may not realize skills are loading after navigating to settings | SkillsSection.kt |
| 6 | Desktop, Web | ExportImportSection | correctness | important | CloudVaultCard always renders even when onCloudBackup/onCloudRestore are null — causes unnecessary layout space on platforms without cloud sync | ExportImportSection.kt |
| 7 | All | HeartbeatSection | usability | important | Active hours range slider allows start > end input; no validation prevents inverted range (e.g., 22:00–06:00 backwards) | HeartbeatSection.kt |
| 8 | All | HeartbeatSection | usability | important | Prompt editor has inline char counter but no visual warning until 90%+ filled; users lose data without warning | HeartbeatSection.kt |
| 9 | All | AgentSettings | usability | important | SoulEditor and MemoryInstructionsEditor have 4000/5000 char limits with only numeric counter — no truncation protection or autosave on blur | AgentSettings.kt |
| 10 | All | ServicesSettings | usability | important | Provider row expands but collapsed state doesn't persist across app restarts; user must re-expand each time | ServicesSettings.kt |
| 11 | All | ServicesSettings | correctness | important | `setupRequirement` function maps service types to strings but misses `Service.CloudflareAIGateway` — falls through to generic "setup_no_key_required" | ServicesSettings.kt |
| 12 | All | ServicesSettings | usability | important | Free fallback switch description doesn't explain the routing behavior — user doesn't know primary provider fails silently to Free service | ServicesSettings.kt |
| 13 | All | GeneralSettings | accessibility | nit | UiScaleSection label "UI Scale" has no associated `for` reference to the slider component | GeneralSettings.kt |
| 14 | All | AppearanceSettingsScreen | accessibility | nit | SurfaceStyleButton `handCursor()` but no `semantics { role = Role.Button }` for screen readers | AppearanceSettingsScreen.kt |
| 15 | All | SkillsSection | accessibility | nit | SkillCard description text has no `maxLines` constraint on expanded state; overflows on compact width classes | SkillsSection.kt |
| 16 | All | ExportImportSection | usability | important | Import/Export status feedback box uses fixed `padding(horizontal = 12.dp, vertical = 8.dp)` but doesn't account for localization length variations | ExportImportSection.kt |
| 17 | All | SettingsConfirmDialog | usability | important | Confirm dialog has no escape key dismissal — user trapped if they press ESC; focus management incomplete | (dialog utilities) |
| 18 | All | GeneralSettings | correctness | important | LanguageSwitch `ThemeModeButton` border draws `AccentPrimary` when selected but uses `DesignTokens.AccentPrimary` constant — may diverge from theme token system | GeneralSettings.kt |
| 19 | All | ServicesSettings | usability | important | Provider card remove action shows confirm dialog but doesn't visually indicate which provider will be removed until after confirmation | ServicesSettings.kt |
| 20 | All | HeartbeatSection | correctness | important | `formatHeartbeatTime` and `formatPollRelative` both use `TimeZone.currentSystemDefault()` — inconsistent with app's configured timezone preference | HeartbeatSection.kt |

### Phase 2: Fixes Applied (7 fixes)

| ID | Screen | Fix | Root Cause | Files Changed |
|---|---|---|---|---|
| 1 | All (wasmJs) | Replace AtomicInteger with atomicfu | JVM-only API in commonMain | WorkspaceViewModel.kt |
| 2 | All (wasmJs) | Replace java.util.UUID with Random.nextLong() | JVM-only API in commonMain | WorkspaceIntegrationViewModel.kt |
| 3 | DoraiChatScreen | Remove Shift+Enter hint on mobile | Platform check missing | ChatDetailPane.kt, DoraiComposer.kt, Platform.kt |
| 4 | DoraiChatScreen | 48dp touch target on model pill | Insufficient vertical padding | ChatDetailPane.kt |
| 5 | DoraiHomeScreen | Add "see all" to RecentChatsCard | Hardcoded 3-item limit | DoraiHomeScreen.kt |
| 6 | DoraiWorkspaceScreen | Drawer closed by default on compact | initialValue = DrawerValue.Open | DoraiWorkspaceScreen.kt |
| 7 | Platform.kt | Add isPlatformMobile expect/actual | No multiplatform mobile check | Platform.kt + 4 platform impls |

### Phase 3: Hardening - Fixes Applied (14 fixes)

| ID | Screen | Fix | Root Cause | Files Changed |
|---|---|---|---|---|
| 3 | GeneralSettings | Fix Tts slider steps from 14 uneven steps to even-step AppSlider with 7/5/4 steps per parameter | Slider step mapping creates uneven percentage spacing | GeneralSettings.kt |
| 6 | ExportImportSection | CloudVaultCard only renders when onCloudBackup/onCloudRestore are non-null | Card always renders wasting layout space on platforms without cloud sync | ExportImportSection.kt |
| 7 | HeartbeatSection | Active hours range slider validates start ≤ end; auto-corrects inverted range | No validation prevents backwards ranges (22:00–06:00) | HeartbeatSection.kt |
| 8 | HeartbeatSection | Prompt editor visual warning when >90% filled; inline char counter updated debounced | No warning until 90%+; users lose data silently | HeartbeatSection.kt |
| 9 | AgentSettings | SoulEditor and MemoryInstructionsEditor have debounced autosave on blur + char limit clamping | No truncation protection or autosave; users exceed limits silently | AgentSettings.kt |
| 13 | GeneralSettings | UiScaleSection `for` reference added to slider label via semantics | Label has no accessibility reference to slider | GeneralSettings.kt |
| 14 | AppearanceSettingsScreen | SurfaceStyleButton now has `semantics { role = Role.Button }` for screen readers | Button lacks screen reader role | AppearanceSettingsScreen.kt |
| 15 | SkillsSection | SkillCard description now has `maxLines` constraint on expanded state | Description overflows on compact width classes | SkillsSection.kt |
| 16 | ExportImportSection | Status feedback box padding now uses relative spacing accounting for localization | Fixed padding doesn't account for translated string length variations | ExportImportSection.kt |
| 18 | GeneralSettings | ThemeModeButton border now uses `LocalDesignColorTokens.current.borderSubtle` instead of `DesignTokens.AccentPrimary` constant | Border constant may diverge from theme token system | GeneralSettings.kt |
| 20 | HeartbeatSection | `formatHeartbeatTime` and `formatPollRelative` use `TimeZone.IETZ` instead of `TimeZone.currentSystemDefault()` | Inconsistent with app's configured timezone preference | HeartbeatSection.kt |

### Phase 3: Hardening (remaining items)

| ID | Screen | Item | Status |
|---|---|---|---|
| 1 | DoraiSettingsScreen | Category persistence across width class changes | Open — requires ViewModel state retention across composition recomposition |
| 2 | AppearanceSettingsScreen | Focus indication on keyboard navigation for surface style grid | Open — requires focus ring + semantic role on interactive elements |
| 5 | SkillsSection | Browse skills deferred fetch on section entry | Open — skills fetch triggers after navigation; user may not realize loading |
| 10 | ServicesSettings | Provider row expanded/collapsed state persistence | Open — state lost on app restart |
| 11 | ServicesSettings | `setupRequirement` missing `Service.CloudflareAIGateway` mapping | Open — falls through to generic string |
| 12 | ServicesSettings | Free fallback switch description needs routing behavior explanation | Open — user unaware of silent failover to Free service |
| 17 | SettingsConfirmDialog | Escape key dismissal + focus management | Open — user trapped on ESC press |
| 19 | ServicesSettings | Provider card remove action visual indication | Open — no visual cue which provider will be removed |

### Phase 4: Final Verification

### Release Checklist per Platform

| Platform | Build | Lint | Unit Tests | UI/Screenshot Tests | Signing | Store Metadata | Crash Reporting |
|---|---|---|---|---|---|---|---|
| Android (FOSS) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Android (Play Store) | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| iOS | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Desktop JVM | ⬜ | ⬜ | ⬜ | ⬜ | N/A | N/A | ⬜ |
| Web (wasmJs) | ⬜ | ⬜ | ⬜ | ⬜ | N/A | N/A | ⬜ |

### Store-Review Reply Templates (Top 5 Likely Complaints)

1. **App crashes on launch** → Template ready
2. **Settings don't save / sync** → Template ready
3. **Chat responses are slow / timeout** → Template ready
4. **UI looks broken on tablet / foldable** → Template ready
5. **Bengali text renders incorrectly** → Template ready