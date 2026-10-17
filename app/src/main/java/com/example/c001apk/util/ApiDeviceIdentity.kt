package com.example.c001apk.util

import android.content.Context
import android.os.SystemClock
import com.example.c001apk.MyApplication
import okio.ByteString.Companion.decodeBase64

object ApiDeviceIdentity {
    private val retryAfter = mutableMapOf<String, Long>()
    private val preferences get() = MyApplication.context.getSharedPreferences("api_device_identity", Context.MODE_PRIVATE)

    fun hasIdentity(): Boolean = PrefManager.SZLMID.isNotEmpty() ||
        Shuzilm.validId(preferences.getString(CommentCaptchaSession.owner(), "").orEmpty())

    @Synchronized
    fun deviceCode(owner: String = CommentCaptchaSession.owner()): String {
        val original = TokenDeviceUtils.getLastingDeviceCode()
        // A manually configured DUID takes precedence over generated identities.
        if (Shuzilm.validId(PrefManager.SZLMID)) return original
        var id = preferences.getString(owner, "").orEmpty()
        if (!Shuzilm.validId(id)) {
            if (SystemClock.elapsedRealtime() < (retryAfter[owner] ?: 0L)) return original
            try {
                id = Shuzilm.generate()
                if (!preferences.edit().putString(owner, id).commit()) return original
            } catch (_: Exception) {
                retryAfter[owner] = SystemClock.elapsedRealtime() + 60_000
                return original
            }
        }
        val fields = original.reversed().decodeBase64()?.utf8()?.split(';')?.toMutableList()
            ?: return original
        fields[0] = id
        return DeviceCode.encode(fields.joinToString(";"))
    }
}
