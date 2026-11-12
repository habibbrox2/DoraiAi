package com.dorai.ai.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `no catalog model is downloadable before license sign-off`() {
        for (model in VOICE_MODEL_CATALOG) {
            assertFalse(
                model.isDownloadable(),
                "${model.id} must stay undownloadable until its license is signed off",
            )
            assertTrue(model.licenseVerified.not(), "${model.id} must not carry licenseVerified=true before sign-off")
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
