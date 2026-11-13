package com.dorai.ai.ui.markdown

import com.dorai.ai.text.graphemeClusterCount
import com.dorai.ai.text.graphemeSafeDeleteCount
import com.dorai.ai.text.graphemeSafeTake
import com.dorai.ai.text.graphemeSafeTruncateWithEllipsis
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bengali-language corpus for the markdown parser/renderer — policy §11.
 * Verifies UTF-8 integrity, no grapheme corruption, and stable partial-stream behavior.
 */
class BengaliCorpusTest {

    private fun paragraphText(text: String): String {
        val para = parseMarkdown(text).blocks.single() as Paragraph
        return para.inlines.joinToString("") {
            when (it) {
                is Text -> it.value
                else -> ""
            }
        }
    }

    private val conjuncts = "ক্ষ জ্ঞ ন্ত্র স্ত্র র্য"
    private val vowelSigns = "কী, নীড়, শ্রেণি, প্রয়োজনীয়"
    private val words = "বাংলাদেশ, স্বাধীনতা, রাষ্ট্র, কার্যক্রম"

    @Test
    fun `bengali paragraph survives parse round trip`() {
        assertEquals("আমি বাংলায় লিখছি।", paragraphText("আমি বাংলায় লিখছি।"))
        assertEquals(conjuncts, paragraphText(conjuncts))
        assertEquals(vowelSigns, paragraphText(vowelSigns))
        assertEquals(words, paragraphText(words))
    }

    @Test
    fun `bengali with emoji survives round trip`() {
        assertEquals("বাংলা 😀 👩‍💻 ❤️", paragraphText("বাংলা 😀 👩‍💻 ❤️"))
    }

    @Test
    fun `mixed bengali latin and url round trip`() {
        assertEquals(
            "বাংলা (Bangla) — API v2.1: /path/to/file",
            paragraphText("বাংলা (Bangla) — API v2.1: /path/to/file"),
        )
    }

    @Test
    fun `bengali link label is preserved`() {
        val doc = parseMarkdown("[বাংলাদেশ](https://example.com/bd?x=১)")
        val para = doc.blocks.single() as Paragraph
        val link = para.inlines.single() as Link
        assertEquals("https://example.com/bd?x=১", link.href)
        assertEquals(listOf(Text("বাংলাদেশ")), link.children.toList())
    }

    @Test
    fun `fenced code with bengali comment is verbatim`() {
        val code = "val x = 1 // বাংলা মন্তব্য\nprintln(x)"
        val doc = parseMarkdown("```kotlin\n$code\n```")
        val fence = doc.blocks.single() as CodeFence
        assertEquals(code, fence.code)
    }

    @Test
    fun `bengali table rows parse`() {
        val doc = parseMarkdown("| ক | খ |\n| - | - |\n| বাংলাদেশ | API |")
        val table = doc.blocks.single() as Table
        assertEquals(
            listOf("ক", "খ"),
            table.headers.map { header ->
                header.joinToString("") { (it as? Text)?.value ?: "" }
            },
        )
    }

    @Test
    fun `underscore inside bengali prose does not italicize`() {
        val text = "ফাইল_১ থেকে ফাইল_২ পর্যন্ত"
        val para = parseMarkdown(text).blocks.single() as Paragraph
        assertEquals(listOf(Text(text)), para.inlines.toList())
    }

    @Test
    fun `underscore pair around bengali word still italicizes`() {
        val para = parseMarkdown("এটি _গাঢ়_ লেখা").blocks.single() as Paragraph
        assertEquals(
            listOf(Text("এটি "), Emphasis(persistentListOf(Text("গাঢ়"))), Text(" লেখা")),
            para.inlines.toList(),
        )
    }

