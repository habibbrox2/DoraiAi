package com.dorai.ai.github

/**
 * Single source of truth for which `git` invocations DORAI may run.
 *
 * The default surface is read-only: write subcommands (`commit`, `push`, `add`, `restore`) are
 * absent from [allowedGitSubcommands] until a later opt-in phase, and destructive flags are
 * rejected everywhere.
 *
 * Write subcommands can be validated through [isAllowedGitArgs] only with the explicit
 * `writeEnabled = true` parameter — the maintainer opt-in gate (`writeOperationsEnabled`).
 * With `writeEnabled = false` (the default) the behavior is exactly the read-only policy.
 * Even when enabled, writes are exact-shape: `git add` requires explicit positional paths
 * (never `.`/`:/`, never flags), `git commit` is exactly `-m <message>` with a 1..200 char
 * message and no other tokens, and `git push` accepts only `origin HEAD`. `GitCredentialBroker`
 * is the only credential source for the network forms (`git push`, `clone`, `fetch`, `pull`).
 *
 * The policy lives in common code so every runner (desktop `ProcessBuilder`,
 * Android proot) can share one definition when it is wired up.
 *
 * No production caller consumes this policy yet. `ProcessManager`/`ShellCommandTool` still own
 * their private executable allowlist for non-git commands; Task 6 is expected to route git
 * commands here. The policy lives in common code so every runner (desktop `ProcessBuilder`,
 * Android proot) can share one definition when it is wired up.
 *
 * Flag handling is fail-closed. Every token is normalized before the deny-list check: `--opt=value`
 * is compared as `--opt`, clustered short options such as `-pf` are expanded to `-p` + `-f`, and
 * abbreviated long options such as `--for` or `--upl` are refused when they abbreviate a denied
 * long option. Subcommands with a small read-only flag surface (`fetch`, `ls-remote`, `branch`,
 * `remote`) then accept only exact allow-listed flags; `clone` and `pull` are exact-token strict.
 * `status`/`log`/`diff`/`rev-parse` expose too many options to enumerate, but none of them can
 * write, and abbreviations of denied long options are still refused.
 *
 * Callers pass the arguments *after* the `git` executable. A leading `git`/`git.exe` token is
 * tolerated so a full argv list can be passed unchanged.
 *
 * The only global options accepted before the subcommand are `-C <dir>` (so `git -C <dir>
 * <subcommand>` works, repeatable) and `--no-pager`. Everything else, including `-c key=value`
 * config injection and `--git-dir`, is rejected.
 */
object GitCommandPolicy {

    val allowedGitSubcommands: Set<String> = setOf(
        "status", "log", "diff", "branch", "remote", "fetch", "clone", "pull", "rev-parse", "ls-remote",
    )

    /**
     * Gated write subcommands. Absent from [allowedGitSubcommands]; accepted only through
     * [isAllowedGitArgs] with `writeEnabled = true`, and only in their exact write shapes
     * (`add` with explicit paths, `commit -m <msg>` only, `push origin HEAD` only).
     */
    val writeGitSubcommands: Set<String> = setOf("add", "commit", "push")
    val rejectedFlags: Set<String> = setOf("--force", "--hard", "--delete", "--prune", "--mirror", "--no-ff")

    /** Short forms of the rejected long flags, plus `-f`/`-D`/`-d` force/delete aliases. */
    private val rejectedShortFlags = setOf("-f", "-D", "-d")

    /**
     * Rejected everywhere because they rewrite history, replace refs, escape the repository, or
     * write files outside the checkout.
     */
    private val extraRejectedFlags = setOf(
        "--force-with-lease", "--force-if-includes",
        "--bare", "--recursive", "--recurse-submodules",
        "--upload-pack", "--exec", "--config", "-c",
        "--git-dir", "--work-tree", "--namespace",
        "--output", "--update-head-ok",
        "--set-upstream", "--set-upstream-to", "--unset-upstream",
    )

    private val branchRejectedFlags = setOf(
        "-m", "-M", "--move", "-c", "-C", "--copy",
        "--edit-description", "-u", "-t", "--track", "--no-track", "--create-reflog",
    )

