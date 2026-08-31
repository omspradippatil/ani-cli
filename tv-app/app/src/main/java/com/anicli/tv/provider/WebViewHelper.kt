package com.anicli.tv.provider

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object WebViewHelper {
    private const val TAG = "WebViewHelper"
    
    suspend fun getHtml(context: Context, url: String): String? {
        Log.d(TAG, "Fetching URL: $url")
        val resultHtml = withTimeoutOrNull(20000) {
            suspendCancellableCoroutine { cont ->
                Handler(Looper.getMainLooper()).post {
                    var finished = false
                    try {
                        val webView = WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        }
                        
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                Log.d(TAG, "onPageFinished: $url")
                                view.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { result ->
                                    val html = result?.removeSurrounding("\"")?.replace("\\u003C", "<")?.replace("\\\"", "\"") ?: ""
                                    Log.d(TAG, "HTML received, length: ${html.length}")
                                    Log.d(TAG, "HTML snippet: ${html.take(200)}")
                                    if (!html.contains("window.location.replace") && !html.contains("Just a moment...")) {
                                        if (!finished) {
                                            finished = true
                                            Log.d(TAG, "Returning valid HTML for $url")
                                            cont.resume(html)
                                        }
                                    } else {
                                        Log.d(TAG, "Ignoring HTML due to redirect or challenge")
                                    }
                                }
                            }
                        }
                        webView.loadUrl(url)
                    } catch (e: Exception) {
                        Log.e(TAG, "WebView exception", e)
                        if (!finished) {
                            finished = true
                            cont.resume(null)
                        }
                    }
                }
            }
        }
        
        if (resultHtml == null) {
            Log.e(TAG, "WebView timeout or null for url: $url")
        }
        return resultHtml
    }
}
