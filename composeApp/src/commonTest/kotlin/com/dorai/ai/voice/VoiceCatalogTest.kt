package com.dorai.ai.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceCatalogTest {

    @Test
    fun `every catalog URL is pinned to an immutable commit`() {
        for (model in VOICE_MODEL_CATALOG) {
            for (file in model.files) {
                val url = model.fileUrl(file)
                assertTrue(
                    isImmutableVoiceModelUrl(url),
                    "${model.id}/${file.name} is not pinned to an immutable commit: $url",
                )
            }
        }
    }

    @Test
    fun `every catalog file pins a non-blank digest and positive size`() {
        for (model in VOICE_MODEL_CATALOG) {
            for (file in model.files) {
                assertEquals(64, file.sha256.length, "digest length for ${file.name}")
                assertTrue(file.sha256.all { it in "0123456789abcdef" }, "digest hex for ${file.name}")
                assertTrue(file.sizeBytes > 0, "size for ${file.name}")
            }
        }
    }

    @Test
    fun `coqui model is downloadable after license sign-off is recorded`() {
        // The sign-off was recorded on 2026-11-13 by the project owner (all flavors,
        // accepting the upstream repo's silence on licensing). A pin bump or a new entry
        // must bring its own recorded sign-off.
        val model = findCatalogVoiceModelById("coqui-vits-bn-custom-female")!!

        assertTrue(model.licenseVerified, "sign-off must be recorded in the catalog")
        assertTrue(model.isDownloadable(), "download must be enabled after sign-off")
        for (entry in VOICE_MODEL_CATALOG) {
            assertTrue(entry.licenseVerified, "${entry.id} needs a recorded sign-off to be downloadable")
        }
    }

    @Test
    fun `coqui model pins the expected commit and total size`() {
        val model = findCatalogVoiceModelById("coqui-vits-bn-custom-female")!!

        assertEquals(114_295_636L, model.totalSizeBytes)
        assertEquals(114_287_329L, model.files.first { it.name == "model.onnx" }.sizeBytes)
    }

    @Test
    fun `digest marker name is stable`() {
        assertEquals("model.onnx.sha256", voiceModelMarkerFileName("model.onnx"))
    }
}
