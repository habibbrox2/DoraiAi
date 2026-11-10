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

    private fun helperScriptContent(tokenPath: String, isWindows: Boolean): String = if (isWindows) {
        buildString {
            appendLine("@echo off")
            appendLine("echo username=x-access-token")
            appendLine("set /p token=<\"$tokenPath\"")
            appendLine("echo password=%token%")
        }
    } else {
        buildString {
            appendLine("#!/bin/sh")
            appendLine("printf 'username=x-access-token\\n'")
            append("printf 'password=%s\\n' \"\$(cat '$tokenPath')\"")
        }
    }
}
