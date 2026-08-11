package com.nichx.unraidassistant.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.core.util.Format
import com.nichx.unraidassistant.data.model.ArrayStateEnum
import com.nichx.unraidassistant.data.model.DashboardData
import com.nichx.unraidassistant.data.model.MetricsData
import com.nichx.unraidassistant.data.model.NotificationOverviewData
import com.nichx.unraidassistant.data.model.ParityCheckStatusEnum
import com.nichx.unraidassistant.data.model.SamplePoint
import com.nichx.unraidassistant.data.model.ServerStatusEnum
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.data.model.TemperatureStatusEnum
import com.nichx.unraidassistant.session.MetricsChannelState
import com.nichx.unraidassistant.session.NotificationChannelState
import com.nichx.unraidassistant.ui.components.DetailDialog
import com.nichx.unraidassistant.ui.components.ErrorBanner
import com.nichx.unraidassistant.ui.components.ErrorState
import com.nichx.unraidassistant.ui.components.GlassCard
import com.nichx.unraidassistant.ui.components.GlowDot
import com.nichx.unraidassistant.ui.components.GlowPill
import com.nichx.unraidassistant.ui.components.GradientRingCard
import com.nichx.unraidassistant.ui.components.InfoRow
import com.nichx.unraidassistant.ui.components.NoServerState
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.ObsidianSnackbarHost
import com.nichx.unraidassistant.ui.components.RefreshAction
import com.nichx.unraidassistant.ui.components.RowOfTwo
import com.nichx.unraidassistant.ui.components.ServerSwitcherChip
import com.nichx.unraidassistant.ui.components.ServerSwitcherSheet
import com.nichx.unraidassistant.ui.components.SkeletonLine
import com.nichx.unraidassistant.ui.components.rememberSkeletonAlpha
import com.nichx.unraidassistant.ui.navigation.Routes
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.StatusColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onManageServers: () -> Unit,
    onOpenWebView: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val switchError by viewModel.switchError.collectAsStateWithLifecycle()
    val notificationOverview by viewModel.notificationOverview.collectAsStateWithLifecycle()
    val notificationChannelState by viewModel.notificationChannelState.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val metricsChannelState by viewModel.metricsChannelState.collectAsStateWithLifecycle()
    var showSwitcher by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(switchError) {
        if (switchError != null) {
            val result = snackbarHostState.showSnackbar(
                message = switchError!!,
                actionLabel = "去编辑",
            )
            if (result == SnackbarResult.ActionPerformed) {
                onManageServers()
            }
            viewModel.consumeSwitchError()
        }
    }

    ObsidianScreenScaffold(
        title = "Dashboard",
        snackbarHost = { ObsidianSnackbarHost(snackbarHostState) },
        actions = {
            ServerSwitcherChip(
                serverName = activeServer?.name ?: "选择服务器",
                onClick = { showSwitcher = true },
            )
            val server = activeServer
            if (server != null) {
                IconButton(onClick = { onOpenWebView(server.baseUrl) }) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = "打开 WebGUI",
                        tint = obsidian.TextSecondary,
                    )
                }
            }
            val success = uiState as? DashboardUiState.Success
            RefreshAction(
                isRefreshing = success?.isRefreshing == true,
                enabled = success != null,
                onClick = viewModel::refresh,
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> DashboardSkeleton(
                Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
                is DashboardUiState.NoServer -> NoServerState(
                    modifier = Modifier.padding(padding),
                    onManageServers = onManageServers,
                )
                is DashboardUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = state.retry,
                    modifier = Modifier.padding(padding),
                )
                is DashboardUiState.Success -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                ) {
                    DashboardContent(
                        data = state.data,
                        storage = state.storage,
                        metrics = metrics,
                        metricsChannelState = metricsChannelState,
                        transientError = state.transientError,
                        onDismissError = viewModel::dismissError,
                        onOpenNotifications = onOpenNotifications,
                        // 通道已连接或重连中时实时概览可信，否则回退到轮询数据
                        liveNotificationOverview =
                            if (notificationChannelState == NotificationChannelState.CONNECTED ||
                                notificationChannelState == NotificationChannelState.RECONNECTING
                            ) {
                                notificationOverview
                            } else {
                                null
                            },
                    )
                }
            }
        }

    if (showSwitcher) {
        ServerSwitcherSheet(
            servers = servers,
            activeServerId = activeServer?.id,
            onSelect = { server ->
                showSwitcher = false
                viewModel.activate(server)
            },
            onManageServers = {
                showSwitcher = false
                onManageServers()
            },
            onDismiss = { showSwitcher = false },
        )
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    storage: StorageData?,
    metrics: MetricsData,
    metricsChannelState: MetricsChannelState,
    transientError: String?,
    onDismissError: () -> Unit,
    onOpenNotifications: () -> Unit,
    liveNotificationOverview: NotificationOverviewData?,
) {
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
        if (transientError != null) {
            item {
                ErrorBanner(
                    message = "连接失败：$transientError，正在自动重试…",
                    onDismiss = onDismissError,
                )
            }
        }
        item { ServerHeroCard(data) }
        item {
            RowOfTwo(
                first = { modifier -> CpuRing(data, metrics, metricsChannelState, modifier) },
                second = { modifier -> MemoryRing(data, metrics, metricsChannelState, modifier) },
            )
        }
        item {
            RowOfTwo(
                first = { modifier -> TemperatureRing(data, metrics, metricsChannelState, modifier) },
                second = { modifier ->
                    if (storage != null) {
                        StorageRing(storage, modifier)
                    } else {
                        // 存储数据暂不可用（查询失败/服务器未支持）时保留半格占位
                        Spacer(modifier)
                    }
                },
            )
        }
        item { NotificationCard(data, liveNotificationOverview, onOpenNotifications) }
    }
}

