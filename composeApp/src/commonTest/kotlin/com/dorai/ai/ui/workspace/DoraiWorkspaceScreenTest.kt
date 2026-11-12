package com.dorai.ai.ui.workspace

import kotlin.test.Test
import kotlin.test.assertEquals

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
                WorkspaceProjectTab.Editor,
                WorkspaceProjectTab.Terminal,
                WorkspaceProjectTab.Git,
            ),
            WorkspaceProjectTab.entries,
        )
    }
}
