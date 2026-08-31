package com.anicli.tv.domain.provider

import android.content.SharedPreferences
import com.anicli.tv.provider.AnikotoProvider
import com.anicli.tv.provider.GoGoAnimeProvider
import com.anicli.tv.provider.RealMediaProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderSelector @Inject constructor(
    private val allAnimeProvider: RealMediaProvider,
    private val goGoAnimeProvider: GoGoAnimeProvider,
    private val anikotoProvider: AnikotoProvider,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val PREF_KEY = "pref_active_provider"
        const val PROVIDER_ALLANIME = "allanime"
        const val PROVIDER_GOGOANIME = "gogoanime"
        const val PROVIDER_ANIKOTO = "anikoto"
    }

    fun getActiveProvider(): MediaProvider {
        return anikotoProvider
    }

    fun getActiveProviderKey(): String = PROVIDER_ANIKOTO

    fun setActiveProvider(providerKey: String) {
        sharedPreferences.edit().putString(PREF_KEY, providerKey).apply()
    }

    fun isGoGoAnime(): Boolean = getActiveProviderKey() == PROVIDER_GOGOANIME
}
