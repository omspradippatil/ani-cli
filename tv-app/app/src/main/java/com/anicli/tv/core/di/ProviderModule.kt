package com.anicli.tv.core.di

import com.anicli.tv.domain.provider.MediaProvider
import com.anicli.tv.provider.RealMediaProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    abstract fun bindMediaProvider(
        realMediaProvider: RealMediaProvider
    ): MediaProvider
}
