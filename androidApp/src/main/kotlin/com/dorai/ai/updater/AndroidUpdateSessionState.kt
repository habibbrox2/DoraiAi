package com.dorai.ai.updater

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages in-memory and persistent state for Android update installation sessions.
 *
 * Persists session metadata to SharedPreferences so installation progress and diagnostic
 * results survive application process restarts and backgrounding.
 */
internal object AndroidUpdateSessionState {
    private const val PREFS_NAME = "dorai_update_session_state"
    private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
    private const val KEY_TARGET_VERSION_CODE = "target_version_code"
    private const val KEY_TARGET_VERSION_NAME = "target_version_name"
    private const val KEY_LAST_STATUS = "last_status"
    private const val KEY_LAST_MESSAGE = "last_message"

    sealed interface InstallState {
        data object Idle : InstallState
        data class Verifying(val message: String) : InstallState
        data class Staging(val sessionId: Int) : InstallState
        data class AwaitingConfirmation(val sessionId: Int, val confirmationIntent: Intent) : InstallState
        data class Installing(val sessionId: Int) : InstallState
        data class Success(val versionCode: Long, val versionName: String) : InstallState
        data class Failed(val statusCode: Int?, val reason: String) : InstallState
    }

    private val _state = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = _state.asStateFlow()

    fun updateState(newState: InstallState) {
        _state.value = newState
    }

    fun recordActiveSession(
        context: Context,
        sessionId: Int,
        targetVersionCode: Long,
        targetVersionName: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ACTIVE_SESSION_ID, sessionId)
            .putLong(KEY_TARGET_VERSION_CODE, targetVersionCode)
            .putString(KEY_TARGET_VERSION_NAME, targetVersionName)
            .remove(KEY_LAST_STATUS)
            .remove(KEY_LAST_MESSAGE)
            .apply()
    }

    fun recordFailure(context: Context, statusCode: Int?, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LAST_STATUS, statusCode ?: -1)
            .putString(KEY_LAST_MESSAGE, message)
            .remove(KEY_ACTIVE_SESSION_ID)
            .apply()
    }

    fun clearActiveSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_TARGET_VERSION_CODE)
            .remove(KEY_TARGET_VERSION_NAME)
            .apply()
    }

    fun getActiveSessionId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACTIVE_SESSION_ID, -1)
    }

    fun getTargetVersionCode(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TARGET_VERSION_CODE, -1L)
    }

    fun getTargetVersionName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_VERSION_NAME, null)
    }

    fun getLastFailure(context: Context): Pair<Int, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val msg = prefs.getString(KEY_LAST_MESSAGE, null) ?: return null
        val status = prefs.getInt(KEY_LAST_STATUS, -1)
        return Pair(status, msg)
    }

    fun reset() {
        _state.value = InstallState.Idle
    }
}
