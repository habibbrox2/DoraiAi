package com.dorai.ai

actual fun applyAndroidLocale(languageCode: String) {
    // iOS resolves the preferred language from NSLocale by the OS; a full language switch
    // requires restarting the app. The app re-reads Locale.setDefault for new lookups.
    // Known limitation; tracked in the remediation plan (docs/superpowers/plans/2026-11-13-ui-bangla-locale-remediation.md).
}
