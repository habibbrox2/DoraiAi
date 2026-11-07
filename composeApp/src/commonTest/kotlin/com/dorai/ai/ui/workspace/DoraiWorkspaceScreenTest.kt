package com.dorai.ai.ui.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoraiWorkspaceScreenTest {

    @Test
    fun `workspace project items and tabs are structured as expected`() {
        val proj = WorkspaceProjectItem(
            name = "DoraiAi",
            path = "g:/Web/DoraiAi",
            branch = "main",
            lastUpdatedText = "Just now",
            changesCount = 3,
        )
        assertEquals("DoraiAi", proj.name)
        assertEquals("main", proj.branch)
        assertEquals(3, proj.changesCount)

        assertEquals(
            listOf(
                WorkspaceProjectTab.Agent,
                WorkspaceProjectTab.Editor,
                WorkspaceProjectTab.Terminal,
                WorkspaceProjectTab.Git,
            ),
            WorkspaceProjectTab.entries,
        )
    }

    @Test
    fun `git change item status letters`() {
        val changeM = GitChangeItem("App.kt", "M")
        val changeA = GitChangeItem("NewFile.kt", "A")
        val changeD = GitChangeItem("OldFile.kt", "D")

        assertEquals("M", changeM.statusLetter)
        assertEquals("A", changeA.statusLetter)
        assertEquals("D", changeD.statusLetter)
    }
}
