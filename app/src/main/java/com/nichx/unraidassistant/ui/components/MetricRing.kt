package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 大数字指标环卡（Obsidian 渐变环）：圆环进度 + 中心大号等宽数值（CPU/内存使用率等）。
 * 颜色可随使用率高低自动切换到警示渐变（高负载红 / 中负载琥珀 / 正常青→靛）。
 */
@Composable
fun MetricRingCard(
    label: String,
    percent: Float,
    valueText: String,
    modifier: Modifier = Modifier,
    subText: String? = null,
) {
    val obsidian = LocalObsidianPalette.current
    val color = when {
        percent >= 0.9f -> obsidian.Red
        percent >= 0.75f -> obsidian.Amber
        else -> obsidian.Cyan
    }
    val brush = when {
        percent >= 0.9f -> Brush.linearGradient(listOf(obsidian.Red, obsidian.Orange))
        percent >= 0.75f -> Brush.linearGradient(listOf(obsidian.Amber, obsidian.Orange))
        else -> Brush.linearGradient(listOf(obsidian.Cyan, obsidian.Indigo))
    }
    GradientRingCard(
        label = label,
        percent = percent,
        valueText = valueText,
        brush = brush,
        modifier = modifier,
        subText = subText,
        valueColor = color,
    )
}

/**
 * 等宽数字强调文本（控制台数值风格），内部复用 [MonoValue]。
 */
@Composable
fun MetricValue(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    MonoValue(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

/**
 * 横向等分布局：把两个卡片并排放置，宽度各占一半，高度取两者较大值保持一致。
 * 内容接收一个 [Modifier]，必须应用到最外层以实现卡片背景填满等高区域。
 */
@Composable
fun RowOfTwo(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            first(Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            second(Modifier.fillMaxSize())
        }
    }
}
