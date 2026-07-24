package com.anicli.tv

import com.anicli.tv.data.remote.CryptoUtils
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import android.util.Base64

class CryptoUtilsTest {

    @Test
    fun testFetchDynamicKey() = runBlocking {
        val client = OkHttpClient()
        
        val AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
        val REFR = "https://mkissa.to"
        val CDN = "https://cdn.mkissa.net/all/mk/_app/immutable"

        val req1 = Request.Builder()
            .url(REFR)
            .header("User-Agent", AGENT)
            .build()

        val page = client.newCall(req1).execute().body?.string()
        if (page == null) {
            println("page is null")
            assert(false)
            return@runBlocking
        }

        val epochMatch = Regex(""""epoch":([0-9]+)""").find(page)
        val partBMatch = Regex(""""partB":"([^"]*)"""").find(page)
        val appUrlMatch = Regex("""(/entry/app\.[A-Za-z0-9_.-]+\.js)""").find(page)

        println("epochMatch: ${epochMatch != null}")
        println("partBMatch: ${partBMatch != null}")
        println("appUrlMatch: ${appUrlMatch != null}")
        
        if (epochMatch == null || partBMatch == null || appUrlMatch == null) {
            assert(false)
            return@runBlocking
        }
        
        val appUrl = CDN + appUrlMatch.groupValues[1]
        println("appUrl: $appUrl")
        
        val req2 = Request.Builder()
            .url(appUrl)
            .header("User-Agent", AGENT)
            .build()

        val appJs = client.newCall(req2).execute().body?.string()
        if (appJs == null) {
            println("appJs is null")
            assert(false)
            return@runBlocking
        }
        
        val chunks = Regex(""""[.][.]/chunks/([A-Za-z0-9_.-]+\.js)"""").findAll(appJs).map { it.groupValues[1] }.toList()
        println("chunks size: ${chunks.size}")
        
        var aaMaskHex: String? = null
        for (chunk in chunks.take(5)) {
            println("fetching chunk: $chunk")
            val reqChunk = Request.Builder()
                .url("$CDN/chunks/$chunk")
                .header("User-Agent", AGENT)
                .build()
            val chunkJs = client.newCall(reqChunk).execute().body?.string() ?: continue
            val maskMatch = Regex("""([0-9a-f]{64})""").find(chunkJs)
            if (maskMatch != null) {
                aaMaskHex = maskMatch.groupValues[1]
                println("Found mask!")
                break
            }
        }
        
        println("aaMaskHex: $aaMaskHex")
        assert(aaMaskHex != null)
    }
}
