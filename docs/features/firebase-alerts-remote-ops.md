# Firebase Alerts & Remote Config

**Last verified:** 2026-11-16

DORAI AI integrates Firebase Cloud Messaging (FCM) v1 and Firebase Remote Config via lightweight Ktor REST clients in Kotlin Multiplatform `commonMain`, providing background task completion push notifications and dynamic configuration without depending on proprietary Google Play Services native SDKs.

## Behavior

### Settings & Configuration UI
- Located under **Settings > Agent > Firebase Remote Alerts**:
  - Firebase Project ID input field.
  - Firebase API Key / Server Token input field.
  - FCM Device Token input field.
  - **Remote Push Alerts** toggle switch.
  - **Send Test Alert** button for live verification of FCM payload delivery.
- If credentials are not configured or the feature toggle is disabled, no network calls to Firebase are executed.

### FCM v1 Push Alerts
- When long-running background agent workflows, autonomous tasks, or heartbeats complete while the app is in the background, an FCM notification is dispatched to the user's registered device token.
- Push alerts support multi-language notification titles and bodies (including natural Bengali script with UTF-8 NFC normalization).
- Includes structured custom data payloads (`taskId`, `taskTitle`, `completedAtEpochMs`) enabling the receiving device to navigate directly to the relevant task record upon click.

### Dynamic Remote Config
- Queries the Firebase Remote Config REST endpoint to fetch dynamic runtime parameters (such as maintenance flags, recommended model lists, or promoted skills).
- Fallbacks to local in-app defaults if the device is offline, requests time out, or remote parameters are unavailable.

### Zero Native SDK Footprint & Portability
- All Firebase operations are built using standard HTTP REST contracts via Ktor and kotlinx.serialization.
- Eliminates heavy native dependencies (`com.google.firebase:*` Gradle plugins and Google Services JSON), ensuring complete compilation and execution parity across Android, Desktop JVM, iOS, and Web Wasm.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/firebase/FirebaseConfig.kt` | Validates Firebase project ID, API key, and device tokens |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/firebase/FirebaseMessagingClient.kt` | Sends FCM v1 notification alerts with structured data payloads |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/firebase/FirebaseRemoteConfigClient.kt` | Fetches and parses dynamic remote configuration parameters |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/dtos/firebase/FirebaseMessagingDto.kt` | FCM v1 message request and response data contracts |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/dtos/firebase/FirebaseRemoteConfigDto.kt` | Remote config response data contracts |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/FirebaseAlertsSection.kt` | Settings card for credentials, alerts toggle, and test alert trigger |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/network/firebase/FirebaseTest.kt` | Unit tests for config validation, Bengali push payload generation, and remote config parsing |

## Tests

Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.network.firebase.FirebaseTest"`
