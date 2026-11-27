package com.dorai.ai.cloud

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GithubAuthProvider
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class DoraiFirebaseAuth {
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    actual suspend fun getSession(forceRefresh: Boolean): Result<DoraiCloudSession?> = runCatching {
        val user = auth.currentUser ?: return@runCatching null
        val token = user.getIdToken(forceRefresh) ?: return@runCatching null
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signInWithEmail(email: String, password: String): Result<DoraiCloudSession> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password)
        val user = result.user ?: error("Firebase sign-in returned no user")
        val token = user.getIdToken(false) ?: error("Firebase sign-in returned no ID token")
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signUpWithEmail(email: String, password: String): Result<DoraiCloudSession> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password)
        val user = result.user ?: error("Firebase sign-up returned no user")
        val token = user.getIdToken(false) ?: error("Firebase sign-up returned no ID token")
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signInWithGoogle(idToken: String, accessToken: String?): Result<DoraiCloudSession> = runCatching {
        val credential = GoogleAuthProvider.credential(idToken, accessToken)
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: error("Firebase Google sign-in returned no user")
        val token = user.getIdToken(false) ?: error("Firebase Google sign-in returned no ID token")
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signInWithFacebook(accessToken: String): Result<DoraiCloudSession> = runCatching {
        val credential = FacebookAuthProvider.credential(accessToken)
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: error("Firebase Facebook sign-in returned no user")
        val token = user.getIdToken(false) ?: error("Firebase Facebook sign-in returned no ID token")
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signInWithGitHub(accessToken: String): Result<DoraiCloudSession> = runCatching {
        val credential = GithubAuthProvider.credential(accessToken)
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: error("Firebase GitHub sign-in returned no user")
        val token = user.getIdToken(false) ?: error("Firebase GitHub sign-in returned no ID token")
        token.toSession(user.uid, user.email)
    }

    actual suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    actual suspend fun getIdToken(forceRefresh: Boolean): Result<String> = runCatching {
        val user = auth.currentUser ?: error("No signed-in Firebase user")
        user.getIdToken(forceRefresh) ?: error("Firebase returned no ID token")
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.toSession(userId: String, email: String?): DoraiCloudSession {
    val expiresAt = jwtExpEpochSeconds(this) ?: (epochSecondsNow() + 3600L)
    return DoraiCloudSession(
        userId = userId,
        email = email,
        accessToken = this,
        refreshToken = null,
        expiresAtEpochSeconds = expiresAt,
    )
}

@OptIn(ExperimentalEncodingApi::class)
private fun jwtExpEpochSeconds(token: String): Long? = runCatching {
    val payload = token.split('.').getOrNull(1) ?: return@runCatching null
    val padded = when (payload.length % 4) {
        2 -> "$payload=="
        3 -> "$payload="
        else -> payload
    }
    val decoded = Base64.UrlSafe.decode(padded).decodeToString()
    val match = Regex("\"exp\"\\s*:\\s*(\\d+)").find(decoded) ?: return@runCatching null
    match.groupValues[1].toLong()
}.getOrNull()
