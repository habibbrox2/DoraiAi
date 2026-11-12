package com.dorai.ai.voice

/**
 * An assistant reply that a voice turn produced and that has not been spoken yet.
 *
 * The reply is identified by its history id so the UI can show it as the message that
 * is currently being spoken.
 */
data class VoiceReply(
    val id: String,
    val text: String,
)
