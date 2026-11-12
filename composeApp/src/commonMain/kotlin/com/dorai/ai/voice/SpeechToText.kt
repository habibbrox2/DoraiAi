package com.dorai.ai.voice

/**
 * Speech-to-text for the voice turn.
 *
 * Common code only ever sees this interface, so the platform implementation can be
 * swapped. The feature plan's on-device sherpa-onnx binding drops in behind this same
 * contract without the chat pipeline noticing.
 */
interface SpeechToText {

    /** True when this device can recognise speech at all. */
    val isAvailable: Boolean

    /**
     * Starts a recognition session and suspends until it produces a transcript or fails.
     *
     * The call returns when the platform reports the final result. [stop] ends the
     * session early and resolves with whatever was heard; [cancel] discards it.
     */
    suspend fun listen(languageTag: String): SpeechToTextResult

    /** Ends the current session, keeping whatever was recognised so far. */
    fun stop()

    /** Aborts the current session and discards the result. */
    fun cancel()

    /** Releases the platform resources held by this engine. */
    fun release()
}

/** The outcome of a recognition session. */
sealed interface SpeechToTextResult {

    /** The engine recognised speech. */
    data class Transcript(val text: String) : SpeechToTextResult

    /** The engine could not produce a transcript. */
    data class Failure(val error: VoiceError) : SpeechToTextResult
}

/**
 * Creates the platform speech-to-text engine, or null when the platform has none.
 *
 * Only Android has an implementation today. The desktop, iOS and web targets return
 * null so the voice entry points stay hidden there, which is what the feature plan
 * scopes this iteration to.
 */
expect fun createSpeechToText(): SpeechToText?
