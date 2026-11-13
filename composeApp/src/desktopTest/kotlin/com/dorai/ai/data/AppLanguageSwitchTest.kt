package com.dorai.ai.data

import com.dorai.ai.applyAppLanguage
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Language-switch behaviour that can be verified without a device.
 *
 * The app applies the selected language in two steps (see `App.kt`):
 *  1. `applyAppLanguage(code)` — sets the platform default locale. This is the JVM/Desktop
 *     path and is verifiable here; on Android the same call also feeds
 *     `applyAndroidLocale`, which recreates the activity so Compose Resources re-resolve.
 *  2. `applyAndroidLocale(code)` — Android only: persists the choice and recreates the
 *     resumed activity. It cannot be exercised without a device and is covered by the
 *     device checklist instead.
 *
 * These tests pin the JVM part of the switch so a regression in language handling is
 * caught in CI rather than on a phone.
 */
class AppLanguageSwitchTest {

    private val original: Locale = Locale.getDefault()

    @AfterTest
    fun restoreDefaultLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun applyingBengaliSetsTheDefaultLocaleToBengali() {
        applyAppLanguage(AppLanguage.Bengali.code)

        val locale = Locale.getDefault()
        assertEquals("bn", locale.language)
        assertEquals("বাংলা", AppLanguage.Bengali.displayName)
    }

    @Test
    fun applyingEnglishSetsTheDefaultLocaleToEnglish() {
        applyAppLanguage(AppLanguage.English.code)

        assertEquals("en", Locale.getDefault().language)
    }

    @Test
    fun switchingLanguagesTwiceEndsAtTheLastSelection() {
        applyAppLanguage(AppLanguage.Bengali.code)
        assertEquals("bn", Locale.getDefault().language)

        applyAppLanguage(AppLanguage.English.code)
        assertEquals("en", Locale.getDefault().language)
    }

    @Test
    fun unknownLanguageCodeDoesNotCrashTheSwitch() {
        // A language code the platform does not know must not throw: the switch runs inside
        // a best-effort block in App.kt, but the platform call itself has to stay safe.
        applyAppLanguage("not-a-locale")

        assertTrue(true, "applyAppLanguage must not throw for an unknown code")
    }

    @Test
    fun everySupportedLanguageHasAResourceDirectory() {
        val resDir = listOf(
            java.io.File("src/commonMain/composeResources"),
            java.io.File("composeApp/src/commonMain/composeResources"),
            java.io.File(".").canonicalFile.parentFile
                ?.let { java.io.File(it, "composeApp/src/commonMain/composeResources") },
        ).filterNotNull().firstOrNull { it.isDirectory }

        assertTrue(resDir != null, "Cannot locate composeResources")

        for (language in AppLanguage.entries) {
            // English is the default locale and lives in values/; other locales use their own
            // values-<code> directory.
            val dir = if (language == AppLanguage.English) {
                java.io.File(resDir, "values")
            } else {
                java.io.File(resDir, "values-${language.code}")
            }
            assertTrue(dir.isDirectory, "Missing resource directory for ${language.code}")
            assertTrue(
                java.io.File(dir, "strings.xml").isFile,
                "Missing strings.xml for ${language.code}",
            )
        }
    }
}
