# Source Set and Platform API Guide

## Source set map
| Source set | Intended role |
|---|---|
| `commonMain` | Shared Kotlin and Compose UI; no JVM/Android-only APIs |
| `androidMain` | Android SDK, Android lifecycle/storage/permissions and Android-specific actuals |
| `desktopMain` | JVM desktop APIs, desktop drivers and desktop actuals |
| `iosMain` | iOS/Native actuals and Darwin-specific integrations |
| `wasmJsMain` | Browser/Wasm actuals and web resources |
| `jvmShared` | Shared JVM code explicitly included in Android and desktop; not a common source set |
| `commonTest` | Cross-platform unit tests |
| `desktopTest` | JVM desktop tests |

## Existing platform contract
- Shared declarations live in `composeApp/src/commonMain/kotlin/com/dorai/ai/Platform.kt`.
- Inspect actual implementations in `Platform.android.kt`, `Platform.jvm.kt`, `Platform.ios.kt`, and `Platform.wasmJs.kt`.
- When adding/changing an `expect` function/property, check every configured target and update only the appropriate `actual` implementations.
- The shared contract already includes HTTP client creation, secure/legacy settings, host address resolution, background dispatcher, drag-and-drop, platform identity, image compression/decoding, file saving, supported-tool declarations, and notification hooks. Reuse it where appropriate rather than creating duplicate platform checks.

## Portability decision tree
1. Is the API available in Kotlin common code and supported by all targets? If yes, use it.
2. Is an existing multiplatform library already used? Prefer the existing abstraction (for example `kotlinx-datetime`, Ktor, FileKit, multiplatform-settings).
3. Is behavior genuinely platform-specific? Add/extend a narrowly scoped `expect/actual` contract, after reviewing every target.
4. Is a feature unsupported on a target? Use an explicit, documented capability/fallback; do not fake support or silently return misleading results.

## Common portability traps
- Avoid direct `java.*`, Android SDK, `System.currentTimeMillis()`, AWT/Swing, filesystem paths, or JVM reflection in `commonMain`.
- Do not replace an API with an approximate alternative until semantics, time zones, cancellation, threading, and failure behavior are understood.
- Avoid target checks scattered through UI; centralize platform-specific behavior where the existing architecture supports it.
- A file in `jvmShared` is still not available to iOS or Wasm.

## Validation
Run the narrowest compile task for the changed source set, then compile all affected targets. Discover actual task names with the checked-out Gradle wrapper before assuming a task exists.
