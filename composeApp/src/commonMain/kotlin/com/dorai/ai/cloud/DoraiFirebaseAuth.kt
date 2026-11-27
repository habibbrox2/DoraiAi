package com.dorai.ai.cloud

/** Firebase Auth service for Dorai Cloud. Platform actuals use GitLive Firebase SDK where available. */
expect class DoraiFirebaseAuth() {
    /** Returns the currently signed-in session, refreshing the ID token if needed. */
    suspend fun getSession(forceRefresh: Boolean = false): Result<DoraiCloudSession?>

    suspend fun signInWithEmail(email: String, password: String): Result<DoraiCloudSession>

    suspend fun signUpWithEmail(email: String, password: String): Result<DoraiCloudSession>

    suspend fun signInWithGoogle(idToken: String, accessToken: String? = null): Result<DoraiCloudSession>

    suspend fun signInWithFacebook(accessToken: String): Result<DoraiCloudSession>

    suspend fun signInWithGitHub(accessToken: String): Result<DoraiCloudSession>

    suspend fun signOut(): Result<Unit>

    /** Gets a fresh Firebase ID token for Supabase third-party auth / Dorai Cloud API. */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String>
}
