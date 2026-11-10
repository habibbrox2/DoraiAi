package com.dorai.ai.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubDeviceFlowTest {

    private fun flow(client: HttpClient, clientId: String? = "test-client-id"): GitHubDeviceFlow = GitHubDeviceFlow(clientId = clientId, httpClient = client)

    @Test
    fun `requestDeviceCode returns awaiting user when device flow succeeds`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"device_code":"device-123","user_code":"WDJB-MJHT","verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}""",
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        val result = flow(client).requestDeviceCode()

        assertTrue(result.isSuccess)
        assertEquals("WDJB-MJHT", result.getOrThrow().userCode)
        assertEquals("https://github.com/login/device", result.getOrThrow().verificationUri)
        assertEquals(5, result.getOrThrow().interval)
        client.close()
    }

    @Test
    fun `pollForToken maps authorization pending to polling without failing`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":"authorization_pending"}""",
                    status = HttpStatusCode.OK,
                )
            },
        )

        val state = flow(client).pollForToken("device-123", intervalSeconds = 5, expiresAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() + 900_000)

        assertIs<GitHubDeviceFlowState.Polling>(state)
        assertEquals(5, state.interval)
        client.close()
    }

    @Test
    fun `pollForToken increases interval by five seconds on slow down`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":"slow_down","interval":10}""",
                    status = HttpStatusCode.OK,
                )
            },
        )

        val state = flow(client).pollForToken("device-123", intervalSeconds = 5, expiresAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() + 900_000)

        assertIs<GitHubDeviceFlowState.Polling>(state)
        assertEquals(10, state.interval)
        client.close()
    }

    @Test
    fun `pollForToken returns expired token after expires in elapses`() = runTest {
        val client = HttpClient(
            MockEngine { error("should not poll when the device code has expired") },
        )

        val state = flow(client).pollForToken("device-123", intervalSeconds = 5, expiresAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - 1)

        assertEquals(GitHubDeviceFlowState.Error(GitHubDeviceFlowError.ExpiredToken), state)
        client.close()
    }

    @Test
    fun `pollForToken returns access denied when user cancels`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(content = """{"error":"access_denied"}""", status = HttpStatusCode.OK)
            },
        )

        val state = flow(client).pollForToken("device-123", intervalSeconds = 5, expiresAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() + 900_000)

        assertEquals(GitHubDeviceFlowState.Error(GitHubDeviceFlowError.AccessDenied), state)
        client.close()
    }

    @Test
    fun `pollForToken throws device flow disabled when app setting missing`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(content = """{"error":"device_flow_disabled"}""", status = HttpStatusCode.OK)
            },
        )

        val state = flow(client).pollForToken("device-123", intervalSeconds = 5, expiresAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() + 900_000)

        assertEquals(GitHubDeviceFlowState.Error(GitHubDeviceFlowError.DeviceFlowDisabled), state)
        client.close()
    }

    @Test
    fun `refreshToken posts grant type refresh token and returns rotated tokens`() = runTest {
        val body = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                (request.body as? TextContent)?.text?.let(body::add)
                respond(
                    content = """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":28800,"refresh_token_expires_in":15897600,"token_type":"bearer","scope":""}""",
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        val result = flow(client).refreshToken("old-refresh")

        assertTrue(result.isSuccess)
        assertEquals("new-access", result.getOrThrow().accessToken)
        assertEquals("new-refresh", result.getOrThrow().refreshToken)
        assertTrue(body.single().contains("grant_type=refresh_token"))
        assertTrue(body.single().contains("refresh_token=old-refresh"))
        client.close()
    }

    @Test
    fun `refreshToken maps bad refresh token to reauth required`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(content = """{"error":"bad_refresh_token"}""", status = HttpStatusCode.OK)
            },
        )

        val result = flow(client).refreshToken("bad-refresh")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
        client.close()
    }
}
