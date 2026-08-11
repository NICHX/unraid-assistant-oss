package com.nichx.unraidassistant.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 路由表与底部导航项定义。
 *
 * 信息架构：底部导航保留 5 个高频 tab（首页 / 存储 / 应用 / 虚拟机 / 系统）；
 * 5 个 tab 由 HomeContainer 内部 Crossfade 管理（非 NavHost 目标），
 * 服务器管理（增删/切换）等二级页由 NavHost 承载。
 * 「应用」tab 为 Docker 容器管理（不含插件管理/应用市场）。
 */
object Routes {
    /** NavHost 根路由：承载 5 个 tab 的容器页，是 NavHost 的 startDestination。 */
    const val HOME = "home"

    const val DASHBOARD = "dashboard"
    const val VM = "vm"
    const val APPS = "apps"
    const val STORAGE = "storage"
    const val SETTINGS = "settings"

    /** 底部导航栏高度，供浮动遮盖时各页面内容底部留白使用。 */
    val BOTTOM_BAR_HEIGHT = 60.dp

    const val SERVER_MANAGE = "server_manage"
    const val ADD_SERVER = "add_server"
    const val EDIT_SERVER = "edit_server/{serverId}"

    /** 通知列表页（二级页）。 */
    const val NOTIFICATIONS = "notifications"

    fun editServer(serverId: String) = "edit_server/$serverId"

    /** 应用内 WebView（WebGUI 深链），url/title 均经 Uri.encode 编码。 */
    const val WEBVIEW = "webview?url={url}&title={title}"
    fun webView(url: String, title: String): String =
        "webview?url=${Uri.encode(url)}&title=${Uri.encode(title)}"

    /** 底部导航栏展示的页面。 */
    val bottomBarItems: List<BottomBarItem> = listOf(
        BottomBarItem(DASHBOARD, "首页", Icons.Filled.Dashboard),
        BottomBarItem(STORAGE, "存储", Icons.Filled.Storage),
        BottomBarItem(APPS, "应用", Icons.Filled.Apps),
        BottomBarItem(VM, "虚拟机", Icons.Filled.DesktopWindows),
        BottomBarItem(SETTINGS, "系统", Icons.Filled.Settings),
    )

    val bottomBarRoutes: Set<String> = bottomBarItems.map { it.route }.toSet()
}

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)
