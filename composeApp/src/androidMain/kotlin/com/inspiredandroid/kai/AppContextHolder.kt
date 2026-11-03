package com.inspiredandroid.kai

import android.content.Context

/**
 * Holds the application context for platform-level helpers that run outside of Compose
 * (e.g. SIM operator detection). Initialized from KaiApplication.onCreate().
 */
object AppContextHolder {
    lateinit var context: Context
    val isInitialized: Boolean get() = ::context.isInitialized
}
