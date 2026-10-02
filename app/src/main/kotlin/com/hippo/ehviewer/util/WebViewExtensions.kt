package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView
import com.hippo.ehviewer.ktor.effectiveUserAgent

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = with(settings) {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true

    // 默认用移动端 UA 以绕过 Cloudflare；用户在高级设置里填了自定义 UA 时优先使用它，
    // 这样在 WebView 里解出的 cf_clearance 才能和 HTTP 请求的 UA 对上。
    userAgentString = effectiveUserAgent(desktop = false)
}
