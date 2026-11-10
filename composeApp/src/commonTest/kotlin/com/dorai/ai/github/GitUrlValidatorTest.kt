package com.dorai.ai.github

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitUrlValidatorTest {

    @Test
    fun `valid https GitHub com URL returns true`() = assertTrue(
        GitUrlValidator.isValidGitHubUrl("https://github.com/owner/repo"),
    )

    @Test
    fun `valid http GitHub com URL returns true`() = assertTrue(
        GitUrlValidator.isValidGitHubUrl("http://github.com/owner/repo"),
    )

    @Test
    fun `bare GitHub com URL returns true`() = assertTrue(
        GitUrlValidator.isValidGitHubUrl("github.com/owner/repo"),
    )

    @Test
    fun `valid GitHub com URL with dot-git suffix returns true`() = assertTrue(
        GitUrlValidator.isValidGitHubUrl("https://github.com/owner/repo.git"),
    )

    @Test
    fun `valid www GitHub com URL returns true`() = assertTrue(
        GitUrlValidator.isValidGitHubUrl("https://www.github.com/owner/repo"),
    )

    @Test
    fun `gitlab com URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("https://gitlab.com/owner/repo"),
    )

    @Test
    fun `bitbucket org URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("https://bitbucket.org/owner/repo"),
    )

    @Test
    fun `URL with embedded credentials returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("https://user:pass@github.com/owner/repo"),
    )

    @Test
    fun `empty URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl(""),
    )

    @Test
    fun `null URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl(null),
    )

    @Test
    fun `blank URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("   "),
    )

    @Test
    fun `random host URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("https://example.com/owner/repo"),
    )

    @Test
    fun `GitHub Enterprise URL returns false`() = assertFalse(
        GitUrlValidator.isValidGitHubUrl("https://github.mycompany.com/owner/repo"),
    )
}
