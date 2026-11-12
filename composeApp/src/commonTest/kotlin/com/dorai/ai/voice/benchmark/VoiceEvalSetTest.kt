package com.dorai.ai.voice.benchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceEvalSetTest {

    private fun containsBengali(text: String): Boolean = text.any { it.code in 0x0980..0x09FF }

    @Test
    fun `has the target size of sentences`() {
        assertEquals(120, VOICE_EVAL_SENTENCES.size)
    }

    @Test
    fun `every category carries the same number of sentences`() {
        val counts = VOICE_EVAL_CATEGORY_COUNTS
        assertEquals(
            setOf(20),
            counts.values.toSet(),
            "each category should carry 20 sentences",
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
    fun `every sentence exercises its own category`() {
        for (sentence in VOICE_EVAL_SENTENCES) {
            when (sentence.category) {
                EvalCategory.NUMBERS -> assertTrue(
                    sentence.text.any { it.isDigit() } || sentence.text.any { it.code in 0x09E6..0x09EF },
                    "${sentence.id} has no digits or Bengali numerals",
                )

                EvalCategory.MIXED_CODE_SWITCH -> assertTrue(
                    sentence.text.any { it in 'a'..'z' || it in 'A'..'Z' },
                    "${sentence.id} mixes no Latin text",
                )

                else -> Unit
            }
        }
    }
}
