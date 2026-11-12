package com.dorai.ai.voice.benchmark

// The web target is not release-ready and voice is disabled there, so this is a plain
// pass-through rather than a JS NFC interop call. Android, desktop and iOS apply real
// NFC, which is what the Phase 0 measurement uses.
internal actual fun normalizeNfc(text: String): String = text
