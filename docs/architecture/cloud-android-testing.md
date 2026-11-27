# DORAI AI Cloud Android Testing Architecture

**Last verified:** 2026-11-27

## Decision

Use **Android Device Streaming, powered by Firebase**, as the primary long-term platform for manual APK validation on real Android hardware in Google Cloud.

Do **not** make the legacy Firebase Test Lab test-matrix workflow the foundation of DORAI AI's day-to-day QA architecture. Test Lab can remain an optional tool for scripted compatibility sweeps, but it is not required for releasing, and no automated release or GitHub Actions testing gate is mandated by this architecture.

The operator performs these steps manually:

1. Build the APK locally.
2. Choose a remote physical device.
3. Install and exercise the app interactively.
4. Capture screenshots, logs, and a short report.
5. Return and erase the device.

## Why Android Device Streaming

Android Device Streaming provides real physical devices hosted in Google data centers and Android Partner Device Labs. The local workstation connects through ADB over SSL, so the same `adb`, install, capture, and interaction tools work without running an emulator on the office PC.

Official capabilities relevant to DORAI AI:

- Real Pixel and partner-lab devices are available on demand.
- The app can be deployed from Android Studio or from the command line and then driven interactively.
- Device rotation, unfolding, screenshots, logs, and ADB commands work during a session.
- Google wipes and factory-resets the device when the session is returned, which is important because DORAI AI tests can contain chats, credentials, and cloud tokens.
- Sessions are billed by device minutes; unused time is returned when the device is returned properly.

