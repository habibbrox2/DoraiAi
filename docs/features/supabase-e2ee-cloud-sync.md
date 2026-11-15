# Supabase E2EE Cloud Sync

**Last verified:** 2026-11-16

Users can back up and restore their DORAI AI configuration and conversation archives across devices via Supabase cloud storage using zero-knowledge client-side encryption (AES-256-GCM). The server never possesses the user's passphrase or unencrypted data.

## Behavior

### Settings & Cloud Vault UI
- Located under **Settings > General > Export / Import > Cloud Vault** (above native file export/import).
- Gated by Supabase credentials: if Supabase URL or Anon Key is missing, the card explains that cloud credentials must be configured first in Agent Settings.
- Shows current cloud sync state (last backup timestamp or "No cloud backup found"), a passphrase input field with visibility toggle, and primary action buttons:
  - **Backup to Cloud Vault**: Exports current configuration and conversations, encrypts the payload locally with the user passphrase, and uploads to Supabase.
  - **Restore from Cloud Vault**: Fetches the latest encrypted payload from Supabase, decrypts with the user passphrase, validates integrity, and restores settings and conversations.

### Cryptographic Design & Zero-Knowledge Guarantee
- **Key Derivation:** The encryption key is derived on the device using PBKDF2 with HMAC-SHA256 (64,000 iterations) using a cryptographically secure random 16-byte salt.
- **Authenticated Encryption:** Data is encrypted with AES-256 in Galois/Counter Mode (GCM) using a unique 12-byte initialization vector (IV) per backup and a 128-bit authentication tag.
- **Integrity Checksum:** A Base64-encoded SHA-256 hash of the plaintext is packaged in the backup envelope. On restore, the decrypted text is verified against the checksum before parsing JSON to catch silent bit-rot or tampering.
- **Zero-Knowledge Security:** Plaintext settings, API keys, and chat conversations never leave device memory unencrypted. The remote Supabase database only stores ciphertext, salt, IV, and the verification hash.

### Fail-Closed & Offline-First Guarantees
- Network timeouts, bad credentials, or invalid passphrases fail safely with localized error messages.
- Decryption with an incorrect passphrase fails during GCM authentication verification without mutating or corrupting existing local state.
- Local export and import via JSON file remain fully functional without any network connectivity.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/sync/E2EECrypto.kt` | Platform-expect crypto contracts (encrypt, decrypt, key derivation) |
| `composeApp/src/jvmShared/kotlin/com/dorai/ai/data/sync/E2EECrypto.jvmShared.kt` | JVM/Android actual implementation using javax.crypto Cipher and SecretKeyFactory |
| `composeApp/src/iosMain/kotlin/com/dorai/ai/data/sync/E2EECrypto.ios.kt` | iOS platform stub implementation |
| `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/data/sync/E2EECrypto.wasmJs.kt` | Web Wasm platform stub implementation |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/dtos/supabase/SupabaseSyncRecordDto.kt` | Encrypted envelope DTO (ciphertext, iv, salt, checksum, version) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/sync/CloudSyncService.kt` | Backup creation and restore orchestration service |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/supabase/SupabaseRestClient.kt` | Lightweight Ktor HTTP client for Supabase PostgREST endpoints |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/CloudVaultSection.kt` | Settings UI card for Cloud Vault passphrase entry and sync actions |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/data/sync/E2EESyncTest.kt` | Unit tests for encryption, round-trip decryption, wrong passphrase, and checksum tampering |

## Tests

Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.data.sync.E2EESyncTest"`
