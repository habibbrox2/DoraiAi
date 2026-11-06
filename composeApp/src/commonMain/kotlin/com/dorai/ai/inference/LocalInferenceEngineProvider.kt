package com.dorai.ai.inference

expect fun createLocalInferenceEngine(): LocalInferenceEngine?

/**
 * Creates the registry of on-device runtimes available on this platform.
 *
 * The registry is populated by platform source sets: each platform registers the runtimes
 * it supports (e.g. LiteRT-LM on Android/Desktop/iOS). Platforms without an on-device
 * runtime return an empty registry.
 *
 * Wiring this into [com.dorai.ai.AppModule] makes runtimes available to the routing layer
 * without changing [RemoteDataRepository] or any existing gateway behavior.
 */
expect fun createLocalRuntimeRegistry(): LocalRuntimeRegistry
