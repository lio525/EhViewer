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
 */
object WebViewSupport {

    /**
     * 检测不到 WebView 时用于 User-Agent 的兜底 Chrome 主版本号。
     * 取值只需“像是一个现代 Chrome”，不必与真实版本一致。
     */
    const val FALLBACK_CHROME_VERSION = "120"

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

    /** 供 User-Agent 使用的 Chrome 主版本号，检测不到 WebView 时退回 [FALLBACK_CHROME_VERSION]。 */
    val chromeMajorVersion: String
        get() = versionName
            ?.substringBefore('.')
            ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?: FALLBACK_CHROME_VERSION
}