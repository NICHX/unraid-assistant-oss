package com.nichx.unraidassistant.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.BuildConfig
import com.nichx.unraidassistant.core.datastore.ThemeMode
import com.nichx.unraidassistant.core.updater.UpdateCheckState
import com.nichx.unraidassistant.session.NotificationChannelState
import com.nichx.unraidassistant.ui.components.InfoCard
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.ObsidianTextField
import com.nichx.unraidassistant.ui.navigation.Routes
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner
import com.nichx.unraidassistant.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageServers: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val pollIntervalSeconds by viewModel.pollIntervalSeconds.collectAsStateWithLifecycle()
    val serverCount by viewModel.serverCount.collectAsStateWithLifecycle()
    val cleartextWhitelist by viewModel.cleartextWhitelist.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val dndEnabled by viewModel.dndEnabled.collectAsStateWithLifecycle()
    val dndStartMinutes by viewModel.dndStartMinutes.collectAsStateWithLifecycle()
    val dndEndMinutes by viewModel.dndEndMinutes.collectAsStateWithLifecycle()
    val channelState by viewModel.channelState.collectAsStateWithLifecycle()
    val channelError by viewModel.channelError.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showAddRuleDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 结果仅用于日志，开关状态已由用户意图决定 */ }

    ObsidianScreenScaffold(title = "系统") { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.xxl,
                end = Spacing.xxl,
                top = Spacing.xxl,
                bottom = Spacing.xxl + Routes.BOTTOM_BAR_HEIGHT,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            item {
                InfoCard(icon = Icons.Filled.BrightnessAuto, title = "外观") {
                    val options = listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "浅色",
                        ThemeMode.DARK to "深色",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        options.forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
            item {
                InfoCard(icon = Icons.Filled.Sync, title = "数据刷新") {
                    Text(
                        text = "监控页面按固定间隔自动刷新，断线后自动重试恢复。",
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                    )
                    Spacer(Modifier.size(Spacing.lg))
                    val options = listOf(10, 30, 60, 300)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        options.forEach { seconds ->
                            FilterChip(
                                selected = pollIntervalSeconds == seconds,
                                onClick = { viewModel.setPollIntervalSeconds(seconds) },
                                label = {
                                    Text(
                                        when (seconds) {
                                            300 -> "5 分钟"
                                            else -> "$seconds 秒"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                InfoCard(icon = Icons.Filled.Notifications, title = "通知与免打扰") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "服务器通知推送",
                                style = MaterialTheme.typography.bodyMedium,
                                color = obsidian.TextPrimary,
                            )
                            Text(
                                text = "通过 WebSocket 实时订阅服务器告警并推送系统通知",
                                style = MaterialTheme.typography.bodySmall,
                                color = obsidian.TextSecondary,
                            )
                        }
                        Spacer(Modifier.size(Spacing.md))
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setNotificationsEnabled(enabled)
                                if (enabled && Build.VERSION.SDK_INT >= 33) {
                                    val permissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!permissionGranted) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.size(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "实时通道",
                            style = MaterialTheme.typography.bodyMedium,
                            color = obsidian.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        val (label, color) = when (channelState) {
                            NotificationChannelState.IDLE -> "未连接" to obsidian.TextSecondary
                            NotificationChannelState.CONNECTED -> "已连接" to obsidian.Green
                            NotificationChannelState.CONNECTING -> "连接中" to obsidian.Amber
                            NotificationChannelState.RECONNECTING -> "重连中" to obsidian.Red
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = color,
                        )
                    }
                    if (channelError != null) {
                        Spacer(Modifier.size(Spacing.xs))
                        Text(
                            text = channelError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = obsidian.TextSecondary,
                            maxLines = 2,
                        )
                    }
                    if (notificationsEnabled) {
                        Spacer(Modifier.size(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "免打扰时段",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = obsidian.TextPrimary,
                                )
                                Text(
                                    text = "时段内收到的事件不推送，仅静默记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = obsidian.TextSecondary,
                                )
                            }
                            Spacer(Modifier.size(Spacing.md))
                            Switch(
                                checked = dndEnabled,
                                onCheckedChange = viewModel::setDndEnabled,
                            )
                        }
                        if (dndEnabled) {
                            Spacer(Modifier.size(Spacing.sm))
                            DndTimeRow(
                                label = "开始",
                                minutes = dndStartMinutes,
                                onSelect = viewModel::setDndStartMinutes,
                                color = obsidian.Amber,
                            )
                            DndTimeRow(
                                label = "结束",
                                minutes = dndEndMinutes,
                                onSelect = viewModel::setDndEndMinutes,
                                color = obsidian.Cyan,
                            )
                        }
                    }
                    Spacer(Modifier.size(Spacing.lg))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ObsidianCorner.listItem)
                            .clickable(onClick = onOpenNotifications)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "通知列表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = obsidian.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = obsidian.TextSecondary,
                        )
                    }
                }
            }
            item {
                InfoCard(icon = Icons.Filled.Dns, title = "服务器") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onManageServers)
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "服务器管理",
                                style = MaterialTheme.typography.bodyLarge,
                                color = obsidian.TextPrimary,
                            )
                            Text(
                                text = "添加、删除或切换已保存的服务器",
                                style = MaterialTheme.typography.bodySmall,
                                color = obsidian.TextSecondary,
                            )
                        }
                        Spacer(Modifier.size(Spacing.md))
                        Text(
                            text = "$serverCount 台",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = obsidian.Cyan,
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = obsidian.TextSecondary,
                        )
                    }
                    Spacer(Modifier.size(Spacing.xs))
                }
            }
            item {
                InfoCard(icon = Icons.Filled.Shield, title = "明文白名单") {
                    Text(
                        text = "应用内仅允许访问白名单中的明文 HTTP 地址，其余明文请求一律拦截（HTTPS 不受影响）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                    )
                    Spacer(Modifier.size(Spacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "放行内网地址",
                                style = MaterialTheme.typography.bodyMedium,
                                color = obsidian.TextPrimary,
                            )
                            Text(
                                text = "192.168.0.0/16 · 10.0.0.0/8 · 172.16.0.0/12 · localhost",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = obsidian.TextSecondary,
                            )
                        }
                        Spacer(Modifier.size(Spacing.md))
                        Switch(
                            checked = cleartextWhitelist.internalEnabled,
                            onCheckedChange = viewModel::setCleartextInternalEnabled,
                        )
                    }
                    if (cleartextWhitelist.customRules.isNotEmpty()) {
                        Spacer(Modifier.size(Spacing.lg))
                        cleartextWhitelist.customRules.forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xxs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = rule.display,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = obsidian.TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { viewModel.removeCleartextRule(rule.value) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "删除 ${rule.display}",
                                        tint = obsidian.TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(Spacing.md))
                    OutlinedButton(
                        onClick = { showAddRuleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(Spacing.sm))
                        Text("添加规则")
                    }
                }
            }
            item {
                InfoCard(icon = Icons.Filled.Info, title = "关于") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Unraid Assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = obsidian.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = obsidian.TextSecondary,
                        )
                    }
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = "本地直连 Unraid 服务器 · GraphQL API",
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                    )
                    Spacer(Modifier.size(Spacing.md))
                    // 服务器与插件依赖说明：
                    // - 核心功能（Dashboard / Docker / VM / 通知）依赖 unraid-api 提供的 GraphQL API。
                    //   Unraid 7.2+ 起 unraid-api 作为系统内置服务随 OS 启用；
                    //   6.x（含 6.12 ~ 7.1）需手动安装 Unraid Connect 插件（dynamix.unraid.net）。
                    Text(
                        text = "服务器要求",
                        style = MaterialTheme.typography.labelLarge,
                        color = obsidian.TextSecondary,
                    )
                    Spacer(Modifier.size(Spacing.xxs))
                    Text(
                        text = "• unRAID OS 7.2 或更高版本（内置 GraphQL API）\n" +
                            "   或 6.x 安装 Unraid Connect 插件",
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                    )
                    Spacer(Modifier.size(Spacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ObsidianCorner.listItem)
                            .clickable { viewModel.checkForUpdates() }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "检查更新",
                                style = MaterialTheme.typography.bodyMedium,
                                color = obsidian.TextPrimary,
                            )
                            Text(
                                text = updateStatusText(updateState),
                                style = MaterialTheme.typography.bodySmall,
                                color = obsidian.TextSecondary,
                                maxLines = 2,
                            )
                        }
                        Spacer(Modifier.size(Spacing.md))
                        if (updateState is UpdateCheckState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = obsidian.Cyan,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = obsidian.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddCleartextRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onConfirm = viewModel::addCleartextRule,
        )
    }
}

