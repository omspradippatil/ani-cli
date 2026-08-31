package com.anicli.tv.provider

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AnikotoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : MediaProvider {

    override val name: String = "Anikoto"

    private val baseUrl = "https://anikoto.cz"
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    )

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        val result = search("naruto")
        if (result.isSuccess) {
            val items = result.getOrNull() ?: emptyList()
            if (items.isNotEmpty()) {
                emit(Result.success(listOf(MediaRow("Popular on Anikoto", items))))
                return@flow
            }
        }
        emit(Result.success(emptyList()))
    }

    override suspend fun search(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/search?keyword=${query.replace(" ", "+")}"
            val request = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext Result.success(emptyList())

            val results = mutableListOf<MediaItem>()
            val pattern = Regex("""<a[^>]*class=["'][^"']*name\s+d-title[^"']*["'][^>]*href=["'](?:https?://[^/]+)?/watch/([^/]+)/ep-1["'][^>]*>([^<]+)</a>""")
            val matches = pattern.findAll(html).toList()
            val uniqueSlugs = mutableSetOf<String>()
            
            for (match in matches) {
                val slug = match.groupValues[1]
                val title = match.groupValues[2].trim()
                
                if (uniqueSlugs.add(slug)) {
                    results.add(MediaItem(
                        id = slug,
                        title = title,
                        posterUrl = ""
                    ))
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> = withContext(Dispatchers.IO) {
        try {
            // Anikoto does not expose episode lists directly in the HTML.
            // We just return a single episode to allow the player to launch, which will then trigger getStreamSources
            Result.success(MediaDetail(
                id = mediaId,
                title = mediaId.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                description = "Sourced from Anikoto.cz. Bypassing Cloudflare via WebView.",
                posterUrl = "",
                backdropUrl = "",
                genres = emptyList(),
                episodes = listOf(Episode(id = mediaId, number = 1, title = "Episode 1", thumbnail = null, isFiller = false))
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> = withContext(Dispatchers.Main) {
        val resultLink = kotlinx.coroutines.withTimeoutOrNull(30000L) {
            suspendCancellableCoroutine<String?> { continuation ->
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.userAgentString = headers["User-Agent"]

                // Fake layout dimensions so Turnstile renders
                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, 1920, 1080)

                webView.webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        val msg = consoleMessage?.message() ?: ""
                        if (msg.isNotBlank()) {
                            android.util.Log.d("AnikotoProviderJS", msg)
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                var isResumed = false
                var pollingJob: kotlinx.coroutines.Job? = null

                webView.webViewClient = object : WebViewClient() {
                    // Ignore SSL errors or normal page finish, we poll externally
                }

                webView.loadUrl("$baseUrl/watch/$episodeId/ep-1")

                // Start polling and injecting independently of page load events
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.Job())
                pollingJob = scope.launch {
                    while (!isResumed) {
                        kotlinx.coroutines.delay(1000)
                        
                        // Evaluate real Anikoto DOM extraction
                        webView.evaluateJavascript("""
                            (function() {
                                try {
                                    if (window.__extractedLink && window.__extractedLink !== 'null') return;
                                    
                                    // 1. Try to click Vidstreaming / fallback server button
                                    var serverBtns = Array.from(document.querySelectorAll('#w-servers .server, #w-servers li, #w-servers button, #w-servers a, .server-item, [data-server-id]'));
                                    var vidstreamBtn = serverBtns.find(function(el) {
                                        var t = (el.textContent || '').toLowerCase();
                                        return t.indexOf('vidstreaming') !== -1 || t.indexOf('gogo') !== -1 || t.indexOf('vidcloud') !== -1;
                                    });
                                    if (vidstreamBtn && !vidstreamBtn.classList.contains('active')) {
                                        console.log("Clicking Vidstreaming server: " + vidstreamBtn.textContent);
                                        vidstreamBtn.click();
                                    }

                                    // 2. Check for iframe inside #player or document
                                    var iframes = document.querySelectorAll('#player iframe, #player-wrapper iframe, #w-player iframe, iframe');
                                    for (var i = 0; i < iframes.length; i++) {
                                        var src = iframes[i].src || iframes[i].getAttribute('data-src') || '';
                                        if (src && src.startsWith('http') && 
                                            src.indexOf('google') === -1 && 
                                            src.indexOf('recaptcha') === -1 && 
                                            src.indexOf('sharethis') === -1 &&
                                            src.indexOf('cloudflare') === -1) {
                                            console.log("Found stream iframe: " + src);
                                            window.__extractedLink = src;
                                            return;
                                        }
                                    }
                                    
                                    // 3. Check for HTML5 video tag directly
                                    var video = document.querySelector('video source, video');
                                    if (video && video.src && video.src.startsWith('http')) {
                                        console.log("Found direct video src: " + video.src);
                                        window.__extractedLink = video.src;
                                        return;
                                    }
                                } catch(e) {
                                    console.log("Extraction JS error: " + e);
                                }
                            })();
                        """.trimIndent(), null)

                        // Check the result
                        webView.evaluateJavascript("window.__extractedLink") { linkResult ->
                            val link = linkResult?.replace("\"", "")
                            if (link != null && link != "null" && link != "undefined" && !isResumed) {
                                isResumed = true
                                android.util.Log.d("AnikotoProvider", "Successfully extracted link: $link")
                                continuation.resume(link)
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    isResumed = true
                    pollingJob?.cancel()
                }
            }
        }

        if (resultLink != null) {
            try {
                android.util.Log.d("AnikotoProvider", "Resolving stream sources for: $resultLink")
                
                if (resultLink.contains(".m3u8") || resultLink.contains(".mp4")) {
                    return@withContext Result.success(listOf(StreamSource(quality = "Auto (Direct)", url = resultLink)))
                }
                
                // Try GoGoAnime / VidStreaming extractor
                val streams = withContext(Dispatchers.IO) {
                    try {
                        com.anicli.tv.extractor.GoGoAnimeExtractor.extract(resultLink, client)
                    } catch (e: Exception) {
                        android.util.Log.w("AnikotoProvider", "GoGo extractor failed: ${e.message}")
                        emptyList()
                    }
                }
                
                if (streams.isNotEmpty()) {
                    val sourceList = streams.map { StreamSource(quality = it.first, url = it.second) }
                    Result.success(sourceList)
                } else {
                    // Fallback to the extracted embed URL directly
                    Result.success(listOf(StreamSource(quality = "Default (Stream)", url = resultLink)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Could not extract video stream. Anikoto took too long to load."))
        }
    }
}
