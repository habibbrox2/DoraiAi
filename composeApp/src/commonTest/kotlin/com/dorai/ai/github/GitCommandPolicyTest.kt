package com.dorai.ai.github

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitCommandPolicyTest {

    @Test
    fun `allows git status porcelain`() = assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("status", "--porcelain=v1")))

    @Test
    fun `rejects git reset hard`() = assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("reset", "--hard")))

    @Test
    fun `rejects git push`() = assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push")))

    @Test
    fun `rejects git clone mirror`() = assertFalse(
        GitCommandPolicy.isAllowedGitArgs(listOf("clone", "--mirror", "https://github.com/a/b")),
    )

    @Test
    fun `allowed subcommands are exactly the read-only set`() {
        assertEquals(
            setOf(
                "status",
                "log",
                "diff",
                "branch",
                "remote",
                "fetch",
                "clone",
                "pull",
                "rev-parse",
                "ls-remote",
            ),
            GitCommandPolicy.allowedGitSubcommands,
        )
    }

    @Test
    fun `rejected flags are exactly the documented set`() {
        assertEquals(
            setOf("--force", "--hard", "--delete", "--prune", "--mirror", "--no-ff"),
            GitCommandPolicy.rejectedFlags,
        )
    }

    @Test
    fun `allows git -C directory form`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("-C", "C:/dorai/projects/demo", "status", "--short")))
        assertTrue(
            GitCommandPolicy.isAllowedGitArgs(
                listOf("-C", "/a", "-C", "/b", "log", "--oneline", "-1"),
            ),
        )
    }

    @Test
    fun `allows an explicit git executable prefix`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("git", "-C", "/repo", "log", "--oneline")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("/usr/bin/git", "status")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("""C:\Program Files\Git\cmd\git.exe""", "status")))
    }

    @Test
    fun `rejects unrecognized global options`() {
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("-c", "core.pager=cat", "status")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("--git-dir=/tmp/other", "status")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("--exec-path=/tmp", "status")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("--no-pager", "log", "-1")))
    }

    @Test
    fun `allows only fast-forward pull`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "--ff-only")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "--ff-only", "origin", "main")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("pull")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "origin", "main")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "--rebase")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "--ff-only", "--no-verify")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("pull", "--ff-only", "--prune")))
    }

    @Test
    fun `clone requires depth one and a single url`() {
        assertTrue(
            GitCommandPolicy.isAllowedGitArgs(listOf("clone", "--depth=1", "https://github.com/a/b.git")),
        )
        assertTrue(
            GitCommandPolicy.isAllowedGitArgs(listOf("clone", "--depth", "1", "https://github.com/a/b.git")),
        )
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("clone", "https://github.com/a/b.git")))
        assertFalse(
            GitCommandPolicy.isAllowedGitArgs(listOf("clone", "--depth=2", "https://github.com/a/b.git")),
        )
        assertFalse(
            GitCommandPolicy.isAllowedGitArgs(
                listOf("clone", "--depth=1", "--recursive", "https://github.com/a/b.git"),
            ),
        )
        assertFalse(
            GitCommandPolicy.isAllowedGitArgs(
                listOf("clone", "--depth=1", "--bare", "https://github.com/a/b.git"),
            ),
        )
        assertFalse(
            GitCommandPolicy.isAllowedGitArgs(
                listOf("clone", "--depth=1", "https://github.com/a/b.git", "target"),
            ),
        )
    }

    @Test
    fun `rejects dangerous flags in otherwise allowed subcommands`() {
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "--prune")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "-p")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "--force")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--delete", "main")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-D", "main")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("diff", "--output=patch.diff")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "--force")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "--hard")))
    }

    @Test
    fun `branch is read only`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--show-current")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-a")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--contains", "main")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--contains=main")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--format=%(refname:short)")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--merged")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--list", "feature/*")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "feature/new")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-f", "main", "HEAD")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-m", "old", "new")))
    }

    @Test
    fun `remote is read only`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "-v")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "show", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "get-url", "origin")))
        assertFalse(
            GitCommandPolicy.isAllowedGitArgs(
                listOf("remote", "add", "origin", "https://github.com/a/b"),
            ),
        )
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "set-url", "origin", "x")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "prune", "origin")))
    }

    @Test
    fun `rejects malformed argument lists`() {
        assertFalse(GitCommandPolicy.isAllowedGitArgs(emptyList()))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("-C")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("-C", "")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("-C", "--force", "status")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "a\nb")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("")))
    }

    @Test
    fun `detects the git executable in any path form`() {
        assertTrue(GitCommandPolicy.isGitExecutable("git"))
        assertTrue(GitCommandPolicy.isGitExecutable("git.exe"))
        assertTrue(GitCommandPolicy.isGitExecutable("/usr/bin/git"))
        assertTrue(GitCommandPolicy.isGitExecutable("""C:\Program Files\Git\cmd\git.exe"""))
        assertFalse(GitCommandPolicy.isGitExecutable("cmd"))
        assertFalse(GitCommandPolicy.isGitExecutable("git-lfs"))
    }

    @Test
    fun `quoteArg leaves safe values untouched`() {
        assertEquals("main", GitCommandPolicy.quoteArg("main"))
        assertEquals("https://github.com/a/b.git", GitCommandPolicy.quoteArg("https://github.com/a/b.git"))
        assertEquals("--depth=1", GitCommandPolicy.quoteArg("--depth=1"))
    }

    @Test
    fun `quoteArg single quotes unsafe values`() {
        assertEquals("'feature/new branch'", GitCommandPolicy.quoteArg("feature/new branch"))
        assertEquals("''", GitCommandPolicy.quoteArg(""))
        assertEquals("'it'\\''s'", GitCommandPolicy.quoteArg("it's"))
        assertEquals("'a;b'", GitCommandPolicy.quoteArg("a;b"))
        assertEquals("'$(rm -rf /)'", GitCommandPolicy.quoteArg("\$(rm -rf /)"))
    }
}
