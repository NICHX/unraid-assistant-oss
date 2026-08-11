package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/** 统计卡中的单个统计项（数值 + 标签）。 */
data class SummaryStatItem(
    val label: String,
    val value: String,
    val color: Color,
)

/**
 * 应用页各子 Tab 统一的「数据统计行」：
 * - 统计项文字居中（数值与标签均居中对齐）；
 * - 右侧 [action] 插槽挂载主操作按钮（如「全部更新」）。
 *
 * Docker（运行中/已停止/可更新）、插件（已安装/可更新/需检查）、
 * 应用市场（应用/分类/更新于）共用同一布局，保证统计行视觉一致。
 */
@Composable
fun SummaryStatsCard(
    stats: List<SummaryStatItem>,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val obsidian = LocalObsidianPalette.current
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stats.forEach { stat ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 数值列宽有限（三列 + 可能存在的 action 按钮），用 BoxWithConstraints 拿到
                    // 实际可用宽度，按等宽字体字符宽度（约 0.6em）反推字号，让
                    // "运行中 3 / 12"、"更新于 2026-08-08" 等长数值完整显示、不被省略号截断；
                    // 极端情况（数字过长）以最小字号 + 省略号兜底。
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val maxFont = MaterialTheme.typography.headlineSmall.fontSize.value
                        val minFont = 13.sp.value
                        val estimated = maxWidth.value / (stat.value.length * 0.6f)
                        val fontSize = estimated.coerceIn(minFont, maxFont).sp
                        Text(
                            text = stat.value,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = stat.color,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.size(Spacing.xxs))
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = obsidian.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (action != null) {
                Spacer(Modifier.size(Spacing.sm))
                action()
            }
        }
    }
}
