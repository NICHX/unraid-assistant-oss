package com.nichx.unraidassistant.feature.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.core.util.Format
import com.nichx.unraidassistant.data.model.ArrayDiskStatusEnum
import com.nichx.unraidassistant.data.model.ArrayDiskTypeEnum
import com.nichx.unraidassistant.data.model.ArrayStateEnum
import com.nichx.unraidassistant.data.model.DiskInfo
import com.nichx.unraidassistant.data.model.ParityCheckInfo
import com.nichx.unraidassistant.data.model.ParityCheckStatusEnum
import com.nichx.unraidassistant.data.model.ShareInfo
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.ui.components.ActionButton
import com.nichx.unraidassistant.ui.components.ConfirmActionHost
import com.nichx.unraidassistant.ui.components.DetailDialog
import com.nichx.unraidassistant.ui.components.DetailRow
import com.nichx.unraidassistant.ui.components.ErrorBanner
import com.nichx.unraidassistant.ui.components.ErrorState
import com.nichx.unraidassistant.ui.components.InfoCard
import com.nichx.unraidassistant.ui.components.InfoRow
import com.nichx.unraidassistant.ui.components.LoadingSkeleton
import com.nichx.unraidassistant.ui.components.NoServerState
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.RefreshAction
import com.nichx.unraidassistant.ui.components.StatusPill
import com.nichx.unraidassistant.ui.navigation.Routes
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: StorageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showManageDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<StorageAction?>(null) }

    ConfirmActionHost(
        action = pendingAction,
        title = { it.confirmTitle() },
        message = { it.confirmMessage() },
        confirmText = { it.confirmText() },
        danger = { it.danger() },
        onConfirm = {
            pendingAction = null
            when (it) {
                is StorageAction.StartArray -> viewModel.startArray()
                is StorageAction.StopArray -> viewModel.stopArray()
                is StorageAction.StartParity -> viewModel.startParityCheck()
                is StorageAction.PauseParity -> viewModel.pauseParityCheck()
                is StorageAction.ResumeParity -> viewModel.resumeParityCheck()
                is StorageAction.CancelParity -> viewModel.cancelParityCheck()
            }
        },
        onDismiss = { pendingAction = null },
    )

    val successState = uiState as? StorageUiState.Success
    if (showManageDialog && successState != null) {
        ArrayManageDialog(
            data = successState.data,
            busyAction = successState.busyAction,
            onAction = { pendingAction = it },
            onDismiss = { showManageDialog = false },
        )
    }

    ObsidianScreenScaffold(
        title = "存储",
        actions = {
            RefreshAction(
                isRefreshing = successState?.isRefreshing == true,
                enabled = successState != null,
                onClick = viewModel::refresh,
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is StorageUiState.Loading -> LoadingSkeleton(
                Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
            is StorageUiState.NoServer -> NoServerState(Modifier.padding(padding))
            is StorageUiState.Error -> ErrorState(
                message = state.message,
                onRetry = state.retry,
                modifier = Modifier.padding(padding),
            )
            is StorageUiState.Success -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                StorageContent(
                    state = state,
                    onOpenManage = { showManageDialog = true },
                    onDismissTransient = viewModel::dismissError,
                    onDismissActionError = viewModel::dismissActionError,
                    onDismissActionMessage = viewModel::dismissActionMessage,
                )
            }
        }
    }
}

/** 存储页操作意图：需要二次确认的操作在此统一收敛。 */
private sealed interface StorageAction {
    data object StartArray : StorageAction
    data object StopArray : StorageAction
    data object StartParity : StorageAction
    data object PauseParity : StorageAction
    data object ResumeParity : StorageAction
    data object CancelParity : StorageAction
}

private fun StorageAction.confirmTitle(): String = when (this) {
    is StorageAction.StartArray -> "启动阵列"
    is StorageAction.StopArray -> "停止阵列"
    is StorageAction.StartParity -> "开始奇偶校验"
    is StorageAction.PauseParity -> "暂停奇偶校验"
    is StorageAction.ResumeParity -> "恢复奇偶校验"
    is StorageAction.CancelParity -> "取消奇偶校验"
}

