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
    fun `rejects every denied long flag`() {
        val cases = listOf(
            // rejectedFlags
            listOf("log", "--force"),
            listOf("log", "--hard"),
            listOf("branch", "--delete", "main"),
            listOf("fetch", "--prune"),
            listOf("clone", "--depth=1", "--mirror", "https://github.com/a/b"),
            listOf("log", "--no-ff"),
            // extraRejectedFlags
            listOf("fetch", "--force-with-lease"),
            listOf("fetch", "--force-if-includes"),
            listOf("fetch", "--set-upstream-to", "origin/main"),
            listOf("fetch", "--unset-upstream"),
            listOf("fetch", "--upload-pack=/tmp/evil"),
            listOf("ls-remote", "--upload-pack=/tmp/evil", "https://github.com/a/b"),
            listOf("status", "--bare"),
            listOf("clone", "--depth=1", "--recursive", "https://github.com/a/b"),
            listOf("status", "--config=core.pager=cat"),
            listOf("diff", "--output=patch.diff"),
            listOf("rev-parse", "--git-dir"),
            listOf("fetch", "--update-head-ok"),
            // branchRejectedFlags
            listOf("branch", "-m", "old", "new"),
            listOf("branch", "-M", "old", "new"),
            listOf("branch", "--move", "old", "new"),
            listOf("branch", "-c", "old", "new"),
            listOf("branch", "-C", "old", "new"),
            listOf("branch", "--copy", "old", "new"),
            listOf("branch", "--edit-description"),
            listOf("branch", "-u", "origin/main"),
            listOf("branch", "-t", "origin/main"),
            listOf("branch", "--track"),
            listOf("branch", "--no-track"),
            listOf("branch", "--create-reflog"),
            // fetchRejectedFlags
            listOf("fetch", "-p"),
            listOf("fetch", "-u"),
            listOf("fetch", "--prune-tags"),
            listOf("fetch", "--set-upstream"),
            listOf("fetch", "--write-fetch-head"),
        )
        cases.forEach { args ->
            assertFalse(GitCommandPolicy.isAllowedGitArgs(args), "expected rejection: ${args.joinToString(" ")}")
        }
    }

    @Test
    fun `rejects short option clusters and abbreviated long options`() {
        val cases = listOf(
            listOf("fetch", "-pf"),
            listOf("fetch", "-uf"),
            listOf("fetch", "-fp"),
            listOf("fetch", "-aP"),
            listOf("fetch", "--for"),
            listOf("fetch", "--forc"),
            listOf("fetch", "--set-u"),
            listOf("fetch", "--upl=/tmp/evil"),
            listOf("ls-remote", "--upl=/tmp/evil"),
            listOf("ls-remote", "--for"),
            listOf("diff", "--out=patch.diff"),
            listOf("rev-parse", "--git-d"),
            listOf("log", "--no-f"),
            listOf("branch", "--delet", "main"),
            listOf("status", "--force=true"),
        )
        cases.forEach { args ->
            assertFalse(GitCommandPolicy.isAllowedGitArgs(args), "expected rejection: ${args.joinToString(" ")}")
        }
    }

    @Test
    fun `rejects control characters`() {
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "a\rb")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "a\u0000b")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("status", "a\nb")))
    }

    @Test
    fun `allows read only fetch and ls-remote flags`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "--all", "--tags", "--depth=1", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "-a", "-t", "-q", "origin", "main")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "--dry-run", "--no-tags")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("fetch", "--depth", "1", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("ls-remote", "--heads", "--tags", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("ls-remote", "-t", "-q", "https://github.com/a/b.git")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("ls-remote", "--symref", "--get-url", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("ls-remote", "--sort=-version:refname", "origin")))
    }

    @Test
    fun `allows read only branch inspection flags`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--no-merged", "main")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--no-merged=main")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--points-at", "HEAD")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--points-at=HEAD")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--sort=-committerdate")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-l", "feature/*")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "-vv")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("branch", "--color=never")))
    }

    @Test
    fun `allows read only remote inspection flags`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "show", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "get-url", "--all", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "show", "-n", "origin")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("remote", "-v")))
    }

    @Test
    fun `rejects remote write actions`() {
        val cases = listOf(
            listOf("remote", "add", "origin", "https://github.com/a/b"),
            listOf("remote", "remove", "origin"),
            listOf("remote", "rm", "origin"),
            listOf("remote", "rename", "origin", "upstream"),
            listOf("remote", "set-url", "origin", "https://github.com/a/b"),
            listOf("remote", "set-head", "origin", "main"),
            listOf("remote", "prune", "origin"),
            listOf("remote", "update"),
            listOf("remote", "show", "origin", "extra"),
        )
        cases.forEach { args ->
            assertFalse(GitCommandPolicy.isAllowedGitArgs(args), "expected rejection: ${args.joinToString(" ")}")
        }
    }

    @Test
    fun `keeps read only status log diff and rev-parse usable`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("status", "-uno")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("status", "--porcelain=v1", "-z")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("diff", "-M90%", "--stat")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("diff", "--name-only", "--", "src")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("log", "-Sneedle", "--oneline", "-n", "20")))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("rev-parse", "--show-toplevel")))
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

    @Test
    fun `write subcommands are rejected without the maintainer opt-in`() {
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "file.txt")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "fix")))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "origin", "HEAD")))
        // Default `writeEnabled = false` must not admit them
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "fix"), writeEnabled = false))
    }

    @Test
    fun `gated write subcommands are exactly add commit push`() {
        assertEquals(setOf("add", "commit", "push"), GitCommandPolicy.writeGitSubcommands)
    }

    @Test
    fun `git add allows only explicit positional paths`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("add", "file.txt"), writeEnabled = true))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("add", "src/Main.kt", "README.md"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "."), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", ":/"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "-A"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "--all"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "-p", "file.txt"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("add", "--force", "file.txt"), writeEnabled = true))
    }

    @Test
    fun `git commit requires exactly -m and a 1 to 200 char message`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "fix the thing"), writeEnabled = true))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "--message", "fix the thing"), writeEnabled = true))
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "x".repeat(200)), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", ""), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "x".repeat(201)), writeEnabled = true))
        // No amend, no --no-verify, no other tokens at all
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "--amend", "-m", "x"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "--no-verify", "-m", "x"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m", "x", "--", "file.txt"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "-m"), writeEnabled = true))
        // Direct flag injection / dangerous flags still refused
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("commit", "--hard", "-m", "x"), writeEnabled = true))
    }

    @Test
    fun `git push is only origin HEAD`() {
        assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("push", "origin", "HEAD"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "origin", "main"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "upstream", "HEAD"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "origin", "main:main"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "--force", "origin", "HEAD"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "-f", "origin", "HEAD"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "--mirror", "origin"), writeEnabled = true))
        assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push", "--set-upstream", "origin", "HEAD"), writeEnabled = true))
    }

    @Test
    fun `isSafeRelativePath accepts explicit relative paths only`() {
        assertTrue(GitUrlValidator.isSafeRelativePath("file.txt"))
        assertTrue(GitUrlValidator.isSafeRelativePath("src/main/kotlin/Foo.kt"))
        assertFalse(GitUrlValidator.isSafeRelativePath("../escape.txt"))
        assertFalse(GitUrlValidator.isSafeRelativePath("/abs/path.txt"))
        assertFalse(GitUrlValidator.isSafeRelativePath("-m"))
        assertFalse(GitUrlValidator.isSafeRelativePath(""))
        assertFalse(GitUrlValidator.isSafeRelativePath(null))
        assertFalse(GitUrlValidator.isSafeRelativePath("C:\\Windows\\system32"))
        assertFalse(GitUrlValidator.isSafeRelativePath("a/../../b"))
    }
}
