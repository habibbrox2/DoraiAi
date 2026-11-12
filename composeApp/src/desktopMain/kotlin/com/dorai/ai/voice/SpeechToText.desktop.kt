package com.dorai.ai.voice

/**
 * Desktop has no speech recognition service, so the voice entry points stay hidden.
 * The feature plan scopes this iteration to Android.
 */
actual fun createSpeechToText(): SpeechToText? = null
