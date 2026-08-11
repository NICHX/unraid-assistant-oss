package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * Obsidian 风格 Snackbar 宿主：玻璃容器 + 细描边 + 品牌青强调操作按钮，
 * 替换默认 inverseSurface 反色容器，保证全局弹条视觉统一。
 */
@Composable
fun ObsidianSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    val shape = MaterialTheme.shapes.medium
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = obsidian.Glass,
            contentColor = obsidian.TextPrimary,
            actionContentColor = obsidian.Cyan,
            dismissActionContentColor = obsidian.TextSecondary,
            shape = shape,
            modifier = Modifier.border(1.dp, obsidian.Border, shape),
        )
    }
}
