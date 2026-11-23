package com.dorai.ai

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import com.dorai.ai.data.AppLocalePolicy
import java.util.Locale

private const val LOCALE_PREFS = "dorai_locale"
private const val LOCALE_KEY = "app_language"

/**
 * Weak holder for the currently resumed/foreground Activity so platform code outside the
 * Activity can trigger a locale recreate. Set by `MainActivity` (androidApp module) in
 * onCreate/onDestroy.
 */
object DoraiLocaleActivityHolder {
    @Volatile
    var activity: Activity? = null
}

private fun storedLanguageCode(context: Context): String? = context.applicationContext
    .getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)
    .getString(LOCALE_KEY, null)

/**
 * Resolves the language before the Activity's Resources are created on a cold start.
 * Called from `MainActivity.attachBaseContext`.
 */
fun resolveAndroidColdStartLanguage(context: Context): String? {
    val platformTags = if (AppLocalePolicy.supportsPlatformAppLocales(Build.VERSION.SDK_INT)) {
        runCatching {
            platformLocaleManager(context)?.applicationLocales
                ?.toLanguageTags()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
        }.getOrNull()
    } else {
        null
    }
    return AppLocalePolicy.resolveColdStartLanguage(storedLanguageCode(context), platformTags)
}

/**
 * Brings the platform per-app locale in sync with the persisted preference. Android 13+ applies
 * `LocaleManager` locales to Resources before Activity creation, which is what makes Compose
 * Resources resolve consistently on a cold start (the `attachBaseContext` wrap alone is not
 * enough). Idempotent: never rewrites an already matching value.
 */
fun syncAndroidAppLocale(context: Context) {
    if (!AppLocalePolicy.supportsPlatformAppLocales(Build.VERSION.SDK_INT)) return
    try {
        val tag = storedLanguageCode(context)?.let { AppLocalePolicy.sanitizeLanguageTag(it) } ?: return
        val manager = platformLocaleManager(context) ?: return
        if (manager.applicationLocales?.toLanguageTags().orEmpty() != tag) {
            manager.setApplicationLocales(LocaleList.forLanguageTags(tag))
        }
    } catch (_: Exception) {
        // Best-effort; a locale switch must never crash the app.
    }
}

actual fun applyAndroidLocale(languageCode: String) {
    try {
        val tag = AppLocalePolicy.sanitizeLanguageTag(languageCode) ?: return
        val appContext = AppContextHolder.context
        appContext.getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LOCALE_KEY, tag)
            .apply()

        syncAndroidAppLocale(appContext)

        val activity = DoraiLocaleActivityHolder.activity ?: findActivity(appContext)
        val activeLanguage = activity?.resources?.configuration?.locales?.get(0)?.language
        if (activity != null && activeLanguage != Locale.forLanguageTag(tag).language) {
            activity.recreate()
        }
    } catch (_: Exception) {
        // Best-effort; a locale switch must never crash the app.
    }
}

// Only called behind an AppLocalePolicy.supportsPlatformAppLocales(SDK_INT) guard.
private fun platformLocaleManager(context: Context): LocaleManager? = context.getSystemService(LocaleManager::class.java)

private fun findActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
