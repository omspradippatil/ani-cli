package com.anicli.tv.extractor

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * GoGoCDN / VidStreaming stream extractor.
 *
 * Replicates the ani-cli GoGoAnime decryption flow:
 *  1. Fetch the GoGoCDN embed page
 *  2. Extract encrypted `data-value` from the page
 *  3. AES-256-CBC decrypt using well-known static keys
 *  4. POST to `/encrypt-ajax.php` to get the stream list JSON
 *  5. Decrypt the response and return `(quality, m3u8_url)` pairs
 *
 * Known static keys (public, extracted from GoGoCDN JS bundle):
 *   Key  = "37911490979715163134003223491201"  (32 bytes)
 *   IV   = "3134003223491201"                  (16 bytes)
 *   Key2 = "54674138327930866480207815084989"  (32 bytes)
 */
object GoGoAnimeExtractor {

    private const val TAG = "GoGoAnimeExtractor"

    // AES keys extracted from GoGoCDN JS bundle — same ones used by ani-cli
    private val ENCRYPTION_KEY = "37911490979715163134003223491201".toByteArray()
    private val ENCRYPTION_IV  = "3134003223491201".toByteArray()
    private val SECOND_KEY     = "54674138327930866480207815084989".toByteArray()

    /**
     * Extract direct HLS stream URLs from a GoGoCDN / VidStreaming / GoGoPlay embed URL.
     * Returns a list of (quality_label, m3u8_url) pairs sorted best quality first.
     */
    suspend fun extract(embedUrl: String, client: OkHttpClient): List<Pair<String, String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val sources = mutableListOf<Pair<String, String>>()

            // Normalize the embed URL
            val normalizedUrl = if (embedUrl.startsWith("//")) "https:$embedUrl" else embedUrl

            // Determine the CDN base (gogo-cdn.com, vidstreaming, gogoplay, etc.)
            val parsedUrl = java.net.URL(normalizedUrl)
            val cdnBase = "${parsedUrl.protocol}://${parsedUrl.host}"

            // Fetch the embed page
            val embedReq = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0")
                .header("Referer", "https://gogoanimes.fi/")
                .build()
            val embedHtml = client.newCall(embedReq).execute().body?.string() ?: return@withContext emptyList()

            // Extract the crypto_key, second_key, iv from the page's script data-value attribute
            // ani-cli pattern: grep data-value from the video iframe page
            val dataValue = extractDataValue(embedHtml) ?: run {
                Log.w(TAG, "Could not find data-value in embed page")
                return@withContext emptyList()
            }

            // Extract the id param from embed URL
            val idParam = parsedUrl.query?.split("&")
                ?.find { it.startsWith("id=") }
                ?.removePrefix("id=")
                ?: extractIdFromPath(parsedUrl.path)
                ?: run {
                    Log.w(TAG, "Could not extract ID from embed URL: $normalizedUrl")
                    return@withContext emptyList()
                }

            // Decrypt the data-value with key1
            val decryptedData = aesDecrypt(dataValue, ENCRYPTION_KEY, ENCRYPTION_IV) ?: return@withContext emptyList()

            // Re-encrypt the id with key1 for the ajax request
            val encryptedId = aesEncrypt(idParam, ENCRYPTION_KEY, ENCRYPTION_IV) ?: return@withContext emptyList()

            // Build the encrypt-ajax.php request URL
            val ajaxUrl = "$cdnBase/encrypt-ajax.php?id=${java.net.URLEncoder.encode(encryptedId, "UTF-8")}&alias=$idParam&${decryptedData.split("&").drop(1).joinToString("&")}"

            val ajaxReq = Request.Builder()
                .url(ajaxUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0")
                .header("Referer", normalizedUrl)
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            val ajaxResponse = client.newCall(ajaxReq).execute().body?.string() ?: return@withContext emptyList()

            // Decrypt the ajax response with key2
            val responseJson = try {
                val jsonObj = JSONObject(ajaxResponse)
                val encryptedData = jsonObj.optString("data", "")
                if (encryptedData.isNotBlank()) {
                    aesDecrypt(encryptedData, SECOND_KEY, ENCRYPTION_IV) ?: ajaxResponse
                } else {
                    ajaxResponse
                }
            } catch (e: Exception) {
                ajaxResponse
            }

            // Parse the sources JSON
            val sourcesJson = JSONObject(responseJson)
            val sourceArr = sourcesJson.optJSONArray("source")

            if (sourceArr != null) {
                for (i in 0 until sourceArr.length()) {
                    val src = sourceArr.getJSONObject(i)
                    val file = src.optString("file", "")
                    val label = src.optString("label", "Auto")
                    if (file.isNotBlank() && (file.contains(".m3u8") || file.contains("m3u8"))) {
                        sources.add(Pair(label.ifBlank { "Auto" }, file))
                    }
                }
            }

            // Also check source_bk (backup sources)
            val backupArr = sourcesJson.optJSONArray("source_bk")
            if (backupArr != null && sources.isEmpty()) {
                for (i in 0 until backupArr.length()) {
                    val src = backupArr.getJSONObject(i)
                    val file = src.optString("file", "")
                    val label = src.optString("label", "Backup")
                    if (file.isNotBlank()) {
                        sources.add(Pair(label.ifBlank { "Backup" }, file))
                    }
                }
            }

            // Sort: 1080 > 720 > 480 > 360 > Auto
            sources.sortedByDescending { (label, _) ->
                when {
                    label.contains("1080") -> 1080
                    label.contains("720")  -> 720
                    label.contains("480")  -> 480
                    label.contains("360")  -> 360
                    else                   -> 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GoGoAnime extraction failed for $embedUrl", e)
            emptyList()
        }
    }

    // ─── AES Helpers ──────────────────────────────────────────────────────────

    private fun aesDecrypt(encryptedB64: String, key: ByteArray, iv: ByteArray): String? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val decrypted = cipher.doFinal(Base64.decode(encryptedB64, Base64.DEFAULT))
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES decrypt failed", e)
            null
        }
    }

    private fun aesEncrypt(plainText: String, key: ByteArray, iv: ByteArray): String? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            Log.e(TAG, "AES encrypt failed", e)
            null
        }
    }

    // ─── HTML Parsing Helpers ─────────────────────────────────────────────────

    /**
     * Extracts the `data-value` attribute from the embed page HTML.
     * GoGoCDN puts it on a <div class="wrapper"> or on the <div id="player">
     */
    private fun extractDataValue(html: String): String? {
        val patterns = listOf(
            Regex("""data-value="([^"]+)""""),
            Regex("""data-value='([^']+)'""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractIdFromPath(path: String): String? {
        // e.g. /embedplus?id=XXXXX or /embed/XXXXX
        val segments = path.split("/").filter { it.isNotBlank() }
        return segments.lastOrNull()?.takeIf { it.length > 4 }
    }
}