/**
 * 首页骨架屏：与 DashboardContent 真实布局同构（Hero 卡 → 两行环卡 → 存储概览卡），
 * 尺寸/间距与真实卡片一致，避免加载完成后的布局跳变。
 */
@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        HeroSkeleton(alpha)
        RowOfTwo(
            first = { m -> RingSkeleton(alpha, m) },
            second = { m -> RingSkeleton(alpha, m) },
        )
        RowOfTwo(
            first = { m -> RingSkeleton(alpha, m) },
            second = { m -> RingSkeleton(alpha, m) },
        )
        NotificationSkeleton(alpha)
    }
}

@Composable
private fun HeroSkeleton(alpha: Float) {
    val color = LocalObsidianPalette.current.TextPrimary.copy(alpha = alpha * 0.30f)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.size(Spacing.xl))
            Column(Modifier.weight(1f)) {
                SkeletonLine(color, widthFraction = 0.4f, height = 18.dp)
                Spacer(Modifier.size(Spacing.sm))
                SkeletonLine(color, widthFraction = 0.55f, height = 12.dp)
            }
            Spacer(Modifier.size(Spacing.md))
            SkeletonLine(color, widthFraction = 0.12f, height = 20.dp)
        }
        Spacer(Modifier.size(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            SkeletonLine(color, widthFraction = 0.15f, height = 14.dp)
        }
    }
}

@Composable
private fun RingSkeleton(alpha: Float, modifier: Modifier = Modifier) {
    val color = LocalObsidianPalette.current.TextPrimary.copy(alpha = alpha * 0.30f)
    GlassCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SkeletonLine(color, widthFraction = 0.3f, height = 14.dp)
            Spacer(Modifier.size(Spacing.xl))
            Box(
                Modifier
                    .size(96.dp)
                    .border(9.dp, color.copy(alpha = 0.5f), CircleShape),
            )
            Spacer(Modifier.size(Spacing.xl))
            SkeletonLine(color, widthFraction = 0.75f, height = 12.dp)
            Spacer(Modifier.size(Spacing.md))
            SkeletonLine(color, widthFraction = 0.5f, height = 12.dp)
        }
    }
}

