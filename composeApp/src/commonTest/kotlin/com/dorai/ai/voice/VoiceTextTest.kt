package com.dorai.ai.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceTextTest {

    // -- code, links and URLs -------------------------------------------------

    @Test
    fun `drops fenced code blocks and keeps the prose around them`() {
        val text = """
            আগে একটা লাইন।
            ```kotlin
            val x = 1
            ```
            পরে আরেকটা লাইন।
        """.trimIndent()

        val spoken = sanitizeForSpeech(text)

        assertFalse("val x = 1" in spoken)
        assertTrue("আগে একটা লাইন।" in spoken)
        assertTrue("পরে আরেকটা লাইন।" in spoken)
    }

    @Test
    fun `drops inline code and keeps the surrounding words`() {
        assertEquals("ব্যবহার করুন", sanitizeForSpeech("`কোড` ব্যবহার করুন"))
    }

    @Test
    fun `keeps a link label and drops its target`() {
        assertEquals("ডকুমেন্ট পড়ুন", sanitizeForSpeech("[ডকুমেন্ট](https://example.com) পড়ুন"))
    }

    @Test
    fun `drops bare urls`() {
        val spoken = sanitizeForSpeech("এই লিঙ্ক দেখুন https://example.com/page?q=1")

        assertFalse("example.com" in spoken)
        assertTrue("এই লিঙ্ক দেখুন" in spoken)
    }

    @Test
    fun `drops a markdown image entirely`() {
        val spoken = sanitizeForSpeech("এখানে ![ছবি](https://example.com/a.png) দেখুন")

        assertFalse("example.com" in spoken)
        assertFalse("ছবি" in spoken)
        assertTrue("এখানে" in spoken)
    }

    // -- emoji and Unicode integrity -----------------------------------------

    @Test
    fun `removes emoji without touching the Bengali around them`() {
        val spoken = sanitizeForSpeech("বাংলা 😀 👩‍💻 ভালো আছে")

        assertFalse("😀" in spoken)
        assertFalse("👩" in spoken)
        assertFalse("💻" in spoken)
        assertTrue("বাংলা" in spoken)
        assertTrue("ভালো আছে" in spoken)
    }

    @Test
    fun `keeps a Bengali zero width joiner sequence intact`() {
        // The joiner is part of the text, not decoration: it must survive untouched.
        val word = "ক‍ষ"

        assertEquals(word, sanitizeForSpeech(word))
    }

    @Test
    fun `removes a supplementary plane emoji as one unit`() {
        // An emoji above U+FFFF is a surrogate pair in a Kotlin String. Dropping one
        // half would leave a broken character behind.
        val spoken = sanitizeForSpeech("a 😀 b")

        assertFalse("😀" in spoken)
        assertFalse(spoken.any { it.isSurrogate() })
    }

    @Test
    fun `keeps Bengali conjuncts and matras intact`() {
        val text = "বাংলাদেশ, স্বাধীনতা, রাষ্ট্র, কার্যক্রম ও প্রয়োজনীয়"

        assertEquals(text, sanitizeForSpeech(text))
    }

    // -- numbers ---------------------------------------------------------------

    @Test
    fun `reads numbers in prose as Bengali words`() {
        assertEquals("পাঁচ", sanitizeForSpeech("৫"))
        assertEquals("পাঁচটি বই", sanitizeForSpeech("৫টি বই"))
        assertEquals("পঞ্চাশ টাকা", sanitizeForSpeech("৫০ টাকা"))
        assertEquals("দুই হাজার ছাব্বিশ সাল", sanitizeForSpeech("২০২৬ সাল"))
    }

    @Test
    fun `reads ascii numbers in prose as Bengali words`() {
        assertEquals("পাঁচ", sanitizeForSpeech("5"))
        assertEquals("দুই হাজার ছাব্বিশ", sanitizeForSpeech("2026"))
    }

    @Test
    fun `reads a thousands separated number as one number`() {
        assertEquals("এক হাজার", sanitizeForSpeech("1,000"))
    }

    @Test
    fun `does not treat a Bengali year as a list marker`() {
        // "2026. নতুন বছর" is prose, not an ordered list item.
        assertEquals("২০২৬. নতুন বছর", sanitizeForSpeech("২০২৬. নতুন বছর"))
    }

    @Test
    fun `leaves identifiers, versions and currency amounts as written`() {
        assertEquals("v2.1", sanitizeForSpeech("v2.1"))
        assertEquals("৳৫০", sanitizeForSpeech("৳৫০"))
        assertEquals("1.5GB", sanitizeForSpeech("1.5GB"))
        assertEquals("HTTP/2.0", sanitizeForSpeech("HTTP/2.0"))
    }

    @Test
    fun `writes Bengali numbers using the Indian numbering system`() {
        assertEquals("শূন্য", bengaliNumberToWords(0))
        assertEquals("এক", bengaliNumberToWords(1))
        assertEquals("দুই", bengaliNumberToWords(2))
        assertEquals("দশ", bengaliNumberToWords(10))
        assertEquals("এগারো", bengaliNumberToWords(11))
        assertEquals("আঠারো", bengaliNumberToWords(18))
        assertEquals("উনিশ", bengaliNumberToWords(19))
        assertEquals("ছাব্বিশ", bengaliNumberToWords(26))
        assertEquals("ছেচল্লিশ", bengaliNumberToWords(46))
        assertEquals("নিরানব্বই", bengaliNumberToWords(99))
        assertEquals("একশত", bengaliNumberToWords(100))
        assertEquals("একশত পাঁচ", bengaliNumberToWords(105))
        assertEquals("একশত পঞ্চাশ", bengaliNumberToWords(150))
        assertEquals("এক হাজার", bengaliNumberToWords(1_000))
        assertEquals("দুই হাজার ছাব্বিশ", bengaliNumberToWords(2_026))
        assertEquals("এক লক্ষ", bengaliNumberToWords(100_000))
        assertEquals("এক লক্ষ পঞ্চাশ হাজার", bengaliNumberToWords(150_000))
        assertEquals("এক কোটি", bengaliNumberToWords(10_000_000))
    }

    // -- script boundaries -----------------------------------------------------

    @Test
    fun `separates a Bengali word from a Latin word that are glued together`() {
        assertEquals("কোড API ব্যবহার করুন", sanitizeForSpeech("কোডAPIব্যবহার করুন"))
    }

    // -- sentence splitting ----------------------------------------------------

    @Test
    fun `splits Bengali sentences on the danda`() {
        val sentences = splitSpeakableSentences("আজ সোমবার। কাল মঙ্গলবার।")

        assertEquals(listOf("আজ সোমবার।", "কাল মঙ্গলবার।"), sentences)
    }

    @Test
    fun `splits on question and exclamation marks`() {
        val sentences = splitSpeakableSentences("কী হলো? খুব ভালো! ঠিক আছে")

        assertEquals(listOf("কী হলো?", "খুব ভালো!", "ঠিক আছে"), sentences)
    }

    @Test
    fun `does not split on a decimal, a version or an abbreviation`() {
        assertEquals(listOf("সংস্করণ 2.1"), splitSpeakableSentences("সংস্করণ 2.1"))
        assertEquals(listOf("যেমন e.g. এভাবে"), splitSpeakableSentences("যেমন e.g. এভাবে"))
    }

    @Test
    fun `splits on a paragraph break`() {
        val sentences = splitSpeakableSentences("প্রথম অনুচ্ছেদ\n\nদ্বিতীয় অনুচ্ছেদ")

        assertEquals(listOf("প্রথম অনুচ্ছেদ", "দ্বিতীয় অনুচ্ছেদ"), sentences)
    }

    @Test
    fun `keeps a soft line break inside one sentence`() {
        val sentences = splitSpeakableSentences("প্রথম লাইন\nদ্বিতীয় লাইন")

        assertEquals(listOf("প্রথম লাইন দ্বিতীয় লাইন"), sentences)
    }

    // -- the whole pipeline ----------------------------------------------------

    @Test
    fun `prepares a Bengali answer for speaking`() {
        val answer = """
            ## ফলাফল

            আপনি `৫` টি ফাইল পেয়েছেন। **সব ঠিক আছে।**

            - প্রথম ধাপ শেষ
            - দ্বিতীয় ধাপ বাকি

            আরও জানতে https://example.com/docs দেখুন।
        """.trimIndent()

        val sentences = toVoiceSentences(answer)

        assertTrue(sentences.isNotEmpty())
        assertTrue(sentences.none { "example.com" in it })
        assertTrue(sentences.none { "#" in it })
        assertTrue(sentences.none { "`" in it })
        assertTrue(sentences.any { "ফলাফল" in it })
    }
}
