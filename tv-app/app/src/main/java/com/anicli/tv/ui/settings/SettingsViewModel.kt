package com.anicli.tv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.data.remote.dto.WebUser
import com.anicli.tv.data.remote.dto.WebUserSettings
import com.anicli.tv.data.remote.dto.WebUserStats
import com.anicli.tv.data.repository.WebUserRepository
import com.anicli.tv.domain.provider.ProviderSelector
import com.anicli.tv.provider.GoGoAnimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferences: android.content.SharedPreferences,
    private val providerSelector: ProviderSelector,
    private val webUserRepository: WebUserRepository
) : ViewModel() {

    // ─── User Profile & Auth State from SQLite Backend ───────────────────────
    val currentUser: StateFlow<WebUser?> = webUserRepository.currentUser
    val userStats: StateFlow<WebUserStats?> = webUserRepository.userStats
    val userSettings: StateFlow<WebUserSettings?> = webUserRepository.userSettings
    val authToken: StateFlow<String?> = webUserRepository.token

    val isAuthLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)

    fun demoLogin(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            val result = webUserRepository.demoLogin()
            isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                authError.value = err.message ?: "Demo login failed"
            }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit = {}) {
        if (username.isBlank() || password.isBlank()) {
            authError.value = "Username and password are required"
            return
        }
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            val result = webUserRepository.login(username, password)
            isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                authError.value = err.message ?: "Login failed"
            }
        }
    }

    fun register(username: String, email: String?, password: String, onSuccess: () -> Unit = {}) {
        if (username.isBlank() || password.isBlank()) {
            authError.value = "Username and password are required"
            return
        }
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            val result = webUserRepository.register(username, email, password)
            isAuthLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                authError.value = err.message ?: "Registration failed"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            webUserRepository.logout()
        }
    }

    // ─── Language preference ────────────────────────────────────────────────
    val preferredLanguage = MutableStateFlow(
        sharedPreferences.getString("pref_language", "dub") ?: "dub"
    )

    fun setPreferredLanguage(lang: String) {
        sharedPreferences.edit().putString("pref_language", lang).apply()
        preferredLanguage.value = lang
    }

    // ─── Provider selection ─────────────────────────────────────────────────
    val activeProvider = MutableStateFlow(providerSelector.getActiveProviderKey())

    fun setActiveProvider(providerKey: String) {
        providerSelector.setActiveProvider(providerKey)
        activeProvider.value = providerKey
    }

    // ─── Web Backend URL ─────────────────────────────────────────────────────
    val backendUrl = MutableStateFlow(
        sharedPreferences.getString(
            com.anicli.tv.provider.WebApiMediaProvider.PREF_BACKEND_URL,
            com.anicli.tv.provider.WebApiMediaProvider.DEFAULT_BACKEND_URL
        ) ?: com.anicli.tv.provider.WebApiMediaProvider.DEFAULT_BACKEND_URL
    )

    fun setBackendUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        sharedPreferences.edit().putString(com.anicli.tv.provider.WebApiMediaProvider.PREF_BACKEND_URL, cleaned).apply()
        backendUrl.value = cleaned
    }

    // ─── GoGoAnime mirror domain ────────────────────────────────────────────
    val gogoDomain = MutableStateFlow(
        sharedPreferences.getString(GoGoAnimeProvider.PREF_DOMAIN, "gogoanime.by") ?: "gogoanime.by"
    )

    fun setGogoDomain(domain: String) {
        val cleaned = domain.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        sharedPreferences.edit().putString(GoGoAnimeProvider.PREF_DOMAIN, cleaned).apply()
        gogoDomain.value = cleaned
    }
}
