package com.example.c001apk.util

import com.example.c001apk.constant.Constants.APP_ID
import com.example.c001apk.constant.Constants.CHANNEL
import com.example.c001apk.constant.Constants.DARK_MODE
import com.example.c001apk.constant.Constants.LOCALE
import com.example.c001apk.constant.Constants.MODE
import com.example.c001apk.constant.Constants.REQUEST_WITH
import com.example.c001apk.util.CookieUtil.SESSID
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder: Request.Builder = request.newBuilder()
        val owner = CommentCaptchaSession.owner()
        val cookie = if (PrefManager.isLogin)
            "uid=${PrefManager.uid}; username=${PrefManager.username}; token=${PrefManager.token}" else SESSID
        val appCode = PrefManager.VERSION_CODE
        val deviceCode = ApiDeviceIdentity.deviceCode(owner)
        val token = CoolapkToken.generate(deviceCode, appCode = appCode.toLong())
        val captcha = CommentCaptchaSession.credential()
        if (captcha != null && captcha.first.isNotEmpty()) {
            builder.url(request.url.newBuilder().setQueryParameter(captcha.first, captcha.second).build())
        }
        val validationCookie = captcha?.takeIf { it.first.isEmpty() }
            ?.let { "; ${CommentCaptchaSession.cookie(it.second)}" }.orEmpty()
        builder.apply {
            addHeader("User-Agent", PrefManager.USER_AGENT)
            addHeader("X-Requested-With", REQUEST_WITH)
            addHeader("X-Sdk-Int", PrefManager.SDK_INT)
            addHeader("X-Sdk-Locale", LOCALE)
            addHeader("X-App-Id", APP_ID)
            addHeader("X-App-Token", token)
            addHeader("X-App-Version", PrefManager.VERSION_NAME)
            addHeader("X-App-Code", appCode)
            addHeader("X-Api-Version", PrefManager.API_VERSION)
            addHeader("X-App-Device", deviceCode)
            addHeader("X-Dark-Mode", DARK_MODE)
            addHeader("X-App-Channel", CHANNEL)
            addHeader("X-App-Mode", MODE)
            addHeader("X-App-Supported", appCode)
            if (request.method != "GET")
                addHeader("Content-Type", "application/x-www-form-urlencoded")
            addHeader("Cookie", cookie + validationCookie)
        }
        if (owner != CommentCaptchaSession.owner()) throw IOException("账号已切换，请重试请求")
        return chain.proceed(builder.build())
    }
}
