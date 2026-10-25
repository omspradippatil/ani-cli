package com.anicli.tv

import com.anicli.tv.data.remote.WebAnimeService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class WebAnimeServiceTest {

    private lateinit var service: WebAnimeService

    @Before
    fun setup() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        service = Retrofit.Builder()
            .baseUrl("https://ani-cli-steel.vercel.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WebAnimeService::class.java)
    }

    @Test
    fun testGetTrending() = runBlocking {
        val res = service.getTrending(page = 1, perPage = 5)
        assertTrue(res.success)
        assertNotNull(res.data)
        assertFalse(res.data!!.isEmpty())

        val first = res.data!![0]
        println("Trending first: ${first.title?.preferredTitle()}, id=${first.id}")
        assertTrue(first.id > 0)
    }

    @Test
    fun testSearch() = runBlocking {
        val res = service.search(query = "Naruto")
        assertTrue(res.success)
        assertNotNull(res.data)
        val anilist = res.data?.anilist
        assertNotNull(anilist)
        assertFalse(anilist!!.isEmpty())
        println("Search Naruto top match: ${anilist[0].title?.preferredTitle()}")
    }

    @Test
    fun testGetAnimeDetailsAndStream() = runBlocking {
        // Test anime 20 (Naruto)
        val detailsRes = service.getAnimeDetails("20")
        assertTrue(detailsRes.success)
        val detail = detailsRes.data
        assertNotNull(detail)
        println("Details title: ${detail?.title?.preferredTitle()}")
        assertNotNull(detail?.streamEpisodes)
        assertFalse(detail!!.streamEpisodes!!.isEmpty())

        val ep1 = detail.streamEpisodes!![0]
        println("Episode 1 ID: ${ep1.id}")

        // Test stream resolution for episode 1
        val streamRes = service.getStream(ep1.id, "sub")
        assertTrue(streamRes.success)
        val stream = streamRes.data
        assertNotNull(stream)
        println("Stream rawM3u8: ${stream?.rawM3u8}")
        assertNotNull(stream?.rawM3u8)
        assertTrue(stream!!.rawM3u8!!.contains(".m3u8"))
    }
}
