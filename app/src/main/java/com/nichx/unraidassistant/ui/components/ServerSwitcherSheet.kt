package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 服务器切换底部弹层：列出所有服务器，点击即切换并关闭；底部提供"管理服务器"入口。
 * 作为服务器切换的高频入口，替代原先占用一级导航的服务器页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSwitcherSheet(
    servers: List<ServerConfig>,
    activeServerId: String?,
    onSelect: (ServerConfig) -> Unit,
    onManageServers: () -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = obsidian.Background,
        shape = ObsidianCorner.sheetTop,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "选择服务器",
                style = MaterialTheme.typography.titleMedium,
                color = obsidian.TextPrimary,
                modifier = Modifier.padding(horizontal = Spacing.xxxxl, vertical = Spacing.xs),
            )
            Spacer(Modifier.size(Spacing.md))
            servers.forEach { server ->
                val isActive = server.id == activeServerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(server) }
                        .padding(horizontal = Spacing.xxxxl, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                        tint = if (isActive) obsidian.Cyan else obsidian.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = obsidian.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${server.protocol.value}://${server.host}:${server.port}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = obsidian.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isActive) {
                        Spacer(Modifier.size(Spacing.xl))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "当前",
                            tint = obsidian.Cyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = obsidian.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageServers)
                    .padding(horizontal = Spacing.xxxxl, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = obsidian.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "管理服务器",
                    style = MaterialTheme.typography.bodyLarge,
                    color = obsidian.TextPrimary,
                )
            }
            Spacer(Modifier.size(Spacing.xxl))
        }
    }
}

/** 服务器切换入口胶囊（显示在 Dashboard 顶栏右侧）。 */
@Composable
fun ServerSwitcherChip(
    serverName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .widthIn(max = 140.dp)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Dns,
            contentDescription = null,
            tint = obsidian.Cyan,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = serverName,
            style = MaterialTheme.typography.labelLarge,
            color = obsidian.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
