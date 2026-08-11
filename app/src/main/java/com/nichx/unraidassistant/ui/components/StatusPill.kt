package com.nichx.unraidassistant.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 状态胶囊：圆点 + 文本，底色取状态色半透明，用于在线/离线、运行/停止等状态展示。
 * Obsidian 发光样式，直接委托给 [GlowPill]。
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    GlowPill(text = text, color = color, modifier = modifier)
}
