package com.anicli.tv.core.di

import com.anicli.tv.domain.provider.MediaProvider
import com.anicli.tv.provider.MockMediaProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    
    @Binds
    @Singleton
    abstract fun bindMediaProvider(
        mockMediaProvider: MockMediaProvider
    ): MediaProvider
}