    /** Flags that consume the following token as their value, so it is not a branch name. */
    private val branchValueFlags = setOf("--contains", "--merged", "--no-merged", "--points-at", "--format", "--sort")

    /** Listing flags take an optional glob pattern as a positional argument. */
    private val branchListFlags = setOf("--list", "-l")

    private val remoteWriteActions = setOf("add", "remove", "rm", "rename", "set-url", "set-head", "prune", "update")

    private val remoteReadActions = setOf("show", "get-url")

    private val fetchRejectedFlags = setOf("-p", "--prune-tags", "-u", "--update-head-ok", "--write-fetch-head")

    /** Every long option that must never be accepted, including when abbreviated. */
    private val deniedLongFlags: Set<String> =
        (rejectedFlags + extraRejectedFlags + branchRejectedFlags + fetchRejectedFlags)
            .filterTo(mutableSetOf()) { it.startsWith("--") }

    /** Short options that must never be accepted, including when clustered. */
    private val deniedShortFlags: Set<String> = rejectedShortFlags

    /**
     * Short options that take an attached value (`-Sneedle`, `-M90%`, `-uno`). Their tail is a
     * value, not a cluster, so it is not expanded into individual flags.
     */
    private val attachedValueShortOptions = setOf('S', 'G', 'L', 'O', 'n', 'U', 'M', 'C', 'j', 'u')

    private val fetchAllowedFlags = setOf(
        "-a", "--all", "-k", "--keep", "-q", "--quiet", "-t", "--tags", "-v", "--verbose",
        "-4", "--ipv4", "-6", "--ipv6",
        "--append", "--atomic", "--dry-run", "--multiple", "--no-auto-gc", "--no-tags",
        "--no-write-fetch-head", "--progress", "--unshallow", "--update-shallow",
        "--depth", "--shallow-since", "--shallow-exclude", "--jobs", "--filter", "--negotiation-tip",
    )

    private val fetchAllowedPrefixes = listOf(
        "--depth=",
        "--shallow-since=",
        "--shallow-exclude=",
        "--jobs=",
        "--filter=",
        "--negotiation-tip=",
    )

    private val lsRemoteAllowedFlags = setOf(
        "-q", "--quiet", "-t", "--tags", "-h", "--heads", "-4", "--ipv4", "-6", "--ipv6",
        "--branches", "--refs", "--get-url", "--symref", "--exit-code", "--sort", "--server-option",
    )

    private val lsRemoteAllowedPrefixes = listOf("--sort=", "--server-option=")

    private val branchAllowedFlags = setOf(
        "-a", "--all", "-r", "--remotes", "-v", "-vv", "--verbose", "-l", "--list", "-i", "--ignore-case",
        "--show-current", "--color", "--no-color", "--column", "--no-column", "--abbrev", "--no-abbrev",
        "--contains", "--no-contains", "--merged", "--no-merged", "--points-at", "--format", "--sort",
        "--omit-empty",
    )

    private val branchAllowedPrefixes = listOf(
        "--color=", "--column=", "--abbrev=", "--contains=", "--no-contains=",
        "--merged=", "--no-merged=", "--points-at=", "--format=", "--sort=",
    )

    private val remoteAllowedFlags = setOf("-v", "--verbose", "-n", "--all", "--push")

    private val safeArgPattern = Regex("[A-Za-z0-9_@%+=:,./-]+")

