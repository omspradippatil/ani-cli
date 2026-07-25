package com.anicli.tv.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object Mp4UploadExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun extract(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0")
                .header("Referer", "https://mkissa.to/")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext null

            val pattern = Pattern.compile("src:\\s*\"([^\"]+)\"")
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                return@withContext matcher.group(1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
