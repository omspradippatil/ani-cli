package com.anicli.tv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    private val providerSelector: ProviderSelector
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _userEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userEmail.value = firebaseAuth.currentUser?.email
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
    
    // ─── Auth ───────────────────────────────────────────────────────────────
    fun signOut() {
        auth.signOut()
        viewModelScope.launch {
            try {
                auth.signInAnonymously()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
