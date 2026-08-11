package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/** "数值 + 标签"统计块：大号等宽数值 + 小标签，用于汇总卡（运行中/已停止等）。 */
@Composable
fun SummaryStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.size(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = obsidian.TextSecondary,
        )
    }
}
