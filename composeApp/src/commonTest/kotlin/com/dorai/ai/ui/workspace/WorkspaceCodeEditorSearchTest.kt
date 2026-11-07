package com.dorai.ai.ui.workspace

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceCodeEditorSearchTest {

    @Test
    fun `empty query or content has no matches`() {
        assertEquals(0, countSearchMatches("hello", ""))
        assertEquals(0, countSearchMatches("", "hello"))
    }

    @Test
    fun `matches are case insensitive and non overlapping`() {
        assertEquals(3, countSearchMatches("Hello hello HELLO", "hello"))
        assertEquals(2, countSearchMatches("aaaa", "aa"))
        assertEquals(1, countSearchMatches("banana", "ana"))
    }

    @Test
    fun `query longer than content has no matches`() {
        assertEquals(0, countSearchMatches("hi", "hello"))
    }
}
