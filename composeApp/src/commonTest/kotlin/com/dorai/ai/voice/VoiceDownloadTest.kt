package com.dorai.ai.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceDownloadTest {

    @Test
    fun `storage gate approves when free space covers total plus 25 percent`() {
        val entry = findCatalogVoiceModelById("coqui-vits-bn-custom-female")!!
        val needed = entry.totalSizeBytes + entry.totalSizeBytes / 4

        assertTrue(storageGate(entry, needed) is StorageGateResult.Approved)
        val refused = storageGate(entry, needed - 1)
        assertTrue(refused is StorageGateResult.Refused)
        assertEquals(needed, refused.neededBytes)
    }

    @Test
    fun `resume offset is the partial length when it fits`() {
        assertEquals(5, resumeOffset(5, 100))
        assertEquals(0, resumeOffset(101, 100))
        assertEquals(0, resumeOffset(0, 100))
    }

    @Test
    fun `progress percent is bounded`() {
        assertEquals(0, voiceDownloadProgressPercent(0, 0))
        assertEquals(0, voiceDownloadProgressPercent(0, 100))
        assertEquals(50, voiceDownloadProgressPercent(50, 100))
        assertEquals(100, voiceDownloadProgressPercent(100, 100))
        assertEquals(100, voiceDownloadProgressPercent(1_000, 100))
    }
}