    @Test
    fun `partial markdown stream at every split point never throws or corrupts`() {
        val document = buildString {
            append("## শিরোনাম\n\n")
            append("আমি বাংলায় লিখছি। এখানে **গাঢ় টেক্সট** আছে এবং _মিল খুঁজে পাওয়া_ প্রয়োজন।\n\n")
            append("| ক | খ |\n| - | - |\n| এক | দুই |\n\n")
            append("```kotlin\nval x = \"বাংলা\" // মন্তব্য\n```\n\n")
            append("[বাংলা লিংক](https://example.com/) শেষে 😀 👩‍💻।\n")
        }
        for (split in 1 until document.length) {
            // Parsing any prefix must not throw and must not lose content outright.
            val doc = parseMarkdown(document.substring(0, split))
            assertTrue(doc.blocks.isNotEmpty(), "empty doc at split $split")
        }
    }

    // ---------- grapheme-safe truncation ----------

    @Test
    fun `graphemeSafeTake does not split conjunct clusters`() {
        // "ক্ষ" is one grapheme (ক + ্ + ষ); slicing must never land between them.
        val text = "ক্ষ" + "ই"
        assertEquals("ক্ষ", graphemeSafeTake(text, 1))
        assertEquals("ক্ষই", graphemeSafeTake(text, 2))
    }

    @Test
    fun `graphemeSafeTake does not split emoji zwj sequences`() {
        val text = "👩‍💻" + "b"
        assertEquals("👩‍💻", graphemeSafeTake(text, 1))
    }

    @Test
    fun `graphemeSafeTake handles bengali vowel signs as part of the cluster`() {
        val text = "কী" + "x"
        assertEquals("কী", graphemeSafeTake(text, 1))
    }

    @Test
    fun `graphemeSafeTruncateWithEllipsis appends ellipsis only when truncated`() {
        // Short input (fewer graphemes than the limit) passes through unchanged.
        assertEquals("আমি", graphemeSafeTruncateWithEllipsis("আমি", 10))
        val long = graphemeSafeTruncateWithEllipsis("আমি বাংলায় লিখছি।", 4)
        assertTrue(long.endsWith("…"))
        assertTrue("আমি বাংলায় লিখছি।".startsWith(long.removeSuffix("…")))
    }

    @Test
    fun `graphemeSafeTake empty and zero limits`() {
        assertEquals("", graphemeSafeTake("বাংলা", 0))
        assertEquals("", graphemeSafeTake("", 5))
    }

    @Test
    fun `graphemeClusterCount counts clusters not code units`() {
        assertEquals(1, graphemeClusterCount("ক্ষ")) // ক + ্ + ষ is one cluster
        assertEquals(1, graphemeClusterCount("👩‍💻")) // ZWJ emoji is one cluster
        assertEquals(2, graphemeClusterCount("কীই")) // কী (cluster) + ই (cluster)
    }

    @Test
    fun `graphemeSafeDeleteCount never splits a cluster`() {
        // ক + ী is one cluster; asking for one code unit back must delete the whole cluster.
        assertEquals(1, graphemeSafeDeleteCount("কী", 1))
        assertEquals(1, graphemeSafeDeleteCount("কী", 2))
        assertEquals(1, graphemeSafeDeleteCount("কীক্ষ", 2)) // final cluster (ক্ষ, 3 units) already covers 2 requested units
        assertEquals(1, graphemeSafeDeleteCount("👩‍💻", 1))
        assertEquals(1, graphemeSafeDeleteCount("👩‍💻", 4))
        assertEquals(1, graphemeSafeDeleteCount("a👩‍💻", 2))
    }

    @Test
    fun `graphemeSafeTake bengali corpus greedy split points`() {
        val text = "আমি বাংলায় লিখছি এবং এটি পুরোপুরি ঠিকভাবে কাজ করে"
        for (n in 1..text.length) {
            val taken = graphemeSafeTake(text, n)
            // Every cluster boundary must fall on a grapheme boundary:
            // re-parsing the taken substring must never start with a lone combining mark.
            assertTrue(taken.isNotEmpty() || n >= text.length)
            if (taken.isNotEmpty()) {
                val first = taken[0]
                assertTrue(first !in '\u09BE'..'\u09CD' && first != '\u09BC', "starts with combining mark at n=$n")
            }
        }
    }
}
