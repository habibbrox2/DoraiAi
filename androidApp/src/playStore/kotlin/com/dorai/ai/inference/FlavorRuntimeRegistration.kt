package com.dorai.ai.inference

import android.content.Context
import android.util.Log

/**
 * Play Store flavor implementation of flavor-specific runtime registration.
 * Registers [AICoreRuntime] into the application's [LocalRuntimeRegistry].
 */
fun registerFlavorRuntimes(context: Context, registry: LocalRuntimeRegistry) {
    try {
        registry.register(AICoreRuntime(context.applicationContext))
    } catch (t: Throwable) {
        Log.w("AICoreRegistration", "Failed to register AICoreRuntime in Play Store build", t)
    }
}
