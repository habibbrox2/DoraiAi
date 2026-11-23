package com.dorai.ai.ui.settings

import androidx.lifecycle.ViewModel
import com.dorai.ai.data.AppSettings
import kotlin.result.Result

/**
 * ViewModel that exposes auth state from [AppSettings] as [StateFlow]s for Compose.
 * All reads are composition-safe; writes go through [AppSettings] methods which persist
 * to shared preferences (survives restarts).
 */
class AuthViewModel(private val settings: AppSettings) : ViewModel() {

    private val _isLoggedIn = androidx.lifecycle.MutableStateFlow(settings.isUserLoggedIn())
    val isLoggedIn: androidx.lifecycle.StateFlow<Boolean> = _isLoggedIn

    private val _userEmail = androidx.lifecycle.MutableStateFlow(settings.getUserEmail())
    val userEmail: androidx.lifecycle.StateFlow<String> = _userEmail

    private val _userDisplayName = androidx.lifecycle.MutableStateFlow(settings.getUserDisplayName())
    val userDisplayName: androidx.lifecycle.StateFlow<String> = _userDisplayName

    fun login(email: String, displayName: String) {
        settings.setUserLoggedIn(true)
        settings.setUserEmail(email)
        settings.setUserDisplayName(displayName)
        _isLoggedIn.value = true
        _userEmail.value = email
        _userDisplayName.value = displayName
    }

    fun logout() {
        settings.setUserLoggedIn(false)
        settings.setUserEmail("")
        settings.setUserDisplayName("")
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userDisplayName.value = ""
    }
}