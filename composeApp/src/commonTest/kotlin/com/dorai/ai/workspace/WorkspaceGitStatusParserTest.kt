package com.dorai.ai.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceGitStatusParserTest {

    private val root = "/root/projects/demo"
    private val nul = "\u0000"

    @Test
    fun `empty output is a clean repository with no changes`() {
        val result = parseGitStatusPorcelain("", root)
        assertTrue(result.changes.isEmpty())
        assertTrue(result.isRepository)
        assertEquals(null, result.error)
        assertNull(result.branch)
    }

    @Test
    fun `branch header is parsed`() {
        val raw = "## main...origin/main [behind 2]$nul M src/Main.kt$nul"
        val result = parseGitStatusPorcelain(raw, root)
        assertEquals("main", result.branch)
        assertEquals(1, result.changes.size)
        assertEquals("$root/src/Main.kt", result.changes[0].path)
        assertEquals(WorkspaceGitStatus.Modified, result.changes[0].status)
    }

    @Test
    fun `unborn branch header keeps its name`() {
        val raw = "## No commits yet on feature-x$nul"
        assertEquals("feature-x", parseGitStatusPorcelain(raw, root).branch)
    }

    @Test
    fun `detached HEAD has no branch`() {
        val raw = "## HEAD (no branch)$nul M a.txt$nul"
        val result = parseGitStatusPorcelain(raw, root)
        assertNull(result.branch)
        assertEquals(1, result.changes.size)
    }

    @Test
    fun `untracked file has neither staged nor unstaged flags`() {
        val result = parseGitStatusPorcelain("?? new.txt$nul", root)
        assertEquals(WorkspaceGitStatus.Untracked, result.changes[0].status)
        assertFalse(result.changes[0].staged)
        assertFalse(result.changes[0].unstaged)
    }

    @Test
    fun `MM is both staged and unstaged`() {
        val result = parseGitStatusPorcelain("MM a.txt$nul", root)
        assertTrue(result.changes[0].staged)
        assertTrue(result.changes[0].unstaged)
        assertEquals(WorkspaceGitStatus.Modified, result.changes[0].status)
    }

    @Test
    fun `AM is added both staged and unstaged`() {
        val result = parseGitStatusPorcelain("AM a.txt$nul", root)
        assertTrue(result.changes[0].staged)
        assertTrue(result.changes[0].unstaged)
        assertEquals(WorkspaceGitStatus.Added, result.changes[0].status)
    }

    @Test
    fun `staged-only and unstaged-only are distinguished`() {
        val stagedOnly = parseGitStatusPorcelain("M  a.txt$nul", root).changes[0]
        assertTrue(stagedOnly.staged)
        assertFalse(stagedOnly.unstaged)

        val unstagedOnly = parseGitStatusPorcelain(" M a.txt$nul", root).changes[0]
        assertFalse(unstagedOnly.staged)
        assertTrue(unstagedOnly.unstaged)
    }

    @Test
    fun `rename skips the original path token`() {
        val raw = "R  new.txt${nul}old.txt$nul M other.txt$nul"
        val result = parseGitStatusPorcelain(raw, root)
        assertEquals(2, result.changes.size)
        assertEquals("$root/new.txt", result.changes[0].path)
        assertEquals(WorkspaceGitStatus.Renamed, result.changes[0].status)
        assertEquals("$root/other.txt", result.changes[1].path)
    }

    @Test
    fun `conflicts map to Conflicted`() {
        assertEquals(
            WorkspaceGitStatus.Conflicted,
            parseGitStatusPorcelain("UU a.txt$nul", root).changes[0].status,
        )
        assertEquals(
            WorkspaceGitStatus.Conflicted,
            parseGitStatusPorcelain("AA a.txt$nul", root).changes[0].status,
        )
        assertEquals(
            WorkspaceGitStatus.Conflicted,
            parseGitStatusPorcelain("DD a.txt$nul", root).changes[0].status,
        )
    }

    @Test
    fun `paths with spaces survive NUL parsing`() {
        val result = parseGitStatusPorcelain(" M my file.kt$nul", root)
        assertEquals("$root/my file.kt", result.changes[0].path)
    }

    @Test
    fun `short noise records are ignored`() {
        val result = parseGitStatusPorcelain("ab$nul M real.txt$nul", root)
        assertEquals(1, result.changes.size)
        assertEquals("$root/real.txt", result.changes[0].path)
    }

    @Test
    fun `branch header helper handles upstream text`() {
        assertEquals("main", parseBranchHeader("## main...origin/main"))
        assertEquals("dev", parseBranchHeader("## dev"))
        assertNull(parseBranchHeader("## HEAD (detached at abc123)"))
    }

    @Test
    fun `gitRelativePath accepts paths under the project`() {
        assertEquals("src/Main.kt", gitRelativePath("/root/projects/demo/src/Main.kt", "demo"))
        assertEquals("my file.kt", gitRelativePath("/root/projects/demo/my file.kt", "demo"))
    }

    @Test
    fun `gitRelativePath rejects traversal and foreign paths`() {
        assertNull(gitRelativePath("/root/projects/demo/../secret.txt", "demo"))
        assertNull(gitRelativePath("/root/projects/demo/src/../../secret.txt", "demo"))
        assertNull(gitRelativePath("/root/projects/other/a.txt", "demo"))
        assertNull(gitRelativePath("relative/a.txt", "demo"))
        assertNull(gitRelativePath("/root/projects/demo/", "demo"))
        assertNull(gitRelativePath("/root/projects/demo/a//b.txt", "demo"))
        assertNull(gitRelativePath("/root/projects/demo/a\u0000b.txt", "demo"))
    }

    @Test
    fun `shellQuote wraps and escapes single quotes`() {
        assertEquals("'abc'", shellQuote("abc"))
        assertEquals("'a b'", shellQuote("a b"))
        assertEquals("'a'\\''b'", shellQuote("a'b"))
        assertEquals("''", shellQuote(""))
    }
}