private fun StorageAction.confirmMessage(): String = when (this) {
    is StorageAction.StartArray -> "确定要启动阵列吗？所有数据磁盘将挂载并开始提供服务。"
    is StorageAction.StopArray -> "确定要停止阵列吗？正在运行的 Docker 容器和虚拟机将受影响，阵列停止期间无法访问共享。"
    is StorageAction.StartParity -> "确定要开始奇偶校验吗？校验期间阵列可正常使用，但磁盘 I/O 负载会升高。"
    is StorageAction.PauseParity -> "确定要暂停奇偶校验吗？校验进度将保留，之后可随时恢复。"
    is StorageAction.ResumeParity -> "确定要恢复暂停的奇偶校验吗？"
    is StorageAction.CancelParity -> "确定要取消奇偶校验吗？本次校验进度将丢失，未检查的部分标记为未校验。"
}

private fun StorageAction.confirmText(): String = when (this) {
    is StorageAction.StartArray -> "启动"
    is StorageAction.StopArray -> "停止"
    is StorageAction.StartParity -> "开始"
    is StorageAction.PauseParity -> "暂停"
    is StorageAction.ResumeParity -> "恢复"
    is StorageAction.CancelParity -> "取消校验"
}

/** 阵列启停为全局状态变更，用红色确认按钮强化警示。 */
private fun StorageAction.danger(): Boolean = when (this) {
    is StorageAction.StartArray -> true
    is StorageAction.StopArray -> true
    is StorageAction.StartParity -> false
    is StorageAction.PauseParity -> false
    is StorageAction.ResumeParity -> false
    is StorageAction.CancelParity -> true
}

