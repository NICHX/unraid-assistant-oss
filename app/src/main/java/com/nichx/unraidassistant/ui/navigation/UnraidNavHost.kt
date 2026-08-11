package com.nichx.unraidassistant.ui.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nichx.unraidassistant.core.notification.NotificationPoster
import com.nichx.unraidassistant.core.updater.UpdateCheckState
import com.nichx.unraidassistant.core.updater.UpdateInfo
import com.nichx.unraidassistant.feature.apps.AppsScreen
import com.nichx.unraidassistant.feature.dashboard.DashboardScreen
import com.nichx.unraidassistant.feature.notifications.NotificationsScreen
import com.nichx.unraidassistant.feature.root.AppViewModel
import com.nichx.unraidassistant.feature.server.AddServerScreen
import com.nichx.unraidassistant.feature.server.ServerManagementScreen
import com.nichx.unraidassistant.feature.settings.SettingsScreen
import com.nichx.unraidassistant.feature.storage.StorageScreen
import com.nichx.unraidassistant.feature.vm.VmScreen
import com.nichx.unraidassistant.feature.webgui.WebViewScreen
import com.nichx.unraidassistant.ui.components.ObsidianGlows
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.UnraidAssistantTheme

private const val PAGE_TRANSITION_MS = 300
private const val TAB_CROSSFADE_MS = 220

/**
 * 应用根组件：恢复主题/会话/上次 tab 后进入导航。
 * 启动恢复期间显示品牌底色占位，避免页面跳变。
 */
@Composable
fun UnraidAssistantApp(viewModel: AppViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val startTab by viewModel.startTab.collectAsStateWithLifecycle()
    val ready by viewModel.isReady.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    UnraidAssistantTheme(themeMode = themeMode) {
        val obsidian = LocalObsidianPalette.current
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                // 将 window 背景统一为品牌底色，保证手势条/系统栏区域与 Compose 层背景一致，
                // 不再出现 enableEdgeToEdge() 默认 scrim 造成的多余背景带。
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(obsidian.Background.toArgb()))
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !obsidian.isDark
                insetsController.isAppearanceLightNavigationBars = !obsidian.isDark
            }
        }

        if (!ready) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = obsidian.Cyan)
            }
        } else {
            UnraidNavHost(
                startTab = startTab,
                onTabSelected = viewModel::saveLastTab,
            )
        }

        // 更新弹窗：单例状态流驱动（启动自动检查 / 设置页手动检查共用）。
        // checkedAt 用于区分「新一次检查」（值变化 → 重新弹窗）与「旋转屏幕/重组」（值不变 → 保持已关闭）。
        val uriHandler = LocalUriHandler.current
        val updateInfo = (updateState as? UpdateCheckState.Available)?.info
        var dismissedCheckAt by rememberSaveable { mutableLongStateOf(-1L) }
        if (updateInfo != null && updateInfo.checkedAt != dismissedCheckAt) {
            UpdateAvailableDialog(
                info = updateInfo,
                onDownload = {
                    dismissedCheckAt = updateInfo.checkedAt
                    uriHandler.openUri(updateInfo.downloadUrl)
                },
                onDismiss = { dismissedCheckAt = updateInfo.checkedAt },
            )
        }
    }
}

/**
 * 应用导航容器（参照 NIplayer v2）。
 *
 * - 5 个底部 tab 由 [HomeContainer] 内部 Crossfade 管理（非 NavHost 目标），
 *   避免 tab 间切换出现方向性滑动动画。
 * - 二级页面（服务器管理/新增/编辑）由 NavHost 承载，采用 iOS 风格 push/pop：
 *   进入 = 淡入 + 从右滑入；离开 = 仅淡出；返回进入 = 仅淡入；返回离开 = 淡出 + 向右滑出。
 * - 底部导航栏浮于内容之上（无预留底栏带），消除多余背景层。
 */
