package com.inspiredandroid.kai.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiffParsingTest {

    private val patch = """
        |diff --git a/src/Main.kt b/src/Main.kt
        |index 1234567..89abcde 100644
        |--- a/src/Main.kt
        |+++ b/src/Main.kt
        |@@ -10,4 +10,5 @@ fun main() {
        | context line
        |-removed line
        |+added line
        | another context
        |\ No newline at end of file
    """.trimMargin()

    @Test
    fun `diff language tag is detected`() {
        assertTrue(isDiffContent("diff", "@@ -1 +1 @@"))
        assertTrue(isDiffContent("PATCH", "@@ -1 +1 @@"))
        assertFalse(isDiffContent("kotlin", "fun main() {}"))
        assertFalse(isDiffContent("text", "--- a\n+++ b\n@@ -1 +1 @@\n-x\n+y"))
    }

    @Test
    fun `language-less unified diff is detected from its shape`() {
        assertTrue(isDiffContent(null, patch))
        assertTrue(
            isDiffContent(
                null,
                "--- a/one.txt\n+++ b/one.txt\n@@ -1 +1 @@\n-old\n+new",
            ),
        )
        assertFalse(isDiffContent(null, "just some prose\nmore prose"))
        assertFalse(isDiffContent(null, "# heading\n\n---\n"))
    }

    @Test
    fun `parses file hunk and line counts`() {
        val entries = parseDiff(patch)
        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("src/Main.kt", entry.file)
        assertEquals(1, entry.added)
        assertEquals(1, entry.removed)
        assertTrue(entry.lines.any { it.kind == DiffLineKind.FileHeader })
        assertTrue(entry.lines.any { it.kind == DiffLineKind.Hunk })
        assertTrue(entry.lines.any { it.kind == DiffLineKind.Meta })
        assertTrue(entry.lines.any { it.kind == DiffLineKind.Context })
    }

    @Test
    fun `hunk line numbers follow each side`() {
        val lines = parseDiff(patch).single().lines
        val deleted = lines.first { it.kind == DiffLineKind.Delete }
        val added = lines.first { it.kind == DiffLineKind.Add }
        // The hunk starts at 10 on both sides; the first context line consumes it.
        assertEquals(11, deleted.oldNumber)
        assertEquals(11, added.newNumber)
        val firstContext = lines.first { it.kind == DiffLineKind.Context }
        assertEquals(10, firstContext.oldNumber)
        assertEquals(10, firstContext.newNumber)
    }

    @Test
    fun `file markers without a git header still name the file`() {
        val entries = parseDiff("--- a/lib/util.kt\n+++ b/lib/util.kt\n@@ -3,2 +3,2 @@\n-old\n+new")
        assertEquals("lib/util.kt", entries.single().file)
        assertEquals(1, entries.single().added)
        assertEquals(1, entries.single().removed)
    }

    @Test
    fun `several files in one fence split into entries`() {
        val two = """
            |diff --git a/one.txt b/one.txt
            |--- a/one.txt
            |+++ b/one.txt
            |@@ -1 +1 @@
            |-old
            |+new
            |diff --git a/two.txt b/two.txt
            |--- a/two.txt
            |+++ b/two.txt
            |@@ -1,2 +1,2 @@
            | keep
            |-gone
            |+fresh
        """.trimMargin()
        val entries = parseDiff(two)
        assertEquals(2, entries.size)
        assertEquals("one.txt", entries[0].file)
        assertEquals("two.txt", entries[1].file)
        assertEquals(1, entries[0].added)
        assertEquals(1, entries[0].removed)
        assertEquals(1, entries[1].added)
        assertEquals(1, entries[1].removed)
    }

    @Test
    fun `unrecognised lines fall back to context`() {
        val entries = parseDiff("not a header\njust text")
        assertEquals(1, entries.size)
        assertEquals(0, entries.single().added)
        assertEquals(0, entries.single().removed)
        assertTrue(entries.single().lines.all { it.kind == DiffLineKind.Context })
    }
}