@Composable
private fun NotificationSkeleton(alpha: Float) {
    val color = LocalObsidianPalette.current.TextPrimary.copy(alpha = alpha * 0.30f)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(ObsidianCorner.skeletonBlock)
                    .background(color),
            )
            Spacer(Modifier.size(Spacing.lg))
            SkeletonLine(color, widthFraction = 0.15f, height = 16.dp)
            Spacer(Modifier.weight(1f))
            SkeletonLine(color, widthFraction = 0.2f, height = 20.dp)
        }
        Spacer(Modifier.size(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(3) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SkeletonLine(color, widthFraction = 0.4f, height = 20.dp)
                    Spacer(Modifier.size(Spacing.sm))
                    SkeletonLine(color, widthFraction = 0.5f, height = 12.dp)
                }
            }
        }
    }
}

/** Hero 卡片：服务器名 + 发光状态点 + 状态胶囊，点击展开低频服务器信息。 */
@Composable
private fun ServerHeroCard(data: DashboardData) {
    val obsidian = LocalObsidianPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val (statusText, statusColor) = when (data.serverStatus) {
        ServerStatusEnum.ONLINE -> "在线" to obsidian.Green
        ServerStatusEnum.OFFLINE -> "离线" to obsidian.Red
        ServerStatusEnum.NEVER_CONNECTED -> "未连接" to obsidian.TextSecondary
    }
    val subtitle = when {
        data.uptime != null -> "已运行 ${data.uptime}"
        !data.serverComment.isNullOrBlank() -> data.serverComment
        else -> ""
    }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowDot(color = statusColor, size = 14.dp)
            Spacer(Modifier.size(Spacing.xl))
            Column(Modifier.weight(1f)) {
                Text(
                    text = data.serverName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = obsidian.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.size(Spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = obsidian.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            GlowPill(text = statusText, color = statusColor)
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.size(Spacing.xxl))
                InfoRow("主机名", data.hostname ?: "—")
                InfoRow("局域网 IP", data.lanIp)
                InfoRow("外网 IP", data.wanIp)
                data.serverComment?.takeIf { it.isNotBlank() }?.let { InfoRow("备注", it) }
                data.unraidVersion?.let { InfoRow("Unraid 版本", it) }
                data.kernelVersion?.let { InfoRow("内核", it) }
                data.cpuBrand?.let { InfoRow("CPU", it) }
                data.arch?.let { InfoRow("架构", it) }
                val sysText = buildString {
                    data.systemManufacturer?.let { append(it) }
                    data.systemModel?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                    data.isVirtual?.let {
                        if (isNotEmpty()) append(" · ")
                        append(if (it) "虚拟机" else "物理机")
                    }
                }
                if (sysText.isNotEmpty()) InfoRow("硬件", sysText)
            }
        }
        Spacer(Modifier.size(Spacing.sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ObsidianCorner.listItem)
                .clickable { expanded = !expanded }
                .padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "收起详情" else "服务器详情",
                style = MaterialTheme.typography.labelMedium,
                color = obsidian.TextSecondary,
            )
            Spacer(Modifier.size(Spacing.xs))
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = obsidian.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CpuRing(
    data: DashboardData,
    metrics: MetricsData,
    channelState: MetricsChannelState,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    var showDetail by rememberSaveable { mutableStateOf(false) }
    // 实时订阅值优先，未接通时回退到轮询数据
    val livePercent = metrics.latestCpu
    val percent = (livePercent ?: data.cpuPercent ?: 0.0).toFloat()
    val sub = buildString {
        data.cpuCores?.let { append("$it 核") }
        data.cpuSpeedGhz?.let {
            if (isNotEmpty()) append(" · ")
            append("${it} GHz")
        }
        if (isEmpty()) append("—")
    }
    Box(
        modifier = modifier
            .clip(ObsidianCorner.card)
            .clickable { showDetail = true },
    ) {
        GradientRingCard(
            label = "CPU",
            percent = percent / 100f,
            valueText = Format.percentage(percent.toDouble(), 0),
            subText = sub,
            brush = Brush.linearGradient(listOf(obsidian.Cyan, obsidian.Indigo)),
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (showDetail) {
        DetailDialog(
            title = "CPU 详情",
            subtitle = sub,
            onDismiss = { showDetail = false },
        ) {
            MetricCurve(
                title = "CPU 使用率",
                points = metrics.cpu,
                color = obsidian.Cyan,
                axisFormatter = PERCENT_AXIS_FORMATTER,
                formatValue = { Format.percentage(it, 0) },
            )
            Spacer(Modifier.size(Spacing.md))
            MetricChannelStatus(channelState)
            Spacer(Modifier.size(Spacing.xl))
            HorizontalDivider(color = obsidian.Border)
            Spacer(Modifier.size(Spacing.xl))
            data.cpuBrand?.let { InfoRow("品牌", it) }
            val cores = buildString {
                data.cpuCores?.let { append("$it 核") }
                data.cpuThreads?.let {
                    if (isNotEmpty()) append(" · ")
                    append("$it 线程")
                }
                if (isEmpty()) append("—")
            }
            InfoRow("核心", cores)
            data.cpuSpeedGhz?.let { InfoRow("主频", "${it} GHz") }
            InfoRow(
                label = "使用率",
                value = Format.percentage(percent.toDouble()),
                valueColor = if (percent >= 90f) obsidian.Red else null,
            )
        }
    }
}

@Composable
private fun MemoryRing(
    data: DashboardData,
    metrics: MetricsData,
    channelState: MetricsChannelState,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    var showDetail by rememberSaveable { mutableStateOf(false) }
    // 实时订阅值优先，未接通时回退到轮询数据
    val livePercent = metrics.latestMemory
    val percent = (livePercent ?: data.memPercent ?: 0.0).toFloat()
    val used = data.memUsedBytes
    val total = data.memTotalBytes
    val sub = when {
        used != null && total != null -> "已用 ${Format.bytes(used)}\n共 ${Format.bytes(total)}"
        total != null -> "共 ${Format.bytes(total)}"
        else -> "—"
    }
    Box(
        modifier = modifier
            .clip(ObsidianCorner.card)
            .clickable { showDetail = true },
    ) {
        GradientRingCard(
            label = "内存",
            percent = percent / 100f,
            valueText = Format.percentage(percent.toDouble(), 0),
            subText = sub,
            brush = Brush.linearGradient(listOf(obsidian.Indigo, obsidian.Violet)),
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (showDetail) {
        DetailDialog(
            title = "内存详情",
            subtitle = sub,
            onDismiss = { showDetail = false },
        ) {
            MetricCurve(
                title = "内存使用率",
                points = metrics.memory,
                color = obsidian.Violet,
                maxY = 100.0,
                axisFormatter = PERCENT_AXIS_FORMATTER,
                formatValue = { Format.percentage(it, 0) },
            )
            Spacer(Modifier.size(Spacing.md))
            MetricChannelStatus(channelState)
            Spacer(Modifier.size(Spacing.xl))
            HorizontalDivider(color = obsidian.Border)
            Spacer(Modifier.size(Spacing.xl))
            total?.let { InfoRow("总容量", Format.bytes(it)) }
            used?.let { InfoRow("已用", Format.bytes(it), valueColor = if (percent >= 90f) obsidian.Red else null) }
            data.memFreeBytes?.let { InfoRow("空闲", Format.bytes(it)) }
            InfoRow(
                label = "使用率",
                value = Format.percentage(percent.toDouble()),
                valueColor = if (percent >= 90f) obsidian.Red else null,
            )
        }
    }
}

@Composable
private fun TemperatureRing(
    data: DashboardData,
    metrics: MetricsData,
    channelState: MetricsChannelState,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    var showDetail by rememberSaveable { mutableStateOf(false) }
    val status = data.hottestTempStatus
    val (statusText, statusColor, brush) = when (status) {
        TemperatureStatusEnum.NORMAL -> Triple(
            "正常", obsidian.Green,
            Brush.linearGradient(listOf(obsidian.Green, obsidian.Cyan)),
        )
        TemperatureStatusEnum.WARNING -> Triple(
            "警告", obsidian.Amber,
            Brush.linearGradient(listOf(obsidian.Amber, obsidian.Orange)),
        )
        TemperatureStatusEnum.CRITICAL -> Triple(
            "危险", obsidian.Red,
            Brush.linearGradient(listOf(obsidian.Red, obsidian.Orange)),
        )
        TemperatureStatusEnum.UNKNOWN, null -> Triple(
            "未知", obsidian.TextSecondary,
            Brush.linearGradient(listOf(obsidian.TextSecondary, obsidian.TextPrimary)),
        )
    }
    // 实时订阅值优先，未接通时回退到轮询数据
    val liveTemp = metrics.latestTemperature
    val temp = liveTemp ?: data.tempAverage
    val sub = buildString {
        append(statusText)
        data.hottestSensorName?.let {
            append(" · ")
            append(it)
            data.hottestTempValue?.let { v -> append(" ${Format.temperature(v)}") }
        }
    }
    Box(
        modifier = modifier
            .clip(ObsidianCorner.card)
            .clickable { showDetail = true },
    ) {
        GradientRingCard(
            label = "温度",
            percent = ((temp ?: 0.0) / 90.0).toFloat(),
            valueText = temp?.let { Format.temperature(it) } ?: "—",
            subText = sub,
            brush = brush,
            valueColor = statusColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (showDetail) {
        DetailDialog(
            title = "温度详情",
            subtitle = sub,
            onDismiss = { showDetail = false },
        ) {
            MetricCurve(
                title = "温度曲线",
                points = metrics.temperature,
                color = obsidian.Amber,
                maxY = 90.0,
                axisFormatter = TEMPERATURE_AXIS_FORMATTER,
                formatValue = { Format.temperature(it) },
            )
            Spacer(Modifier.size(Spacing.md))
            MetricChannelStatus(channelState)
            Spacer(Modifier.size(Spacing.xl))
            HorizontalDivider(color = obsidian.Border)
            Spacer(Modifier.size(Spacing.xl))
            data.tempAverage?.let { InfoRow("平均温度", Format.temperature(it)) }
            data.hottestSensorName?.let { InfoRow("最热传感器", it) }
            data.hottestTempValue?.let { v ->
                InfoRow("最热温度", Format.temperature(v), valueColor = statusColor)
            }
            data.tempWarningCount?.let { c ->
                InfoRow("警告传感器", c.toString(), valueColor = if (c > 0) obsidian.Amber else null)
            }
            data.tempCriticalCount?.let { c ->
                InfoRow("危险传感器", c.toString(), valueColor = if (c > 0) obsidian.Red else null)
            }
        }
    }
}

/**
 * 通知统计卡：通知是离散计数而非连续量，不适合进度环语义，
 * 改为全宽统计卡——标题行 + 信息/警告/告警三计数，点击直达通知列表页。
 */
@Composable
private fun NotificationCard(
    data: DashboardData,
    liveOverview: NotificationOverviewData?,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    // 实时订阅概览优先；未激活通道时回退到轮询查询的 DashboardData
    val unreadInfo = liveOverview?.unread?.info ?: data.unreadInfo
    val unreadWarning = liveOverview?.unread?.warning ?: data.unreadWarning
    val unreadAlert = liveOverview?.unread?.alert ?: data.unreadAlert
    val unreadTotal = liveOverview?.unread?.total ?: data.unreadTotal
    val accent = when {
        unreadAlert > 0 -> obsidian.Red
        unreadWarning > 0 -> obsidian.Amber
        else -> obsidian.Cyan
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenNotifications),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = obsidian.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Spacing.lg))
            Text(
                text = "通知",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = obsidian.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            GlowPill(text = "$unreadTotal 未读", color = accent)
        }
        Spacer(Modifier.size(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountStat("信息", unreadInfo, obsidian.Cyan)
            CountStat("警告", unreadWarning, obsidian.Amber)
            CountStat("告警", unreadAlert, obsidian.Red)
        }
    }
}

/** 通知统计卡内的单项计数：上方数值（按级别着色），下方标签。 */
@Composable
private fun CountStat(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val obsidian = LocalObsidianPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else obsidian.TextSecondary,
        )
        Spacer(Modifier.size(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = obsidian.TextSecondary,
        )
    }
}

/**
 * 存储阵列环卡：容量使用率是连续量，与温度/CPU 等指标环语义一致，
 * 环心显示使用率，副标显示阵列状态与已用/总量，点击展开磁盘与校验详情。
 */
@Composable
private fun StorageRing(data: StorageData, modifier: Modifier = Modifier) {
    val obsidian = LocalObsidianPalette.current
    var showDetail by rememberSaveable { mutableStateOf(false) }
    val totalKb = data.capacity?.totalKb?.toLongOrNull() ?: 0L
    val usedKb = data.capacity?.usedKb?.toLongOrNull() ?: 0L
    val freeKb = data.capacity?.freeKb?.toLongOrNull() ?: 0L
    val percent = if (totalKb > 0L) usedKb.toFloat() / totalKb else 0f
    val (stateText, stateColor) = data.arrayState.toUi()
    val (brush, valueColor) = when {
        percent >= 0.9f -> Brush.linearGradient(listOf(obsidian.Red, obsidian.Orange)) to obsidian.Red
        percent >= 0.75f -> Brush.linearGradient(listOf(obsidian.Amber, obsidian.Orange)) to obsidian.Amber
        else -> Brush.linearGradient(listOf(obsidian.Cyan, obsidian.Indigo)) to obsidian.Indigo
    }
    val sub = buildString {
        append(stateText)
        if (totalKb > 0L) {
            append(" · ")
            append(Format.bytes(usedKb * 1024))
            append(" / ")
            append(Format.bytes(totalKb * 1024))
        }
    }
    Box(
        modifier = modifier
            .clip(ObsidianCorner.card)
            .clickable { showDetail = true },
    ) {
        GradientRingCard(
            label = "存储",
            percent = percent,
            valueText = if (totalKb > 0L) {
                Format.percentage(usedKb.toDouble() / totalKb * 100, 0)
            } else {
                "—"
            },
            subText = sub,
            brush = brush,
            valueColor = valueColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (showDetail) {
        DetailDialog(
            title = "存储阵列详情",
            subtitle = stateText,
            onDismiss = { showDetail = false },
        ) {
            InfoRow("阵列状态", stateText, valueColor = stateColor)
            if (totalKb > 0L) {
                InfoRow(
                    label = "使用率",
                    value = Format.percentage(usedKb.toDouble() / totalKb * 100),
                    valueColor = if (percent >= 0.9f) obsidian.Red else null,
                )
                InfoRow(
                    label = "已用",
                    value = Format.bytes(usedKb * 1024),
                    valueColor = if (percent >= 0.9f) obsidian.Red else null,
                )
                InfoRow("空闲", Format.bytes(freeKb * 1024))
                InfoRow("总量", Format.bytes(totalKb * 1024))
            }
            InfoRow("磁盘数", "${data.dataDisks.size} 数据 / ${data.parities.size} Parity / ${data.cacheDisks.size} 缓存")
            val parityCheck = data.parityCheck
            val (parityText, parityColor) = when {
                data.parities.isEmpty() -> "未配置校验盘" to StatusColors.Alert
                parityCheck?.status == ParityCheckStatusEnum.RUNNING -> "校验中 · ${parityCheck?.progress ?: 0}%" to StatusColors.Running
                parityCheck?.status == ParityCheckStatusEnum.PAUSED -> "已暂停 · ${parityCheck?.progress ?: 0}%" to StatusColors.Paused
                parityCheck?.status == ParityCheckStatusEnum.FAILED -> "上次校验失败" to StatusColors.Alert
                parityCheck?.status == ParityCheckStatusEnum.COMPLETED -> "上次校验正常" to StatusColors.Running
                parityCheck?.status == ParityCheckStatusEnum.CANCELLED -> "上次校验已取消" to StatusColors.Stopped
                else -> "尚未校验" to StatusColors.Stopped
            }
            InfoRow("奇偶校验", parityText, valueColor = parityColor)
        }
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

/**
 * 弹窗内的单序列实时曲线：标题行 + 折线图（或空数据占位）。
 * 纵轴按指标独立缩放（CPU 动态、内存 0–100%、温度 0–90°C），横轴为最近 5 分钟。
 * [maxY] 传 null 时按窗口内数据峰值向上取整到 10 的倍数动态缩放，低占用时曲线不会被压扁在底部。
 * 弹窗内容可滚动，故禁用图表滚动手势避免滚动冲突。
 */
@Composable
private fun MetricCurve(
    title: String,
    points: List<SamplePoint>,
    color: Color,
    axisFormatter: CartesianValueFormatter,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    maxY: Double? = null,
) {
    val obsidian = LocalObsidianPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.size(Spacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = obsidian.TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = points.lastOrNull()?.value?.let(formatValue) ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = "最近 5 分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = obsidian.TextSecondary,
                )
            }
        }
        Spacer(Modifier.size(Spacing.md))
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(ObsidianCorner.skeletonBlock)
                    .background(obsidian.Border.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "等待实时数据…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = obsidian.TextSecondary,
                )
            }
        } else {
            val line = LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(color)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                areaFill = LineCartesianLayer.AreaFill.single(
                    Fill(Brush.verticalGradient(listOf(color.copy(alpha = 0.20f), Color.Transparent))),
                ),
                interpolator = LineCartesianLayer.Interpolator.cubic(0.4f),
            )
            val lines = remember(line) { listOf(line) }
            val modelProducer = remember { CartesianChartModelProducer() }
            val effectiveMaxY = remember(points, maxY) {
                if (maxY != null) {
                    maxY
                } else {
                    val peak = points.maxOfOrNull { it.value } ?: 0.0
                    max(DYNAMIC_MAX_Y_FLOOR, ceil(peak / 10.0) * 10.0)
                }
            }

            LaunchedEffect(points) {
                modelProducer.runTransaction {
                    lineModel {
                        series(
                            x = points.map { (it.timestamp / 1000L).toDouble() },
                            y = points.map { it.value },
                        )
                    }
                }
            }

            val xFormatter = remember {
                CartesianValueFormatter { _, value, _ ->
                    Instant.ofEpochSecond(value.toLong())
                        .atZone(ZoneId.systemDefault())
                        .format(X_AXIS_TIME_FORMAT)
                }
            }
            // 组件内部自带 remember，直接创建；颜色随主题变化重建
            val axisLabel = rememberAxisLabelComponent(
                style = TextStyle(color = obsidian.TextSecondary, fontSize = 10.sp),
            )
            val axisLine = rememberAxisLineComponent(fill = Fill(obsidian.Border))
            val chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lines),
                    rangeProvider = remember(effectiveMaxY) {
                        CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = effectiveMaxY)
                    },
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = axisLine,
                    label = axisLabel,
                    tick = null,
                    guideline = null,
                    valueFormatter = axisFormatter,
                    itemPlacer = remember(effectiveMaxY) {
                        VerticalAxis.ItemPlacer.step(step = { effectiveMaxY / 4 })
                    },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = axisLine,
                    label = axisLabel,
                    tick = null,
                    guideline = null,
                    valueFormatter = xFormatter,
                ),
                // 固定 60 秒一个刻度，避免毫秒时间戳导致标签爆炸
                getXStep = { _, _, _ -> 60.0 },
            )
            CartesianChartHost(
                chart = chart,
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                animationSpec = null,
                animateIn = false,
            )
        }
    }
}

