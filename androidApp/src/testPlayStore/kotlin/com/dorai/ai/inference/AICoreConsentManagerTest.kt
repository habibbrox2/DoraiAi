package com.dorai.ai.inference

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AICoreConsentManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var consentManager: AICoreConsentManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        consentManager = AICoreConsentManager(fakePrefs)
    }

    @Test
    fun defaultState_isNotGranted() {
        assertFalse(consentManager.isConsentGranted)
    }

    @Test
    fun grantConsent_setsGrantedToTrue() {
        consentManager.grantConsent()
        assertTrue(consentManager.isConsentGranted)
        assertTrue(fakePrefs.getBoolean(AICoreConsentManager.KEY_CONSENT_GRANTED, false))
    }

    @Test
    fun revokeConsent_setsGrantedToFalse() {
        consentManager.grantConsent()
        assertTrue(consentManager.isConsentGranted)

        consentManager.revokeConsent()
        assertFalse(consentManager.isConsentGranted)
        assertFalse(fakePrefs.getBoolean(AICoreConsentManager.KEY_CONSENT_GRANTED, false))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (map[key] as? MutableSet<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class FakeEditor(private val backingMap: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearFlag = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { pending[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { pending[key!!] = values }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { pending[key!!] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { pending[key!!] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { pending[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { pending[key!!] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { pending[key!!] = null }
            override fun clear(): SharedPreferences.Editor = apply { clearFlag = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearFlag) backingMap.clear()
                for ((k, v) in pending) {
                    if (v == null) backingMap.remove(k) else backingMap[k] = v
                }
            }
        }
    }
}
