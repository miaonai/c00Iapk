package com.example.c001apk.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.c001apk.util.CommentCaptcha
import com.example.c001apk.util.CommentCaptchaSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson

class CommentCaptchaDialog : DialogFragment() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val captcha = CommentCaptcha(args.getString("id")!!, args.getString("field").orEmpty())
        val owner = args.getString("owner")!!
        val web = WebView(requireContext())
        webView = web
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
        }
        web.addJavascriptInterface(Bridge { token ->
            web.post {
                if (!isAdded) return@post
                if (CommentCaptchaSession.save(owner, captcha, token)) {
                    parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
                } else {
                    Toast.makeText(requireContext(), "验证未完成或账号已切换，请重试", Toast.LENGTH_SHORT).show()
                }
                dismissAllowingStateLoss()
            }
        }, "CoolapkCaptcha")
        val id = Gson().toJson(captcha.id)
        web.loadDataWithBaseURL("https://account.coolapk.com/", """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:16px;font-family:sans-serif"><p id="status">正在加载酷安人机验证…</p><div id="captcha"></div>
            <script>
            var finished = false;
            var timer = setTimeout(function(){document.getElementById('status').textContent='验证加载超时，请取消后重试';},15000);
            function startCaptcha(){
              initNECaptcha({captchaId:$id,element:'#captcha',mode:'popup',apiVersion:2,protocol:'https',width:'280px',lang:'zh-CN',
                onVerify:function(err,data){
                  if(!err && data && data.validate && !finished){finished=true;clearTimeout(timer);CoolapkCaptcha.verified('NEC:'+$id.slice(0,8)+':'+data.validate);}
                },onClose:function(){document.getElementById('status').textContent='验证已取消，可关闭后重试';}
              },function(instance){clearTimeout(timer);document.getElementById('status').textContent='请完成验证';instance.verify();},
                function(){document.getElementById('status').textContent='验证加载失败，请取消后重试';});
            }
            </script><script src="https://cstaticdun.126.net/load.min.js" onload="startCaptcha()"
              onerror="document.getElementById('status').textContent='验证脚本加载失败，请检查网络后重试'"></script></body></html>
        """.trimIndent(), "text/html", "UTF-8", null)
        return MaterialAlertDialogBuilder(requireContext()).setTitle("酷安人机验证")
            .setView(web, 0, 0, 0, 0).setNegativeButton("取消", null).create().also {
                web.minimumHeight = (400 * resources.displayMetrics.density).toInt()
            }
    }

    override fun onDestroyView() {
        webView?.apply { removeJavascriptInterface("CoolapkCaptcha"); stopLoading(); destroy() }
        webView = null
        super.onDestroyView()
    }

    @Keep
    class Bridge(private val complete: (String) -> Unit) {
        @JavascriptInterface fun verified(token: String) = complete(token)
    }

    companion object {
        const val RESULT_KEY = "comment_captcha_verified"
        fun show(manager: FragmentManager, captcha: CommentCaptcha) {
            if (manager.isStateSaved || manager.findFragmentByTag(RESULT_KEY) != null) return
            CommentCaptchaDialog().apply {
                arguments = Bundle().apply {
                    putString("id", captcha.id)
                    putString("field", captcha.field)
                    putString("owner", CommentCaptchaSession.owner())
                }
            }.show(manager, RESULT_KEY)
        }
    }
}
