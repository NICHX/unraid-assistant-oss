package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * Obsidian 页面骨架：透明 Scaffold + 统一透明顶栏。
 * 全局背景与光晕由应用根容器统一绘制，页面层不再重复透明容器样板。
 * [titleContent] 非空时优先渲染为顶栏标题（如子 Tab 选择器），替代 [title] 文本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianScreenScaffold(
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    ObsidianBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                TopAppBar(
                    title = {
                        if (titleContent != null) {
                            titleContent()
                        } else if (title != null) {
                            Text(title, color = obsidian.TextPrimary)
                        }
                    },
                    navigationIcon = navigationIcon ?: {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = actions,
                )
            },
            floatingActionButton = floatingActionButton ?: {},
        ) { padding ->
            content(padding)
        }
    }
}
