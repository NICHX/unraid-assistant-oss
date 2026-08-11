package com.nichx.unraidassistant.feature.docker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.core.datastore.ContentViewMode
import com.nichx.unraidassistant.core.network.MirrorConfig
import com.nichx.unraidassistant.core.util.Format
import com.nichx.unraidassistant.data.model.ContainerStateEnum
import com.nichx.unraidassistant.data.model.DockerContainerInfo
import com.nichx.unraidassistant.data.model.DockerLogLine
import com.nichx.unraidassistant.ui.components.ActionButton
import com.nichx.unraidassistant.ui.components.ConfirmDialog
import com.nichx.unraidassistant.ui.components.ContentViewToggle
import com.nichx.unraidassistant.ui.components.DetailDialog
import com.nichx.unraidassistant.ui.components.DetailRow
import com.nichx.unraidassistant.ui.components.EntityCard
import com.nichx.unraidassistant.ui.components.ErrorBanner
import com.nichx.unraidassistant.ui.components.ErrorBannerStack
import com.nichx.unraidassistant.ui.components.ErrorState
import com.nichx.unraidassistant.ui.components.FullPageState
import com.nichx.unraidassistant.ui.components.LoadingSkeleton
import com.nichx.unraidassistant.ui.components.MirrorAsyncImage
import com.nichx.unraidassistant.ui.components.NoServerState
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.RefreshAction
import com.nichx.unraidassistant.ui.components.StatusPill
import com.nichx.unraidassistant.ui.components.SummaryStatItem
import com.nichx.unraidassistant.ui.components.SummaryStatsCard
import com.nichx.unraidassistant.ui.navigation.Routes
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.StatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class ContainerFilter(val label: String) {
    ALL("全部"),
    RUNNING("运行中"),
    STOPPED("已停止"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreen(
    viewModel: DockerViewModel = hiltViewModel(),
    topBarContent: (@Composable () -> Unit)? = null,
    onOpenWebView: (String) -> Unit = {},
) {
    val obsidian = LocalObsidianPalette.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logState by viewModel.logState.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val mirrorConfig by viewModel.mirrorConfig.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(ContainerFilter.ALL) }
    var detailContainer by remember { mutableStateOf<DockerContainerInfo?>(null) }
    var logContainer by remember { mutableStateOf<DockerContainerInfo?>(null) }
    var pendingAction by remember { mutableStateOf<ContainerAction?>(null) }
    var pendingUpdateAll by remember { mutableStateOf(false) }
    var editContainer by remember { mutableStateOf<DockerContainerInfo?>(null) }
    var busyActionKind by remember { mutableStateOf<ContainerActionKind?>(null) }

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
                    is ContainerAction.Start -> viewModel.startContainer(action.container.id)
                    is ContainerAction.Stop -> viewModel.stopContainer(action.container.id)
                    is ContainerAction.Restart -> viewModel.restartContainer(action.container.id)
                    is ContainerAction.Pause -> viewModel.pauseContainer(action.container.id)
                    is ContainerAction.Resume -> viewModel.unpauseContainer(action.container.id)
                    is ContainerAction.Update -> viewModel.updateContainer(action.container.id)
                }
            },
            onDismiss = { pendingAction = null },
        )
    }

    if (pendingUpdateAll) {
        ConfirmDialog(
            title = "全部更新",
            message = "确定要更新所有存在新版本的容器到最新镜像吗？更新期间容器将逐个重启。",
            confirmText = "更新",
            danger = true,
            onConfirm = {
                pendingUpdateAll = false
                viewModel.updateAllContainers()
            },
            onDismiss = { pendingUpdateAll = false },
        )
    }

    val successState = uiState as? DockerUiState.Success

    LaunchedEffect(successState?.data?.containers) {
        val snapshot = detailContainer ?: return@LaunchedEffect
        successState?.data?.containers
            ?.firstOrNull { it.id == snapshot.id }
            ?.let { detailContainer = it }
    }

    LaunchedEffect(successState?.busyContainerId, detailContainer?.id) {
        if (successState?.busyContainerId != detailContainer?.id) {
            busyActionKind = null
        }
    }

    detailContainer?.let { container ->
        val isBusy = successState?.busyContainerId == container.id
        ContainerDetailDialog(
            container = container,
            mirrorConfig = mirrorConfig,
            busy = isBusy,
            busyKind = busyActionKind,
            onAction = { action -> pendingAction = action },
            onOpenLogs = {
                logContainer = container
                viewModel.openLogs(container.id)
            },
            onOpenWebView = onOpenWebView,
            onEditLocal = { editContainer = container },
            onDismiss = { detailContainer = null },
        )
    }

    editContainer?.let { container ->
        val isBusy = successState?.busyContainerId == container.id
        EditContainerDialog(
            container = container,
            busy = isBusy,
            onSave = { autoStart, wait ->
                viewModel.updateAutoStart(container.id, autoStart, wait) { success ->
                    if (success) {
                        editContainer = null
                        detailContainer = detailContainer?.copy(autoStart = autoStart, autoStartWait = wait)
                    }
                }
            },
            onDismiss = { editContainer = null },
        )
    }

    logContainer?.let { container ->
        LogViewerDialog(
            containerName = container.name,
            state = logState,
            onToggleStreaming = viewModel::toggleLogStreaming,
            onDismiss = {
                viewModel.closeLogs()
                logContainer = null
            },
        )
    }

    // 日志弹窗随 Tab 切换等场景离开组合时，主动关闭轮询避免后台空转。
    // 在 effect 创建时快照弹窗是否打开，避免 key 从 null 变为容器（刚打开弹窗）时
    // onDispose 读到最新值而误关刚启动的轮询。
    DisposableEffect(logContainer) {
        val wasOpen = logContainer != null
        onDispose {
            if (wasOpen) {
                viewModel.closeLogs()
            }
        }
    }

    ObsidianScreenScaffold(
        title = "Docker",
        titleContent = topBarContent,
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
            is DockerUiState.Loading -> LoadingSkeleton(
                Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
                is DockerUiState.NoServer -> NoServerState(Modifier.padding(padding))
                is DockerUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = state.retry,
                    modifier = Modifier.padding(padding),
                )
                is DockerUiState.Empty -> FullPageState(
                    icon = Icons.Filled.Inbox,
                    iconTint = obsidian.TextSecondary,
                    title = "暂无 Docker 容器",
                    subtitle = "服务器上未检测到容器",
                    modifier = Modifier.padding(padding),
                )
                is DockerUiState.Success -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                ) {
                    DockerContent(
                        state = state,
                        filter = filter,
                        viewMode = viewMode,
                        mirrorConfig = mirrorConfig,
                        onFilterChange = { filter = it },
                        onOpenDetail = { detailContainer = it },
                        onUpdateAll = { pendingUpdateAll = true },
                        onDismissTransient = viewModel::dismissError,
                        onDismissActionError = viewModel::dismissActionError,
                        onDismissActionMessage = viewModel::dismissActionMessage,
                    )
                }
            }
    }
}

