package com.dorai.ai.github

/**
 * Validates that a given string is a well-formed GitHub.com repository URL.
 *
 * Only `github.com` is allowed — GitHub Enterprise URLs are rejected (per P0 read-only audit:
 * no custom domain, no cloud backend, no webhooks).
 *
 * Accepted formats:
 *   - https://github.com/owner/repo
 *   - http://github.com/owner/repo
 *   - github.com/owner/repo         (scheme-relative / bare host)
 *   - https://github.com/owner/repo.git  (with .git suffix)
 *
 * Rejected:
 *   - Any host other than github.com (gitlab.com, bitbucket.org, git.example.com, etc.)
 *   - URLs with embedded credentials (user:pass@)
 *   - Malformed URLs (missing host, missing path, invalid characters)
 *   - Empty or null input
 *
 * The validation is intentionally strict: it does not attempt to resolve the URL or
 * reach out to GitHub. It only checks structural safety so that downstream callers
 * (e.g., `GitCommandPolicy.isAllowedClone` or a future UI input field) cannot be
 * tricked into accepting a malicious or unexpected host.
 */
object GitUrlValidator {

    private val githubHostPattern = Regex(
        pattern = """^((https?://)?(www\.)?github\.com(/[^/]+/[^/]+)?(\.git)?)$""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    private val credentialInUrlPattern = Regex(
        pattern = """[^:@/]+:[^@/]+@""",
    )

    /** Returns true only when [url] is a valid GitHub.com repository URL. */
    fun isValidGitHubUrl(url: String?): Boolean {
        if (url == null || url.isBlank()) return false
        // Reject URLs with embedded credentials (user:pass@)
        if (credentialInUrlPattern.containsMatchIn(url)) return false
        // Must match the GitHub.com host pattern
        return githubHostPattern.matches(url)
    }

    /** Returns true only when [name] is a safe project folder name (no path traversal). */
    fun isValidProjectName(name: String?): Boolean {
        if (name == null || name.isBlank()) return false
        return name.isNotEmpty() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."
    }

    /**
     * Returns true only when [path] is an explicit, non-traversing relative path, safe to hand to
     * `git add -- <path>`: not absolute (no leading `/` or `\`, no drive letter), no `..` or
     * empty segments, and never `-`-prefixed so it cannot be mistaken for a flag.
     */
    fun isSafeRelativePath(path: String?): Boolean {
        if (path == null || path.isBlank()) return false
        if (path.any { it == '\u0000' || it == '\n' || it == '\r' }) return false
        if (path.startsWith("/") || path.startsWith("\\") || path.startsWith("-")) return false
        if (path.length >= 3 && path[0].isLetter() && path[1] == ':' && (path[2] == '/' || path[2] == '\\')) {
            return false // Windows drive-absolute path
        }
        val segments = path.split('/', '\\')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return false
        return true
    }
}