@Composable
private fun StorageContent(
    state: StorageUiState.Success,
    onOpenManage: () -> Unit,
    onDismissTransient: () -> Unit,
    onDismissActionError: () -> Unit,
    onDismissActionMessage: () -> Unit,
) {
    val data = state.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.xxl,
            end = Spacing.xxl,
            top = Spacing.xxl,
            bottom = Spacing.xxl + Routes.BOTTOM_BAR_HEIGHT,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        if (state.transientError != null) {
            item {
                ErrorBanner(
                    message = "连接失败：${state.transientError}，正在自动重试…",
                    onDismiss = onDismissTransient,
                )
            }
        }
        if (state.actionError != null) {
            item {
                ErrorBanner(
                    message = "操作失败：${state.actionError}",
                    onDismiss = onDismissActionError,
                )
            }
        }
        if (state.actionMessage != null) {
            item {
                ErrorBanner(
                    message = state.actionMessage,
                    isError = false,
                    onDismiss = onDismissActionMessage,
                )
            }
        }
        item {
            ArraySummaryCard(
                data = data,
                onOpenManage = onOpenManage,
            )
        }
        if (data.parityCheck != null || data.parities.isNotEmpty()) {
            item {
                ParityCheckCard(
                    parity = data.parityCheck,
                    parities = data.parities,
                )
            }
        }
        if (data.parities.isNotEmpty()) {
            item { SectionHeader("Parity 磁盘") }
            items(data.parities, key = { it.id }) { DiskCard(it) }
        }
        if (data.dataDisks.isNotEmpty()) {
            item { SectionHeader("数据磁盘") }
            items(data.dataDisks, key = { it.id }) { DiskCard(it) }
        }
        if (data.cacheDisks.isNotEmpty()) {
            item { SectionHeader("缓存磁盘") }
            items(data.cacheDisks, key = { it.id }) { DiskCard(it) }
        }
        data.bootDisk?.let {
            item { SectionHeader("启动盘") }
            item { DiskCard(it) }
        }
        if (data.shares.isNotEmpty()) {
            item { SectionHeader("共享") }
            items(data.shares, key = { it.id }) { ShareCard(it) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val obsidian = LocalObsidianPalette.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = obsidian.TextSecondary,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

/** 阵列只读摘要卡：仅展示状态与统计，启停等操作收敛到"阵列管理"弹窗，避免误触。 */
@Composable
private fun ArraySummaryCard(
    data: StorageData,
    onOpenManage: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val (stateText, stateColor) = data.arrayState.toUi()
    InfoCard(
        icon = Icons.Filled.Storage,
        title = "阵列状态",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(stateText, stateColor)
                Spacer(Modifier.size(Spacing.md))
                ActionButton(
                    text = "管理",
                    icon = Icons.Filled.Settings,
                    tint = obsidian.Cyan,
                    onClick = onOpenManage,
                )
            }
        },
    ) {
        data.capacity?.let { cap ->
            val totalKb = cap.totalKb.toLongOrNull() ?: 0L
            val usedKb = cap.usedKb.toLongOrNull() ?: 0L
            val freeKb = cap.freeKb.toLongOrNull() ?: 0L
            InfoRow("总容量", Format.bytes(totalKb * 1024))
            InfoRow("已用 / 空闲", "${Format.bytes(usedKb * 1024)} / ${Format.bytes(freeKb * 1024)}")
            if (totalKb > 0L) {
                Spacer(Modifier.size(Spacing.lg))
                UsageBar(usedKb.toFloat() / totalKb.toFloat())
                Spacer(Modifier.size(Spacing.sm))
                InfoRow("使用率", Format.percentage(usedKb.toDouble() / totalKb * 100))
            }
        }
        InfoRow("磁盘数", "${data.dataDisks.size} 数据 / ${data.parities.size} Parity / ${data.cacheDisks.size} 缓存")
        InfoRow("共享数", data.shares.size.toString())
    }
}

/** 奇偶校验只读状态卡片：校验状态与进度；校验盘详情见下方独立分区，操作见「阵列管理」。 */
@Composable
private fun ParityCheckCard(
    parity: ParityCheckInfo?,
    parities: List<DiskInfo>,
) {
    val obsidian = LocalObsidianPalette.current
    val (statusText, statusColor) = (parity?.status ?: ParityCheckStatusEnum.NEVER_RUN).toUi()
    InfoCard(
        icon = Icons.Filled.Balance,
        title = "奇偶校验",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (parities.isEmpty()) {
                    StatusPill("无校验盘", StatusColors.Alert)
                } else {
                    StatusPill(statusText, statusColor)
                }
            }
        },
    ) {
        val progress = parity?.progress ?: 0
        if (parities.isEmpty()) {
            Text(
                text = "未配置 Parity 磁盘，奇偶校验不可用",
                style = MaterialTheme.typography.bodySmall,
                color = StatusColors.Alert,
            )
        } else if (parity != null && (parity.status == ParityCheckStatusEnum.RUNNING || parity.status == ParityCheckStatusEnum.PAUSED)) {
            Spacer(Modifier.size(Spacing.lg))
            UsageBar(progress.toFloat() / 100f)
            Spacer(Modifier.size(Spacing.sm))
            InfoRow("进度", Format.percentage(progress.toDouble()))
            parity.speed?.let { InfoRow("速度", "$it MB/s") }
            parity.errors?.let { err ->
                InfoRow(
                    label = "错误",
                    value = err.toString(),
                    valueColor = if (err > 0) StatusColors.Alert else null,
                )
            }
            parity.durationSeconds?.let { sec ->
                InfoRow("已用", Format.uptime(sec.toLong()))
            }
        } else {
            Text(
                text = parity?.status?.description() ?: "尚未执行过校验",
                style = MaterialTheme.typography.bodySmall,
                color = obsidian.TextSecondary,
            )
            parity?.durationSeconds?.let { sec ->
                Spacer(Modifier.size(Spacing.xxs))
                Text(
                    text = "上次耗时 ${Format.uptime(sec.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                )
            }
        }
    }
}

