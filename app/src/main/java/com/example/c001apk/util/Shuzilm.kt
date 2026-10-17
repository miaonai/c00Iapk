package com.example.c001apk.util

import com.google.gson.JsonParser
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import java.io.ByteArrayOutputStream

/** Synthetic identifiers only; no account cookies or hardware identifiers are sent. */
object Shuzilm {
    private const val ENDPOINT = "https://auni.telecome.cn/a/mdna/report?v=8.7&t=m&p=F9BD0FBDACFE80FCF34370FBCFEE1A98&r=5bb7efdd4c794f0e9563317b7802b995&n=0&l=2&h=0"
    private val key = "xp7j@&!v5]2k#+3z{n9\$q^r%[8f1*d4e:c>0l?m/6)yb-=w}".toByteArray()
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(12, TimeUnit.SECONDS).build()

    fun validId(id: String) = id.matches(Regex("DU[A-Za-z0-9_-]{8,126}"))

    internal fun xor(bytes: ByteArray) = ByteArray(bytes.size) {
        (bytes[it].toInt() xor key[it % key.size].toInt()).toByte()
    }

    internal fun decode(bytes: ByteArray): String {
        require(bytes.size <= 65536) { "数盟响应过大" }
        val raw = xor(bytes)
        val decoded = try {
            InflaterInputStream(raw.inputStream()).use { it.readBytesBounded() }
        } catch (_: java.util.zip.ZipException) { raw }
        val id = JsonParser.parseString(String(decoded, Charsets.UTF_8)).asJsonObject
            .get("n_a")?.asString.orEmpty()
        require(validId(id)) { "数盟未返回有效设备 ID" }
        return id
    }

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            if (output.size() + count > 65536) throw IOException("数盟响应过大")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    fun generate(): String {
        val random = SecureRandom()
        fun hex(size: Int) = ByteArray(size).also { random.nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        val payload = JsonObject().apply {
            addProperty("DUv", "com.coolapk.market")
            addProperty("3mS", "v9.0.1")
            addProperty("AAA", "v1.0")
            addProperty("75c", "11e7b222083a4b732b4b14811f6fc05995a01415eb")
            addProperty("sfOo", hex(4).take(7).uppercase())
            addProperty("foO", hex(8).take(15))
            addProperty("2cO", "${System.currentTimeMillis() / 1000.0}")
            addProperty("w91", hex(16).uppercase())
            addProperty("gEd", hex(16))
            addProperty("6yY", hex(32))
        }
        val compressed = ByteArrayOutputStream().also { output ->
            DeflaterOutputStream(output).use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        }.toByteArray()
        val request = Request.Builder().url(ENDPOINT)
            .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 8.1; TK Watch Build/OPM2.171019.012)")
            .header("Accept-Encoding", "identity")
            .post(xor(compressed).toRequestBody("application/x-www-form-urlencoded".toMediaType())).build()
        return client.newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("数盟服务返回 HTTP ${it.code}")
            decode(it.body!!.byteStream().use { stream -> stream.readBytesBounded() })
        }
    }
}
