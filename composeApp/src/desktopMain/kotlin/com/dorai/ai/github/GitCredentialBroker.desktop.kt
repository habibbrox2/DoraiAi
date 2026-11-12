package com.dorai.ai.github

import com.dorai.ai.getAppFilesDirectory
import java.io.File
import java.util.UUID

actual fun createGitCredentialBroker(): GitCredentialBroker = DesktopGitCredentialBroker()

/** Desktop broker: helper script + token sidecar under the app files dir, owner-only. */
private class DesktopGitCredentialBroker(
    private val vault: GitHubTokenVault = createGitHubTokenVault(),
) : GitCredentialBroker {

    @Volatile
    private var helperScript: File? = null

    @Volatile
    private var tokenFile: File? = null

    override fun credentialGitConfigArgs(command: List<String>): GitCredentialBroker.CredentialContext {
        val token = vault.readAccessToken() ?: error("No GitHub access token in vault")
        val appDir = File(getAppFilesDirectory())
        appDir.mkdirs()

        val random = UUID.randomUUID().toString().substring(0, 8)

        val newTokenFile = File(appDir, "git-credential-token-$random")
        ownerOnly(newTokenFile)
        newTokenFile.writeText(token)

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val ext = if (isWindows) ".bat" else ".sh"
        val newHelperScript = File(appDir, "git-credential-helper-$random$ext")

        newHelperScript.writeText(helperScriptContent(newTokenFile.absolutePath, isWindows))

        if (isWindows) {
            ownerOnly(newHelperScript)
        } else {
            newHelperScript.setExecutable(true, false)
            ownerOnly(newHelperScript)
        }

        helperScript = newHelperScript
        tokenFile = newTokenFile

        val helperPath = newHelperScript.absolutePath
        val gitArgs = listOf("-c", "credential.helper=!$helperPath")

        return GitCredentialBroker.CredentialContext(
            gitArgs = gitArgs,
            release = {
                helperScript?.delete()
                tokenFile?.delete()
                helperScript = null
                tokenFile = null
            },
        )
    }

    private fun ownerOnly(file: File) {
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setReadable(true, true)
        file.setWritable(true, true)
    }

    /**
     * Credential helper script. Reads git's key=value request from stdin and emits the token
     * ONLY when the requested host is exactly github.com — for any other host it stays silent
     * so a repo's `insteadOf` redirects / proxy targets cannot siphon the GitHub token.
     */
    private fun helperScriptContent(tokenPath: String, isWindows: Boolean): String = if (isWindows) {
        // PowerShell layer: parses stdin line-by-line (git sends protocol=..., host=..., path=...).
        // Path is single-quoted inside the PS string; single quotes in the path are doubled.
        val psSafeTokenPath = tokenPath.replace("'", "''")
        buildString {
            appendLine("@echo off")
            appendLine(
                "powershell -NoProfile -ExecutionPolicy Bypass -Command " +
                    "\"\$host_=''; " +
                    "foreach(\$l in ([Console]::In.ReadToEnd() -split '\\r?\\n')){ " +
                    "if(\$l -match '^host=(.*)$'){ \$host_ = \$Matches[1] } }; " +
                    "if(\$host_ -cne 'github.com'){ exit 0 }; " +
                    "Write-Output 'username=x-access-token'; " +
                    "Write-Output ('password=' + (Get-Content -Raw '$psSafeTokenPath'))\"",
            )
        }
    } else {
        // POSIX sh layer: split each input line on the first '=' and inspect the host field.
        val shSafeTokenPath = tokenPath.replace("'", "'\\''")
        buildString {
            appendLine("#!/bin/sh")
            appendLine("host=\"\"")
            appendLine("while IFS='=' read -r key value; do")
            appendLine("    case \"\$key\" in")
            appendLine("        host) host=\"\$value\" ;;")
            appendLine("    esac")
            appendLine("done")
            appendLine("if [ \"\$host\" != \"github.com\" ]; then")
            appendLine("    exit 0")
            appendLine("fi")
            appendLine("printf 'username=x-access-token\\n'")
            appendLine("printf 'password=%s\\n' \"\$(cat '$shSafeTokenPath')\"")
        }
    }
}
