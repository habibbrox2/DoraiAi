package com.dorai.ai.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceModeControllerTest {

    private class FakeSpeechToText(
        override val isAvailable: Boolean = true,
        private val result: SpeechToTextResult = SpeechToTextResult.Transcript("হ্যালো"),
    ) : SpeechToText {
        var stopped = false
        var cancelled = false
        var released = false
        var languageTag: String? = null

        override suspend fun listen(languageTag: String): SpeechToTextResult {
            this.languageTag = languageTag
            return result
        }

        override fun stop() {
            stopped = true
        }

        override fun cancel() {
            cancelled = true
        }

        override fun release() {
            released = true
        }
    }

    @Test
    fun `reports the transcript of a completed turn`() = runTest {
        val engine = FakeSpeechToText()
        // An eager dispatcher makes the turn run to completion inside `start`, so the
        // assertions below do not depend on scheduler timing.
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val controller = VoiceModeController(engine, scope)
        val transcripts = mutableListOf<String>()
        val collector = scope.launch { controller.transcripts.collect { transcripts += it } }

        controller.start("bn")

        assertEquals(listOf("হ্যালো"), transcripts)
        assertEquals("bn", engine.languageTag)
        assertEquals(VoiceModeState.THINKING, controller.state.value)
        collector.cancel()
    }

    @Test
    fun `reports no speech when the transcript is empty`() = runTest {
        val engine = FakeSpeechToText(result = SpeechToTextResult.Transcript("   "))
        val controller = VoiceModeController(engine, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.start("bn")

        assertEquals(VoiceError.NO_SPEECH_DETECTED, controller.error.value)
        assertEquals(VoiceModeState.IDLE, controller.state.value)
    }

    @Test
    fun `reports the failure of the recognizer`() = runTest {
        val engine = FakeSpeechToText(
            result = SpeechToTextResult.Failure(VoiceError.NETWORK_REQUIRED),
        )
        val controller = VoiceModeController(engine, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.start("bn")

        assertEquals(VoiceError.NETWORK_REQUIRED, controller.error.value)
        assertEquals(VoiceModeState.IDLE, controller.state.value)
    }

    @Test
    fun `reports an unavailable recognizer`() = runTest {
        val engine = FakeSpeechToText(isAvailable = false)
        val controller = VoiceModeController(engine, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.start("bn")

        assertEquals(VoiceError.RECOGNIZER_UNAVAILABLE, controller.error.value)
        assertFalse(controller.isSupported)
    }

    @Test
    fun `reports an unavailable recognizer when the platform has none`() = runTest {
        val controller = VoiceModeController(null, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.start("bn")

        assertEquals(VoiceError.RECOGNIZER_UNAVAILABLE, controller.error.value)
        assertFalse(controller.isSupported)
    }

    @Test
    fun `stops and cancels the engine`() = runTest {
        val engine = FakeSpeechToText()
        val controller = VoiceModeController(engine, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.stop()
        assertTrue(engine.stopped)

        controller.cancel()
        assertTrue(engine.cancelled)
        assertEquals(VoiceModeState.IDLE, controller.state.value)
    }

    @Test
    fun `releases the engine`() = runTest {
        val engine = FakeSpeechToText()
        val controller = VoiceModeController(engine, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.release()

        assertTrue(engine.released)
    }

    @Test
    fun `marks the turn as speaking and back to idle`() = runTest {
        val controller = VoiceModeController(FakeSpeechToText(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        controller.markSpeaking()
        assertEquals(VoiceModeState.SPEAKING, controller.state.value)

        controller.markIdle()
        assertEquals(VoiceModeState.IDLE, controller.state.value)
    }
}
