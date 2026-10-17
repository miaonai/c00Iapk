package com.example.c001apk.util

import com.example.c001apk.MyApplication
import com.example.c001apk.logic.model.TotalReplyResponse
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.net.URLEncoder

data class CommentCaptcha(val id: String, val field: String) {
    companion object {
        private const val DEFAULT_ID = "414e5c9b866a03db03f860a1a9101672"

        fun from(response: TotalReplyResponse): CommentCaptcha? {
            fun configuration(element: JsonElement?): CommentCaptcha? = runCatching {
                val value = if (element?.isJsonPrimitive == true)
                    JsonParser.parseString(element.asString) else element
                val obj = value?.takeIf { it.isJsonObject }?.asJsonObject ?: return@runCatching null
                val id = obj.get("captchaId")?.asString ?: return@runCatching null
                if (!id.matches(Regex("[A-Za-z0-9]{8,64}"))) return@runCatching null
                val field = obj.get("captchaField")?.asString.orEmpty()
                CommentCaptcha(id, field.takeIf { it.matches(Regex("_[A-Za-z0-9_]{1,63}")) }.orEmpty())
            }.getOrNull()
            configuration(response.messageExtra ?: response.extra)?.let { return it }
            response.captchaId?.takeIf { it.matches(Regex("[A-Za-z0-9]{8,64}")) }?.let {
                return CommentCaptcha(it, response.captchaField.orEmpty()
                    .takeIf { field -> field.matches(Regex("_[A-Za-z0-9_]{1,63}")) }.orEmpty())
            }
            if (response.messageStatus?.contains("captcha") == true ||
                response.code == 403 && response.message?.contains("验证码") == true) {
                return CommentCaptcha(DEFAULT_ID, "")
            }
            return null
        }
    }
}

object CommentCaptchaSession {
    fun owner() = if (PrefManager.isLogin) "account:${PrefManager.uid}" else "guest"
    private val preferences get() = MyApplication.context.getSharedPreferences("comment_captcha", 0)

    fun save(owner: String, captcha: CommentCaptcha, token: String): Boolean {
        if (owner != owner() || !token.startsWith("NEC:${captcha.id.take(8)}:") || token.length > 16384) return false
        preferences.edit().putString("$owner:token", token).putString("$owner:field", captcha.field).apply()
        return true
    }

    fun credential(): Pair<String, String>? {
        val owner = owner()
        val token = preferences.getString("$owner:token", null) ?: return null
        return preferences.getString("$owner:field", "").orEmpty() to token
    }

    fun cookie(token: String) = "validate=" + URLEncoder.encode(token, "UTF-8")
}