    /**
     * Returns true only when [args] is a `git` invocation this app may execute.
     *
     * Checks, in order: no empty/control-character tokens, an accepted global-option prefix, an
     * allowed subcommand, the normalized deny list, then the per-subcommand rules.
     *
     * With the default `writeEnabled = false` this is the read-only surface and every write
     * subcommand is refused. With `writeEnabled = true` the gated write subcommands
     * ([writeGitSubcommands]) are additionally admitted, but only in their exact write shapes.
     */
    fun isAllowedGitArgs(args: List<String>, writeEnabled: Boolean = false): Boolean {
        val tokens = if (args.isNotEmpty() && isGitExecutable(args.first())) args.drop(1) else args
        if (tokens.isEmpty()) return false
        if (tokens.any { token -> token.isEmpty() || token.any { it == '\n' || it == '\r' || it == '\u0000' } }) {
            return false
        }

        val (subcommand, rest) = splitGlobalOptions(tokens) ?: return false
        if (rest.any { isRejectedFlag(it) }) return false

        if (subcommand in writeGitSubcommands) {
            if (!writeEnabled) return false
            return when (subcommand) {
                "add" -> isAllowedAdd(rest)
                "commit" -> isAllowedCommit(rest)
                "push" -> isAllowedPush(rest)
                else -> false
            }
        }
        if (subcommand !in allowedGitSubcommands) return false

        return when (subcommand) {
            "pull" -> isAllowedPull(rest)
            "clone" -> isAllowedClone(rest)
            "branch" -> isAllowedBranch(rest)
            "remote" -> isAllowedRemote(rest)
            "fetch" -> isAllowedFetch(rest)
            "ls-remote" -> isAllowedLsRemote(rest)
            else -> true
        }
    }

    /**
     * Quotes one argument for a POSIX shell (`sh`/`bash`, e.g. the Android guest runner).
     *
     * Values made only of shell-safe characters are returned unchanged; everything else is wrapped
     * in single quotes with embedded quotes escaped as `'\''`. Callers that build a `ProcessBuilder`
     * argument list should pass arguments separately instead of quoting them.
     */
    fun quoteArg(value: String): String {
        if (value.isEmpty()) return "''"
        if (safeArgPattern.matches(value)) return value
        return "'" + value.replace("'", "'\\''") + "'"
    }

    /**
     * True when [token] names the `git` executable in any path form (`git`, `/usr/bin/git`,
     * `C:\Program Files\Git\cmd\git.exe`). Lets a caller such as `ProcessManager` route git
     * commands here instead of re-implementing executable detection.
     */
    fun isGitExecutable(token: String): Boolean {
        val baseName = token.substringAfterLast('/').substringAfterLast('\\').lowercase()
        return baseName == "git" || baseName == "git.exe"
    }

    /**
     * Consumes leading `-C <dir>` / `--no-pager` options and returns the subcommand plus the
     * remaining arguments, or null when an unrecognized global option appears.
     */
    private fun splitGlobalOptions(tokens: List<String>): Pair<String, List<String>>? {
        var index = 0
        while (index < tokens.size) {
            when (val token = tokens[index]) {
                "-C" -> {
                    val path = tokens.getOrNull(index + 1) ?: return null
                    if (path.startsWith("-")) return null
                    index += 2
                }

                "--no-pager" -> index += 1

                else -> {
                    if (token.startsWith("-")) return null
                    return token to tokens.drop(index + 1)
                }
            }
        }
        return null
    }

    private fun flagName(token: String): String {
        if (!token.startsWith("-")) return token
        val separator = token.indexOf('=')
        return if (separator > 0) token.substring(0, separator) else token
    }

    /**
     * Fail-closed deny check over a normalized token.
     *
     * Catches exact denied flags, long options abbreviated to a denied long option (`--for`,
     * `--upl`, `--set-u`) and denied short options hidden in a cluster (`-pf`, `-uf`).
     */
    private fun isRejectedFlag(token: String): Boolean {
        if (!token.startsWith("-")) return false
        val name = flagName(token)
        if (name in rejectedFlags || name in extraRejectedFlags) return true
        if (name in deniedShortFlags) return true

        if (name.startsWith("--") && name.length > 2) {
            return deniedLongFlags.any { denied -> denied.length > name.length && denied.startsWith(name) }
        }

        if (name.length > 2 && name[1] !in attachedValueShortOptions) {
            return name.drop(1).any { short -> "-$short" in deniedShortFlags }
        }
        return false
    }

