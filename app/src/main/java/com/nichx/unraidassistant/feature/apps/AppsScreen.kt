package com.nichx.unraidassistant.feature.apps

import androidx.compose.runtime.Composable
import com.nichx.unraidassistant.feature.docker.DockerScreen

/**
 * 应用页（底部 Tab）：Docker 容器管理。
 * 本版本仅提供 Docker 容器管理，直接渲染 DockerScreen 单子页。
 */
@Composable
fun AppsScreen(
    onOpenWebView: (url: String, title: String) -> Unit,
) {
    DockerScreen(
        onOpenWebView = { url -> onOpenWebView(url, "容器 WebUI") },
    )
}
