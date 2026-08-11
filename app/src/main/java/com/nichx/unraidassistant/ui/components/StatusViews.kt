package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * 未选择服务器状态：提示用户先在 Dashboard 或设置中激活一台服务器。
 */
@Composable
fun NoServerState(
    modifier: Modifier = Modifier,
    onManageServers: (() -> Unit)? = null,
) {
    val obsidian = LocalObsidianPalette.current
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = obsidian.TextSecondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = "未选择服务器",
            style = MaterialTheme.typography.titleMedium,
            color = obsidian.TextPrimary,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "在 Dashboard 顶栏选择，或在设置中添加一台服务器",
            style = MaterialTheme.typography.bodyMedium,
            color = obsidian.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (onManageServers != null) {
            Spacer(Modifier.size(24.dp))
            OutlinedButton(onClick = onManageServers) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = obsidian.Cyan,
                )
                Spacer(Modifier.size(6.dp))
                Text("管理服务器", color = obsidian.TextPrimary)
            }
        }
    }
}

/**
 * 加载失败状态：全屏错误 + 重试按钮。轮询会自动恢复，此页仅用于首次加载即失败。
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    FullPageState(
        icon = Icons.Filled.ErrorOutline,
        iconTint = obsidian.Red,
        title = message,
        modifier = modifier,
        action = {
            Button(onClick = onRetry) { Text("重试") }
        },
    )
}

/** 通用全屏状态页（空态 / 错误态共用布局语法）。 */
@Composable
fun FullPageState(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val obsidian = LocalObsidianPalette.current
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = obsidian.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = obsidian.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.size(24.dp))
            action()
        }
    }
}
