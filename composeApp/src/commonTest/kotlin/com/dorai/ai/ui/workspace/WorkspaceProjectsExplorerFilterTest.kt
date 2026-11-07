package com.dorai.ai.ui.workspace

import com.dorai.ai.workspace.WorkspaceGitStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceProjectsExplorerFilterTest {

    private val files = listOf(
        ProjectFile(path = "/root/projects/demo/src", name = "src", isDirectory = true),
        ProjectFile(path = "/root/projects/demo/src/Main.kt", name = "Main.kt", isDirectory = false),
        ProjectFile(
            path = "/root/projects/demo/README.md",
            name = "README.md",
            isDirectory = false,
            gitStatus = WorkspaceGitStatus.Modified,
        ),
    )

    @Test
    fun `empty query returns everything for All`() {
        assertEquals(files, filterProjectFiles(files, "", ProjectFilter.All))
    }

    @Test
    fun `search matches name and path case insensitively`() {
        assertEquals(
            listOf(files[1]),
            filterProjectFiles(files, "main", ProjectFilter.All),
        )
        assertEquals(
            listOf(files[1]),
            filterProjectFiles(files, "SRC/main", ProjectFilter.All),
        )
    }

    @Test
    fun `Files filter hides folders`() {
        assertEquals(
            listOf(files[1], files[2]),
            filterProjectFiles(files, "", ProjectFilter.Files),
        )
    }

    @Test
    fun `Folders filter hides files`() {
        assertEquals(
            listOf(files[0]),
            filterProjectFiles(files, "", ProjectFilter.Folders),
        )
    }

    @Test
    fun `Modified filter keeps only git changed entries`() {
        assertEquals(
            listOf(files[2]),
            filterProjectFiles(files, "", ProjectFilter.Modified),
        )
    }

    @Test
    fun `query and filter combine`() {
        assertEquals(
            emptyList(),
            filterProjectFiles(files, "readme", ProjectFilter.Folders),
        )
        assertEquals(
            listOf(files[2]),
            filterProjectFiles(files, "readme", ProjectFilter.Modified),
        )
    }
}