/** 阵列管理弹窗：阵列启停 + 奇偶校验控制，均为敏感操作，执行前再弹 ConfirmDialog。 */
@Composable
private fun ArrayManageDialog(
    data: StorageData,
    busyAction: StorageActionType?,
    onAction: (StorageAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val arrayStarted = data.arrayState == ArrayStateEnum.STARTED
    val arrayBusy = busyAction == StorageActionType.ARRAY_START || busyAction == StorageActionType.ARRAY_STOP
    val hasParity = data.parities.isNotEmpty()
    val (stateText, stateColor) = data.arrayState.toUi()
    DetailDialog(
        title = "阵列管理",
        subtitle = "全局状态操作，请谨慎执行",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "阵列状态",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = obsidian.TextPrimary,
        )
        Spacer(Modifier.size(Spacing.md))
        DetailRow("当前状态", stateText, valueColor = stateColor, monospace = false)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionButton(
                text = if (arrayStarted) "停止阵列" else "启动阵列",
                icon = if (arrayStarted) Icons.Filled.PowerSettingsNew else Icons.Filled.PlayArrow,
                tint = if (arrayStarted) obsidian.Red else obsidian.Green,
                enabled = !arrayBusy,
                busy = arrayBusy,
                onClick = {
                    onAction(if (arrayStarted) StorageAction.StopArray else StorageAction.StartArray)
                },
            )
        }

        Spacer(Modifier.size(Spacing.xxxl))
        Text(
            text = "奇偶校验",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = obsidian.TextPrimary,
        )
        Spacer(Modifier.size(Spacing.md))
        data.parityCheck?.let { parity ->
            val (statusText, statusColor) = parity.status.toUi()
            DetailRow("校验状态", statusText, valueColor = statusColor, monospace = false)
            val progress = parity.progress ?: 0
            if (parity.status == ParityCheckStatusEnum.RUNNING || parity.status == ParityCheckStatusEnum.PAUSED) {
                DetailRow("进度", Format.percentage(progress.toDouble()))
                parity.speed?.let { DetailRow("速度", "$it MB/s") }
                parity.errors?.let { err ->
                    DetailRow(
                        "错误数",
                        err.toString(),
                        valueColor = if (err > 0) StatusColors.Alert else null,
                    )
                }
            }
        }
        if (!hasParity) {
            InfoRow(
                label = "未配置校验盘",
                value = "请先添加 Parity 磁盘",
                valueColor = StatusColors.Alert,
            )
        }
        Spacer(Modifier.size(Spacing.md))
        ParityManageActions(
            status = data.parityCheck?.status ?: ParityCheckStatusEnum.NEVER_RUN,
            arrayStarted = arrayStarted,
            hasParity = hasParity,
            busyAction = busyAction,
            onAction = onAction,
        )
    }
}

@Composable
private fun ParityManageActions(
    status: ParityCheckStatusEnum,
    arrayStarted: Boolean,
    hasParity: Boolean,
    busyAction: StorageActionType?,
    onAction: (StorageAction) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val busy = busyAction != null &&
        (busyAction == StorageActionType.PARITY_START ||
            busyAction == StorageActionType.PARITY_PAUSE ||
            busyAction == StorageActionType.PARITY_RESUME ||
            busyAction == StorageActionType.PARITY_CANCEL)
    val busyPause = busyAction == StorageActionType.PARITY_PAUSE
    val busyCancel = busyAction == StorageActionType.PARITY_CANCEL
    val busyResume = busyAction == StorageActionType.PARITY_RESUME
    val busyStart = busyAction == StorageActionType.PARITY_START
    when (status) {
        ParityCheckStatusEnum.RUNNING -> {
            ActionButton(
                text = "暂停",
                icon = Icons.Filled.SmartButton,
                tint = obsidian.TextSecondary,
                enabled = !busy,
                busy = busyPause,
                onClick = { onAction(StorageAction.PauseParity) },
            )
            ActionButton(
                text = "取消",
                icon = Icons.Filled.Close,
                tint = obsidian.Red,
                enabled = !busy,
                busy = busyCancel,
                onClick = { onAction(StorageAction.CancelParity) },
            )
        }
        ParityCheckStatusEnum.PAUSED -> {
            ActionButton(
                text = "恢复",
                icon = Icons.Filled.PlayArrow,
                tint = obsidian.Green,
                enabled = !busy,
                busy = busyResume,
                onClick = { onAction(StorageAction.ResumeParity) },
            )
            ActionButton(
                text = "取消",
                icon = Icons.Filled.Close,
                tint = obsidian.Red,
                enabled = !busy,
                busy = busyCancel,
                onClick = { onAction(StorageAction.CancelParity) },
            )
        }
        else -> {
            when {
                !arrayStarted -> Text(
                    text = "阵列未启动，无法开始校验",
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                )
                !hasParity -> Text(
                    text = "未配置校验盘，奇偶校验不可用",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.Alert,
                )
                else -> ActionButton(
                    text = "开始校验",
                    icon = Icons.Filled.Balance,
                    tint = obsidian.Cyan,
                    enabled = !busy,
                    busy = busyStart,
                    onClick = { onAction(StorageAction.StartParity) },
                )
            }
        }
    }
}

