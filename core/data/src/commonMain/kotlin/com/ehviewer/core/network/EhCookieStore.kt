package com.ehviewer.core.network

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object EhCookieStore : CookiesStorage {
    private val manager = getCookieManager()
    private val urlE = URLBuilder(URLProtocol.HTTPS, "e-hentai.org").build()
    private val urlEx = URLBuilder(URLProtocol.HTTPS, "exhentai.org").build()

    fun removeAllCookies() = manager.removeAllCookies()

    fun hasSignedIn(): Boolean = manager.getCookies(urlE)?.run {
        containsKey(KEY_IPB_MEMBER_ID) && containsKey(KEY_IPB_PASS_HASH)
    } == true

    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private const val KEY_HATH_PERKS = "hath_perks"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = Cookie(
        name = KEY_CONTENT_WARNING,
        value = CONTENT_WARNING_NOT_SHOW,
    )

    fun clearIgneous() {
        manager.setCookie(
            urlEx,
            Cookie(KEY_IGNEOUS, "", maxAge = 0, domain = urlEx.host, path = "/"),
        )
    }

    fun getUserId() = manager.getCookies(urlE)?.get(KEY_IPB_MEMBER_ID)

    fun getHathPerks() = manager.getCookies(urlE)?.get(KEY_HATH_PERKS)?.substringBefore('-')

    fun getIdentityCookies(): List<Pair<String, String?>> {
        val eCookies = manager.getCookies(urlE)
        val exCookies = manager.getCookies(urlEx)
        val ipbMemberId = eCookies?.get(KEY_IPB_MEMBER_ID)
        val ipbPassHash = eCookies?.get(KEY_IPB_PASS_HASH)
        val igneous = exCookies?.get(KEY_IGNEOUS)
        return listOf(
            KEY_IPB_MEMBER_ID to ipbMemberId,
            KEY_IPB_PASS_HASH to ipbPassHash,
            KEY_IGNEOUS to igneous,
        )
    }

    fun isCloudflareBypassed() = manager.getCookies(urlE)?.containsKey(KEY_CF_CLEARANCE) == true

    const val KEY_CF_CLEARANCE = "cf_clearance"

    /** 允许手动导入的 Cookie 键；其余键（`path`/`domain`/`expires` 等属性、无关 Cookie）一律忽略。 */
    private val IMPORTABLE_KEYS = setOf(
        KEY_IPB_MEMBER_ID,
        KEY_IPB_PASS_HASH,
        KEY_IGNEOUS,
        KEY_CF_CLEARANCE,
        KEY_HATH_PERKS,
    )

    /**
     * 从一段文本导入 Cookie，返回成功写入的条目数。
     *
     * 用途：设备上的 WebView 版本过旧、渲染不了 Cloudflare 的验证页时，先在能正常通过验证的
     * 浏览器里登录，用 Cookie 导出扩展（如 Cookie-Editor）复制出来，再粘贴进来。
     *
     * 接受两种常见格式：
     * - 单行 `ipb_member_id=1; ipb_pass_hash=abc; cf_clearance=xyz`
     * - 多行 `Set-Cookie: ipb_member_id=1; path=/; domain=.e-hentai.org`
     *
     * 识别出的键会同时写入 e-hentai.org 与 exhentai.org：`igneous` 只在 ex 有意义，
     * `cf_clearance` 两个域各需一份，多写一份是无害的。
     */
    fun importCookies(raw: String): Int {
        var count = 0
        for ((name, value) in parseCookiePairs(raw)) {
            for (url in listOf(urlE, urlEx)) {
                manager.setCookie(url, Cookie(name, value, domain = url.host, path = "/"))
            }
            count++
        }
        flush()
        return count
    }

    private fun parseCookiePairs(raw: String): List<Pair<String, String>> = raw
        .replace("Set-Cookie:", " ", ignoreCase = true)
        .split(';', '\n', '\r')
        .mapNotNull { chunk ->
            val index = chunk.indexOf('=')
            if (index <= 0) return@mapNotNull null
            val name = chunk.substring(0, index).trim()
            val value = chunk.substring(index + 1).trim().trim('"')
            if (name in IMPORTABLE_KEYS && value.isNotEmpty()) name to value else null
        }

    fun flush() = manager.flush()

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name != KEY_UTMP_NAME) {
            manager.setCookie(requestUrl, cookie)
        }
    }

    override fun close() = Unit

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val checkTips = requestUrl.host == urlE.host
        return manager.getCookies(requestUrl)?.mapTo(mutableListOf()) {
            Cookie(it.key, it.value)
        }?.apply {
            if (checkTips) {
                add(sTipsCookie)
            }
        } ?: if (checkTips) listOf(sTipsCookie) else emptyList()
    }
}
