package com.inspiredandroid.kai.tts

import com.inspiredandroid.kai.data.AppSettings
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError

interface TextToSpeechService {
    val isAvailable: Boolean
    suspend fun speak(text: String)
    fun stop()
    fun pause()
    fun resume()
}

class TextToSpeechServiceImpl(
    private val textToSpeech: TextToSpeechInstance?,
    private val appSettings: AppSettings? = null,
) : TextToSpeechService {
    override val isAvailable: Boolean = textToSpeech != null

    override suspend fun speak(text: String) {
        if (textToSpeech == null) return
        try {
            appSettings?.let {
                try {
                    textToSpeech.rate = it.getTtsRate()
                } catch (_: Exception) {
                }
                try {
                    textToSpeech.pitch = it.getTtsPitch()
                } catch (_: Exception) {
                }
                try {
                    textToSpeech.volume = (it.getTtsVolume() * 100).toInt()
                } catch (_: Exception) {
                }
            }
            textToSpeech.say(text = text)
        } catch (_: TextToSpeechSynthesisInterruptedError) {
            // Expected interruption
        } catch (_: Exception) {
            // Handle gracefully
        }
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun pause() {
        textToSpeech?.stop()
    }

    override fun resume() {
    }
}
