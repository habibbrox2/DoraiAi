package com.dorai.ai

actual fun applyAndroidLocale(languageCode: String) {
    // wasmJs has no Android-style configuration context; browser locale drives rendering.
}
