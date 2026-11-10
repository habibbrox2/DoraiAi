package com.dorai.ai.github

import android.content.Context
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.util.UUID

actual fun createGitCredentialBroker(): GitCredentialBroker = AndroidGitCredentialBroker()

private class AndroidGitCredentialBroker : GitCredentialBroker {

    private val context: Context by inject(Context::class.java)

    @Volatile
    private var helperScript: File? = null

    @Volatile
    private var tokenFile: File? = null

    override fun credentialGitConfigArgs(command: List<String>): CredentialContext {
        val vault = createGitHubTokenVault()
        val token = vault.readAccessToken() ?: error("No GitHub access token in vault")

        val dir = context.cacheDir
        val random = UUID.randomUUID().toString().substring(0, 8)

        tokenFile = File(dir, "git-credential-token-$random")
        ownerOnly(tokenFile)
        tokenFile.writeText(token)

        helperScript = File(dir, "git-credential-helper-$random.sh")
        helperScript.writeText(
            """#!/bin/sh
printf 'username=x-access-token\n'
printf 'password=%s\n' "$(cat '${tokenFile.absolutePath}')"
            """.trimIndent(),
        )

        // chmod 0600 equivalent: owner read/write/execute only.
        ownerOnly(helperScript)
        helperScript?.setExecutable(true, false)

        val gitArgs = listOf("-c", "credential.helper=!${helperScript!!.absolutePath}")

        return CredentialContext(
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
}