/** 容器操作意图：需要二次确认的操作在此统一收敛。 */
private sealed interface ContainerAction {
    val container: DockerContainerInfo

    data class Start(override val container: DockerContainerInfo) : ContainerAction
    data class Stop(override val container: DockerContainerInfo) : ContainerAction
    data class Restart(override val container: DockerContainerInfo) : ContainerAction
    data class Pause(override val container: DockerContainerInfo) : ContainerAction
    data class Resume(override val container: DockerContainerInfo) : ContainerAction
    data class Update(override val container: DockerContainerInfo) : ContainerAction
}

/** 容器操作类型：用于标识详情弹窗中当前正在执行的操作，以在对应按钮内展示加载态。 */
private enum class ContainerActionKind { Start, Stop, Restart, Pause, Resume, Update }

private fun ContainerAction.kind(): ContainerActionKind = when (this) {
    is ContainerAction.Start -> ContainerActionKind.Start
    is ContainerAction.Stop -> ContainerActionKind.Stop
    is ContainerAction.Restart -> ContainerActionKind.Restart
    is ContainerAction.Pause -> ContainerActionKind.Pause
    is ContainerAction.Resume -> ContainerActionKind.Resume
    is ContainerAction.Update -> ContainerActionKind.Update
}

private fun ContainerAction.confirmTitle(): String = when (this) {
    is ContainerAction.Start -> "启动容器"
    is ContainerAction.Stop -> "停止容器"
    is ContainerAction.Restart -> "重启容器"
    is ContainerAction.Pause -> "暂停容器"
    is ContainerAction.Resume -> "恢复容器"
    is ContainerAction.Update -> "更新容器"
}

