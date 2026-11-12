package com.dorai.ai

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Weak holder for the currently resumed/foreground Activity so platform code outside the
 * Activity can trigger a locale recreate. Set by `MainActivity` (androidApp module) in
 * onCreate/onDestroy.
 */
object DoraiLocaleActivityHolder {
    @Volatile
    var activity: Activity? = null
}

actual fun applyAndroidLocale(languageCode: String) {
    try {
        val appContext = AppContextHolder.context
        // Persist for attachBaseContext on the next cold start. Not a secret.
        appContext.getSharedPreferences("dorai_locale", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", languageCode)
            .apply()

        val activity = DoraiLocaleActivityHolder.activity ?: findActivity(appContext)
        if (activity != null && activity.resources.configuration.locales.get(0).language != languageCode) {
            activity.recreate()
        }
    } catch (_: Exception) {
        // Best-effort; a locale switch must never crash the app.
    }
}

private fun findActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
