package com.dorai.ai.cloud

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class DoraiFirebaseAuth {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = httpClient { }
    private var cached: DoraiCloudSession? = null

    actual suspend fun getSession(forceRefresh: Boolean): Result<DoraiCloudSession?> = runCatching {
        val current = cached ?: return@runCatching null
        if (!forceRefresh && !current.isExpired()) return@runCatching current
        val refresh = current.refreshToken ?: return@runCatching null
        val refreshed = refreshIdToken(refresh).getOrThrow()
        cached = refreshed
        refreshed
    }

    actual suspend fun signInWithEmail(email: String, password: String): Result<DoraiCloudSession> = signInWithPassword(email.trim(), password, signUp = false)

    actual suspend fun signUpWithEmail(email: String, password: String): Result<DoraiCloudSession> = signInWithPassword(email.trim(), password, signUp = true)

    actual suspend fun signInWithGoogle(idToken: String, accessToken: String?): Result<DoraiCloudSession> = signInWithIdp("google.com", idToken = idToken, accessToken = accessToken)

    actual suspend fun signInWithFacebook(accessToken: String): Result<DoraiCloudSession> = signInWithIdp("facebook.com", accessToken = accessToken)

    actual suspend fun signInWithGitHub(accessToken: String): Result<DoraiCloudSession> = signInWithIdp("github.com", accessToken = accessToken)

    actual suspend fun signOut(): Result<Unit> = runCatching {
        cached = null
    }

    actual suspend fun getIdToken(forceRefresh: Boolean): Result<String> = runCatching {
        val session = getSession(forceRefresh).getOrThrow() ?: error("No signed-in Firebase user")
        session.accessToken
    }

    private suspend fun signInWithPassword(email: String, password: String, signUp: Boolean): Result<DoraiCloudSession> = runCatching {
        val endpoint = if (signUp) "accounts:signUp" else "accounts:signInWithPassword"
        val body = """{"email":${jsonString(email)},"password":${jsonString(password)},"returnSecureToken":true}"""
        val response = client.post("$IDENTITY_URL/v1/$endpoint?key=$FIREBASE_WEB_API_KEY") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error("Firebase auth failed [${response.status.value}]: ${text.take(200)}")
        parseAuthResponse(text)
    }

    private suspend fun signInWithIdp(
        providerId: String,
        idToken: String? = null,
        accessToken: String? = null,
    ): Result<DoraiCloudSession> = runCatching {
        val postBody = buildString {
            if (idToken != null) append("id_token=$idToken&")
            if (accessToken != null) append("access_token=$accessToken&")
            append("providerId=$providerId")
        }
        val body = """{"postBody":${jsonString(postBody)},"requestUri":"http://localhost","returnIdpCredential":true,"returnSecureToken":true}"""
        val response = client.post("$IDENTITY_URL/v1/accounts:signInWithIdp?key=$FIREBASE_WEB_API_KEY") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error("Firebase social sign-in failed [${response.status.value}]: ${text.take(200)}")
        parseAuthResponse(text)
    }

    private suspend fun refreshIdToken(refreshToken: String): Result<DoraiCloudSession> = runCatching {
        val body = "grant_type=refresh_token&refresh_token=$refreshToken"
        val response = client.post("$SECURE_TOKEN_URL/v1/token?key=$FIREBASE_WEB_API_KEY") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error("Firebase token refresh failed [${response.status.value}]: ${text.take(200)}")
        val obj = json.parseToJsonElement(text).jsonObject
        val idToken = obj["id_token"]?.jsonPrimitive?.content ?: error("Missing id_token")
        val userId = obj["user_id"]?.jsonPrimitive?.content ?: error("Missing user_id")
        val newRefresh = obj["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
        val expiresIn = obj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        val session = DoraiCloudSession(
            userId = userId,
            email = cached?.email,
            accessToken = idToken,
            refreshToken = newRefresh,
            expiresAtEpochSeconds = epochSecondsNow() + expiresIn,
        )
        cached = session
        session
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseAuthResponse(text: String): DoraiCloudSession {
        val obj = json.parseToJsonElement(text).jsonObject
        val idToken = obj["idToken"]?.jsonPrimitive?.content ?: error("Missing idToken")
        val refreshToken = obj["refreshToken"]?.jsonPrimitive?.content
        val localId = obj["localId"]?.jsonPrimitive?.content ?: error("Missing localId")
        val email = obj["email"]?.jsonPrimitive?.content
        val expiresIn = obj["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        val session = DoraiCloudSession(
            userId = localId,
            email = email,
            accessToken = idToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = epochSecondsNow() + expiresIn,
        )
        cached = session
        return session
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    companion object {
        private const val FIREBASE_WEB_API_KEY = DORAI_FIREBASE_WEB_API_KEY
        private const val IDENTITY_URL = "https://identitytoolkit.googleapis.com"
        private const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com"
    }
}
