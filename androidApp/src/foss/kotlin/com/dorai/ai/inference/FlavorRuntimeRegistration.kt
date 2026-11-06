package com.dorai.ai.inference

import android.content.Context

/**
 * FOSS flavor implementation: no-op.
 * Strictly guarantees zero inclusion and zero registration of proprietary AICore / Play Services runtimes.
 */
fun registerFlavorRuntimes(context: Context, registry: LocalRuntimeRegistry) {
    // No proprietary runtimes registered in FOSS builds
}