/**
 * 「关于」卡片检查更新行的状态文案（发现新版本时弹窗由根组件统一负责）。
 */
private fun updateStatusText(state: UpdateCheckState): String = when (state) {
    UpdateCheckState.Idle -> "点击检查 GitHub 最新版本"
    UpdateCheckState.Checking -> "正在检查…"
    UpdateCheckState.UpToDate -> "已是最新版本"
    is UpdateCheckState.Available -> "发现新版本 ${state.info.latestVersion}"
    is UpdateCheckState.Error -> state.message
}

/**
 * 免打扰时段选择行：标签 + 当前时刻（点击展开整点下拉）。
 * 跨午夜区间由 [SettingsDataStore] 的分钟数比较逻辑处理，此处仅做显示。
 */
@Composable
private fun DndTimeRow(
    label: String,
    minutes: Int,
    onSelect: (Int) -> Unit,
    color: Color,
) {
    val obsidian = LocalObsidianPalette.current
    var expanded by remember { mutableStateOf(false) }
    val hour = minutes / 60
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = obsidian.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%02d:00".format(hour),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = color,
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = obsidian.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = obsidian.MenuContainer,
        ) {
            (0 until 24).forEach { h ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "%02d:00".format(h),
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(h * 60)
                    },
                )
            }
        }
    }
}

/**
 * 添加白名单规则对话框：输入网段 / IP / 主机名 / 通配符，经 ViewModel 校验，
 * 非法输入就地提示错误；合法则回调 [onConfirm]（返回 true 表示成功）并关闭。
 */
@Composable
private fun AddCleartextRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean,
) {
    val obsidian = LocalObsidianPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = obsidian.Background,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("添加白名单规则", color = obsidian.TextPrimary) },
        text = {
            Column {
                Text(
                    text = "支持：内网网段（192.168.0.0/16）、IP（192.168.1.5）、主机名（nas.local）、通配符（*.example.com）",
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                )
                Spacer(Modifier.size(Spacing.xl))
                ObsidianTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    singleLine = true,
                    label = "规则",
                    placeholder = "例如 192.168.0.0/16",
                    isError = error != null,
                    supportingText = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onConfirm(input)) {
                        onDismiss()
                    } else {
                        error = "格式无效，请输入网段、IP 或主机名"
                    }
                },
            ) {
                Text("添加", color = obsidian.Cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = obsidian.TextSecondary)
            }
        },
    )
}