@Composable
fun UnraidNavHost(
    startTab: String,
    onTabSelected: (String) -> Unit,
) {
    val navController = rememberNavController()
    val obsidian = LocalObsidianPalette.current

    // 系统通知点击深链：冷启动/后台唤醒时跳转通知列表页。
    // LifecycleResumeEffect 覆盖 onCreate 与 onNewIntent 两种投递路径；
    // rememberSaveable + removeExtra 防止旋转/再次前台时重复跳转。
    val activity = LocalView.current.context as Activity
    var notificationDeepLinkHandled by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(activity) {
        val intent = activity.intent
        if (!notificationDeepLinkHandled &&
            intent.getBooleanExtra(NotificationPoster.EXTRA_OPEN_NOTIFICATIONS, false)
        ) {
            notificationDeepLinkHandled = true
            intent.removeExtra(NotificationPoster.EXTRA_OPEN_NOTIFICATIONS)
            navController.navigate(Routes.NOTIFICATIONS)
        }
        onPauseOrDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(obsidian.Background),
    ) {
        ObsidianGlows(Modifier.fillMaxSize())
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = {
                fadeIn(animationSpec = tween(PAGE_TRANSITION_MS)) +
                    slideInHorizontally(
                        animationSpec = tween(PAGE_TRANSITION_MS),
                        initialOffsetX = { it / 4 },
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(PAGE_TRANSITION_MS))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(PAGE_TRANSITION_MS))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(PAGE_TRANSITION_MS)) +
                    slideOutHorizontally(
                        animationSpec = tween(PAGE_TRANSITION_MS),
                        targetOffsetX = { it / 4 },
                    )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(
                route = Routes.HOME,
                enterTransition = { fadeIn(animationSpec = tween(PAGE_TRANSITION_MS)) },
                exitTransition = { fadeOut(animationSpec = tween(PAGE_TRANSITION_MS)) },
                popEnterTransition = { fadeIn(animationSpec = tween(PAGE_TRANSITION_MS)) },
                popExitTransition = { fadeOut(animationSpec = tween(PAGE_TRANSITION_MS)) },
            ) {
                HomeContainer(
                    startTab = startTab,
                    onManageServers = { navController.navigate(Routes.SERVER_MANAGE) },
                    onOpenWebView = { url, title ->
                        navController.navigate(Routes.webView(url, title))
                    },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onTabSelected = onTabSelected,
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWebView = { url ->
                        navController.navigate(Routes.webView(url, "通知链接"))
                    },
                )
            }
            composable(Routes.SERVER_MANAGE) {
                ServerManagementScreen(
                    onBack = { navController.popBackStack() },
                    onAddServer = { navController.navigate(Routes.ADD_SERVER) },
                    onEditServer = { serverId ->
                        navController.navigate(Routes.editServer(serverId))
                    },
                )
            }
            composable(Routes.ADD_SERVER) {
                AddServerScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.EDIT_SERVER,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType }),
            ) { entry ->
                val serverId = entry.arguments?.getString("serverId")
                AddServerScreen(
                    serverId = serverId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.WEBVIEW,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "WebGUI" },
                ),
            ) { entry ->
                val url = entry.arguments?.getString("url").orEmpty().let { Uri.decode(it) }
                val title = entry.arguments?.getString("title").orEmpty().let { Uri.decode(it) }
                WebViewScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * 首页容器：5 个 tab 通过 Crossfade 切换（无方向性动画），
 * 底部导航栏浮于内容之上（BottomCenter），不占用内容空间。
 */
@Composable
private fun HomeContainer(
    startTab: String,
    onManageServers: () -> Unit,
    onOpenWebView: (url: String, title: String) -> Unit,
    onOpenNotifications: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(startTab) }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentTab,
            animationSpec = tween(TAB_CROSSFADE_MS),
            label = "tabCrossfade",
            modifier = Modifier.fillMaxSize(),
        ) { route ->
            when (route) {
                Routes.DASHBOARD -> DashboardScreen(
                    onManageServers = onManageServers,
                    onOpenWebView = { url -> onOpenWebView(url, "WebGUI") },
                    onOpenNotifications = onOpenNotifications,
                )
                Routes.STORAGE -> StorageScreen()
                Routes.APPS -> AppsScreen(
                    onOpenWebView = onOpenWebView,
                )
                Routes.VM -> VmScreen()
                Routes.SETTINGS -> SettingsScreen(
                    onManageServers = onManageServers,
                    onOpenNotifications = onOpenNotifications,
                )
            }
        }

        BottomBar(
            currentRoute = currentTab,
            onNavigate = { route ->
                currentTab = route
                onTabSelected(route)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 发现新版本弹窗（浏览器下载方案）：点击「去下载」用系统浏览器打开 GitHub Release 的
 * APK 下载地址（下载/安装由浏览器与系统安装器完成）；「稍后」关闭并记住本次检查。
 */
@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = obsidian.Background,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("发现新版本 ${info.latestVersion}", color = obsidian.TextPrimary) },
        text = {
            Column {
                Text(
                    text = "当前版本 ${info.currentVersion}，更新包托管于 GitHub Release。",
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                )
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.size(Spacing.lg))
                    Text(
                        text = "更新内容",
                        style = MaterialTheme.typography.titleSmall,
                        color = obsidian.TextPrimary,
                    )
                    Spacer(Modifier.size(Spacing.sm))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("去下载", color = obsidian.Cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后", color = obsidian.TextSecondary)
            }
        },
    )
}
