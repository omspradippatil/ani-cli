package com.anicli.tv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _userEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userEmail.value = firebaseAuth.currentUser?.email
        }
    }
    
    val preferredLanguage = MutableStateFlow(sharedPreferences.getString("pref_language", "dub") ?: "dub")

    fun setPreferredLanguage(lang: String) {
        sharedPreferences.edit().putString("pref_language", lang).apply()
        preferredLanguage.value = lang
    }
    
    fun signOut() {
        auth.signOut()
        // Sign in anonymously right after sign out
        viewModelScope.launch {
            try {
                auth.signInAnonymously()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
