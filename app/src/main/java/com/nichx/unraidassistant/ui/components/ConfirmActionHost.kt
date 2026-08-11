package com.nichx.unraidassistant.ui.components

import androidx.compose.runtime.Composable

/**
 * 危险操作二次确认宿主：页面持有 [action]（如 sealed interface 的操作意图）时自动弹出确认框。
 * 标题/文案/按钮/危险级别由调用方提供映射函数，确认后回调 [onConfirm]。
 */
@Composable
fun <T> ConfirmActionHost(
    action: T?,
    title: (T) -> String,
    message: (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
    confirmText: (T) -> String = { "确认" },
    dismissText: String = "取消",
    danger: (T) -> Boolean = { false },
) {
    action?.let {
        ConfirmDialog(
            title = title(it),
            message = message(it),
            confirmText = confirmText(it),
            dismissText = dismissText,
            danger = danger(it),
            onConfirm = { onConfirm(it) },
            onDismiss = onDismiss,
        )
    }
}
