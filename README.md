# DORAI AI

<div align="center">

<img src="site/img/logo_animation.svg" height="125" alt="DORAI AI logo">

**Your AI. Your World.**

**A Hybrid AI Assistant & Agent Platform** for chat, coding, research, automation, and work across local and cloud environments.

[Repository](https://github.com/habibbrox2/DoraiAi) · [Documentation](docs/) · [Feature specifications](docs/features/)

<img src="https://img.shields.io/badge/Platform-Android-34a853.svg?logo=android" alt="Android">
<img src="https://img.shields.io/badge/Platform-iOS-lightgrey.svg?logo=apple" alt="iOS">
<img src="https://img.shields.io/badge/Platform-Desktop-JVM-e10707.svg?logo=openjdk" alt="Desktop JVM">

</div>

## What is DORAI AI?

DORAI AI brings conversational AI and agentic workflows together in one cross-platform application. Use it to talk with AI models, work with tools, develop software, research topics, and automate tasks—with a choice of cloud services and supported local execution.

The project is built primarily with **Kotlin Multiplatform** and **Compose Multiplatform**. Its architecture is designed to support Android, iOS, Windows, macOS, and Linux. Platform capabilities and build readiness may vary; Web/Wasm distribution is still under development.

## What you can do

- **Chat** — Work with multiple AI providers from a unified conversation interface.
- **AI agents** — Connect model responses with tools and multi-step workflows.
- **Coding** — Use AI-assisted development capabilities and supported execution tools.
- **Research** — Explore questions and information with connected AI services and tools.
- **Automation** — Build workflows around supported tools, scheduled checks, and agent capabilities.
- **Local and cloud AI** — Choose supported cloud providers or run compatible models locally on Android using LiteRT.
- **Persistent memory** — Retain useful conversation context across sessions.
- **MCP integrations** — Connect to Model Context Protocol servers with URL and network protections.
- **Voice and media** — Use text-to-speech and image attachments where supported.
- **Privacy-conscious storage** — Keep conversation data locally with the application's storage protections.

> Feature availability depends on the platform, configured provider, permissions, and execution environment.

## AI providers and model services

DORAI AI supports a broad range of model services, including:

[OpenAI](https://openai.com) · [Anthropic](https://www.anthropic.com) · [Google Gemini](https://ai.google.dev) · [DeepSeek](https://www.deepseek.com) · [Mistral](https://mistral.ai) · [xAI](https://x.ai) · [OpenRouter](https://openrouter.ai) · [Groq](https://groq.com) · [NVIDIA](https://developer.nvidia.com) · [Cerebras](https://cerebras.ai) · [Ollama](https://ollama.com) · [Together AI](https://www.together.ai) · [Hugging Face](https://huggingface.co) · [Fireworks AI](https://fireworks.ai) · [Perplexity](https://www.perplexity.ai) · OpenAI-compatible APIs · LiteRT on-device models

Provider availability, model catalogs, and free-tier access can change. Check each provider's terms and current model availability.

## Security and privacy

Security is a core engineering focus. Current protections include:

- **MCP request validation** — Validate URLs and destinations, re-check connections, constrain redirects, and sanitize forwarded headers.
- **Shell execution controls** — Use a fail-closed command policy and direct process execution rather than shell-string interpretation.
- **Android network security** — Restrict cleartext traffic to explicitly permitted local development endpoints.
- **Backup controls** — Disable Android app backup and exclude protected application data from backup rules.
- **Release integrity** — Verify release artifact checksums and updater metadata before installation.

These controls reduce specific attack surfaces; they do not guarantee that every configuration, provider, tool, or execution environment is risk-free. Review the source and security documentation before enabling powerful tools.

## Platforms

| Platform | Project status |
|---|---|
| Android | Primary mobile target |
| iOS | Kotlin Multiplatform target |
| Windows | Desktop target |
| macOS | Desktop target |
| Linux | Desktop target |
| Web/Wasm | Under development; not currently a release target |

## Downloads and release status

**The first DORAI AI release is not published yet.**

This repository retains historical upstream release entries. Those legacy Kai builds are not DORAI AI releases and should not be installed as DORAI AI.

The first DORAI release will be announced here after the release build, signing identity, package metadata, and update manifest have been verified. Until then, there are no official DORAI AI binaries to download.

## Build and development

DORAI AI uses Gradle and Kotlin Multiplatform. A separate pnpm-managed TypeScript workspace supports future web-facing modules and integrations; it does not replace the Kotlin application runtime.

### Requirements

- JDK 21
- Git
- Android SDK for Android builds
- Node.js and pnpm for TypeScript workspace checks

### Quick start

```bash
# Clone the repository
git clone https://github.com/habibbrox2/DoraiAi.git
cd DoraiAi

# Check the JDK
java -version

# Run the Kotlin tests
./gradlew :composeApp:test
./gradlew :composeApp:desktopTest

# Build the Android debug variant (foss flavor, default)
./gradlew :androidApp:assembleFossDebug

# Run TypeScript workspace checks
pnpm frontend:check
```

Web/Wasm distribution is not release-ready. See the development guides before attempting platform-specific builds.

## Project documentation

- [System architecture](docs/architecture/system-architecture.md)
- [Hybrid architecture](docs/architecture/hybrid-architecture.md)
- [Feature specifications](docs/features/)
- [TypeScript workspace setup](docs/development/typescript-setup.md)
- [Hybrid development guide](docs/development/hybrid-development-guide.md)
- [Brand assets and guidelines](docs/branding/dorai/README.md)
- [Brand name usage](docs/branding/brand-name-usage.md)

See [AGENTS.md](AGENTS.md) for repository conventions and [CLAUDE.md](CLAUDE.md) for project-specific development guidance.


## Credits

- DORAI AI project: [habibbrox2](https://github.com/habibbrox2)
- [Mistral AI](https://mistral.ai/)
