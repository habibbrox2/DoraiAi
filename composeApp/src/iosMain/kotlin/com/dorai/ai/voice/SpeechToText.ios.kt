package com.dorai.ai.voice

/**
 * iOS has no speech recognition service wired up yet, so the voice entry points stay
 * hidden. The feature plan scopes this iteration to Android.
 */
actual fun createSpeechToText(): SpeechToText? = null
