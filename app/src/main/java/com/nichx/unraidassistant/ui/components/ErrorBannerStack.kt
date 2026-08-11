package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 页内错误横幅组：连接失败（自动重试）/ 操作失败 / 操作成功提示三连。
 * 统一各页面"transientError / actionError / actionMessage"的横幅调用样板。
 */
@Composable
fun ErrorBannerStack(
    transientError: String? = null,
    actionError: String? = null,
    actionMessage: String? = null,
    modifier: Modifier = Modifier,
    onDismissTransient: () -> Unit = {},
    onDismissActionError: () -> Unit = {},
    onDismissActionMessage: () -> Unit = {},
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (transientError != null) {
            ErrorBanner(
                message = "连接失败：$transientError，正在自动重试…",
                onDismiss = onDismissTransient,
            )
        }
        if (actionError != null) {
            ErrorBanner(
                message = "操作失败：$actionError",
                onDismiss = onDismissActionError,
            )
        }
        if (actionMessage != null) {
            ErrorBanner(
                message = actionMessage,
                isError = false,
                onDismiss = onDismissActionMessage,
            )
        }
    }
}
