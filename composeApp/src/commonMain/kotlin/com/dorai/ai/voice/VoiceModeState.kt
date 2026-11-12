package com.dorai.ai.voice

/**
 * Where a voice turn currently is.
 *
 * A voice turn is: listen → transcribe → answer → speak. The first two steps belong to
 * the platform speech engine, the last two to the chat pipeline and the synthesizer.
 * The state is shared so the UI renders one consistent picture of the turn no matter
 * which step is running.
 */
enum class VoiceModeState {
    /** No voice turn in progress. */
    IDLE,

    /** The microphone is open and the platform engine is capturing speech. */
    LISTENING,

    /** The engine has the audio and is producing a transcript. */
    TRANSCRIBING,

    /** The transcript was sent to the chat pipeline and the answer is being produced. */
    THINKING,

    /** The assistant's answer is being spoken aloud. */
    SPEAKING,
}

/** Why a voice turn ended without producing a transcript. */
enum class VoiceError {
    /** The user declined the microphone permission. */
    PERMISSION_DENIED,

    /** This device has no speech recognition service, or it is disabled. */
    RECOGNIZER_UNAVAILABLE,

    /** The engine heard nothing it could turn into text. */
    NO_SPEECH_DETECTED,

    /** Recognition needs a network connection the device does not have. */
    NETWORK_REQUIRED,

    /** Another recognition session was already running. */
    BUSY,

    /** The user cancelled the turn. */
    CANCELLED,

    /** Anything else the platform reported. */
    UNKNOWN,
}
