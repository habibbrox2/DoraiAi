package com.dorai.ai.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Long,
    val interval: Long,
)

@Serializable
data class GitHubTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String,
    val scope: String? = null,
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class GitHubInstallation(
    val id: Long,
    val account: GitHubInstallationAccount,
    @SerialName("repository_selection") val repositorySelection: String? = null,
    val permissions: Map<String, String>? = null,
)

@Serializable
data class GitHubInstallationAccount(
    val login: String,
    val id: Long,
    val type: String,
)

@Serializable
data class GitHubRepository(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: GitHubRepositoryOwner,
    val `private`: Boolean,
    @SerialName("default_branch") val defaultBranch: String,
    @SerialName("html_url") val htmlUrl: String,
)

@Serializable
data class GitHubRepositoryOwner(
    val login: String,
    val id: Long,
    val type: String,
)

@Serializable
data class GitHubRepoPermissions(
    val contents: String,
    @SerialName("pull_requests") val pullRequests: String,
)
