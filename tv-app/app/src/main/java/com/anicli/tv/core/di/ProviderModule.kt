package com.anicli.tv.core.di

import com.anicli.tv.domain.provider.MediaProvider
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the active [MediaProvider] via [ProviderSelector].
 *
 * Both [com.anicli.tv.provider.RealMediaProvider] (AllAnime) and
 * [com.anicli.tv.provider.GoGoAnimeProvider] are Hilt singletons (via @Inject constructors).
 * This module resolves the active one at runtime based on SharedPreferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideActiveMediaProvider(selector: ProviderSelector): MediaProvider {
        // NOTE: This provides the provider at injection time.
        // ViewModels that need dynamic switching should inject ProviderSelector directly.
        return selector.getActiveProvider()
    }
}
