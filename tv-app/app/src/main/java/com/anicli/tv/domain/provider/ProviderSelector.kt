package com.anicli.tv.domain.provider

import android.content.SharedPreferences
import com.anicli.tv.provider.AnikotoProvider
import com.anicli.tv.provider.GoGoAnimeProvider
import com.anicli.tv.provider.RealMediaProvider
import com.anicli.tv.provider.WebApiMediaProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderSelector @Inject constructor(
    private val webApiMediaProvider: WebApiMediaProvider,
    private val allAnimeProvider: RealMediaProvider,
    private val goGoAnimeProvider: GoGoAnimeProvider,
    private val anikotoProvider: AnikotoProvider,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val PREF_KEY = "pref_active_provider"
        const val PROVIDER_WEB_API = "web_api"
        const val PROVIDER_ALLANIME = "allanime"
        const val PROVIDER_GOGOANIME = "gogoanime"
        const val PROVIDER_ANIKOTO = "anikoto"
    }

    fun getActiveProvider(): MediaProvider {
        return when (getActiveProviderKey()) {
            PROVIDER_WEB_API -> webApiMediaProvider
            PROVIDER_ALLANIME -> allAnimeProvider
            PROVIDER_GOGOANIME -> goGoAnimeProvider
            PROVIDER_ANIKOTO -> anikotoProvider
            else -> webApiMediaProvider
        }
    }

    fun getActiveProviderKey(): String {
        return sharedPreferences.getString(PREF_KEY, PROVIDER_WEB_API) ?: PROVIDER_WEB_API
    }

    fun setActiveProvider(providerKey: String) {
        sharedPreferences.edit().putString(PREF_KEY, providerKey).apply()
    }

    fun isWebApi(): Boolean = getActiveProviderKey() == PROVIDER_WEB_API
    fun isGoGoAnime(): Boolean = getActiveProviderKey() == PROVIDER_GOGOANIME
}
