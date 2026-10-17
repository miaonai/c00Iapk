package com.example.c001apk.util

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import org.mindrot.jbcrypt.BCrypt

/** Token V3 protocol ported from coolapk-desktop v1.36.1 (see THIRD_PARTY_NOTICES.md). */
object CoolapkToken {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val authBlob by lazy {
        CoolapkToken::class.java.getResourceAsStream("/coolapk/auth_blob.b64")!!.bufferedReader()
            .use { it.readText().trim().decodeBase64()!!.utf8() }
    }

    fun generate(deviceCode: String, timestamp: Long = System.currentTimeMillis() / 1000,
                 appCode: Long = 2604201): String {
        for (offset in 0L..15L) {
            try {
                return generateAt(deviceCode, timestamp + offset, appCode)
            } catch (_: IllegalArgumentException) {
                // Some seconds produce a salt outside bcrypt's alphabet; follow the upstream retry.
            }
        }
        error("无法生成酷安 v3 请求签名")
    }

    private fun generateAt(deviceCode: String, timestamp: Long, appCode: Long): String {
        val index = (((timestamp + appCode) % 100) * 4 + 128).toInt()
        val segment = authBlob.substring(index, index + 128).decodeBase64()!!.utf8()
        val plain = "com.coolapk.market&$segment&${deviceCode.encodeUtf8().md5().hex()}&$timestamp&$appCode"
        val password = plain.encodeUtf8().base64().encodeUtf8().md5().hex()
        val saltSource = "${timestamp.toString(16)}/${plain.encodeUtf8().md5().hex()}"
            .encodeUtf8().base64().take(22)
        val shifted = (ALPHABET.indexOf(saltSource.last()) - 5 + 64) % 64
        val salt = saltSource.dropLast(1) + ALPHABET[shifted]
        require(salt.all { it in ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/" })
        val hash = BCrypt.hashpw(password, "\$2a\$04\$$salt").replaceFirst("\$2a\$", "\$2y\$")
        return "v3" + hash.encodeUtf8().base64().trimEnd('=')
    }
}