    /** Every flag token must be an exact allowed flag or match an allowed value prefix. */
    private fun areFlagsAllowed(rest: List<String>, exact: Set<String>, prefixes: List<String>): Boolean = rest.all { token ->
        !token.startsWith("-") ||
            token in exact ||
            prefixes.any { prefix -> token.length > prefix.length && token.startsWith(prefix) }
    }

    /** `git pull` is allowed only in its fast-forward-only form. */
    private fun isAllowedPull(rest: List<String>): Boolean {
        if ("--ff-only" !in rest) return false
        return rest.all { it == "--ff-only" || !it.startsWith("-") }
    }

    /**
     * `git add` requires 1+ explicit positional paths and no flags: never `.`, never `:/`,
     * never `-A`/`--all`/`-p`. Path tokens are checked for traversal by the caller
     * ([com.dorai.ai.github.GitUrlValidator] for project names; the workspace layer for
     * per-file paths).
     */
    private fun isAllowedAdd(rest: List<String>): Boolean {
        if (rest.isEmpty()) return false
        return rest.all { token -> !token.startsWith("-") && token != "." && token != ":/" }
    }

    /** `git commit` is exactly `-m <message>` with a 1..200 char message and no other tokens. */
    private fun isAllowedCommit(rest: List<String>): Boolean {
        if (rest.size != 2) return false
        if (rest[0] != "-m" && rest[0] != "--message") return false
        val message = rest[1]
        return message.isNotEmpty() && message.length <= 200
    }

    /** `git push` is only `git push origin HEAD` — never force, never another remote or refspec. */
    private fun isAllowedPush(rest: List<String>): Boolean = rest == listOf("origin", "HEAD")

    /** `git clone` is allowed only as `clone --depth=1 <single-url>`. */
    private fun isAllowedClone(rest: List<String>): Boolean {
        var hasDepth = false
        val positionals = mutableListOf<String>()
        var index = 0
        while (index < rest.size) {
            when (val token = rest[index]) {
                "--depth=1" -> hasDepth = true

                "--depth" -> {
                    if (rest.getOrNull(index + 1) != "1") return false
                    hasDepth = true
                    index += 1
                }

                else -> {
                    if (token.startsWith("-")) return false
                    positionals += token
                }
            }
            index += 1
        }
        return hasDepth && positionals.size == 1
    }

    /**
     * `git branch` may list and inspect branches but never create, rename, move, or delete one, so
     * positional arguments are only accepted as the value of an inspection flag or as a `--list`
     * glob pattern.
     */
    private fun isAllowedBranch(rest: List<String>): Boolean {
        if (!areFlagsAllowed(rest, branchAllowedFlags, branchAllowedPrefixes)) return false
        val listMode = rest.any { it in branchListFlags || flagName(it) in branchListFlags }

        var expectsValue = false
        for (token in rest) {
            if (expectsValue) {
                expectsValue = false
                if (!token.startsWith("-")) continue
            }
            if (token.startsWith("-")) {
                expectsValue = flagName(token) in branchValueFlags && '=' !in token
                continue
            }
            if (!listMode) return false
        }
        return true
    }

    /** `git remote` may read remotes but never add, remove, rename, or rewrite one. */
    private fun isAllowedRemote(rest: List<String>): Boolean {
        if (!areFlagsAllowed(rest, remoteAllowedFlags, emptyList())) return false
        val positionals = rest.filter { !it.startsWith("-") }
        if (positionals.isEmpty()) return true
        val action = positionals.first()
        if (action in remoteWriteActions) return false
        if (action !in remoteReadActions) return false
        return positionals.size <= 2
    }

    /** `git fetch` accepts only its read-only flag surface. */
    private fun isAllowedFetch(rest: List<String>): Boolean {
        if (!areFlagsAllowed(rest, fetchAllowedFlags, fetchAllowedPrefixes)) return false
        return rest.none { flagName(it) in fetchRejectedFlags }
    }

    /** `git ls-remote` accepts only its read-only flag surface. */
    private fun isAllowedLsRemote(rest: List<String>): Boolean = areFlagsAllowed(rest, lsRemoteAllowedFlags, lsRemoteAllowedPrefixes)
}
