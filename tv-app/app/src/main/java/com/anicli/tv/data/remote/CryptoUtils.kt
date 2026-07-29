package com.anicli.tv.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.text.Charsets

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class DynamicKey(val epoch: Int, val key: ByteArray, val serverTimeOffset: Long, val buildId: String, val lane: String)

object CryptoUtils {

    private const val AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
    private const val REFR = "https://mkissa.to"
    private const val CDN = "https://cdn.mkissa.net/all/mk"
    private const val API = "https://api.mkissa.net"

    private fun generateEpoch(): Int {
        val nowMs = System.currentTimeMillis()
        val epoch = nowMs / 259200000L
        return if (nowMs - (epoch * 259200000L) < 86400000L && epoch > 0) {
            (epoch - 1).toInt()
        } else {
            epoch.toInt()
        }
    }

    private fun generateMask(buildId: String): ByteArray {
        val b64s = listOf(
            "10d13/213hI=",
            "XdTA6FaAQSo=",
            "KRCUT9lZh1o=",
            "Aj3ASCIDMnw="
        )
        val keyHex = java.lang.StringBuilder()
        for (block in b64s.indices) {
            val b64 = b64s[block]
            val decoded = Base64.decode(b64, Base64.DEFAULT)
            for (byteIdx in decoded.indices) {
                val embedded = decoded[byteIdx].toInt() and 0xFF
                val index = block * 8 + byteIdx
                val c = buildId[index % buildId.length]
                val asciiVal = c.code
                val buildMaskByte = asciiVal xor (((index * 17) + 31) and 255)
                val tweak = ((block * 41) + (byteIdx * 7)) and 255
                val value = embedded xor buildMaskByte xor tweak
                keyHex.append(String.format("%02x", value))
            }
        }
        return hexStringToByteArray(keyHex.toString())
    }

    /**
     * HMAC-SHA256 helper — mirrors JS `yg(key, message)`.
     * Uses the raw key bytes directly (unlike old code which used a hardcoded string key).
     */
    private fun hmacSha256(keyBytes: ByteArray, message: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
    }

