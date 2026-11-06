package com.dorai.ai.inference

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user consent for Google Gemini Nano / Android AICore on-device inference.
 *
 * In accordance with Google Generative AI Additional Terms of Service, consent must be explicitly
 * given before the AICore runtime is activated, and may be revoked at any time.
 */
class AICoreConsentManager(
    private val prefs: SharedPreferences,
) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    var isConsentGranted: Boolean
        get() = prefs.getBoolean(KEY_CONSENT_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT_GRANTED, value).apply()

    fun grantConsent() {
        isConsentGranted = true
    }

    fun revokeConsent() {
        isConsentGranted = false
    }

    companion object {
        const val PREFS_NAME = "aicore_consent_prefs"
        const val KEY_CONSENT_GRANTED = "aicore_tos_consent_granted"
        const val TERMS_OF_SERVICE_URL = "https://policies.google.com/terms/generative-ai"
    }
}
