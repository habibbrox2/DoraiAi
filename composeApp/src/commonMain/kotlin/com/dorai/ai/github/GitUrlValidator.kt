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
}
