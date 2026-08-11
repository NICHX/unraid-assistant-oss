package com.nichx.unraidassistant.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nichx.unraidassistant.core.datastore.ContentViewMode
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/** 顶栏视图切换按钮：始终显示"另一种模式"的图标，点击即切换。 */
@Composable
fun ContentViewToggle(
    mode: ContentViewMode,
    onModeChange: (ContentViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    val target = if (mode == ContentViewMode.GRID) ContentViewMode.LIST else ContentViewMode.GRID
    IconButton(onClick = { onModeChange(target) }, modifier = modifier) {
        Icon(
            imageVector = if (target == ContentViewMode.GRID) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
            contentDescription = if (target == ContentViewMode.GRID) "切换到网格视图" else "切换到列表视图",
            tint = obsidian.TextSecondary,
        )
    }
}
