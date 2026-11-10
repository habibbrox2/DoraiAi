package com.dorai.ai.workspace

/**
 * Parses `git status --porcelain=v1 -z -b` output. Multiplatform so it is
 * covered by `commonTest` — the Android manager only runs the guest command and
 * hands the raw bytes here.
 *
 * Records are NUL-terminated. A rename/copy record is followed by the original
 * path as its own record, which is skipped rather than read as a change.
 * With `-b` the first record is the `## <branch>...` header.
 */
internal fun parseGitStatusPorcelain(raw: String, root: String): WorkspaceGitResult {
    if (raw.isEmpty()) return WorkspaceGitResult(changes = emptyList(), isRepository = true)
    val tokens = raw.split('\u0000')
    var branch: String? = null
    var index = 0
    // Branch header: "## main...upstream [ahead 1]" / "## No commits yet on main"
    // / "## HEAD (no branch)". Only valid as the very first record.
    val first = tokens.firstOrNull()
    if (first != null && first.startsWith("## ")) {
        branch = parseBranchHeader(first)
        index = 1
    }
    val changes = mutableListOf<WorkspaceGitChange>()
    while (index < tokens.size) {
        val record = tokens[index]
        index++
        // "XY <path>": anything shorter is proot/pty noise, not a status entry.
        if (record.length < 4) continue
        val x = record[0]
        val y = record[1]
        if (x == '!') continue // ignored files are not pending changes
        // Separator must be a space; otherwise this is noise that happens to be long.
        if (record[2] != ' ') continue
        val path = record.substring(3)
        val isRenameOrCopy = x == 'R' || y == 'R' || x == 'C' || y == 'C'
        if (isRenameOrCopy) {
            index++ // the original path follows as its own NUL token
        }
        changes += WorkspaceGitChange(
            path = "$root/$path",
            status = gitStatusOf(x, y),
            staged = x != ' ' && x != '?',
            unstaged = y != ' ' && y != '?',
        )
    }
    return WorkspaceGitResult(changes = changes, branch = branch, isRepository = true)
}

/** Extracts the branch name from a `## ...` status header, or null when there is none. */
internal fun parseBranchHeader(header: String): String? {
    val body = header.removePrefix("## ").trim()
    if (body.isEmpty()) return null
    // "No commits yet on main" — the branch still exists and is worth showing.
    if (body.startsWith("No commits yet on ")) {
        return body.removePrefix("No commits yet on ").trim().takeIf { it.isNotEmpty() }
    }
    // "HEAD (no branch)" / "HEAD (detached at abc123)" — detached, no branch name.
    if (body.startsWith("HEAD")) return null
    // "main...origin/main [behind 2]" — branch is the token before "...".
    val beforeUpstream = body.substringBefore("...")
    val branch = beforeUpstream.trim().substringBefore(" ")
    return branch.takeIf { it.isNotEmpty() }
}

private fun gitStatusOf(x: Char, y: Char): WorkspaceGitStatus = when {
    x == '?' || y == '?' -> WorkspaceGitStatus.Untracked
    x == 'U' || y == 'U' || (x == 'A' && y == 'A') || (x == 'D' && y == 'D') -> WorkspaceGitStatus.Conflicted
    x == 'R' || y == 'R' -> WorkspaceGitStatus.Renamed
    x == 'A' || y == 'A' -> WorkspaceGitStatus.Added
    x == 'D' || y == 'D' -> WorkspaceGitStatus.Deleted
    else -> WorkspaceGitStatus.Modified
}

/**
 * Parses `git log --oneline` output. Each line is `<hash> <subject>`.
 * Only the hash is required; subject may be empty.
 */
internal fun parseGitLogOneline(raw: String): List<WorkspaceGitLogEntry> {
    if (raw.isBlank()) return emptyList()
    return raw.lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val spaceIdx = line.indexOf(' ')
            if (spaceIdx <= 0) {
                null
            } else {
                WorkspaceGitLogEntry(
                    hash = line.substring(0, spaceIdx),
                    subject = line.substring(spaceIdx + 1),
                    authorName = "",
                    dateIso = "",
                )
            }
        }
}

/**
 * Converts a guest-absolute change path back to the repository-relative path
 * `git` expects, or null when the path is not under the project (traversal
 * attempts like `../` are rejected — write operations must never leave the
 * project folder).
 */
internal fun gitRelativePath(absoluteGuestPath: String, project: String): String? {
    val prefix = "/root/projects/$project/"
    if (!absoluteGuestPath.startsWith(prefix)) return null
    val relative = absoluteGuestPath.removePrefix(prefix)
    if (relative.isEmpty()) return null
    if (relative.split('/').any { it == ".." || it.isEmpty() }) return null
    if (relative.contains('\u0000')) return null
    return relative
}

/**
 * Single-quote shell escaping for one argument. Newlines stay literal inside
 * single quotes; an embedded `'` closes, escapes, and reopens the quote.
 */
internal fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
