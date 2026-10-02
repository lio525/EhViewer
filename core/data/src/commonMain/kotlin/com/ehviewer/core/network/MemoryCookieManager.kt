package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url

/**
 * 纯内存的 Cookie 存储。
 *
 * 当设备没有可用的系统 WebView 时，`android.webkit.CookieManager` 无法初始化
 * （它由 WebView provider 提供实现），[AndroidCookieManager] 会退回这个实现，
 * 让 App 至少能在访客模式下正常浏览、下载，而不是启动即崩溃。
 *
 * 实现上使用「不可变快照 + 整体替换」的写时复制方式：内部持有的 [Map] 一旦发布就不再修改，
 * 因此不需要锁，也不用担心 JVM 之外的平台缺少 `synchronized`。
 * 极端并发下可能丢失一次写入，对这个降级实现来说可以接受。
 *
 * 与浏览器行为的差异（有意为之的简化）：
 * - 只保存在内存里，进程结束即丢失，登录态无法跨启动保留；
 * - 域名匹配只做「完全相同」或「子域」两种判定，不区分 scheme / path / Secure / HttpOnly；
 * - 不从磁盘读取，也不会写磁盘。
 */
class MemoryCookieManager : CookieManager {

    /** 以注册域为 key，保存该域下的 `name -> value`；整体替换，读到的永远是完整快照。 */
    private var store: Map<String, Map<String, String>> = emptyMap()

    override fun getCookies(url: Url): Map<String, String>? {
        val host = normalize(url.host)
        val snapshot = store
        val result = mutableMapOf<String, String>()
        for ((domain, cookies) in snapshot) {
            if (host == domain || host.endsWith(".$domain")) {
                result.putAll(cookies)
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }

    override fun setCookie(url: Url, cookie: Cookie) {
        val domain = normalize(cookie.domain ?: url.host)
        val snapshot = store
        val cookies = snapshot[domain].orEmpty()
        // ktor 约定 maxAge == 0 表示立即删除
        val updated = if (cookie.maxAge == 0) {
            cookies - cookie.name
        } else {
            cookies + (cookie.name to cookie.value)
        }
        store = if (updated.isEmpty()) snapshot - domain else snapshot + (domain to updated)
    }

    override fun removeAllCookies() {
        store = emptyMap()
    }

    override fun flush() = Unit

    private fun normalize(host: String) = host.lowercase().removePrefix(".")
}