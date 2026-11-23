package com.dorai.ai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression coverage for the Android cold-start locale policy.
 *
 * The device-side bug was that `attachBaseContext` alone did not make Compose Resources resolve
 * the app language consistently: a cold start could mix English and Bengali strings in one
 * screen. The fix is to publish the persisted choice through LocaleManager on API 33+ and to
 * resolve the cold-start language from that authoritative source. This suite pins the pure part
 * of that policy so it cannot silently regress.
 */
class AppLocalePolicyTest {

    @Test
    fun platformAppLocalesRequireApi33() {
        assertFalse(AppLocalePolicy.supportsPlatformAppLocales(32))
        assertTrue(AppLocalePolicy.supportsPlatformAppLocales(33))
        assertTrue(AppLocalePolicy.supportsPlatformAppLocales(37))
    }

    @Test
    fun sanitizeAcceptsSupportedLanguageTags() {
        assertEquals("bn", AppLocalePolicy.sanitizeLanguageTag("bn"))
        assertEquals("en", AppLocalePolicy.sanitizeLanguageTag(" en "))
        assertEquals("bn-BD", AppLocalePolicy.sanitizeLanguageTag("bn-BD"))
    }

    @Test
    fun sanitizeRejectsMalformedValues() {
        assertNull(AppLocalePolicy.sanitizeLanguageTag(null))
        assertNull(AppLocalePolicy.sanitizeLanguageTag(""))
        assertNull(AppLocalePolicy.sanitizeLanguageTag("b"))
        assertNull(AppLocalePolicy.sanitizeLanguageTag("123"))
        assertNull(AppLocalePolicy.sanitizeLanguageTag("en-"))
        assertNull(AppLocalePolicy.sanitizeLanguageTag("en-!!!"))
    }

    @Test
    fun coldStartUsesPersistedLanguageWhenPlatformHasNoLocales() {
        // Pre-API-33 devices and upgrades: only the preference exists.
        assertEquals(
            "bn",
            AppLocalePolicy.resolveColdStartLanguage("bn", null),
        )
        assertEquals(
            "bn",
            AppLocalePolicy.resolveColdStartLanguage("bn", emptyList()),
        )
    }

    @Test
    fun coldStartPrefersPlatformLocalesOverThePersistedValue() {
        // The platform value is what the system already applied to Resources; it must win.
        assertEquals(
            "bn",
            AppLocalePolicy.resolveColdStartLanguage("bn", listOf("bn")),
        )
        assertEquals(
            "en-US",
            AppLocalePolicy.resolveColdStartLanguage("bn", listOf("en-US")),
        )
    }

    @Test
    fun coldStartUsesPlatformLocaleWhenNoPreferenceIsStored() {
        assertEquals(
            "bn",
            AppLocalePolicy.resolveColdStartLanguage(null, listOf("bn")),
        )
    }

    @Test
    fun coldStartIgnoresInvalidValuesFromEitherSource() {
        assertEquals(
            "bn",
            AppLocalePolicy.resolveColdStartLanguage("b", listOf("bn")),
        )
        assertEquals(
            "en",
            AppLocalePolicy.resolveColdStartLanguage("en", listOf("x", "")),
        )
        assertNull(AppLocalePolicy.resolveColdStartLanguage(null, null))
        assertNull(AppLocalePolicy.resolveColdStartLanguage("???", listOf("")))
    }
}
