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
        
        // Simple Rate Limiting Interceptor (Max 1 request per 500ms to avoid API bans)
        val lastRequestTime = AtomicLong(0)
        val rateLimitInterceptor = okhttp3.Interceptor { chain ->
            synchronized(this) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime.get()
                if (elapsed < 500) {
                    Thread.sleep(500 - elapsed)
                }
                lastRequestTime.set(System.currentTimeMillis())
            }
            chain.proceed(chain.request())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(rateLimitInterceptor)
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

        val forceRangeInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            if (originalRequest.header("Range") == null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Range", "bytes=0-")
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(originalRequest)
            }
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(forceRangeInterceptor)
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
}
