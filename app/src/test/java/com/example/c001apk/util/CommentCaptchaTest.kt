package com.example.c001apk.util

import com.example.c001apk.logic.model.TotalReplyResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class CommentCaptchaTest {
    private fun parse(json: String) = CommentCaptcha.from(Gson().fromJson(json, TotalReplyResponse::class.java))

    @Test fun preservesDynamicChallengeAndCookieMode() {
        val challenge = parse("""{"messageStatus":"err_request_captcha_v2","messageExtra":{"captchaId":"abcdef1234567890","captchaField":""}}""")!!
        assertEquals("abcdef1234567890", challenge.id)
        assertEquals("", challenge.field)
    }

    @Test fun acceptsStringEncodedConfigurationAndCustomQueryField() {
        val extra = Gson().toJson("""{"captchaId":"abcdef1234567890","captchaField":"_v2_post_token"}""")
        assertEquals("_v2_post_token", parse("""{"extra":$extra}""")!!.field)
    }

    @Test fun handlesStringErrorsAndDefaultCaptchaConfiguration() {
        assertNotNull(parse("""{"code":403,"error":"captcha","messageStatus":"err_request_captcha_v2"}"""))
        assertNotNull(parse("""{"captchaId":"abcdef1234567890","captchaField":"_token"}"""))
        assertNull(parse("""{"data":[]}"""))
        assertNull(parse("""{"code":403,"message":"访问过于频繁"}"""))
    }

    @Test fun rejectsUntrustedQueryFieldAndHtmlInCaptchaId() {
        assertEquals("", parse("""{"captchaId":"abcdef1234567890","captchaField":"id"}""")!!.field)
        assertNull(parse("""{"captchaId":"<script>alert(1)</script>"}"""))
    }
}