    /**
     * Generates the `x-aa-boot` header token.
     *
     * New JS algorithm (reverse-engineered from DnMm_so9.js MS() function):
     *   1. mask = generateMask(buildId)
     *   2. h    = HMAC_SHA256(key=mask,  msg="aa-boot:t:"+buildId)
     *   3. p    = buildId+":"+keyGroup+":"+hostname+":"+epoch+":"+contentLane
     *   4. y    = HMAC_SHA256(key=h, msg=p)
     *   5. token = hex(y)  [via NS() — byte array → 2-char hex per byte, zero-padded]
     *
     * @param buildId       e.g. "74"
     * @param epoch         rolling epoch integer
     * @param contentLane   e.g. "k2"
     * @param keyGroup      "mkissa" for mkissa.to (resolved by qS() in JS)
     * @param hostname      "mkissa.to" (stripped of www.)
     */
    private fun generateAaBoot(
        buildId: String,
        epoch: Int,
        contentLane: String = "k2",
        keyGroup: String = "mkissa",
        hostname: String = "mkissa.to"
    ): String {
        val mask = generateMask(buildId)

        // Step 1: HMAC(mask, "aa-boot:t:"+buildId)
        val h = hmacSha256(mask, "aa-boot:t:$buildId")

        // Step 2: payload = buildId:keyGroup:hostname:epoch:contentLane
        val payload = "$buildId:$keyGroup:$hostname:$epoch:$contentLane"
        val y = hmacSha256(h, payload)

        // Step 3: NS(y) — hex-encode each byte zero-padded to 2 chars
        val sb = StringBuilder()
        for (b in y) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    suspend fun fetchDynamicKey(client: OkHttpClient): DynamicKey = withContext(Dispatchers.IO) {
        val req1 = Request.Builder()
            .url(REFR)
            .header("User-Agent", AGENT)
            .build()

        val response1 = client.newCall(req1).execute()
        if (!response1.isSuccessful) throw Exception("mkissa.to HTTP ${response1.code}")
        val page = response1.body?.string() ?: throw Exception("mkissa.to body is null")

        val appUrlMatch = Regex("""(https://[^"]+/entry/app\.[^"]+\.js)""").find(page)
        val appUrl = appUrlMatch?.groupValues?.get(1) ?: throw Exception("Failed to find appUrl")

        val req2 = Request.Builder()
            .url(appUrl)
            .header("User-Agent", AGENT)
            .build()

        val response2 = client.newCall(req2).execute()
        if (!response2.isSuccessful) throw Exception("app.js HTTP ${response2.code}")
        val appJs = response2.body?.string() ?: throw Exception("app.js body is null")
        
        val chunks = Regex(""""([.][.]/chunks/[^"]+\.js)"""").findAll(appJs).map { it.groupValues[1] }.toList()
        if (chunks.isEmpty()) throw Exception("No chunks found in app.js")

        var js = ""
        for (chunk in chunks.take(5)) {
            val chunkUrl = "$CDN/_app/immutable/" + chunk.replace("../", "")
            val reqChunk = Request.Builder()
                .url(chunkUrl)
                .header("User-Agent", AGENT)
                .build()
            val chunkJs = client.newCall(reqChunk).execute().body?.string() ?: continue
            js += chunkJs + "\n"
        }

        val buildIdMatch = Regex("""!=="string"\?"([0-9]+)"""").findAll(js.replace(" ", "")).lastOrNull()
        val laneMatch = Regex("""="(k[0-9]+)"""").findAll(js).lastOrNull()

        if (buildIdMatch == null) throw Exception("Failed to find buildId")
        if (laneMatch == null) throw Exception("Failed to find lane")

        val buildId = buildIdMatch.groupValues[1]
        val lane = laneMatch.groupValues[1]
        
        val epoch = generateEpoch()
        val maskBytes = generateMask(buildId)

        // Generate the new-style aa-boot token
        val aaBoot = generateAaBoot(
            buildId = buildId,
            epoch = epoch,
            contentLane = lane,
            keyGroup = "mkissa",   // qS("mkissa.to") → "mkissa"
            hostname = "mkissa.to"
        )

        val bootUrl = "$API/client-crypto/v1/bootstrap?buildId=$buildId&k=$lane"
        val bootReq = Request.Builder()
            .url(bootUrl)
            .header("User-Agent", AGENT)
            .header("x-build-id", buildId)
            .header("x-aa-boot", aaBoot)
            .header("Referer", REFR)
            .header("Origin", REFR)
            .build()
            
        val bootResp = client.newCall(bootReq).execute()
        if (!bootResp.isSuccessful) throw Exception("bootstrap HTTP ${bootResp.code}: ${bootResp.body?.string()}")
        val bootBody = bootResp.body?.string() ?: throw Exception("bootstrap body is null")
        
        var partBBase64 = ""
        try {
            val json = JSONObject(bootBody)
            partBBase64 = json.getString("partB")
        } catch (e: Exception) {
            throw Exception("Failed to parse partB from JSON: $bootBody")
        }
        
        val partB = Base64.decode(partBBase64, Base64.DEFAULT)
        val key = ByteArray(32)
        for (i in 0 until 32) {
            key[i] = maskBytes[i] xor partB[i]
        }
        
        var serverTimeOffset = 0L
        val dateHeader = bootResp.header("Date")
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

        DynamicKey(epoch, key, serverTimeOffset, buildId, lane)
    }

    fun generateAaReq(key: ByteArray, epoch: Int, queryHash: String, serverTimeOffset: Long = 0L, buildId: String, lane: String): String {
        val adjustedTime = System.currentTimeMillis() + serverTimeOffset
        val ts = (adjustedTime / 300000L) * 300000L
        val payloadIv = "$epoch:$queryHash:$ts".toByteArray(Charsets.UTF_8)
        
        val md = MessageDigest.getInstance("SHA-256")
        val iv = md.digest(payloadIv).copyOfRange(0, 12)

        val payload = """{"v":1,"ts":$ts,"epoch":$epoch,"buildId":"$buildId","qh":"$queryHash","k":"$lane"}""".toByteArray(Charsets.UTF_8)

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
