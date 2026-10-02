package com.hippo.ehviewer.ktor

import com.ehviewer.core.network.EhCookieStore
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.util.WebViewSupport
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import io.ktor.util.appendIfNameAbsent

// 设备上可能没有可用的 WebView provider，此时退回一个兜底的 Chrome 版本号，不要直接 NPE。
private val WebViewVersion = WebViewSupport.chromeMajorVersion
val CHROME_MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$WebViewVersion.0.0.0 Mobile Safari/537.36"
private val CHROME_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$WebViewVersion.0.0.0 Safari/537.36"
private const val CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
private const val CHROME_ACCEPT_LANGUAGE = "en-US,en;q=0.9"

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureCommon(redirect: Boolean = true) = apply {
    install(HttpCookies) {
        storage = EhCookieStore
    }
    install(HttpTimeout) {
        reset()
        requestTimeoutMillis = 10_000
    }
    install(UserAgent)
    defaultRequest {
        headers.appendIfNameAbsent(HttpHeaders.Accept, CHROME_ACCEPT)
        header(HttpHeaders.AcceptLanguage, CHROME_ACCEPT_LANGUAGE)
    }
    followRedirects = redirect
}

private val UserAgent = createClientPlugin("UserAgent") {
    onRequest { request, _ ->
        request.userAgent(effectiveUserAgent(Settings.desktopSite.value))
    }
}

/**
 * 实际发给服务器的 User-Agent。
 *
 * 默认用（已被钳制过的）WebView 版本拼出来的字符串；如果用户在高级设置里填了自定义 UA，
 * 则原样使用。[WebViewSupport.chromeMajorVersion] 保证默认值不会是一个过旧的 Chrome 版本，
 * 避免被 Cloudflare 直接 challenge。
 *
 * 自定义 UA 的用途：Cloudflare 的 `cf_clearance` 与「解出验证时的 UA」绑定，
 * 从浏览器导出 Cookie 导入本应用时，必须让这里的 UA 与导出浏览器一致，否则 clearance 不生效。
 */
fun effectiveUserAgent(desktop: Boolean): String =
    Settings.customUserAgent.value.trim().ifEmpty {
        if (desktop) CHROME_USER_AGENT else CHROME_MOBILE_USER_AGENT
    }

fun HttpTimeoutConfig.reset() = apply {
    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
}
