package com.dorai.ai.voice.benchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceEvalSetTest {

    private fun containsBengali(text: String): Boolean = text.any { it.code in 0x0980..0x09FF }

    private fun containsLatin(text: String): Boolean = text.any { it in 'a'..'z' || it in 'A'..'Z' }

    @Test
    fun `has the target size of sentences`() {
        assertEquals(200, VOICE_EVAL_SENTENCES.size)
    }

    @Test
    fun `every intent carries the same number of sentences`() {
        val counts = VOICE_EVAL_CATEGORY_COUNTS
        assertEquals(
            setOf(20),
            counts.values.toSet(),
            "each intent should carry 20 sentences",
        )
        assertEquals(EvalCategory.entries.toSet(), counts.keys)
    }

    @Test
    fun `sentence ids are unique`() {
        val ids = VOICE_EVAL_SENTENCES.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every sentence is speakable Bengali text`() {
        for (sentence in VOICE_EVAL_SENTENCES) {
            assertTrue(sentence.text.isNotBlank(), "${sentence.id} has blank text")
            assertTrue(sentence.text.length in 5..200, "${sentence.id} has odd length ${sentence.text.length}")
            assertTrue(containsBengali(sentence.text), "${sentence.id} contains no Bengali characters")
        }
    }

    @Test
    fun `the set exercises the code-mixed case`() {
        // The BengVoice dataset reports 24.2% of utterances mixing Bengali and English.
        // The subset selection keeps that property, so a fair number of sentences must
        // mix Latin in.
        val mixed = VOICE_EVAL_SENTENCES.count { containsLatin(it.text) }
        assertTrue(mixed >= 20, "expected at least 20 code-mixed sentences, got $mixed")
    }
}
