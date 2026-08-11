package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 通用指标卡片（Obsidian 玻璃卡）：图标 + 标题 + 可选的右侧尾部内容（如状态徽章）。
 * 替代原先各页面重复的 SectionCard 实现，统一"控制台"卡片语法。
 */
@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = obsidian.Cyan,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = obsidian.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Spacer(Modifier.size(Spacing.md))
                trailing()
            }
        }
        Spacer(Modifier.size(Spacing.xl))
        content()
    }
}

/**
 * 指标行：左侧标签 + 右侧等宽字体数值（控制台风格）。[valueColor] 用于状态色高亮。
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    valueMaxLines: Int = 1,
) {
    val obsidian = LocalObsidianPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = obsidian.TextSecondary,
        )
        Spacer(Modifier.size(Spacing.xl))
        MonoValue(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = valueMaxLines,
        )
    }
}
