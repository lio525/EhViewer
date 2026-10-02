package com.hippo.ehviewer.util

import android.content.pm.PackageInfo
import android.webkit.CookieManager
import androidx.webkit.WebViewCompat
import splitties.init.appCtx

/**
 * 系统 WebView 能力检测。
 *
 * EHViewer 的登录、Cloudflare 验证、My Tags、UConfig 都依赖系统的 [android.webkit.WebView]。
 * 它的实现由系统里的 WebView provider（`com.google.android.webview` / `com.android.webview` 等）提供，
 * App 自身无法内置浏览器引擎。当设备没有可用的 provider 时：
 *
 * - [WebViewCompat.getCurrentWebViewPackage] 返回 `null`，直接 `!!` 会抛 NPE；
 * - [android.webkit.CookieManager.getInstance] 会抛 `AndroidRuntimeException`。
 *
 * 所以所有对 WebView 的访问都要先经过本对象判断；不可用时走降级路径：
 * Cookie 退回内存实现（见 `com.ehviewer.core.network.MemoryCookieManager`），
 * 依赖 WebView 的页面改成“提示 + 外部浏览器打开”。
 *
 * 除了“有没有”，还要判断“够不够新”：部分设备自带的 provider 版本过旧，
 * 加载 Cloudflare 验证页会直接失败或被无限循环拦截。低于 [MIN_SUPPORTED_CHROME_MAJOR]
 * 时用 [isVersionOutdated] 标记，由 UI 提示用户升级并优先推荐不依赖 WebView 的登录方式。
 */
object WebViewSupport {

    /**
     * 检测不到 WebView 时用于 User-Agent 的兜底 Chrome 主版本号。
     * 取值只需“像是一个现代 Chrome”，不必与真实版本一致。
     */
    const val FALLBACK_CHROME_VERSION = "120"

    /**
     * 能正常通过 Cloudflare 验证的 Chrome 主版本下限。
     *
     * E-Hentai 的登录页由 Cloudflare Turnstile 保护，它依赖较新的 JS/CSS 特性与 TLS 参数；
     * 90 之前的 WebView 还会因为证书链与密码套件过旧被直接拒绝。取 100 留出余量，
     * 高于该值的旧版本仍可能失败，所以这里只做“提醒”而不是硬性禁用。
     */
    const val MIN_SUPPORTED_CHROME_MAJOR = 100

    /** WebView provider 在 Google Play 上的详情页；用于引导不支持应用内升级的用户手动更新。 */
    const val PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id="

    private val webViewPackageInfo: PackageInfo? by lazy {
        runCatching { WebViewCompat.getCurrentWebViewPackage(appCtx) }.getOrNull()
    }

    /**
     * WebView provider 是否可用的权威判定。
     *
     * [android.webkit.CookieManager] 与 WebView 共用同一个 provider：能取到它的实例，
     * 就说明 provider 已经成功加载；取不到就说明设备确实没有可用的 WebView。
     * 比只看 [WebViewCompat.getCurrentWebViewPackage] 更可靠——后者在某些机型上会抛异常。
     */
    private val providerAvailable: Boolean by lazy {
        runCatching { CookieManager.getInstance() }.isSuccess
    }

    /** 是否可以安全使用 WebView 相关功能。 */
    val canUseWebView: Boolean
        get() = providerAvailable

    /** WebView provider 的包名，例如 `com.google.android.webview`；检测不到时为 `null`。 */
    val packageName: String?
        get() = webViewPackageInfo?.packageName

    /** WebView 的版本名，例如 `120.0.6099.43`；检测不到时为 `null`。 */
    val versionName: String?
        get() = webViewPackageInfo?.versionName

    /** WebView 的 Chrome 主版本号，例如 `120.0.6099.43` → `120`；无法解析时为 `null`。 */
    val chromeMajorVersionInt: Int?
        get() = versionName?.substringBefore('.')?.toIntOrNull()

    /**
     * provider 存在但版本低于 [MIN_SUPPORTED_CHROME_MAJOR]。
     *
     * 版本号解析不出来时一律返回 `false`：宁可漏报也不要误报，
     * 否则会把能正常登录的设备也挡在提示页后面。
     */
    val isVersionOutdated: Boolean
        get() = providerAvailable && chromeMajorVersionInt?.let { it < MIN_SUPPORTED_CHROME_MAJOR } == true

    /** 是否可以正常使用 WebView 相关功能：provider 可用，且版本不算太旧。 */
    val isUsableWebView: Boolean
        get() = canUseWebView && !isVersionOutdated

    /** 用于引导用户更新 WebView 的商店链接；拿不到包名时退回通用 WebView 条目。 */
    val playStoreUrl: String
        get() = PLAY_STORE_URL_PREFIX + (packageName ?: "com.google.android.webview")

    /** 供 User-Agent 使用的 Chrome 主版本号，检测不到 WebView 时退回 [FALLBACK_CHROME_VERSION]。 */
    val chromeMajorVersion: String
        get() = versionName
            ?.substringBefore('.')
            ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?: FALLBACK_CHROME_VERSION
}