Source: [Android Device Streaming, powered by Firebase](https://developer.android.com/studio/run/android-device-streaming).

## Prerequisites

### Android Studio path

- A recent stable Android Studio. The oldest supported version at the time of writing is **Android Studio Panda 2**; older versions cannot access current Cloud services.
- Sign-in to Android Studio with a Google account that has **Editor** or **Owner** access to the Firebase/Google Cloud project, or both `Firebase Test Lab Direct Access Admin` and `Service Usage Consumer` IAM roles.
- A Firebase project with device streaming enabled. Use `dorai-ai-app` when the test needs the real DORAI AI Firebase Auth, App Check, Crashlytics, and Performance configuration. A separate project may be used for pure UI checks, but it must not be presented as production auth verification.
- Billing/quota available for device minutes. Spark-plan trial minutes may be available; usage beyond the no-cost allowance can incur charges.

### Android CLI path

The Android CLI is useful when the operator prefers the terminal:

```powershell
android auth login
android device remote projects
android device remote models
```

`android auth login` is interactive and must be completed by the operator. No token, service account, or login credential belongs in the repository.

To avoid passing a project on every command, set a default in the local `~/.androidrc`; this file stays outside version control:

```text
device remote --project <your-qa-project-id>
```

Environment note (2026-11-27): the Android CLI is installed here, `android auth login` completed successfully, `dorai-ai-app` is listed as streamable, and Pixel 9a is available as `tegu/35`. The `devicestreaming.googleapis.com` API had to be enabled for `dorai-ai-app` before reservations worked. Use the real project when the check includes Firebase Auth, App Check, or provider configuration.

Source: [Android CLI `device remote`](https://developer.android.com/tools/agents/android-cli/commands/device_remote).

## Manual session workflow (Android CLI)

```powershell
# 1. List eligible projects and available real device models.
android device remote projects
android device remote models

# 2. Reserve and connect a device. Example model id comes from `models`.
android device remote create <codename>/<api> --project=<your-qa-project-id>

# 3. Build and install the APK locally.
.\gradlew.bat :androidApp:assembleFossDebug

adb install -r androidApp\build\outputs\apk\foss\debug\androidApp-foss-debug.apk

# 4. Exercise the app and collect evidence.
adb shell am start -n com.dorai.ai/com.dorai.ai.MainActivity
android screen capture -o <report-folder>\01-launch.png
adb logcat -d > <report-folder>\logcat.txt

# 5. Extend the reservation if the manual pass needs more time.
android device remote extend <reservation-id>

# 6. Always release the device. This returns unused minutes and wipes user data.
android device remote disconnect <reservation-id>
android device remote remove <reservation-id>
```

If `create` is called with `--connect=false`, connect explicitly with:

```powershell
android device remote connect <reservation-id>
```

### Windows daemon troubleshooting (observed 2026-11-27)

In this environment, `create`/`connect` reported `Failed to start connection process` because the CLI's detached Java launcher exited immediately, even though the reservation was ACTIVE. The hidden `daemon-connect` command works when started as a detached PowerShell process:

```powershell
$exe = (Get-Command android).Source
$out = "<report-folder>\remote-daemon.out.log"
$err = "<report-folder>\remote-daemon.err.log"
Start-Process -FilePath $exe `
  -ArgumentList @('device','remote','daemon-connect','<reservation-id>','--project=dorai-ai-app') `
  -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err -PassThru
```

The log should report `Device connected on port <port>`. The device appears in ADB as `localhost:<port>` (not `127.0.0.1:<port>`), so use that exact serial for `adb -s` commands. Keep the detached daemon alive for the session; if it exits, the remote device disconnects.

Reservations are short and quota-limited. During the 2026-11-27 pass, a reservation ended while the UI pass was still running; the daemon then logged `Device disconnected`, the ADB serial disappeared, and a follow-up reservation failed with `RESOURCE_EXHAUSTED: Insufficient device quota remaining`. Build and install the APK before reserving, prepare the exact checklist, and extend early if the pass needs more time.

## Manual session workflow (Android Studio)

1. Open **View > Tool Windows > Device Manager** and select the Firebase tab.
2. Sign in and select the QA Firebase project.
3. Choose **Start** next to a default device, or use **+ > Select Remote Device** to add models from the full catalog or partner device labs.
4. Deploy `com.dorai.ai` from the run configuration, then use the **Running Devices** window to interact with the real phone.
5. Use **Extend Reservation** when more time is needed.
6. Use **Return and Erase Device** when finished. Do not leave a reservation idle.

## DORAI AI manual QA checklist

Use the `foss` debug build for local feature work. Use the `playStore` debug build when checking Play Integrity/App Check behavior or a release-like provider configuration.

For a debug APK, Firebase App Check uses the debug provider. Its one-time debug token is printed in Logcat and must be registered with `firebase appcheck:debugtokens:create ... --project dorai-ai-app --app <android-app-id>` before Firebase calls are fully attested. Never commit the token; revoke it with `firebase appcheck:debugtokens:delete` when debugging is finished. A 2026-11-27 emulator run initially showed `App attestation failed`, then succeeded after registering the emitted token.

### Account and cloud auth

1. Cold start the app and open **Settings > Account**.
2. Confirm the Dorai Cloud card shows the signed-out state and three full-width, labelled sign-in actions.
3. Open the email sheet; confirm email/password fields, password visibility toggle, localized labels, and disabled submit until input is valid.
4. Sign in with an existing Firebase email/password user.
5. Confirm the status chip changes to connected and usage data loads.
6. Trigger a cloud call with a stale token to verify the 401 forced-refresh/retry path provisions and uses the `role=authenticated` claim.
7. Sign out and confirm the card returns to signed-out state with cleared password state.
8. If social providers are configured, repeat Google, Facebook, and GitHub sign-in through the Firebase redirect handler and `dorai://cloud/auth` return link.

### Cloud vault and Firebase services

1. Open **Settings > General > Cloud Vault** and confirm the passphrase visibility labels are localized.
2. Confirm backup/restore actions expose errors without leaking raw tokens or passphrases.
3. Open the Firebase alerts section and confirm the test-alert action uses a localized label and has a full touch target.
4. Review logcat for auth, token refresh, App Check, and Crashlytics/Performance initialization errors.

### Visual and accessibility pass

- Capture portrait and, on foldable/tablet models, wide layouts.
- Verify contrast in light and dark themes.
- Verify 48dp touch targets and no clipped labels.
- Verify Bengali strings are rendered correctly and no hardcoded English remains in the touched settings surfaces.
- Rotate the device and confirm state survives the configuration change.

## Evidence and report format

Store manual reports outside the app release pipeline. Suggested layout:

```text
qa/<date>-android-device-streaming/
  build.txt                 # Gradle task, flavor, commit/worktree state
  device.txt                # model, API level, reservation id (no credentials)
  screenshots/
  logcat.txt
  findings.md
```

`findings.md` should contain:

- Build flavor and APK hash.
- Device model and API level.
- Steps executed.
- PASS/FAIL per checklist item.
- Screenshot/log references for each failure.
- Severity and proposed fix owner.

Never paste Firebase service-account JSON, ID tokens, refresh tokens, Supabase keys, or user passwords into a report or repository file.

## Boundary with release automation

This architecture is intentionally manual:

- No GitHub Actions job is required to upload the APK.
- No automatic release is triggered by a successful device session.
- The operator decides when a device session is needed and whether findings block a release.
- CI can continue to run deterministic formatting, unit, and screenshot checks; those checks do not replace this manual real-device pass.
