package com.anicli.tv.core.di

import android.content.Context
import androidx.room.Room
import com.anicli.tv.data.local.AppDatabase
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.remote.AnimeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "anicli_tv_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFavoriteDao(appDatabase: AppDatabase): FavoriteDao {
        return appDatabase.favoriteDao()
    }

    @Provides
    fun provideWatchProgressDao(appDatabase: AppDatabase): WatchProgressDao {
        return appDatabase.watchProgressDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAnimeApiService(okHttpClient: OkHttpClient): AnimeApiService {
        return Retrofit.Builder()
            // Using a public hosted Consumet API instance for demonstration
            .baseUrl("https://api.consumet.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }
}
