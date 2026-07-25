package com.anicli.tv.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.text.Charsets

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class DynamicKey(val epoch: Int, val key: ByteArray, val serverTimeOffset: Long)

object CryptoUtils {

    private const val AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
    private const val REFR = "https://mkissa.to"
    private const val CDN = "https://cdn.mkissa.net/all/mk/_app/immutable"

    suspend fun fetchDynamicKey(client: OkHttpClient): DynamicKey = withContext(Dispatchers.IO) {
        val req1 = Request.Builder()
            .url(REFR)
            .header("User-Agent", AGENT)
            .build()

        val response1 = client.newCall(req1).execute()
        if (!response1.isSuccessful) throw Exception("mkissa.to HTTP ${response1.code}")
        val page = response1.body?.string() ?: throw Exception("mkissa.to body is null")

        val epochMatch = Regex(""""epoch":([0-9]+)""").find(page)
        val partBMatch = Regex(""""partB":"([^"]*)"""").find(page)
        val appUrlMatch = Regex("""(/entry/app\.[A-Za-z0-9_.-]+\.js)""").find(page)

        if (epochMatch == null) throw Exception("Failed to find epoch")
        if (partBMatch == null) throw Exception("Failed to find partB")
        if (appUrlMatch == null) throw Exception("Failed to find appUrl")

        val dateHeader = response1.header("Date")
        var serverTimeOffset = 0L
        if (dateHeader != null) {
            try {
                val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                format.timeZone = TimeZone.getTimeZone("GMT")
                val serverTime = format.parse(dateHeader)?.time ?: System.currentTimeMillis()
                serverTimeOffset = serverTime - System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val epoch = epochMatch.groupValues[1].toInt()
        val partB = Base64.decode(partBMatch.groupValues[1], Base64.DEFAULT)
        val appUrl = CDN + appUrlMatch.groupValues[1]

        val req2 = Request.Builder()
            .url(appUrl)
            .header("User-Agent", AGENT)
            .build()

        val response2 = client.newCall(req2).execute()
        if (!response2.isSuccessful) throw Exception("app.js HTTP ${response2.code}")
        val appJs = response2.body?.string() ?: throw Exception("app.js body is null")
        
        val chunks = Regex(""""[.][.]/chunks/([A-Za-z0-9_.-]+\.js)"""").findAll(appJs).map { it.groupValues[1] }.toList()
        if (chunks.isEmpty()) throw Exception("No chunks found in app.js")

        var aaMaskHex: String? = null
        for (chunk in chunks.take(8)) {
            val reqChunk = Request.Builder()
                .url("$CDN/chunks/$chunk")
                .header("User-Agent", AGENT)
                .build()
            val chunkJs = client.newCall(reqChunk).execute().body?.string() ?: continue
            val maskMatch = Regex("""([0-9a-f]{64})""").find(chunkJs)
            if (maskMatch != null) {
                aaMaskHex = maskMatch.groupValues[1]
                break
            }
        }

        if (aaMaskHex == null) throw Exception("Failed to find aaMaskHex in chunks")

        val mask = hexStringToByteArray(aaMaskHex)
        val key = ByteArray(32)
        for (i in 0 until 32) {
            key[i] = mask[i] xor partB[i]
        }

        DynamicKey(epoch, key, serverTimeOffset)
    }

    fun generateAaReq(key: ByteArray, epoch: Int, queryHash: String, serverTimeOffset: Long = 0L): String {
        val adjustedTime = System.currentTimeMillis() + serverTimeOffset
        val ts = (adjustedTime / 300000L) * 300000L
        val payloadIv = "$epoch:$queryHash:$ts".toByteArray(Charsets.UTF_8)
        
        val md = MessageDigest.getInstance("SHA-256")
        val iv = md.digest(payloadIv).copyOfRange(0, 12)

        val payload = """{"v":1,"ts":$ts,"epoch":$epoch,"qh":"$queryHash"}""".toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKeySpec = SecretKeySpec(key, "AES")
        val gcmParameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)
        
        val cipherTextWithTag = cipher.doFinal(payload)
        
        val out = ByteArray(1 + iv.size + cipherTextWithTag.size)
        out[0] = 0x01
        System.arraycopy(iv, 0, out, 1, iv.size)
        System.arraycopy(cipherTextWithTag, 0, out, 1 + iv.size, cipherTextWithTag.size)
        
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decryptAllAnimeStream(encryptedB64: String, key: ByteArray): String {
        try {
            val payloadBytes = Base64.decode(encryptedB64, Base64.DEFAULT)
            
            // 1 byte prefix, 12 bytes IV, rest is ciphertext + tag
            val iv = payloadBytes.copyOfRange(1, 13)
            val cipherTextWithTag = payloadBytes.copyOfRange(13, payloadBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKeySpec = SecretKeySpec(key, "AES")
            val gcmParameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)

            val decryptedBytes = cipher.doFinal(cipherTextWithTag)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
