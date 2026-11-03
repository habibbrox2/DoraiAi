package com.inspiredandroid.kai

import java.util.Locale

actual fun applyAppLanguage(languageCode: String) {
    try {
        Locale.setDefault(Locale.forLanguageTag(languageCode))
    } catch (_: Exception) {
        // ignore
    }
}
