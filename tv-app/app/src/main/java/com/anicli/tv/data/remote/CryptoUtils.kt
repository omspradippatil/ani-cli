package com.anicli.tv.data.remote

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

object CryptoUtils {

    // The key string used in ani-cli
    private const val KEY_STRING = "Xot36i3lK3:v1"

    /**
     * Decrypts the 'tobeparsed' base64 encoded string from AllAnime API.
     * Ported directly from ani-cli's process_response() bash function.
     */
    fun decryptAllAnimeStream(encryptedB64: String): String {
        try {
            // 1. Generate SHA-256 key from the KEY_STRING
            val md = MessageDigest.getInstance("SHA-256")
            val keyBytes = md.digest(KEY_STRING.toByteArray(Charsets.UTF_8))
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            // 2. Decode the base64 payload
            // ani-cli: base64 -d
            val payloadBytes = Base64.decode(encryptedB64, Base64.DEFAULT)

            // 3. Extract IV
            // ani-cli: dd if="$tmp" bs=1 skip=1 count=12
            // It skips the first byte, takes 12 bytes.
            val ivHexBytes = payloadBytes.copyOfRange(1, 13)
            
            // ani-cli: ctr="${iv}00000002"
            // The IV is essentially the 12 extracted bytes as hex string + "00000002".
            // Let's get the hex representation of those 12 bytes.
            val ivHex = ivHexBytes.joinToString("") { "%02x".format(it) }
            val fullIvHex = ivHex + "00000002"
            
            // Convert fullIvHex (32 chars) back to 16 bytes for the IvParameterSpec
            val fullIvBytes = hexStringToByteArray(fullIvHex)
            val ivParameterSpec = IvParameterSpec(fullIvBytes)

            // 4. Extract CipherText
            // ani-cli: skip=13 count=$ct_len, where ct_len = file_size - 13 - 16
            val cipherTextLength = payloadBytes.size - 13 - 16
            if (cipherTextLength <= 0) return ""
            
            val cipherTextBytes = payloadBytes.copyOfRange(13, 13 + cipherTextLength)

            // 5. Decrypt using AES-256-CTR
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
            
            val decryptedBytes = cipher.doFinal(cipherTextBytes)
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
