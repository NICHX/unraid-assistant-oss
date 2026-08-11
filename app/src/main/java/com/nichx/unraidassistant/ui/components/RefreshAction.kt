package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/** 顶栏刷新按钮：刷新中显示同色转圈，否则显示刷新图标。 */
@Composable
fun RefreshAction(
    isRefreshing: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = obsidian.Cyan,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "刷新",
                tint = obsidian.TextSecondary,
            )
        }
    }
}
