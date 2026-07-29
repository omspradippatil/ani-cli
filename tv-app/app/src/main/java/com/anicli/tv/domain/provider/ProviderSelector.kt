package com.anicli.tv.domain.provider

import android.content.SharedPreferences
import com.anicli.tv.provider.GoGoAnimeProvider
import com.anicli.tv.provider.RealMediaProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that reads `pref_active_provider` from SharedPreferences and
 * returns the currently active [MediaProvider].
 *
 * This is injected into all ViewModels instead of a single hardcoded provider,
 * allowing the user to switch between AllAnime and GoGoAnime at runtime via Settings.
 *
 * Values for `pref_active_provider`:
 *   "allanime"  → RealMediaProvider (AllAnime GraphQL native scraper)
 *   "gogoanime" → GoGoAnimeProvider (GoGoAnime HTML scraper + GoGoCDN extractor)
 */
@Singleton
class ProviderSelector @Inject constructor(
    private val allAnimeProvider: RealMediaProvider,
    private val goGoAnimeProvider: GoGoAnimeProvider,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val PREF_KEY = "pref_active_provider"
        const val PROVIDER_ALLANIME = "allanime"
        const val PROVIDER_GOGOANIME = "gogoanime"
    }

    /** Returns the currently active provider based on the user's preference. */
    fun getActiveProvider(): MediaProvider {
        return when (sharedPreferences.getString(PREF_KEY, PROVIDER_GOGOANIME)) {
            PROVIDER_ALLANIME -> allAnimeProvider
            else              -> goGoAnimeProvider
        }
    }

    /** Returns the current active provider name key. */
    fun getActiveProviderKey(): String =
        sharedPreferences.getString(PREF_KEY, PROVIDER_GOGOANIME) ?: PROVIDER_GOGOANIME

    /** Switches to the specified provider and persists the choice. */
    fun setActiveProvider(providerKey: String) {
        sharedPreferences.edit().putString(PREF_KEY, providerKey).apply()
    }

    /** Returns true if GoGoAnime is currently the active provider. */
    fun isGoGoAnime(): Boolean = getActiveProviderKey() == PROVIDER_GOGOANIME
}
