package com.nichx.unraidassistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner

/**
 * 骨架呼吸动画透明度（0.20 → 0.50 往返），供各页面骨架屏复用同一节奏。
 */
@Composable
fun rememberSkeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    return alpha
}

/**
 * 加载占位骨架屏（Obsidian 玻璃卡风格）：半透明玻璃卡 + 呼吸律动的占位块，
 * 与真实卡片同构同尺寸，避免加载完成后的布局跳变。
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
) {
    val alpha = rememberSkeletonAlpha()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(itemCount) { SkeletonCard(alpha) }
    }
}

@Composable
private fun SkeletonCard(alpha: Float) {
    val obsidian = LocalObsidianPalette.current
    val placeholderColor = obsidian.TextPrimary.copy(alpha = alpha * 0.30f)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(placeholderColor),
            )
            Spacer(Modifier.size(10.dp))
            SkeletonLine(placeholderColor, widthFraction = 0.4f, height = 16.dp)
        }
        Spacer(Modifier.height(14.dp))
        SkeletonLine(placeholderColor, widthFraction = 0.85f, height = 14.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(placeholderColor, widthFraction = 0.6f, height = 14.dp)
    }
}

/** 骨架占位块（圆角胶囊），页面级骨架屏可复用。 */
@Composable
fun SkeletonLine(color: Color, widthFraction: Float, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(ObsidianCorner.pill)
            .background(color),
    )
}

/**
 * 编辑表单骨架：匹配编辑页「分区标题 + 玻璃卡内输入框组 + 底部主操作按钮」的真实结构，
 * 供长表单页加载时使用，避免与列表骨架形状错位。
 */
@Composable
fun EditFormSkeleton(
    modifier: Modifier = Modifier,
    fieldsPerCard: List<Int> = listOf(3, 2),
) {
    val alpha = rememberSkeletonAlpha()
    val obsidian = LocalObsidianPalette.current
    val placeholderColor = obsidian.TextPrimary.copy(alpha = alpha * 0.30f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fieldsPerCard.forEachIndexed { index, fieldCount ->
            if (index > 0) Spacer(Modifier.height(4.dp))
            SkeletonLine(placeholderColor, widthFraction = 0.25f, height = 16.dp)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(fieldCount) { SkeletonField(placeholderColor) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(ObsidianCorner.pill)
                .background(placeholderColor),
        )
    }
}

/** 表单输入框占位：标签短条 + 圆角输入框，模拟 ObsidianTextField 形态。 */
@Composable
private fun SkeletonField(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkeletonLine(color, widthFraction = 0.3f, height = 12.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(ObsidianCorner.listItem)
                .background(color.copy(alpha = 0.5f)),
        )
    }
}
