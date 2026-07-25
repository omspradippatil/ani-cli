package com.anicli.tv.extractor

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object OkRuExtractor {
    private val gson = Gson()

    suspend fun extract(url: String, client: OkHttpClient): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()

            val pattern = Pattern.compile("data-options=\"([^\"]+)\"")
            val matcher = pattern.matcher(html)
            
            if (matcher.find()) {
                val dataOptions = matcher.group(1)?.replace("&quot;", "\"") ?: return@withContext emptyList()
                val options = gson.fromJson(dataOptions, JsonObject::class.java)
                val flashvars = options.getAsJsonObject("flashvars")
                val metadataStr = flashvars?.get("metadata")?.asString
                if (metadataStr != null) {
                    val metadata = gson.fromJson(metadataStr, JsonObject::class.java)
                    val videos = metadata.getAsJsonArray("videos")
                    if (videos != null) {
                        for (video in videos) {
                            val v = video.asJsonObject
                            val name = v.get("name")?.asString ?: "Unknown"
                            val videoUrl = v.get("url")?.asString
                            if (videoUrl != null) {
                                result.add(Pair(name, videoUrl))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OkRuExtractor", "Failed to extract from ok.ru: ${e.message}", e)
        }
        return@withContext result
    }
}