private fun ContainerAction.confirmMessage(): String = when (this) {
    is ContainerAction.Start -> "确定要启动「${container.name}」吗？"
    is ContainerAction.Stop -> "确定要停止「${container.name}」吗？容器将停止对外提供服务，此操作可能导致数据未及时落盘。"
    is ContainerAction.Restart -> "确定要重启「${container.name}」吗？短暂中断后自动恢复。"
    is ContainerAction.Pause -> "确定要暂停「${container.name}」吗？暂停后进程挂起但容器保留。"
    is ContainerAction.Resume -> "确定要恢复「${container.name}」吗？"
    is ContainerAction.Update -> "确定要更新「${container.name}」到最新镜像吗？更新后容器将使用新镜像重新创建。"
}

private fun ContainerAction.confirmText(): String = when (this) {
    is ContainerAction.Start -> "启动"
    is ContainerAction.Stop -> "停止"
    is ContainerAction.Restart -> "重启"
    is ContainerAction.Pause -> "暂停"
    is ContainerAction.Resume -> "恢复"
    is ContainerAction.Update -> "更新"
}

/** 停机/更新等影响服务可用性的操作用红色确认按钮强化警示。 */
private fun ContainerAction.danger(): Boolean = when (this) {
    is ContainerAction.Start -> false
    is ContainerAction.Stop -> true
    is ContainerAction.Restart -> false
    is ContainerAction.Pause -> false
    is ContainerAction.Resume -> false
    is ContainerAction.Update -> true
}

