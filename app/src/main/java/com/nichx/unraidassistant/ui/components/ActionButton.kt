package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * Obsidian 操作按钮：玻璃底 + 细描边 + 圆角，图标与文本同色强调。
 * 用于详情弹窗/管理弹窗内的二级操作（启停、更新、校验等）。
 * [busy] = true 时以同色转圈替代图标，操作期间禁止重复点击。
 */
@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color,
    enabled: Boolean = true,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val shape = MaterialTheme.shapes.medium
    val interactive = enabled || busy
    val resolvedTint = if (interactive) tint else obsidian.TextSecondary
    Row(
        modifier = modifier
            .alpha(if (interactive) 1f else 0.45f)
            .clip(shape)
            .background(if (interactive) obsidian.Glass else obsidian.Track)
            .border(1.dp, if (interactive) obsidian.Border else obsidian.Border.copy(alpha = 0.5f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = resolvedTint,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedTint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = resolvedTint,
        )
    }
}
