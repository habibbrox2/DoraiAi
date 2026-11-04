# Security Policy

## Reporting a Vulnerability

Please report security vulnerabilities via [GitHub Security Advisories](https://github.com/habibbrox2/DoraiAi/security/advisories/new). Do not open a public issue.

### What to include
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Protection Boundaries

### Android Network Security
- HTTPS enforced by default (`cleartextTrafficPermitted="false"`)
- Cleartext HTTP permitted only for `localhost`, `127.0.0.1`, `[::1]`, and `10.0.2.2` (Android emulator host)
- System trust anchors only (no user certificate trust)
- Self-hosted servers on LAN IPs require HTTPS or an SSH tunnel

### Android Backup
- `allowBackup="false"` — no ADB or cloud backup of app data
- In-app export/import provides migration path (JSON-based, user-controlled)
- Encrypted prefs and conversation DB excluded from backup rules

### MCP Server Security
- URL validation at config time and connection time (SSRF mitigation)
- Scheme restriction: HTTPS only for remote hosts; HTTP only for localhost
- IP-range filtering: private, loopback, link-local, multicast, reserved, and cloud metadata ranges blocked by default
- `allowPrivateIps` opt-in for self-hosted MCP on LAN (does not allow loopback, cloud metadata, multicast, or reserved)
- Header sanitization (CRLF/control character injection prevention)
- Redirect validation (max 3 hops, each target validated)
- Lenient JSON parsing disabled
- **Limitations:** DNS rebinding TOCTOU window exists; proxy bypass possible; no connection-time IP pinning

### Shell Command Security (Desktop)
- Blocked patterns for known destructive commands (best-effort, not a security boundary)
- No approval gate implemented yet (planned for P3)
- **Limitation:** Regex-based blocking is bypassable; treat model-generated commands as untrusted

### Credential Storage
- Android: EncryptedSharedPreferences (AES-256-GCM, hardware-backed MasterKey)
- Desktop: AES-256-GCM encrypted file (key stored alongside ciphertext, owner-only permissions)
- **Limitation:** Desktop key is file-level protection only, not hardware-backed

### Sandbox Isolation (Android)
- Linux sandbox runs via proot (user-space root) — NOT a complete OS sandbox
- Bind mounts expose host paths (home directory, project folders)
- README claims of "no access to host system" should be understood as best-effort isolation
- **Limitation:** proot does not provide kernel-level isolation; privileged operations may be possible

### Offline AI
- Local inference via LiteRT (Android + Desktop JVM)
- Model integrity verification not yet implemented (planned for P4)
- **Limitation:** Offline functionality limited to supported model formats and platforms