/** 实时通道状态提示：弹窗内低调展示，连接中断时以小字提示不打断阅读。 */
@Composable
private fun MetricChannelStatus(state: MetricsChannelState) {
    val obsidian = LocalObsidianPalette.current
    val (text, color) = when (state) {
        MetricsChannelState.CONNECTED -> "实时推送中" to obsidian.Green
        MetricsChannelState.CONNECTING -> "正在连接实时通道…" to obsidian.Amber
        MetricsChannelState.RECONNECTING -> "实时通道中断，正在重连…" to obsidian.Orange
        MetricsChannelState.IDLE -> "实时通道未开启，展示最近数据" to obsidian.TextSecondary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlowDot(color = color, size = 6.dp)
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = obsidian.TextSecondary,
        )
    }
}

/** 曲线纵轴格式化：百分比（CPU / 内存共用）。 */
private val PERCENT_AXIS_FORMATTER = CartesianValueFormatter { _, value, _ ->
    "${value.roundToInt()}%"
}

/** 曲线纵轴格式化：摄氏度（温度）。 */
private val TEMPERATURE_AXIS_FORMATTER = CartesianValueFormatter { _, value, _ ->
    "${value.roundToInt()}°"
}

private val X_AXIS_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 动态纵轴的下限：CPU 空闲时占用很低，至少保留 0–20% 的显示幅度避免曲线贴底。 */
private const val DYNAMIC_MAX_Y_FLOOR = 20.0
