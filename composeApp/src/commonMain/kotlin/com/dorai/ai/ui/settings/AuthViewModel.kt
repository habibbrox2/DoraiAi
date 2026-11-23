package com.dorai.ai.ui.settings

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dorai.ai.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel that exposes auth state from [AppSettings] as [StateFlow]s for Compose.
 * All reads are composition-safe; writes go through [AppSettings] methods which persist
 * to shared preferences (survives restarts).
 */
class AuthViewModel(private val settings: AppSettings) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(settings.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userEmail = MutableStateFlow(settings.getUserEmail())
    val userEmail: StateFlow<String> = _userEmail

    private val _userDisplayName = MutableStateFlow(settings.getUserDisplayName())
    val userDisplayName: StateFlow<String> = _userDisplayName

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