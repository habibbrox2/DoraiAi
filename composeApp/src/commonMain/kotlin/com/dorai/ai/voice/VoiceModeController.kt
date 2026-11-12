package com.dorai.ai.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives one voice turn: listen → transcribe → hand the transcript to the chat pipeline.
 *
 * The controller owns no conversation state. A transcript leaves this class as a plain
 * string and becomes an ordinary USER turn in the chat pipeline, exactly like a typed
 * message — a voice turn gets no extra tools, no extra network access, and no separate
 * transcript store.
 *
 * Speaking the answer stays with the app: the synthesizer lives next to the chat screen,
 * so this class only exposes [markSpeaking] and [markIdle] for it to report progress.
 */
class VoiceModeController(
    private val speechToText: SpeechToText?,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(VoiceModeState.IDLE)
    val state: StateFlow<VoiceModeState> = _state.asStateFlow()

    private val _error = MutableStateFlow<VoiceError?>(null)
    val error: StateFlow<VoiceError?> = _error.asStateFlow()

    private val _transcripts = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val transcripts: SharedFlow<String> = _transcripts.asSharedFlow()

    /** True when this device can run a voice turn at all. */
    val isSupported: Boolean = speechToText?.isAvailable == true

    private var listenJob: Job? = null

    /**
     * Starts listening. The returned transcript arrives on [transcripts] once the
     * platform engine has recognised speech.
     *
     * Does nothing when a turn is already running, or when the device has no speech
     * recognition service — the latter is reported through [error].
     */
    fun start(languageTag: String) {
        val engine = speechToText
        if (engine == null || !engine.isAvailable) {
            _error.value = VoiceError.RECOGNIZER_UNAVAILABLE
            return
        }
        if (listenJob?.isActive == true) return
        _error.value = null
        _state.value = VoiceModeState.LISTENING
        listenJob = scope.launch {
            when (val result = engine.listen(languageTag)) {
                is SpeechToTextResult.Transcript -> {
                    _state.value = VoiceModeState.THINKING
                    val text = result.text.trim()
                    if (text.isEmpty()) {
                        _state.value = VoiceModeState.IDLE
                        _error.value = VoiceError.NO_SPEECH_DETECTED
                    } else {
                        _transcripts.emit(text)
                    }
                }

                is SpeechToTextResult.Failure -> {
                    _state.value = VoiceModeState.IDLE
                    _error.value = result.error
                }
            }
        }
    }

    /** Ends the current session, keeping whatever was recognised so far. */
    fun stop() {
        speechToText?.stop()
    }

    /** Aborts the current turn. */
    fun cancel() {
        listenJob?.cancel()
        listenJob = null
        speechToText?.cancel()
        _state.value = VoiceModeState.IDLE
    }

    /** Reports that the assistant's answer is being spoken aloud. */
    fun markSpeaking() {
        _state.value = VoiceModeState.SPEAKING
    }

    /** Reports that the voice turn is over. */
    fun markIdle() {
        _state.value = VoiceModeState.IDLE
    }

    /** Releases the platform resources held by the speech engine. */
    fun release() {
        cancel()
        speechToText?.release()
    }
}
