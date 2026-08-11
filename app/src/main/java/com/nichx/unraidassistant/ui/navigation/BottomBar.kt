package com.nichx.unraidassistant.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 底部导航栏（5 项）。参照 NIplayer NiBottomBar：
 * 大圆角 Card 容器 + 跟随选中项滑动的 Pill 指示器，
 * 浮于内容之上（无预留底栏带）；深/浅色分别使用独立容器色，适配深色模式。
 */
@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    val barShape = MaterialTheme.shapes.extraLarge
    val barColor = obsidian.BarContainer
    val pillColor = obsidian.BarIndicator
    val barHeight = Routes.BOTTOM_BAR_HEIGHT

    val tabs = Routes.bottomBarItems
    val selectedIndex = remember(currentRoute, tabs) {
        tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }
    val tabCount = tabs.size

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableStateOf(0f) }

    val tabWidthPx = if (tabCount > 0 && containerWidthPx > 0f) containerWidthPx / tabCount else 0f
    val pillWidthPx = (tabWidthPx - with(density) { Spacing.md.toPx() }).coerceAtLeast(0f)
    val pillHeightPx = with(density) { 51.dp.toPx() }
    val pillRadiusPx = with(density) { 28.dp.toPx() }

    val pillOffsetX = remember { Animatable(0f) }

    LaunchedEffect(containerWidthPx) {
        if (containerWidthPx > 0f) {
            val target = selectedIndex * tabWidthPx + (tabWidthPx - pillWidthPx) / 2f
            pillOffsetX.snapTo(target)
        }
    }

    LaunchedEffect(selectedIndex) {
        if (containerWidthPx > 0f && tabWidthPx > 0f) {
            val target = selectedIndex * tabWidthPx + (tabWidthPx - pillWidthPx) / 2f
            pillOffsetX.animateTo(target, tween(durationMillis = 320))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.xxl, end = Spacing.xxl)
            .navigationBarsPadding(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            shape = barShape,
            colors = CardDefaults.cardColors(containerColor = barColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        if (it.width.toFloat() != containerWidthPx) {
                            containerWidthPx = it.width.toFloat()
                        }
                    }
                    .drawBehind {
                        if (pillWidthPx > 0f && pillHeightPx > 0f) {
                            val pillY = (size.height - pillHeightPx) / 2f
                            drawRoundRect(
                                color = pillColor,
                                topLeft = Offset(pillOffsetX.value, pillY),
                                size = Size(pillWidthPx, pillHeightPx),
                                cornerRadius = CornerRadius(pillRadiusPx, pillRadiusPx),
                            )
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { item ->
                        BottomBarItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部导航栏单项按钮。选中态/非选中态通过颜色动画过渡，
 * 取消 Material ripple 反馈，由滑动 Pill 统一表达选中态。
 */
@Composable
private fun BottomBarItem(
    item: BottomBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) obsidian.Cyan else obsidian.TextSecondary,
        animationSpec = tween(300),
        label = "bottomBarItemColor",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}
