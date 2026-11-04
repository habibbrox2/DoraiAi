# DORAI AI

<div align="center">

<img src="site/img/dorai-logo.svg" height="125" alt="DORAI AI logo">

**DORAI AI** ? A security-hardened AI assistant with persistent memory, built on Kotlin Multiplatform + Compose Multiplatform.

Runs on **Android, iOS, Windows, macOS, and Linux**. Web/Wasm support remains under development.

[Legacy website](https://kai9000.com) ? [Legacy documentation](https://kai9000.com/docs/) ? [Source](https://github.com/habibbrox2/DoraiAi) ? [DORAI release status](https://github.com/habibbrox2/DoraiAi)

<img src="https://img.shields.io/badge/Platform-Android-34a853.svg?logo=android" alt="Android"> <img src="https://img.shields.io/badge/Platform-iOS-lightgrey.svg?logo=apple" alt="iOS"> <img src="https://img.shields.io/badge/Platform-Desktop-JVM-e10707.svg?logo=openjdk" alt="Desktop JVM">

</div>

## What's Different

DORAI AI is a security-hardened fork of [Kai 9000](https://github.com/SimonSchubert/Kai).

### Security Hardening
- **MCP SSRF Protection** ? URL validation (scheme, IP ranges, credentials, encoding), connection-time re-validation, redirect checks (max 3 hops), and header sanitization.
- **Shell Command Hardening** ? Fail-closed allowlist, direct `ProcessBuilder` execution (no shell interpretation), rejection of metacharacters, path traversal, and dangerous flags.
- **Android Network Security** ? HTTPS-only by default; cleartext restricted to localhost, 127.0.0.1, and 10.0.2.2.
- **Android Backup Disabled** ? `allowBackup=false`; encrypted prefs and conversation DB excluded from backups.
- **Release Integrity** ? SHA-256 checksums for all release artifacts.

### Branding & UI
- DORAI brand colors (Navy + Cyan) applied across the UI.
- Updated logo and visual identity. Assets live in [`docs/branding/dorai/`](docs/branding/dorai/README.md) ? see [brand name usage](docs/branding/brand-name-usage.md).
- Chat, settings, accessibility, and cross-platform responsiveness improvements.

## Features

- **Persistent memory** ? Remembers important details across conversations and uses them automatically.
- **Customizable soul** ? Edit the system prompt to define personality and behavior.
- **Multi-service fallback** ? 30+ LLM providers with automatic failover.
- **On-device inference** ? Run models locally on Android with LiteRT (offline capable).
- **Tool execution** ? Web search, notifications, calendar events, shell commands, and more.
- **MCP server support** ? Connect to remote tool servers via MCP with SSRF protection.
- **Autonomous heartbeat** ? Periodic self-checks that surface items needing attention.
- **Settings import/export** ? Back up and restore settings as JSON.
- **Encrypted storage** ? Conversations stored locally with encryption.
- **Text-to-speech** ? Listen to AI responses.
- **Linux sandbox (Android)** ? Run shell commands/scripts in a secure sandboxed Linux environment.
- **Image attachments** ? Attach images to conversations.

## Supported Services

[Atlas Cloud](https://www.atlascloud.ai?utm_source=github&utm_medium=link&utm_campaign=DORAI) ? [Anthropic](https://console.anthropic.com) ? [OpenAI](https://openai.com) ? [Google Gemini](https://aistudio.google.com) ? [DeepSeek](https://www.deepseek.com) ? [Mistral](https://mistral.ai) ? [xAI](https://x.ai) ? [OpenRouter](https://openrouter.ai) ? [Requesty](https://requesty.ai) ? [Groq](https://groq.com) ? [NVIDIA](https://developer.nvidia.com) ? [Cerebras](https://cerebras.ai) ? [Ollama Cloud](https://ollama.com) ? [LongCat](https://longcat.chat) ? [Together AI](https://together.ai) ? [Hugging Face](https://huggingface.co) ? [Venice AI](https://venice.ai) ? [Moonshot AI](https://moonshot.cn) ? [Z.AI](https://z.ai) ? [MiniMax](https://minimax.io) ? [AIHubMix](https://aihubmix.com) ? [Deep Infra](https://deepinfra.com) ? [Fireworks AI](https://fireworks.ai) ? [OpenCode](https://opencode.ai) ? [AI Horde](https://stablehorde.net/) ? [1min.AI](https://1min.ai) ? [Perplexity](https://www.perplexity.ai) ? OpenAI-compatible APIs ? LiteRT on-device (Android) ? Free tier (no API key required)

## Downloads

**First DORAI AI release pending.** The renamed repository still contains legacy Kai releases; do not install those builds as DORAI AI. The first DORAI download will be published only after signing and update-manifest verification.

| Planned platform | Planned release artifact |
|----------|--------|
| Android | `DORAI-AI-v<version>-Android.apk` (FOSS) |
| Windows (x64) | `DORAI-AI-v<version>-windows.msi` |

## Development

- **Build system:** Gradle (Kotlin DSL). See [`AGENTS.md`](AGENTS.md) for agent/build conventions and [`CLAUDE.md`](CLAUDE.md) for project-specific rules.
- **Feature specs:** [`docs/features/`](docs/features/) ? product/behavior specs (update when changing feature logic).
- **Knowledge bundles:** [`docs/knowledge/`](docs/knowledge/) ? OKF-style bundles for policy/snapshots (update both bundles and runtime sources).
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
# Web/Wasm distribution is not release-ready yet.
```

## Hybrid Development Foundation

DORAI uses Kotlin Multiplatform + Compose Multiplatform as its primary application platform. A separate pnpm-managed TypeScript workspace is available for future web-facing modules and integrations. The two build systems remain independent; the existing Kotlin/Wasm target is not a TypeScript runtime.

- [System Architecture](docs/architecture/system-architecture.md)
- [Hybrid Architecture](docs/architecture/hybrid-architecture.md)
- [TypeScript Workspace Setup](docs/development/typescript-setup.md)
- [Hybrid Development Guide](docs/development/hybrid-development-guide.md)

Run TypeScript checks from the repository root with pnpm frontend:check. No existing Kotlin feature or TypeScript/JavaScript product source is migrated by this foundation.

## Upstream attribution and legacy distributions

DORAI AI is forked from [Kai 9000](https://github.com/SimonSchubert/Kai) by [Simon Schubert](https://github.com/SimonSchubert).

- [Legacy website](https://kai9000.com) ? [Legacy documentation](https://kai9000.com/docs/) ? [Upstream repo](https://github.com/SimonSchubert/Kai) ? [Upstream releases](https://github.com/SimonSchubert/Kai/releases)
- [App Store ? upstream](https://apps.apple.com/us/app/kai-ai/id6758148023) ? [Google Play ? upstream](https://play.google.com/store/apps/details?id=com.inspiredandroid.kai) ? [F-Droid ? upstream](https://f-droid.org/en/packages/com.inspiredandroid.kai/) ? [Web app ? upstream](https://kai9000.com/app/)
- [Homebrew ? upstream package](https://brew.sh): `brew install --cask simonschubert/tap/kai` ? [AUR ? upstream package](https://aur.archlinux.org): `yay -S kai-bin` ? [Winget ? upstream package](https://learn.microsoft.com/windows/package-manager/winget): `winget install SimonSchubert.Kai`

## Credits

- Original Kai 9000 by [Simon Schubert](https://github.com/SimonSchubert)
- DORAI AI fork by [habibbrox2](https://github.com/habibbrox2)
- [Mistral AI](https://mistral.ai/)