@Composable
private fun DockerContent(
    state: DockerUiState.Success,
    filter: ContainerFilter,
    viewMode: ContentViewMode,
    mirrorConfig: MirrorConfig,
    onFilterChange: (ContainerFilter) -> Unit,
    onOpenDetail: (DockerContainerInfo) -> Unit,
    onUpdateAll: () -> Unit,
    onDismissTransient: () -> Unit,
    onDismissActionError: () -> Unit,
    onDismissActionMessage: () -> Unit,
) {
    val containers = state.data.containers
    val filtered = when (filter) {
        ContainerFilter.ALL -> containers
        ContainerFilter.RUNNING -> containers.filter { it.state == ContainerStateEnum.RUNNING }
        ContainerFilter.STOPPED -> containers.filter { it.state != ContainerStateEnum.RUNNING }
    }
    val headerBlocks = dockerHeaderBlocks(
        state = state,
        filter = filter,
        onFilterChange = onFilterChange,
        onUpdateAll = onUpdateAll,
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
            items(filtered, key = { it.id }) { container ->
                val obsidian = LocalObsidianPalette.current
                val (stateText, stateColor) = container.state.toUi()
                EntityCard(
                    title = container.name,
                    subtitle = container.image,
                    statusText = stateText,
                    statusColor = stateColor,
                    isGrid = true,
                    onClick = { onOpenDetail(container) },
                    icon = { ContainerIcon(container.iconUrl, mirrorConfig) },
                    badge = if (container.isUpdateAvailable == true) {
                        {
                            Text(
                                text = "有更新",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = obsidian.Amber,
                            )
                        }
                    } else {
                        null
                    },
                )
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
            filtered.forEach { container ->
                item(key = container.id) {
                    val obsidian = LocalObsidianPalette.current
                    val (stateText, stateColor) = container.state.toUi()
                    EntityCard(
                        title = container.name,
                        subtitle = container.image,
                        statusText = stateText,
                        statusColor = stateColor,
                        isGrid = false,
                        onClick = { onOpenDetail(container) },
                        icon = { ContainerIcon(container.iconUrl, mirrorConfig) },
                        badge = if (container.isUpdateAvailable == true) {
                            {
                                Text(
                                    text = "有更新",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = obsidian.Amber,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** 顶部区块（错误横幅/汇总/过滤）在网格与列表两种布局间复用。 */
private fun dockerHeaderBlocks(
    state: DockerUiState.Success,
    filter: ContainerFilter,
    onFilterChange: (ContainerFilter) -> Unit,
    onUpdateAll: () -> Unit,
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
    add {
        DockerSummaryCard(
            containers = state.data.containers,
            busy = state.busyContainerId == "__ALL__",
            onUpdateAll = onUpdateAll,
        )
    }
    add {
        ContainerFilterRow(filter = filter, onFilterChange = onFilterChange)
    }
}

@Composable
private fun ContainerFilterRow(
    filter: ContainerFilter,
    onFilterChange: (ContainerFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ContainerFilter.entries.forEach { f ->
            FilterChip(
                selected = filter == f,
                onClick = { onFilterChange(f) },
                label = { Text(f.label) },
            )
        }
    }
}

@Composable
private fun DockerSummaryCard(
    containers: List<DockerContainerInfo>,
    busy: Boolean,
    onUpdateAll: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val running = containers.count { it.state == ContainerStateEnum.RUNNING }
    val updates = containers.count { it.isUpdateAvailable == true }
    SummaryStatsCard(
        stats = listOf(
            SummaryStatItem("运行中", "$running / ${containers.size}", obsidian.Green),
            SummaryStatItem("已停止", (containers.size - running).toString(), obsidian.TextSecondary),
            SummaryStatItem(
                label = "可更新",
                value = updates.toString(),
                color = if (updates > 0) obsidian.Amber else obsidian.TextSecondary,
            ),
        ),
        action = if (updates > 0) {
            {
                ActionButton(
                    text = "全部更新",
                    icon = Icons.Filled.SystemUpdateAlt,
                    tint = obsidian.Cyan,
                    enabled = !busy,
                    busy = busy,
                    onClick = onUpdateAll,
                )
            }
        } else {
            null
        },
    )
}

/** 容器详情弹窗：完整信息 + 二级操作（操作仍需 ConfirmDialog 二次确认）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContainerDetailDialog(
    container: DockerContainerInfo,
    mirrorConfig: MirrorConfig,
    busy: Boolean,
    busyKind: ContainerActionKind?,
    onAction: (ContainerAction) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenWebView: (String) -> Unit,
    onEditLocal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val uriHandler = LocalUriHandler.current
    val (stateText, stateColor) = container.state.toUi()
    DetailDialog(
        title = container.name,
        subtitle = "容器详情",
        onDismiss = onDismiss,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContainerIcon(container.iconUrl, mirrorConfig)
            Spacer(Modifier.size(Spacing.xl))
            Column {
                Text(
                    text = container.image,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = obsidian.TextPrimary,
                )
                Spacer(Modifier.size(Spacing.xs))
                StatusPill(stateText, stateColor)
            }
        }
        Spacer(Modifier.size(Spacing.xxl))

        DetailRow("状态", container.status.ifBlank { stateText })
        DetailRow("自动启动", if (container.autoStart) "是" else "否")
        container.lanIpPorts?.takeIf { it.isNotEmpty() }?.let { ports ->
            DetailRow("端口映射", ports.joinToString(", "))
        }
        container.sizeRootFs?.takeIf { it > 0L }?.let { size ->
            DetailRow("根文件系统", Format.bytes(size))
        }
        DetailRow("创建时间", Format.epochSeconds(container.created.toLong()))
        DetailRow("是否有更新", if (container.isUpdateAvailable == true) "是" else "否")

        Spacer(Modifier.size(Spacing.xl))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ActionButton(
                text = "查看日志",
                icon = Icons.AutoMirrored.Filled.Article,
                tint = obsidian.TextSecondary,
                onClick = onOpenLogs,
            )
            ActionButton(
                text = "自动启动",
                icon = Icons.Filled.Settings,
                tint = obsidian.TextSecondary,
                enabled = !busy,
                onClick = onEditLocal,
            )
            container.webUiUrl?.let { url ->
                ActionButton(
                    text = "打开 WebUI",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    tint = obsidian.Cyan,
                    onClick = { runCatching { uriHandler.openUri(url) } },
                )
                ActionButton(
                    text = "应用内打开",
                    icon = Icons.Filled.Language,
                    tint = obsidian.Cyan,
                    onClick = { onOpenWebView(url) },
                )
            }
        }

        Spacer(Modifier.size(Spacing.xxl))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (container.state) {
                ContainerStateEnum.RUNNING -> {
                    ActionButton(
                        text = "暂停",
                        icon = Icons.Filled.SmartButton,
                        tint = obsidian.TextSecondary,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Pause,
                        onClick = { onAction(ContainerAction.Pause(container)) },
                    )
                    ActionButton(
                        text = "重启",
                        icon = Icons.Filled.RestartAlt,
                        tint = obsidian.TextSecondary,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Restart,
                        onClick = { onAction(ContainerAction.Restart(container)) },
                    )
                    if (container.isUpdateAvailable == true) {
                        ActionButton(
                            text = "更新",
                            icon = Icons.Filled.SystemUpdateAlt,
                            tint = obsidian.Cyan,
                            enabled = !busy,
                            busy = busyKind == ContainerActionKind.Update,
                            onClick = { onAction(ContainerAction.Update(container)) },
                        )
                    }
                    ActionButton(
                        text = "停止",
                        icon = Icons.Filled.PowerSettingsNew,
                        tint = obsidian.Red,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Stop,
                        onClick = { onAction(ContainerAction.Stop(container)) },
                    )
                }
                ContainerStateEnum.PAUSED -> {
                    ActionButton(
                        text = "恢复",
                        icon = Icons.Filled.PlayArrow,
                        tint = obsidian.Green,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Resume,
                        onClick = { onAction(ContainerAction.Resume(container)) },
                    )
                    ActionButton(
                        text = "停止",
                        icon = Icons.Filled.PowerSettingsNew,
                        tint = obsidian.Red,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Stop,
                        onClick = { onAction(ContainerAction.Stop(container)) },
                    )
                }
                ContainerStateEnum.EXITED -> {
                    ActionButton(
                        text = "启动",
                        icon = Icons.Filled.PlayArrow,
                        tint = obsidian.Green,
                        enabled = !busy,
                        busy = busyKind == ContainerActionKind.Start,
                        onClick = { onAction(ContainerAction.Start(container)) },
                    )
                    if (container.isUpdateAvailable == true) {
                        ActionButton(
                            text = "更新",
                            icon = Icons.Filled.SystemUpdateAlt,
                            tint = obsidian.Cyan,
                            enabled = !busy,
                            busy = busyKind == ContainerActionKind.Update,
                            onClick = { onAction(ContainerAction.Update(container)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainerIcon(iconUrl: String?, mirrorConfig: MirrorConfig) {
    val obsidian = LocalObsidianPalette.current
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(obsidian.Glass, shape)
            .border(1.dp, obsidian.Border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (iconUrl != null) {
            MirrorAsyncImage(
                url = iconUrl,
                mirrorConfig = mirrorConfig,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent, shape),
                loading = { IconFallback() },
                error = { IconFallback() },
            )
        } else {
            IconFallback()
        }
    }
}

@Composable
private fun IconFallback() {
    val obsidian = LocalObsidianPalette.current
    Icon(
        imageVector = Icons.Filled.ViewInAr,
        contentDescription = null,
        tint = obsidian.TextSecondary,
        modifier = Modifier.size(22.dp),
    )
}

/** 容器自动启动设置弹窗：通过 updateAutostartConfiguration 修改自动启动开关与启动等待时间。 */
@Composable
private fun EditContainerDialog(
    container: DockerContainerInfo,
    busy: Boolean,
    onSave: (Boolean, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    var autoStart by remember { mutableStateOf(container.autoStart) }
    var waitText by remember { mutableStateOf((container.autoStartWait ?: 0).toString()) }
    DetailDialog(
        title = "自动启动设置",
        subtitle = container.name,
        onDismiss = onDismiss,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "自动启动",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = obsidian.TextPrimary,
                )
                Spacer(Modifier.size(Spacing.xxs))
                Text(
                    text = "Unraid 启动后自动运行该容器",
                    style = MaterialTheme.typography.bodySmall,
                    color = obsidian.TextSecondary,
                )
            }
            Switch(
                checked = autoStart,
                onCheckedChange = { autoStart = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = obsidian.Cyan,
                    checkedTrackColor = obsidian.Cyan.copy(alpha = 0.4f),
                    uncheckedThumbColor = obsidian.TextSecondary,
                    uncheckedTrackColor = obsidian.Glass,
                    uncheckedBorderColor = obsidian.Border,
                ),
            )
        }
        if (autoStart) {
            Spacer(Modifier.size(Spacing.xxl))
            OutlinedTextField(
                value = waitText,
                onValueChange = { input ->
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        waitText = input
                    }
                },
                label = { Text("启动等待（秒）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = obsidian.TextPrimary,
                    unfocusedTextColor = obsidian.TextPrimary,
                    focusedBorderColor = obsidian.Cyan,
                    unfocusedBorderColor = obsidian.Border,
                    focusedLabelColor = obsidian.Cyan,
                    unfocusedLabelColor = obsidian.TextSecondary,
                    cursorColor = obsidian.Cyan,
                ),
            )
            Spacer(Modifier.size(Spacing.xs))
            Text(
                text = "启动容器前等待的秒数（可选）",
                style = MaterialTheme.typography.labelSmall,
                color = obsidian.TextSecondary,
            )
        }
        Spacer(Modifier.size(Spacing.xxxl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                text = "保存",
                icon = Icons.Filled.Check,
                tint = obsidian.Cyan,
                enabled = !busy,
                busy = busy,
                onClick = {
                    onSave(
                        autoStart,
                        if (autoStart) waitText.toIntOrNull()?.takeIf { it >= 0 } else null,
                    )
                },
            )
        }
    }
}

/** 容器日志查看弹窗：终端风格等宽展示，增量追加，跟随到底部自动滚动。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerDialog(
    containerName: String,
    state: LogUiState,
    onToggleStreaming: () -> Unit,
    onDismiss: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val shape = MaterialTheme.shapes.extraLarge
    val listState = rememberLazyListState()
    val lines = state.lines
    var prevCount by remember { mutableStateOf(0) }

    // 新增日志行时，若用户停留在底部附近则自动滚到底；翻看历史时保持当前位置。
    LaunchedEffect(lines.size) {
        if (lines.isEmpty()) return@LaunchedEffect
        if (lines.size > prevCount) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (prevCount == 0 || lastVisible >= prevCount - 1) {
                listState.scrollToItem(lines.size - 1)
            }
        }
        prevCount = lines.size
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .background(obsidian.Background, shape)
                .border(1.dp, obsidian.Border, shape)
                .padding(Spacing.xxxl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "容器日志",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = obsidian.TextPrimary,
                    )
                    Spacer(Modifier.size(Spacing.xxs))
                    Text(
                        text = containerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    text = when {
                        state.loading -> "加载中"
                        state.streaming -> "实时"
                        else -> "已暂停"
                    },
                    color = when {
                        state.loading -> StatusColors.Paused
                        state.streaming -> StatusColors.Running
                        else -> StatusColors.Stopped
                    },
                )
                Spacer(Modifier.size(Spacing.md))
                IconButton(
                    onClick = onToggleStreaming,
                    enabled = !state.loading,
                ) {
                    Icon(
                        imageVector = if (state.streaming) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.streaming) "暂停刷新" else "恢复刷新",
                        tint = obsidian.TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = obsidian.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.xl))

            if (state.error != null) {
                ErrorBanner(
                    message = if (state.streaming) {
                        "日志流中断：${state.error}，自动重试中…"
                    } else {
                        "日志流中断：${state.error}，已停止重试，可点右上角恢复"
                    },
                    onDismiss = {},
                )
                Spacer(Modifier.size(Spacing.lg))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(obsidian.Glass, MaterialTheme.shapes.medium)
                    .border(1.dp, obsidian.Border, MaterialTheme.shapes.medium)
                    .padding(Spacing.lg),
            ) {
                when {
                    state.loading && lines.isEmpty() -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = obsidian.Cyan,
                    )
                    lines.isEmpty() -> Text(
                        text = "暂无日志输出",
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(lines.size) { index ->
                            LogLineItem(lines[index])
                        }
                    }
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Text(
                text = "最近 200 行 · 每 2 秒增量刷新",
                style = MaterialTheme.typography.labelSmall,
                color = obsidian.TextSecondary,
            )
        }
    }
}

@Composable
private fun LogLineItem(line: DockerLogLine) {
    val obsidian = LocalObsidianPalette.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = line.timestamp.toLogTime(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = obsidian.TextSecondary,
            modifier = Modifier.width(84.dp),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = line.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = obsidian.TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.size(Spacing.xxs))
}

/** RFC3339（UTC）时间戳 → 本地时区的 HH:mm:ss.SSS；解析失败回退到 'T' 之后的时间部分。 */
private fun String.toLogTime(): String = runCatching {
    Instant.parse(this)
        .atZone(ZoneId.systemDefault())
        .format(LOG_TIME_FORMAT)
}.getOrDefault(substringAfter('T', this).removeSuffix("Z"))

private val LOG_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private fun ContainerStateEnum.toUi(): Pair<String, Color> = when (this) {
    ContainerStateEnum.RUNNING -> "运行中" to StatusColors.Running
    ContainerStateEnum.PAUSED -> "已暂停" to StatusColors.Paused
    ContainerStateEnum.EXITED -> "已停止" to StatusColors.Stopped
}
