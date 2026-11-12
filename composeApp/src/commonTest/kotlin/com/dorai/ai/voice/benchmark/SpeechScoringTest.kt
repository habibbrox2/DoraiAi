package com.dorai.ai.voice.benchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeechScoringTest {

    @Test
    fun `identical reference and hypothesis score zero`() {
        val score = SpeechScoring.score("আজ দুপুরে বাজার করতে যেতে হবে।", "আজ দুপুরে বাজার করতে যেতে হবে।")

        assertEquals(0.0, score.cer)
        assertEquals(0.0, score.wer)
    }

    @Test
    fun `latin text is case insensitive`() {
        assertEquals(0.0, SpeechScoring.cer("Hello World", "hello world"))
    }

    @Test
    fun `punctuation is ignored when comparing`() {
        assertEquals(0.0, SpeechScoring.cer("হ্যালো! বিশ্ব?", "হ্যালো বিশ্ব"))
        assertEquals(0.0, SpeechScoring.cer("আজ। আগামীকাল।", "আজ আগামীকাল"))
    }

    @Test
    fun `decomposed and precomposed text score equal after NFC`() {
        // "é" written as e + combining acute.
        assertEquals(0.0, SpeechScoring.cer("é", "é"))
    }

    @Test
    fun `whitespace is collapsed and trimmed`() {
        assertEquals("হ্যালো বিশ্ব", SpeechScoring.normalizeForScoring("  হ্যালো    বিশ্ব  "))
    }

    @Test
    fun `cer counts substitutions insertions and deletions`() {
        // one substitution in three code points
        assertEquals(1.0 / 3.0, SpeechScoring.cer("abc", "abd"), 1e-9)
        // one deletion in four code points
        assertEquals(1.0 / 4.0, SpeechScoring.cer("abcd", "abd"), 1e-9)
    }

    @Test
    fun `cer measures astral characters as one code point each`() {
        // 😀 and 😁 are supplementary-plane code points: a one-code-point substitution,
        // not two UTF-16 code units.
        assertEquals(1.0 / 3.0, SpeechScoring.cer("a😀b", "a😁b"), 1e-9)
    }

    @Test
    fun `wer counts word level edits`() {
        assertEquals(1.0 / 4.0, SpeechScoring.wer("the quick brown fox", "the quick brown"), 1e-9)
        assertEquals(2.0 / 3.0, SpeechScoring.wer("a b c", "x y c"), 1e-9)
    }

    @Test
    fun `empty reference scores zero when hypothesis is also empty`() {
        assertEquals(0.0, SpeechScoring.cer("", ""))
        assertEquals(0.0, SpeechScoring.wer("", ""))
    }

    @Test
    fun `empty reference with a non-empty hypothesis scores one`() {
        assertEquals(1.0, SpeechScoring.cer("", "abc"))
        assertEquals(1.0, SpeechScoring.wer("", "abc"))
    }

    @Test
    fun `levenshtein operates on lists`() {
        assertEquals(2, SpeechScoring.levenshtein(listOf("a", "b", "c"), listOf("x", "b", "y")))
        assertEquals(0, SpeechScoring.levenshtein(listOf("a"), listOf("a")))
    }

    @Test
    fun `bangladesh and west bengal accents share the same spelling`() {
        // Bengali is written the same; only pronunciation differs.
        assertEquals(0.0, SpeechScoring.cer("বাংলাদেশ", "বাংলাদেশ"))
    }
}