private fun ParityCheckStatusEnum.toUi(): Pair<String, Color> = when (this) {
    ParityCheckStatusEnum.NEVER_RUN -> "未运行过" to StatusColors.Stopped
    ParityCheckStatusEnum.RUNNING -> "校验中" to StatusColors.Running
    ParityCheckStatusEnum.PAUSED -> "已暂停" to StatusColors.Paused
    ParityCheckStatusEnum.COMPLETED -> "已完成" to StatusColors.Running
    ParityCheckStatusEnum.CANCELLED -> "已取消" to StatusColors.Stopped
    ParityCheckStatusEnum.FAILED -> "失败" to StatusColors.Alert
}

private fun ParityCheckStatusEnum.description(): String = when (this) {
    ParityCheckStatusEnum.NEVER_RUN -> "尚未执行过奇偶校验"
    ParityCheckStatusEnum.RUNNING -> "奇偶校验进行中"
    ParityCheckStatusEnum.PAUSED -> "奇偶校验已暂停"
    ParityCheckStatusEnum.COMPLETED -> "上次校验正常完成"
    ParityCheckStatusEnum.CANCELLED -> "上次校验被取消"
    ParityCheckStatusEnum.FAILED -> "上次校验失败"
}

@Composable
private fun DiskCard(disk: DiskInfo) {
    val obsidian = LocalObsidianPalette.current
    val (icon, tint) = disk.type.toUi()
    val (statusText, statusColor) = disk.status.toUi()
    InfoCard(
        icon = icon,
        title = disk.name ?: disk.device ?: "未命名",
        trailing = { StatusPill(statusText, statusColor) },
    ) {
        disk.device?.let { InfoRow("设备", it) }
        disk.tempCelsius?.let { temp ->
            InfoRow(
                label = "温度",
                value = Format.temperature(temp.toDouble()),
                valueColor = when {
                    temp >= 55 -> StatusColors.Alert
                    temp >= 45 -> StatusColors.Paused
                    else -> null
                },
            )
        }
        disk.isRotational?.let { InfoRow("类型", if (it) "HDD" else "SSD") }
        disk.sizeKb?.let { InfoRow("容量", Format.bytes(it * 1024)) }
        if (disk.type == ArrayDiskTypeEnum.DATA || disk.type == ArrayDiskTypeEnum.CACHE) {
            val fsSize = disk.fsSizeKb ?: 0L
            val used = disk.fsUsedKb ?: 0L
            if (fsSize > 0L) {
                Spacer(Modifier.size(Spacing.lg))
                UsageBar(used.toFloat() / fsSize.toFloat())
                Spacer(Modifier.size(Spacing.sm))
                InfoRow("已用 ${Format.bytes(used * 1024)} / ${Format.bytes(fsSize * 1024)}", Format.percentage(used.toDouble() / fsSize * 100))
            }
            val reads = disk.numReads
            val writes = disk.numWrites
            val errors = disk.numErrors
            if (reads != null || writes != null || errors != null) {
                Spacer(Modifier.size(Spacing.xs))
                InfoRow(
                    label = "读写 / 错误",
                    value = "${reads ?: 0} / ${writes ?: 0} / ${errors ?: 0}",
                    valueColor = if ((errors ?: 0) > 0L) StatusColors.Alert else null,
                )
            }
        }
    }
}

