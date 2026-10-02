package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderSetCookieHeader

/**
 * 系统 CookieManager 的适配器。
 *
 * 正常的实现是 [android.webkit.CookieManager]，它由系统的 WebView provider 提供。
 * 设备上没有可用 WebView 时 `getInstance()` 会抛 `AndroidRuntimeException`，
 * 这里探测一次并退回 [MemoryCookieManager]，避免启动即崩溃。
 */
class AndroidCookieManager : CookieManager {
    private val manager: CookieManager =
        runCatching { android.webkit.CookieManager.getInstance() }
            .getOrNull()
            ?.let(::SystemCookieManager)
            ?: MemoryCookieManager()

    override fun getCookies(url: Url) = manager.getCookies(url)
    override fun setCookie(url: Url, cookie: Cookie) = manager.setCookie(url, cookie)
    override fun removeAllCookies() = manager.removeAllCookies()
    override fun flush() = manager.flush()
}

private class SystemCookieManager(
    private val manager: android.webkit.CookieManager,
) : CookieManager {
    // 每个方法都兜一层：provider 存在但状态异常时（例如运行中被停用）也不要把 App 带崩。
    override fun getCookies(url: Url): Map<String, String>? =
        runCatching { manager.getCookie(url.toString()) }.getOrNull()?.let { parseClientCookiesHeader(it) }

    override fun setCookie(url: Url, cookie: Cookie) {
        runCatching { manager.setCookie(url.toString(), renderSetCookieHeader(cookie)) }
    }

    override fun removeAllCookies() {
        runCatching { manager.removeAllCookies(null) }
    }

    override fun flush() {
        runCatching { manager.flush() }
    }
}

actual fun getCookieManager(): CookieManager = AndroidCookieManager()
