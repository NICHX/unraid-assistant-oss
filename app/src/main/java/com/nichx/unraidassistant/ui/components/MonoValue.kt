package com.nichx.unraidassistant.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * 等宽数字强调文本（控制台数值风格）。
 * 统一"等宽 + SemiBold"组合，替代各信息行重复指定的写法。
 */
@Composable
fun MonoValue(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val obsidian = LocalObsidianPalette.current
    Text(
        text = text,
        modifier = modifier,
        style = style,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        color = color ?: obsidian.TextPrimary,
        maxLines = maxLines,
        overflow = overflow,
    )
}
