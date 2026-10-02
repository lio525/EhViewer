package com.hippo.ehviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.util.WebViewSupport

/**
 * 系统 WebView 版本过旧时的提示条。
 *
 * 只在 [WebViewSupport.isVersionOutdated] 为真时显示，用来解释“为什么登录页一直转圈 / 验证失败”，
 * 并提供一条升级路径。注意这里不阻止用户继续操作：实际失败与否取决于站点，
 * 硬性拦截反而会让“只是版本号解析不准”的设备彻底用不了。
 */
@Composable
fun WebViewOutdatedBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.webview_outdated_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.webview_outdated_message,
                    WebViewSupport.versionName.orEmpty(),
                    WebViewSupport.MIN_SUPPORTED_CHROME_MAJOR,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { context.openWebViewUpgrade() }) {
                    Text(stringResource(R.string.webview_outdated_update))
                }
            }
        }
    }
}

/**
 * 登录页里的紧凑版提示：版本过旧时建议改用不依赖 WebView 的账号密码登录。
 */
@Composable
fun WebViewOutdatedNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            R.string.webview_outdated_sign_in_notice,
            WebViewSupport.versionName.orEmpty(),
        ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}
