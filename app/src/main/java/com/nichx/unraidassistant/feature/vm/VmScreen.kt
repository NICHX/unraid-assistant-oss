package com.nichx.unraidassistant.feature.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.core.datastore.ContentViewMode
import com.nichx.unraidassistant.data.model.VmInfo
import com.nichx.unraidassistant.data.model.VmStateEnum
import com.nichx.unraidassistant.ui.components.ActionButton
import com.nichx.unraidassistant.ui.components.ConfirmDialog
import com.nichx.unraidassistant.ui.components.ContentViewToggle
import com.nichx.unraidassistant.ui.components.DetailDialog
import com.nichx.unraidassistant.ui.components.DetailRow
import com.nichx.unraidassistant.ui.components.EntityCard
import com.nichx.unraidassistant.ui.components.ErrorBannerStack
import com.nichx.unraidassistant.ui.components.ErrorState
import com.nichx.unraidassistant.ui.components.FullPageState
import com.nichx.unraidassistant.ui.components.GlassCard
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
fun VmScreen(
    viewModel: VmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    var detailVm by remember { mutableStateOf<VmInfo?>(null) }
    var pendingAction by remember { mutableStateOf<VmAction?>(null) }
    var busyActionKind by remember { mutableStateOf<VmActionKind?>(null) }

    pendingAction?.let { action ->
        ConfirmDialog(
            title = action.confirmTitle(),
            message = action.confirmMessage(),
            confirmText = action.confirmText(),
            danger = action.danger(),
            onConfirm = {
                pendingAction = null
                busyActionKind = action.kind()
                when (action) {
                    is VmAction.Start -> viewModel.startVm(action.vm.id)
                    is VmAction.Stop -> viewModel.stopVm(action.vm.id)
                    is VmAction.ForceStop -> viewModel.forceStopVm(action.vm.id)
                    is VmAction.Pause -> viewModel.pauseVm(action.vm.id)
                    is VmAction.Resume -> viewModel.resumeVm(action.vm.id)
                    is VmAction.Reboot -> viewModel.rebootVm(action.vm.id)
                }
            },
            onDismiss = { pendingAction = null },
        )
    }

    val successState = uiState as? VmUiState.Success

    // 操作/轮询刷新后同步弹窗快照：状态文本与操作按钮随最新数据更新。
    LaunchedEffect(successState?.data?.vms) {
        val snapshot = detailVm ?: return@LaunchedEffect
        successState?.data?.vms
            ?.firstOrNull { it.id == snapshot.id }
            ?.let { detailVm = it }
    }

    // 操作完成（busyVmId 清除）后复位弹窗内的操作中标记。
    LaunchedEffect(successState?.busyVmId, detailVm?.id) {
        if (successState?.busyVmId != detailVm?.id) {
            busyActionKind = null
        }
    }

    detailVm?.let { vm ->
        val isBusy = successState?.busyVmId == vm.id
        VmDetailDialog(
            vm = vm,
            busy = isBusy,
            busyKind = busyActionKind,
            onAction = { action -> pendingAction = action },
            onDismiss = { detailVm = null },
        )
    }

    ObsidianScreenScaffold(
        title = "虚拟机",
        actions = {
            ContentViewToggle(
                mode = viewMode,
                onModeChange = viewModel::setViewMode,
            )
            RefreshAction(
                isRefreshing = successState?.isRefreshing == true,
                enabled = successState != null,
                onClick = viewModel::refresh,
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is VmUiState.Loading -> LoadingSkeleton(
                Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
            is VmUiState.NoServer -> NoServerState(Modifier.padding(padding))
            is VmUiState.Error -> ErrorState(
                message = state.message,
                onRetry = state.retry,
                modifier = Modifier.padding(padding),
            )
            is VmUiState.Success -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                VmContent(
                    state = state,
                    viewMode = viewMode,
                    onOpenDetail = { detailVm = it },
                    onDismissTransient = viewModel::dismissError,
                    onDismissActionError = viewModel::dismissActionError,
                    onDismissActionMessage = viewModel::dismissActionMessage,
                )
            }
        }
    }
}

/** 虚拟机操作意图：需要二次确认的操作在此统一收敛。 */
private sealed interface VmAction {
    val vm: VmInfo

    data class Start(override val vm: VmInfo) : VmAction
    data class Stop(override val vm: VmInfo) : VmAction
    data class ForceStop(override val vm: VmInfo) : VmAction
    data class Pause(override val vm: VmInfo) : VmAction
    data class Resume(override val vm: VmInfo) : VmAction
    data class Reboot(override val vm: VmInfo) : VmAction
}

/** 操作类型：确认后用于在弹窗对应按钮上显示"操作中"转圈。 */
private enum class VmActionKind { Start, Stop, ForceStop, Pause, Resume, Reboot }

private fun VmAction.kind(): VmActionKind = when (this) {
    is VmAction.Start -> VmActionKind.Start
    is VmAction.Stop -> VmActionKind.Stop
    is VmAction.ForceStop -> VmActionKind.ForceStop
    is VmAction.Pause -> VmActionKind.Pause
    is VmAction.Resume -> VmActionKind.Resume
    is VmAction.Reboot -> VmActionKind.Reboot
}

private fun VmAction.confirmTitle(): String = when (this) {
    is VmAction.Start -> "启动虚拟机"
    is VmAction.Stop -> "关闭虚拟机"
    is VmAction.ForceStop -> "强制停止虚拟机"
    is VmAction.Pause -> "暂停虚拟机"
    is VmAction.Resume -> "恢复虚拟机"
    is VmAction.Reboot -> "重启虚拟机"
}

private fun VmAction.confirmMessage(): String = when (this) {
    is VmAction.Start -> "确定要启动「${vm.name}」吗？"
    is VmAction.Stop -> "确定要关闭「${vm.name}」吗？系统将向虚拟机发送 ACPI 关机信号。"
    is VmAction.ForceStop -> "确定要强制停止「${vm.name}」吗？相当于拔掉电源，未保存的数据将会丢失！"
    is VmAction.Pause -> "确定要暂停「${vm.name}」吗？运行状态将保留在内存中。"
    is VmAction.Resume -> "确定要恢复「${vm.name}」吗？"
    is VmAction.Reboot -> "确定要重启「${vm.name}」吗？"
}

private fun VmAction.confirmText(): String = when (this) {
    is VmAction.Start -> "启动"
    is VmAction.Stop -> "关闭"
    is VmAction.ForceStop -> "强制停止"
    is VmAction.Pause -> "暂停"
    is VmAction.Resume -> "恢复"
    is VmAction.Reboot -> "重启"
}

/** 停机/强制停止等影响可用性或数据安全的操作用红色确认按钮强化警示。 */
private fun VmAction.danger(): Boolean = when (this) {
    is VmAction.Start -> false
    is VmAction.Stop -> true
    is VmAction.ForceStop -> true
    is VmAction.Pause -> false
    is VmAction.Resume -> false
    is VmAction.Reboot -> true
}

@Composable
private fun VmContent(
    state: VmUiState.Success,
    viewMode: ContentViewMode,
    onOpenDetail: (VmInfo) -> Unit,
    onDismissTransient: () -> Unit,
    onDismissActionError: () -> Unit,
    onDismissActionMessage: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val vms = state.data.vms
    val vmIcon: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(obsidian.Glass, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DesktopWindows,
                contentDescription = null,
                tint = obsidian.Indigo,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    val headerBlocks = vmHeaderBlocks(
        state = state,
        vms = vms,
        onDismissTransient = onDismissTransient,
        onDismissActionError = onDismissActionError,
        onDismissActionMessage = onDismissActionMessage,
    )
    if (viewMode == ContentViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.xxl,
                end = Spacing.xxl,
                top = Spacing.xxl,
                bottom = Spacing.xxl + Routes.BOTTOM_BAR_HEIGHT,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            headerBlocks.forEach { block ->
                item(span = { GridItemSpan(maxLineSpan) }) { block() }
            }
            if (vms.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FullPageState(
                        icon = Icons.Filled.DesktopWindows,
                        iconTint = obsidian.TextSecondary,
                        title = "暂无虚拟机",
                        subtitle = "在 Unraid WebUI 中创建虚拟机后，这里会显示其状态",
                    )
                }
            } else {
                items(vms, key = { it.id }) { vm ->
                    val (stateText, stateColor) = vm.state.toUi()
                    EntityCard(
                        title = vm.name,
                        subtitle = "UUID ${vm.id.substringAfter(':').take(13)}…",
                        statusText = stateText,
                        statusColor = stateColor,
                        isGrid = viewMode == ContentViewMode.GRID,
                        onClick = { onOpenDetail(vm) },
                        icon = vmIcon,
                        gridHeight = 130.dp,
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.xxl,
                end = Spacing.xxl,
                top = Spacing.xxl,
                bottom = Spacing.xxl + Routes.BOTTOM_BAR_HEIGHT,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            headerBlocks.forEach { block ->
                item { block() }
            }
            if (vms.isEmpty()) {
                item {
                    FullPageState(
                        icon = Icons.Filled.DesktopWindows,
                        iconTint = obsidian.TextSecondary,
                        title = "暂无虚拟机",
                        subtitle = "在 Unraid WebUI 中创建虚拟机后，这里会显示其状态",
                    )
                }
            } else {
                vms.forEach { vm ->
                    item(key = vm.id) {
                        val (stateText, stateColor) = vm.state.toUi()
                        EntityCard(
                            title = vm.name,
                            subtitle = "UUID ${vm.id.substringAfter(':').take(13)}…",
                            statusText = stateText,
                            statusColor = stateColor,
                            isGrid = viewMode == ContentViewMode.GRID,
                            onClick = { onOpenDetail(vm) },
                            icon = vmIcon,
                            gridHeight = 130.dp,
                        )
                    }
                }
            }
        }
    }
}

/** 顶部区块（错误横幅/汇总）在网格与列表两种布局间复用。 */
private fun vmHeaderBlocks(
    state: VmUiState.Success,
    vms: List<VmInfo>,
    onDismissTransient: () -> Unit,
    onDismissActionError: () -> Unit,
    onDismissActionMessage: () -> Unit,
): List<@Composable () -> Unit> = buildList {
    if (state.transientError != null || state.actionError != null || state.actionMessage != null) {
        add {
            ErrorBannerStack(
                transientError = state.transientError,
                actionError = state.actionError,
                actionMessage = state.actionMessage,
                onDismissTransient = onDismissTransient,
                onDismissActionError = onDismissActionError,
                onDismissActionMessage = onDismissActionMessage,
            )
        }
    }
    add { VmSummaryCard(vms) }
}

@Composable
private fun VmSummaryCard(vms: List<VmInfo>) {
    val obsidian = LocalObsidianPalette.current
    val running = vms.count { it.state == VmStateEnum.RUNNING }
    val paused = vms.count { it.state == VmStateEnum.PAUSED }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            SummaryStat(
                label = "运行中",
                value = "$running / ${vms.size}",
                color = obsidian.Green,
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                label = "已暂停",
                value = paused.toString(),
                color = obsidian.Amber,
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                label = "已停止",
                value = (vms.size - running - paused).toString(),
                color = obsidian.TextSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.size(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = obsidian.TextSecondary,
        )
    }
}

/** 虚拟机详情弹窗：完整信息 + 二级操作（操作仍需 ConfirmDialog 二次确认）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VmDetailDialog(
    vm: VmInfo,
    busy: Boolean,
    busyKind: VmActionKind?,
    onAction: (VmAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val (stateText, stateColor) = vm.state.toUi()
    DetailDialog(
        title = vm.name,
        subtitle = "虚拟机详情",
        onDismiss = onDismiss,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(obsidian.Glass, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DesktopWindows,
                    contentDescription = null,
                    tint = obsidian.Indigo,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(Spacing.xl))
            StatusPill(stateText, stateColor)
        }
        Spacer(Modifier.size(Spacing.xxl))

        DetailRow("UUID", vm.id.substringAfter(':'))
        DetailRow("状态", stateText, valueColor = stateColor, monospace = false)

        Spacer(Modifier.size(Spacing.xxl))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (vm.state) {
                VmStateEnum.RUNNING, VmStateEnum.IDLE -> {
                    ActionButton(
                        text = "暂停",
                        icon = Icons.Filled.SmartButton,
                        tint = obsidian.TextSecondary,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.Pause,
                        onClick = { onAction(VmAction.Pause(vm)) },
                    )
                    ActionButton(
                        text = "重启",
                        icon = Icons.Filled.RestartAlt,
                        tint = obsidian.TextSecondary,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.Reboot,
                        onClick = { onAction(VmAction.Reboot(vm)) },
                    )
                    ActionButton(
                        text = "关闭",
                        icon = Icons.Filled.PowerSettingsNew,
                        tint = obsidian.Red,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.Stop,
                        onClick = { onAction(VmAction.Stop(vm)) },
                    )
                    ActionButton(
                        text = "强制停止",
                        icon = Icons.Filled.PowerSettingsNew,
                        tint = obsidian.Red,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.ForceStop,
                        onClick = { onAction(VmAction.ForceStop(vm)) },
                    )
                }
                VmStateEnum.PAUSED, VmStateEnum.PMSUSPENDED -> {
                    ActionButton(
                        text = "恢复",
                        icon = Icons.Filled.PlayArrow,
                        tint = obsidian.Green,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.Resume,
                        onClick = { onAction(VmAction.Resume(vm)) },
                    )
                    ActionButton(
                        text = "强制停止",
                        icon = Icons.Filled.PowerSettingsNew,
                        tint = obsidian.Red,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.ForceStop,
                        onClick = { onAction(VmAction.ForceStop(vm)) },
                    )
                }
                VmStateEnum.SHUTDOWN, VmStateEnum.SHUTOFF, VmStateEnum.CRASHED, VmStateEnum.NOSTATE -> {
                    ActionButton(
                        text = "启动",
                        icon = Icons.Filled.PlayArrow,
                        tint = obsidian.Green,
                        enabled = !busy,
                        busy = busyKind == VmActionKind.Start,
                        onClick = { onAction(VmAction.Start(vm)) },
                    )
                }
            }
        }
    }
}

private fun VmStateEnum.toUi(): Pair<String, Color> = when (this) {
    VmStateEnum.RUNNING, VmStateEnum.IDLE -> "运行中" to StatusColors.Running
    VmStateEnum.PAUSED, VmStateEnum.PMSUSPENDED -> "已暂停" to StatusColors.Paused
    VmStateEnum.SHUTDOWN, VmStateEnum.SHUTOFF -> "已停止" to StatusColors.Stopped
    VmStateEnum.CRASHED -> "已崩溃" to StatusColors.Alert
    VmStateEnum.NOSTATE -> "无状态" to StatusColors.Stopped
}
