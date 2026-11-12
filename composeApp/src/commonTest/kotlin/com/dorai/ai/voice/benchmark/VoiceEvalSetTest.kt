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

    @Test
    fun `structure set carries ten sentences per structure`() {
        val counts = VOICE_STRUCTURE_EVAL_COUNTS

        assertEquals(setOf(10), counts.values.toSet(), "each structure should carry 10 sentences")
        assertEquals(SentenceStructure.entries.toSet(), counts.keys)
    }

    @Test
    fun `structure ids are unique and distinct from the command ids`() {
        val commandIds = VOICE_EVAL_SENTENCES.map { it.id }.toSet()
        val ids = VOICE_STRUCTURE_EVAL_SENTENCES.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.none { it in commandIds }, "structure ids must not collide with command ids")
    }

    @Test
    fun `structure sentences are speakable Bengali text without code-mix`() {
        // The structure subset was selected pure-script so the structure effect is not
        // confounded with code-mixing.
        for (sentence in VOICE_STRUCTURE_EVAL_SENTENCES) {
            assertTrue(sentence.text.isNotBlank(), "${sentence.id} has blank text")
            assertTrue(sentence.text.length in 5..200, "${sentence.id} has odd length ${sentence.text.length}")
            assertTrue(containsBengali(sentence.text), "${sentence.id} contains no Bengali characters")
            assertTrue(!containsLatin(sentence.text), "${sentence.id} should be pure-script")
        }
    }
}
