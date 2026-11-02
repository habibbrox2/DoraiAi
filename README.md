# Brox Kai v2.0

<div align="center">

<img src="site/img/logo_animation_readme.svg" height="125" alt="Brox Kai v2.0 animated logo">

**Brox Kai v2.0** — A security-hardened AI assistant with persistent memory, built on Kotlin Multiplatform + Compose Multiplatform.

Runs on **Android, iOS, Windows, macOS, Linux, and Web (Wasm)**.

[Website](https://kai9000.com) · [Documentation](https://kai9000.com/docs/) · [Source](https://github.com/habibbrox2/brox-kai-2.0) · [Releases](https://github.com/habibbrox2/brox-kai-2.0/releases)

<img src="https://img.shields.io/badge/Platform-Web-f7df1c?logo=javascript" alt="Web"> <img src="https://img.shields.io/badge/Platform-Android-34a853.svg?logo=android" alt="Android"> <img src="https://img.shields.io/badge/Platform-iOS-lightgrey.svg?logo=apple" alt="iOS"> <img src="https://img.shields.io/badge/Platform-Desktop-JVM-e10707.svg?logo=openjdk" alt="Desktop JVM">

</div>

## What's Different

Brox Kai v2.0 is a security-hardened fork of [Kai 9000](https://github.com/SimonSchubert/Kai).

### Security Hardening
- **MCP SSRF Protection** — URL validation (scheme, IP ranges, credentials, encoding), connection-time re-validation, redirect checks (max 3 hops), and header sanitization.
- **Shell Command Hardening** — Fail-closed allowlist, direct `ProcessBuilder` execution (no shell interpretation), rejection of metacharacters, path traversal, and dangerous flags.
- **Android Network Security** — HTTPS-only by default; cleartext restricted to localhost, 127.0.0.1, and 10.0.2.2.
- **Android Backup Disabled** — `allowBackup=false`; encrypted prefs and conversation DB excluded from backups.
- **Release Integrity** — SHA-256 checksums for all release artifacts.

### Branding & UI
- Brox Kai brand colors (Navy + Cyan) applied across the UI.
- Updated logo and visual identity. Assets live in [`docs/branding/brox-kai-v2-svg/`](docs/branding/brox-kai-v2-svg/README.md) — see [brand name usage](docs/branding/brand-name-usage.md).
- Chat, settings, accessibility, and cross-platform responsiveness improvements.

## Features

- **Persistent memory** — Remembers important details across conversations and uses them automatically.
- **Customizable soul** — Edit the system prompt to define personality and behavior.
- **Multi-service fallback** — 30+ LLM providers with automatic failover.
- **On-device inference** — Run models locally on Android with LiteRT (offline capable).
- **Tool execution** — Web search, notifications, calendar events, shell commands, and more.
- **MCP server support** — Connect to remote tool servers via MCP with SSRF protection.
- **Autonomous heartbeat** — Periodic self-checks that surface items needing attention.
- **Settings import/export** — Back up and restore settings as JSON.
- **Encrypted storage** — Conversations stored locally with encryption.
- **Text-to-speech** — Listen to AI responses.
- **Linux sandbox (Android)** — Run shell commands/scripts in a secure sandboxed Linux environment.
- **Image attachments** — Attach images to conversations.

## Supported Services

[Atlas Cloud](https://www.atlascloud.ai?utm_source=github&utm_medium=link&utm_campaign=Kai) · [Anthropic](https://console.anthropic.com) · [OpenAI](https://openai.com) · [Google Gemini](https://aistudio.google.com) · [DeepSeek](https://www.deepseek.com) · [Mistral](https://mistral.ai) · [xAI](https://x.ai) · [OpenRouter](https://openrouter.ai) · [Requesty](https://requesty.ai) · [Groq](https://groq.com) · [NVIDIA](https://developer.nvidia.com) · [Cerebras](https://cerebras.ai) · [Ollama Cloud](https://ollama.com) · [LongCat](https://longcat.chat) · [Together AI](https://together.ai) · [Hugging Face](https://huggingface.co) · [Venice AI](https://venice.ai) · [Moonshot AI](https://moonshot.cn) · [Z.AI](https://z.ai) · [MiniMax](https://minimax.io) · [AIHubMix](https://aihubmix.com) · [Deep Infra](https://deepinfra.com) · [Fireworks AI](https://fireworks.ai) · [OpenCode](https://opencode.ai) · [AI Horde](https://stablehorde.net/) · [1min.AI](https://1min.ai) · [Perplexity](https://www.perplexity.ai) · OpenAI-compatible APIs · LiteRT on-device (Android) · Free tier (no API key required)

## Downloads

Get builds from [GitHub Releases](https://github.com/habibbrox2/brox-kai-2.0/releases/tag/v2.0.0). The release tag is technical metadata; the product name remains Brox Kai v2.0.

| Platform | Format |
|----------|--------|
| Android | `Brox-Kai-2.0-Android.apk` (FOSS) |
| Windows (x64) | `Brox-Kai-2.0-Windows-x64.msi`, `Brox-Kai-2.0-Windows-x64.zip` |

## Development

- **Build system:** Gradle (Kotlin DSL). See [`AGENTS.md`](AGENTS.md) for agent/build conventions and [`CLAUDE.md`](CLAUDE.md) for project-specific rules.
- **Feature specs:** [`docs/features/`](docs/features/) — product/behavior specs (update when changing feature logic).
- **Knowledge bundles:** [`docs/knowledge/`](docs/knowledge/) — OKF-style bundles for policy/snapshots (update both bundles and runtime sources).
- **Branding:** [`docs/branding/`](docs/branding/)

### Quick Start

```bash
# Verify Java 21
java -version

# Run tests
./gradlew :composeApp:test
./gradlew :composeApp:jvmTest

# Build
./gradlew :composeApp:assembleDebug        # Android
./gradlew :composeApp:wasmJsBrowserDistribution  # Web
```

## Upstream (Kai 9000)

Brox Kai v2.0 is forked from [Kai 9000](https://github.com/SimonSchubert/Kai) by [Simon Schubert](https://github.com/SimonSchubert).

- [Website](https://kai9000.com) · [Documentation](https://kai9000.com/docs/) · [Upstream repo](https://github.com/SimonSchubert/Kai) · [Upstream releases](https://github.com/SimonSchubert/Kai/releases)
- [App Store](https://apps.apple.com/us/app/kai-ai/id6758148023) · [Google Play](https://play.google.com/store/apps/details?id=com.inspiredandroid.kai) · [F-Droid](https://f-droid.org/en/packages/com.inspiredandroid.kai/) · [Web app](https://kai9000.com/app/)
- [Homebrew](https://brew.sh): `brew install --cask simonschubert/tap/kai` · [AUR](https://aur.archlinux.org): `yay -S kai-bin` · [Winget](https://learn.microsoft.com/windows/package-manager/winget): `winget install SimonSchubert.Kai`

## Credits

- Original Kai 9000 by [Simon Schubert](https://github.com/SimonSchubert)
- Brox Kai v2.0 fork by [habibbrox2](https://github.com/habibbrox2)
- [Mistral AI](https://mistral.ai/)
