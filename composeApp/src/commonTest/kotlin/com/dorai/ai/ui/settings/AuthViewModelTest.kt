package com.dorai.ai.ui.settings

import com.dorai.ai.cloud.DoraiAuthClient
import com.dorai.ai.data.AppSettings
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTest {

    private fun authClient(status: HttpStatusCode, body: String): DoraiAuthClient = DoraiAuthClient(
        httpClient = HttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ),
        supabaseUrl = "https://project.supabase.co",
        publishableKey = "sb_publishable_test",
    )

    private val tokenResponse = """
        {"access_token":"access-1","refresh_token":"refresh-1","expires_in":3600,
         "user":{"id":"user-1","email":"user@example.com"}}
    """.trimIndent()

    @Test
    fun `signIn persists session and exposes state`() = runTest {
        val settings = AppSettings(MapSettings())
        val viewModel = AuthViewModel(settings, authClient(HttpStatusCode.OK, tokenResponse))

        val result = viewModel.signIn(" user@example.com ", "secret", "Asha")

        assertTrue(result.isSuccess)
        assertTrue(viewModel.isLoggedIn.value)
        assertEquals("user@example.com", viewModel.userEmail.value)
        assertEquals("Asha", viewModel.userDisplayName.value)
        assertEquals("access-1", settings.getSupabaseAccessToken())
        assertEquals("user-1", settings.getUserId())
        val session = settings.getDoraiCloudSession()
        assertNotNull(session)
        assertEquals("refresh-1", session.refreshToken)
    }

    @Test
    fun `signIn failure leaves the user logged out`() = runTest {
        val settings = AppSettings(MapSettings())
        val viewModel = AuthViewModel(
            settings,
            authClient(HttpStatusCode.Unauthorized, """{"msg":"Invalid login credentials"}"""),
        )

        val result = viewModel.signIn("user@example.com", "wrong", "Asha")

        assertTrue(result.isFailure)
        assertFalse(viewModel.isLoggedIn.value)
        assertEquals("", settings.getSupabaseAccessToken())
        assertNull(settings.getDoraiCloudSession())
    }

    @Test
    fun `refreshSession replaces the stored access token`() = runTest {
        val settings = AppSettings(MapSettings())
        val signInViewModel = AuthViewModel(settings, authClient(HttpStatusCode.OK, tokenResponse))
        assertTrue(signInViewModel.signIn("user@example.com", "secret", "Asha").isSuccess)

        val refreshed = """
            {"access_token":"access-2","refresh_token":"refresh-2","expires_in":3600,
             "user":{"id":"user-1","email":"user@example.com"}}
        """.trimIndent()
        val refreshViewModel = AuthViewModel(settings, authClient(HttpStatusCode.OK, refreshed))

        assertTrue(refreshViewModel.refreshSession().isSuccess)
        assertEquals("access-2", settings.getSupabaseAccessToken())
        assertEquals("user@example.com", refreshViewModel.userEmail.value)
    }

    @Test
    fun `signOut clears local session even when remote sign out fails`() = runTest {
        val settings = AppSettings(MapSettings())
        val signedIn = AuthViewModel(settings, authClient(HttpStatusCode.OK, tokenResponse))
        assertTrue(signedIn.signIn("user@example.com", "secret", "Asha").isSuccess)

        val failing = AuthViewModel(
            settings,
            authClient(HttpStatusCode.InternalServerError, """{"msg":"nope"}"""),
        )

        val result = failing.signOut()

        assertTrue(result.isFailure)
        assertFalse(failing.isLoggedIn.value)
        assertEquals("", failing.userEmail.value)
        assertEquals("", settings.getSupabaseAccessToken())
        assertNull(settings.getDoraiCloudSession())
    }

    @Test
    fun `updateDisplayName persists the edit`() = runTest {
        val settings = AppSettings(MapSettings())
        val viewModel = AuthViewModel(settings, authClient(HttpStatusCode.OK, tokenResponse))
        assertTrue(viewModel.signIn("user@example.com", "secret", "Asha").isSuccess)

        viewModel.updateDisplayName("  Asha Rahman  ")

        assertEquals("Asha Rahman", viewModel.userDisplayName.value)
        assertEquals("Asha Rahman", settings.getUserDisplayName())
    }
}