@Composable
private fun ShareCard(share: ShareInfo) {
    val obsidian = LocalObsidianPalette.current
    val usedKb = share.usedKb ?: 0L
    val freeKb = share.freeKb ?: 0L
    val sizeKb = (share.sizeKb ?: 0L).let { if (it > 0L) it else usedKb + freeKb }
    InfoCard(icon = Icons.Filled.FolderShared, title = share.name ?: "未命名") {
        InfoRow("容量", Format.bytes(sizeKb * 1024))
        InfoRow("已用 / 空闲", "${Format.bytes(usedKb * 1024)} / ${Format.bytes(freeKb * 1024)}")
        share.cache?.let { InfoRow("缓存", if (it) "启用" else "关闭") }
        if (sizeKb > 0L) {
            Spacer(Modifier.size(Spacing.lg))
            UsageBar(usedKb.toFloat() / sizeKb.toFloat())
            Spacer(Modifier.size(Spacing.sm))
            InfoRow("使用率", Format.percentage(usedKb.toDouble() / sizeKb * 100))
        }
    }
}

/** 容量使用率进度条（Obsidian 渐变）：随使用率变黄/变红。 */
@Composable
private fun UsageBar(percent: Float, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val obsidian = LocalObsidianPalette.current
    val color = when {
        percent >= 0.9f -> obsidian.Red
        percent >= 0.75f -> obsidian.Amber
        else -> obsidian.Cyan
    }
    LinearProgressIndicator(
        progress = { percent.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = color,
        trackColor = obsidian.Track,
    )
}

@Composable
private fun ArrayDiskTypeEnum.toUi(): Pair<ImageVector, Color> {
    val obsidian = LocalObsidianPalette.current
    return when (this) {
        ArrayDiskTypeEnum.PARITY -> Icons.Filled.Balance to obsidian.Amber
        ArrayDiskTypeEnum.DATA -> Icons.Filled.Dataset to obsidian.Cyan
        ArrayDiskTypeEnum.CACHE -> Icons.Filled.Bolt to obsidian.Indigo
        ArrayDiskTypeEnum.BOOT, ArrayDiskTypeEnum.FLASH -> Icons.Filled.Usb to obsidian.TextSecondary
    }
}

@Composable
private fun ArrayStateEnum.toUi(): Pair<String, Color> {
    val obsidian = LocalObsidianPalette.current
    return when (this) {
        ArrayStateEnum.STARTED -> "已启动" to StatusColors.Running
        ArrayStateEnum.STOPPED -> "已停止" to StatusColors.Stopped
        ArrayStateEnum.NEW_ARRAY -> "新阵列" to StatusColors.Paused
        ArrayStateEnum.RECON_DISK -> "重建中" to StatusColors.Paused
        ArrayStateEnum.DISABLE_DISK -> "禁用磁盘" to StatusColors.Alert
        ArrayStateEnum.SWAP_DSBL -> "替换禁用" to StatusColors.Alert
        ArrayStateEnum.INVALID_EXPANSION -> "无效扩展" to StatusColors.Alert
        ArrayStateEnum.PARITY_NOT_BIGGEST -> "Parity 不足" to StatusColors.Alert
        ArrayStateEnum.TOO_MANY_MISSING_DISKS -> "缺失磁盘过多" to StatusColors.Alert
        ArrayStateEnum.NEW_DISK_TOO_SMALL -> "新盘过小" to StatusColors.Alert
        ArrayStateEnum.NO_DATA_DISKS -> "无数据盘" to StatusColors.Stopped
    }
}

@Composable
private fun ArrayDiskStatusEnum?.toUi(): Pair<String, Color> {
    return when (this) {
        null -> "—" to StatusColors.Stopped
        ArrayDiskStatusEnum.DISK_OK -> "正常" to StatusColors.Running
        ArrayDiskStatusEnum.DISK_NP -> "未在位" to StatusColors.Stopped
        ArrayDiskStatusEnum.DISK_NP_MISSING -> "缺失" to StatusColors.Alert
        ArrayDiskStatusEnum.DISK_INVALID -> "无效" to StatusColors.Alert
        ArrayDiskStatusEnum.DISK_WRONG -> "错误盘" to StatusColors.Alert
        ArrayDiskStatusEnum.DISK_DSBL -> "已禁用" to StatusColors.Alert
        ArrayDiskStatusEnum.DISK_NP_DSBL -> "缺失并禁用" to StatusColors.Alert
        ArrayDiskStatusEnum.DISK_DSBL_NEW -> "新盘禁用" to StatusColors.Paused
        ArrayDiskStatusEnum.DISK_NEW -> "新盘" to StatusColors.Paused
    }
}
