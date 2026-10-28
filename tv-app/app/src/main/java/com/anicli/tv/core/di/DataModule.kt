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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import android.content.SharedPreferences

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("anicli_tv_prefs", Context.MODE_PRIVATE)
    }

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
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        // Trust all certificates to fix emulator SSL chain validation issues
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
            object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }
        )

        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        // Browser User-Agent and Referer interceptor for image CDNs and APIs
        val defaultHeadersInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            if (original.header("User-Agent") == null) {
                builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            }
            if (original.header("Referer") == null && original.url.host.contains("anilist.co")) {
                builder.header("Referer", "https://anilist.co/")
            }
            chain.proceed(builder.build())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(defaultHeadersInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("VideoClient")
    fun provideVideoOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
            object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }
        )

        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val bootstrapClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        val baseDns = okhttp3.dnsoverhttps.DnsOverHttps.Builder().client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(java.net.InetAddress.getByName("8.8.8.8"), java.net.InetAddress.getByName("8.8.4.4"))
            .build()
            
        val dns = object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                val all = baseDns.lookup(hostname)
                val ipv4Only = all.filterIsInstance<java.net.Inet4Address>()
                return ipv4Only.ifEmpty { all }
            }
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(logging)
            .dns(dns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAnimeApiService(okHttpClient: OkHttpClient): AnimeApiService {
        return Retrofit.Builder()
            // Using AllAnime GraphQL API matching ani-cli
            .baseUrl("https://api.mkissa.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWebAnimeService(
        okHttpClient: OkHttpClient,
        sharedPreferences: SharedPreferences
    ): com.anicli.tv.data.remote.WebAnimeService {
        val defaultUrl = com.anicli.tv.provider.WebApiMediaProvider.DEFAULT_BACKEND_URL
        var prefUrl = sharedPreferences.getString(com.anicli.tv.provider.WebApiMediaProvider.PREF_BACKEND_URL, defaultUrl)
            ?.trim()?.takeIf { it.isNotBlank() } ?: defaultUrl

        // Automatically migrate legacy / proxy URLs directly to production Cloudflare Worker
        if (prefUrl.contains("localhost") || prefUrl.contains("10.0.2.2") || prefUrl.contains("vercel.app")) {
            prefUrl = defaultUrl
            sharedPreferences.edit().putString(com.anicli.tv.provider.WebApiMediaProvider.PREF_BACKEND_URL, defaultUrl).apply()
        }

        val baseUrl = if (prefUrl.endsWith("/")) prefUrl else "$prefUrl/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.anicli.tv.data.remote.WebAnimeService::class.java)
    }
}
