package com.nichx.unraidassistant.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * 通用确认对话框。用于危险操作（删除服务器、启停阵列等）的二次确认。
 * Obsidian 玻璃风格：圆角深色容器 + 主题强调色按钮。
 * [danger] = true 时确认按钮以红色强调，用于停机/删除等不可逆或高风险操作。
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val confirmColor = if (danger) obsidian.Red else obsidian.Cyan
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = obsidian.Background,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, color = obsidian.TextPrimary) },
        text = { Text(message, color = obsidian.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = confirmColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = obsidian.TextSecondary) }
        },
    )
}
