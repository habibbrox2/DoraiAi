# Getting Started

## Installation

### App Stores

DORAI AI's official App Store, Google Play, and F-Droid listings have not been published yet. The legacy upstream listings install a different application and must not be used as DORAI downloads.

### Web

The DORAI AI web app is not published yet. Web/Wasm support remains under development.

### Package Managers

Homebrew, AUR, and Winget packages have not been registered for DORAI AI yet. Use the official [GitHub Releases](https://github.com/habibbrox2/DoraiAi/releases) page when the first release is published.

### Direct Downloads

The repository currently contains legacy Dorai Ai releases only. Those APKs and installers belong to a different application identity and must not be installed as DORAI AI. The first DORAI release will be published after production signing and update-manifest verification. Track the project at the [DORAI AI repository](https://github.com/habibbrox2/DoraiAi).

## First Steps

1. Launch DORAI AI - you'll see the chat screen with an animated welcome
2. Start chatting immediately using the **Free** tier (no API key needed)
3. For better models, open **Settings** and add a service (e.g. OpenAI, Gemini, DeepSeek)
4. Enter your API key - DORAI AI validates the connection and loads available models automatically
5. Prefer to stay offline? On Android, iOS and desktop, add the **Local Model** service and download an on-device model - see [On-Device Inference](features/on-device-inference.md)

## Adding a Service

1. Open Settings
2. Tap **Add Service** and pick a provider
3. Paste your API key
4. Select a model from the dropdown
5. Drag services to reorder - the first one is your primary, the rest are fallbacks

See [Multi-Service](features/multi-service.md) for the full details on providers and fallback behavior.
