package com.nichx.unraidassistant.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * Obsidian 玻璃卡片：半透明深色表面 + 顶部高光渐变 + 细描边。
 * 视觉等效玻璃但零实时 blur 开销，滚动列表安全。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val shape = ObsidianCorner.card
    Box(
        modifier = modifier
            .clip(shape)
            .background(obsidian.Glass)
            .border(1.dp, obsidian.Border, shape),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(obsidian.Highlight, Color.Transparent),
                        startY = 0f,
                        endY = 150f,
                    ),
                ),
        )
        Column(Modifier.padding(Spacing.xxl)) { content() }
    }
}

/** 发光状态点：中心实心圆点 + 外层径向光晕。 */
@Composable
fun GlowDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    Box(
        modifier = modifier.size(size * 3.2f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.45f),
                            color.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** 发光状态胶囊：圆点 + 文本，底色为状态色半透明 + 描边，用于在线/离线等状态。 */
@Composable
fun GlowPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = ObsidianCorner.pill
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.30f), shape)
            .padding(horizontal = Spacing.xl, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowDot(color = color, size = 6.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/**
 * 渐变指标环卡：圆环进度（外发光 + 渐变描边）+ 中心大号等宽数值。
 * 进度变化以 spring 物理动画驱动。
 */
@Composable
fun GradientRingCard(
    label: String,
    percent: Float,
    valueText: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    subText: String? = null,
    valueColor: Color? = null,
) {
    val obsidian = LocalObsidianPalette.current
    val resolvedValueColor = valueColor ?: obsidian.TextPrimary
    val target = percent.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "ringProgress",
    )
    GlassCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = obsidian.TextSecondary,
            )
            Spacer(Modifier.size(Spacing.xl))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    val glowStroke = stroke + 7.dp.toPx()
                    val inset = glowStroke / 2
                    val arcSize = Size(size.width - glowStroke, size.height - glowStroke)
                    drawArc(
                        color = obsidian.Track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (animated > 0.01f) {
                        drawArc(
                            brush = brush,
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            alpha = 0.25f,
                            style = Stroke(width = glowStroke, cap = StrokeCap.Round),
                        )
                        drawArc(
                            brush = brush,
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                MonoValue(
                    text = valueText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = resolvedValueColor,
                )
            }
            if (subText != null) {
                Spacer(Modifier.size(Spacing.lg))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    modifier = Modifier.heightIn(min = 32.dp),
                )
            }
        }
    }
}

/**
 * 背景光晕：页面背后的径向渐变光源，营造玻璃漂浮感。浅色模式下转为柔和淡彩。
 */
@Composable
fun ObsidianGlows(modifier: Modifier = Modifier) {
    val obsidian = LocalObsidianPalette.current
    Box(modifier) {
        Box(
            Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-190).dp)
                .background(
                    Brush.radialGradient(
                        listOf(obsidian.GlowCyan, Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .offset(x = (-170).dp, y = (-150).dp)
                .background(
                    Brush.radialGradient(
                        listOf(obsidian.GlowIndigo, Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 140.dp, y = 150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(obsidian.GlowAmber, Color.Transparent),
                    ),
                ),
        )
    }
}

/**
 * Obsidian 页面内容容器（透明）。
 *
 * 全局背景与光晕由应用根容器（UnraidNavHost）统一绘制；
 * 页面层不再重复绘制背景，避免导航栏周围出现多余的背景带/分界线。
 */
@Composable
fun ObsidianBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}
