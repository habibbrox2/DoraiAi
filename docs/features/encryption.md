# Encryption & Secure Storage

**Last verified:** 2026-11-11

All app settings — including sensitive ones such as API keys and email passwords — are stored in a single platform-specific secure settings store. Each platform uses the strongest available mechanism.

## Sensitive Data

The following data is stored in secure settings:
- Service API keys (per-instance)
- Email account passwords
- Splinterlands posting key
- MCP server request headers (e.g. authorization tokens), stored with the server list
- Encryption key for legacy conversation migration
- Conversation history on the browser build only — on Android, iOS, and desktop, conversations live in a local SQLite database in app-private storage (unencrypted at the application layer; protected by the OS app sandbox and any platform full-disk/file-based encryption). See [chat.md](chat.md) for the storage layout and migration chain.

## Platform Implementations

### Android
- **Mechanism:** `EncryptedSharedPreferences` via `dev.spght:encryptedprefs-ktx` (community fork of deprecated `androidx.security:security-crypto`)
- **Key encryption:** AES-256-SIV
- **Value encryption:** AES-256-GCM
- **Key management:** Android Keystore (`MasterKey` with `AES256_GCM` scheme)
- **Size limit:** ~2 MB per value (SharedPreferences limit)
- **File location:** App-private `kai_secure_prefs` SharedPreferences
- **Restore recovery:** If the encrypted file cannot be opened (typically after Android Auto Backup restores it onto a device whose hardware-bound Keystore key did not transfer), the file is deleted and a fresh empty store is created — settings and keys must be re-entered.
- **Legacy migration:** On first run, values from the old plaintext `com.dorai.ai_preferences` (selected service, model ids, API keys, base URL, app-open counter, plus pre-encryption copies of the encryption key, email account data and per-account passwords, and splinterlands account/posting keys) are copied into the encrypted store once. The migration is **verify-then-clear**: after the encrypted destination is confirmed to hold every migrated value, the plaintext originals are removed per-key (never a wholesale clear — on iOS the legacy store may share UserDefaults with the system), the destination value wins on conflict, and the migration runs at most once.

### iOS
- **Mechanism:** `KeychainSettings` (`com.russhwolf/multiplatform-settings`)
- **Encryption:** Hardware-backed Keychain encryption (AES-256-GCM via Secure Enclave on supported devices)
- **Key management:** Managed by iOS Keychain Services
- **Size limit:** Effectively unlimited
- **Service identifier:** `com.dorai.ai`
- **Legacy migration:** On first run, the same legacy keys are copied once from `NSUserDefaults` — same verify-then-clear semantics (per-key removal, destination wins, runs at most once).

### Desktop (macOS, Windows, Linux)
- **Mechanism:** `EncryptedFileSettings` — custom file-backed `Settings` implementation
- **Encryption:** AES-256-GCM via `javax.crypto`
- **Key management:** 256-bit random key generated via `SecureRandom`. On Windows the key is stored DPAPI-protected (`CryptProtectData`, user scope) at `~/.dorai/settings.key` — the raw key never sits on disk. On macOS/Linux the owner-only key file is used as before (OS keystore integration is not yet available for those platforms). Legacy raw-key installs are transparently re-protected on first launch after update.
- **IV:** 12-byte random IV per write, prepended to ciphertext
- **Authentication tag:** 128-bit GCM tag (integrity + authenticity)
- **File format:** `[12-byte IV][AES-GCM ciphertext + tag]`
- **File location:** `~/.dorai/settings.aes`
- **Legacy data folder:** `~/.kai/` (pre-rename). On first run with a missing `~/.dorai`, the legacy `~/.kai` tree is copied into `~/.dorai` (non-destructive copy) so existing installs keep their data while new writes land under the new name.
- **Write safety:** Every change re-encrypts the whole store and writes it to a temporary file that is atomically renamed over the old one, so a crash mid-write never leaves a truncated file. Concurrent writers are serialized. The key and settings files are restricted to owner read/write.
- **Unreadable file:** If the settings file fails to decrypt (wrong or missing key, truncation, tampering), it is renamed aside to `settings.aes.unreadable-<timestamp>` and the app starts with empty settings, so the original is never overwritten.
- **Size limit:** None (file-based)
- **Migration:** On first run, migrates existing data from Java Preferences (`Preferences.userRoot()`) and clears the old store. Java Preferences was the previous backend but has an 8 KB per-value hard limit.

### Web (WASM)
- **Mechanism:** `StorageSettings` (browser `localStorage`)
- **Encryption:** None (browser sandbox provides isolation)
- **Size limit:** ~5 MB total (browser-dependent)

## Legacy Conversation Storage

Prior to the current database-backed storage, conversations were stored in an encrypted file (`conversations.enc`) using XOR encryption with a 32-byte random key, and later as a JSON blob in secure settings. Both are migrated automatically on first load:

1. Read `conversations.enc` from the app files directory
2. Decrypt using XOR with the key from `encryption_key` in settings
3. Persist to the current conversation storage (database, or settings blob on the browser build)
4. Delete the legacy file

On database-capable platforms, a conversation blob found in secure settings is likewise imported into the database and removed from settings on first load.

The XOR encryption key is retained in settings for any devices that haven't migrated yet.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../Platform.kt` | `expect fun createSecureSettings()` declaration |
| `composeApp/src/androidMain/.../Platform.android.kt` | Android EncryptedSharedPreferences setup |
| `composeApp/src/iosMain/.../Platform.ios.kt` | iOS KeychainSettings setup |
| `composeApp/src/desktopMain/.../Platform.jvm.kt` | Desktop EncryptedFileSettings wiring |
| `composeApp/src/desktopMain/.../data/EncryptedFileSettings.kt` | AES-256-GCM file-backed Settings implementation |
| `composeApp/src/desktopMain/.../data/DesktopKeyProtector.kt` | DPAPI (Windows) protection of the desktop settings key |
| `composeApp/src/wasmJsMain/.../Platform.wasmJs.kt` | Web localStorage setup |
| `composeApp/src/commonMain/.../data/ConversationStorage.kt` | Legacy XOR migration logic |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | Settings access layer for all app data |
| `composeApp/src/commonMain/.../data/AppSettingsMigrations.kt` | One-time copy from legacy plaintext settings (Android/iOS); verify-then-clear: copies keys, verifies the destination holds every migrated value, then removes the plaintext originals per-key |
| `composeApp/src/commonMain/.../data/ConversationPersistence.kt` | Database vs. settings-blob conversation storage; imports a settings blob into the database |
