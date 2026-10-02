package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.ehviewer.core.network.EhCookieStore
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.WebViewUnavailable
import com.hippo.ehviewer.ui.WebViewOutdatedBanner
import com.hippo.ehviewer.util.WebViewSupport
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.awaitCancellation

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    if (WebViewSupport.canUseWebView) {
        val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
        LaunchedEffect(state) {
            snapshotFlow { !state.isLoading }.collect { hasFinished ->
                if (hasFinished) {
                    if (EhCookieStore.isCloudflareBypassed()) {
                        Settings.desktopSite.value = false
                    }
                    if (EhCookieStore.hasSignedIn()) {
                        EhCookieStore.flush()
                        postLogin()
                        state.webView?.destroy()
                        bgWork { awaitCancellation() }
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize()) {
            // 版本过旧时 Cloudflare 验证极易失败，这里给出解释和升级入口，但不禁用页面。
            if (WebViewSupport.isVersionOutdated) {
                WebViewOutdatedBanner()
            }
            WebView(
                state = state,
                modifier = Modifier.weight(1f),
                onCreated = {
                    EhUtils.signOut()
                    it.setDefaultSettings()
                },
            )
        }
    } else {
        // 设备上没有可用的系统 WebView：不要触碰任何 WebView API（否则会直接抛异常），改用外部浏览器兜底。
        // 注意外部浏览器无法把 Cookie 写回 App，所以这里只解决“能看到页面”，不能替代登录 / Cloudflare 验证。
        WebViewUnavailable(EhUrl.URL_SIGN_IN)
    }
}
