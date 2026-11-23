package com.dorai.ai.data

/**
 * Device-independent policy for the Android per-app display locale.
 *
 * Android 13 (API 33) added `LocaleManager.setApplicationLocales`, which applies the app's
 * language to its Resources *before* the Activity is created. That is the only reliable way to
 * make Compose Resources resolve consistently on a cold start. The older `attachBaseContext`
 * wrap remains as a fallback below API 33 (and as an idempotent extra layer above it).
 *
 * Kept in commonMain with no platform calls so the cold-start resolution can be unit-tested.
 */
object AppLocalePolicy {
    const val PLATFORM_APP_LOCALES_API_LEVEL: Int = 33

    fun supportsPlatformAppLocales(apiLevel: Int): Boolean = apiLevel >= PLATFORM_APP_LOCALES_API_LEVEL

    /**
     * Returns a trimmed, plausible BCP-47 tag, or null for a malformed value. Never throws: a
     * corrupt preference must not break startup.
     */
    fun sanitizeLanguageTag(languageCode: String?): String? {
        val tag = languageCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val segments = tag.split("-")
        val language = segments.first()
        if (language.length !in 2..3 || !language.all { it.isLetter() }) return null
        if (segments.drop(1).any { segment ->
                segment.isEmpty() || segment.length > 8 || !segment.all { it.isLetterOrDigit() }
            }
        ) {
            return null
        }
        return tag
    }

    /**
     * Language to apply before Resources are created on a cold start.
     *
     * The platform value wins when present because the system has already applied it to
     * Resources; the persisted preference covers older devices and upgrades where the platform
     * value has not been populated yet.
     */
    fun resolveColdStartLanguage(
        storedLanguageCode: String?,
        platformLanguageTags: List<String>?,
    ): String? {
        val fromPlatform = platformLanguageTags?.firstNotNullOfOrNull { sanitizeLanguageTag(it) }
        val fromPreference = sanitizeLanguageTag(storedLanguageCode)
        return fromPlatform ?: fromPreference
    }
}
