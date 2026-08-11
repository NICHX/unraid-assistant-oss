package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 实体卡片：图标框 + 名称 + 等宽副标题 + 状态胶囊的统一布局。
 * 支持列表（横向：信息居左、状态居右）与网格（纵向：信息在上、状态在底部）两种模式，
 * 供 Docker 容器 / 虚拟机等实体列表复用，消除各页面逐行重复的卡片实现。
 *
 * [badge] 为列表模式状态左侧、网格模式底部右侧的可选标签（如"有更新"）。
 * [trailingAction] 为列表模式徽标与状态胶囊之间的可选操作（如"打开页面"），网格模式不展示。
 */
@Composable
fun EntityCard(
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    isGrid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    badge: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    gridHeight: Dp = 130.dp,
) {
    if (!isGrid) {
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntityIconBox(icon)
                Spacer(Modifier.size(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    EntityTitle(title)
                    Spacer(Modifier.size(Spacing.xxs))
                    EntitySubtitle(subtitle)
                }
                if (badge != null) {
                    Spacer(Modifier.size(Spacing.md))
                    badge()
                }
                if (trailingAction != null) {
                    Spacer(Modifier.size(Spacing.md))
                    trailingAction()
                }
                Spacer(Modifier.size(Spacing.md))
                StatusPill(statusText, statusColor)
            }
        }
    } else {
        GlassCard(
            modifier = modifier
                .height(gridHeight)
                .clickable(onClick = onClick),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EntityIconBox(icon)
                    Spacer(Modifier.size(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        EntityTitle(title)
                        Spacer(Modifier.size(Spacing.xxs))
                        EntitySubtitle(subtitle)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(statusText, statusColor)
                    Spacer(Modifier.weight(1f))
                    if (badge != null) {
                        badge()
                    }
                }
            }
        }
    }
}

/** 实体图标框：40dp 玻璃底 + 细描边。 */
@Composable
private fun EntityIconBox(
    icon: @Composable () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(obsidian.Glass, MaterialTheme.shapes.medium)
            .border(1.dp, obsidian.Border, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun EntityTitle(title: String) {
    val obsidian = LocalObsidianPalette.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = obsidian.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EntitySubtitle(subtitle: String) {
    val obsidian = LocalObsidianPalette.current
    Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = obsidian.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
