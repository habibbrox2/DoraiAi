package com.dorai.ai.inference

actual fun createLocalRuntimeRegistry(): LocalRuntimeRegistry = DefaultLocalRuntimeRegistry().apply {
    val engine = createLocalInferenceEngine()
    if (engine != null) {
        register(LiteRtRuntime(engine))
    }
}
