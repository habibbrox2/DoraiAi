package com.dorai.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.java.KoinJavaComponent.inject
import kotlin.coroutines.resume

/**
 * The platform speech recognizer.
 *
 * The engine is the device's own speech service, so there is no model to download and
 * no native dependency to ship. Whether it can work offline depends on the device: the
 * recognizer is asked to prefer its offline model, and a device that has no offline
 * Bengali model reports a network error instead of silently sending audio away — the
 * voice turn then fails honestly rather than pretending to be private.
 *
 * The recognizer is created, started and stopped on the main thread, because the
 * platform requires it. Every callback arrives on that same thread, so the pending
 * result is read and cleared in one place.
 */
internal class AndroidSpeechToText(private val context: Context) : SpeechToText {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null

    /** Resolver of the session that is running, or null when no session is active. */
    private var activeResult: ((SpeechToTextResult) -> Unit)? = null

    /** The best text heard so far, used when the engine ends the session without a result. */
    private var partialText: String? = null

    override val isAvailable: Boolean
        get() = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }

    override suspend fun listen(languageTag: String): SpeechToTextResult = suspendCancellableCoroutine { continuation ->
        mainHandler.post {
            if (!continuation.isActive) return@post
            if (!isAvailable) {
                continuation.resume(SpeechToTextResult.Failure(VoiceError.RECOGNIZER_UNAVAILABLE))
                return@post
            }
            if (activeResult != null) {
                continuation.resume(SpeechToTextResult.Failure(VoiceError.BUSY))
                return@post
            }
            val engine = obtainRecognizer()
            if (engine == null) {
                continuation.resume(SpeechToTextResult.Failure(VoiceError.RECOGNIZER_UNAVAILABLE))
                return@post
            }
            partialText = null
            activeResult = { result -> if (continuation.isActive) continuation.resume(result) }
            engine.setRecognitionListener(listener)
            try {
                engine.startListening(buildIntent(languageTag))
            } catch (_: Exception) {
                finish(SpeechToTextResult.Failure(VoiceError.UNKNOWN))
            }
        }
    }

    override fun stop() {
        mainHandler.post {
            val engine = recognizer ?: return@post
            if (activeResult == null) return@post
            try {
                engine.stopListening()
            } catch (_: Exception) {
                finish(SpeechToTextResult.Failure(VoiceError.UNKNOWN))
                return@post
            }
            // Not every engine reports a result after stopListening(). Settle the turn
            // with the best partial text so a voice turn can never hang.
            mainHandler.postDelayed({
                if (activeResult == null) return@postDelayed
                val partial = partialText
                finish(
                    if (partial.isNullOrBlank()) {
                        SpeechToTextResult.Failure(VoiceError.NO_SPEECH_DETECTED)
                    } else {
                        SpeechToTextResult.Transcript(partial)
                    },
                )
            }, STOP_SETTLE_DELAY_MS)
        }
    }

    override fun cancel() {
        mainHandler.post {
            if (activeResult != null) {
                finish(SpeechToTextResult.Failure(VoiceError.CANCELLED))
            }
            try {
                recognizer?.cancel()
            } catch (_: Exception) {
                // Nothing to cancel.
            }
        }
    }

    override fun release() {
        mainHandler.post {
            activeResult = null
            try {
                recognizer?.destroy()
            } catch (_: Exception) {
                // Already destroyed.
            }
            recognizer = null
        }
    }

    private fun obtainRecognizer(): SpeechRecognizer? {
        recognizer?.let { return it }
        return try {
            SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildIntent(languageTag: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Ask for the on-device model. A device without one reports a network error,
        // which the caller surfaces instead of quietly streaming the audio away.
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }

    /** Resolves the pending session exactly once. */
    private fun finish(result: SpeechToTextResult) {
        val resolver = activeResult ?: return
        activeResult = null
        resolver(result)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            finish(SpeechToTextResult.Failure(mapError(error)))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            finish(
                if (text.isNullOrBlank()) {
                    SpeechToTextResult.Failure(VoiceError.NO_SPEECH_DETECTED)
                } else {
                    SpeechToTextResult.Transcript(text)
                },
            )
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) partialText = text
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapError(error: Int): VoiceError = when (error) {
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> VoiceError.NETWORK_REQUIRED

        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> VoiceError.NO_SPEECH_DETECTED

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION_DENIED

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.BUSY

        SpeechRecognizer.ERROR_CLIENT -> VoiceError.CANCELLED

        else -> VoiceError.UNKNOWN
    }

    private companion object {
        const val STOP_SETTLE_DELAY_MS = 400L
    }
}

/**
 * Creates the Android speech engine. Returns null when Koin is not running, so a
 * screenshot or unit-test environment without a container degrades to "no voice"
 * instead of crashing.
 */
actual fun createSpeechToText(): SpeechToText? = try {
    val context: Context by inject(Context::class.java)
    AndroidSpeechToText(context.applicationContext)
} catch (_: Throwable) {
    null
}
