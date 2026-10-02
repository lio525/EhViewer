/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryDetail
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.util.WebViewSupport
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val intent = CustomTabsIntent.Builder().apply { setShowTitle(true) }.build()

context(ctx: Context)
fun openBrowser(url: String) {
    if (url.isEmpty()) return
    try {
        intent.launchUrl(ctx, url.toUri())
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.no_browser_installed, Toast.LENGTH_LONG).show()
    }
}

/**
 * 打开 WebView provider 的商店详情页，引导用户升级系统 WebView。
 *
 * 装不了 Play 商店的设备（部分国产 ROM、模拟器）会退回浏览器打开网页版商店；
 * 连浏览器都没有时由 [openBrowser] 提示。
 */
context(ctx: Context)
fun openWebViewUpgrade() {
    val pkg = WebViewSupport.packageName
    if (pkg != null) {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri())
        try {
            ctx.startActivity(market)
            return
        } catch (_: ActivityNotFoundException) {
            // 没有商店，继续走浏览器。
        }
    }
    openBrowser(WebViewSupport.playStoreUrl)
}

context(_: DestinationsNavigator)
fun jumpToReaderByPage(url: String, detail: GalleryDetail): Boolean {
    GalleryPageUrlParser.parse(url)?.let {
        if (it.gid == detail.gid) {
            navToReader(detail.galleryInfo, it.page)
            return true
        }
    }
    return false
}
