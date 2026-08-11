package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.StatusColors
import com.nichx.unraidassistant.ui.theme.UnraidAssistantTheme

@Preview(name = "EntityCard 列表模式", showBackground = true)
@Composable
private fun EntityCardListPreview() {
    UnraidAssistantTheme {
        EntityCard(
            title = "qbit",
            subtitle = "lscr.io/linuxserver/qbittorrent:latest",
            statusText = "运行中",
            statusColor = StatusColors.Running,
            isGrid = false,
            onClick = {},
            icon = { PreviewIcon() },
        )
    }
}

@Preview(name = "EntityCard 网格模式", showBackground = true)
@Composable
private fun EntityCardGridPreview() {
    UnraidAssistantTheme {
        Row(
            modifier = Modifier.padding(Spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            EntityCard(
                title = "qbit",
                subtitle = "lscr.io/linuxserver/qbittorrent:latest",
                statusText = "运行中",
                statusColor = StatusColors.Running,
                isGrid = true,
                onClick = {},
                modifier = Modifier.width(160.dp),
                icon = { PreviewIcon() },
            )
            EntityCard(
                title = "plex",
                subtitle = "lscr.io/linuxserver/plex:latest",
                statusText = "已停止",
                statusColor = StatusColors.Stopped,
                isGrid = true,
                onClick = {},
                modifier = Modifier.width(160.dp),
                icon = { PreviewIcon() },
            )
        }
    }
}

@Preview(name = "MonoValue + SummaryStat", showBackground = true)
@Composable
private fun StatsPreview() {
    UnraidAssistantTheme {
        Row(
            modifier = Modifier.padding(Spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxxxl),
        ) {
            SummaryStat(label = "运行中", value = "3", color = StatusColors.Running)
            SummaryStat(label = "已停止", value = "2", color = StatusColors.Stopped)
            MonoValue(text = "68.4%", color = Color(0xFF22D3EE))
        }
    }
}

@Preview(name = "RefreshAction", showBackground = true)
@Composable
private fun RefreshActionPreview() {
    UnraidAssistantTheme {
        Row(modifier = Modifier.padding(Spacing.xxl)) {
            RefreshAction(isRefreshing = false, onClick = {})
        }
    }
}

@Preview(name = "ErrorBannerStack", showBackground = true)
@Composable
private fun ErrorBannerStackPreview() {
    UnraidAssistantTheme {
        Column(modifier = Modifier.padding(Spacing.xxl)) {
            ErrorBannerStack(
                transientError = "网络超时",
                actionMessage = "容器已重启",
            )
        }
    }
}

@Preview(name = "ObsidianScreenScaffold", showBackground = true)
@Composable
private fun ObsidianScreenScaffoldPreview() {
    UnraidAssistantTheme {
        ObsidianScreenScaffold(title = "预览页") { padding ->
            Text(
                text = "页面骨架：透明顶栏 + 内容区",
                modifier = Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
        }
    }
}

@Composable
private fun PreviewIcon() {
    val obsidian = LocalObsidianPalette.current
    Icon(
        imageVector = Icons.Filled.Dns,
        contentDescription = null,
        tint = obsidian.Cyan,
        modifier = Modifier.size(22.dp),
    )
}
