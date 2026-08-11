package com.nichx.unraidassistant.session

import com.nichx.unraidassistant.data.model.DashboardData
import com.nichx.unraidassistant.data.model.MetricSample
import com.nichx.unraidassistant.data.model.MetricsData
import com.nichx.unraidassistant.data.model.SamplePoint
import com.nichx.unraidassistant.data.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** 实时指标订阅通道状态。 */
enum class MetricsChannelState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

/**
 * Dashboard 实时曲线数据中枢：订阅 WebSocket 推送的 CPU / 内存 / 温度指标，
 * 维护最近一段时间的时间序列缓冲（切换服务器时清空），
 * 连接断开时以轮询查询降级补采样，指数退避自动重连。
 *
 * 订阅随 [metrics] 收集方启停（页面可见性驱动），离开 Dashboard 后自动关闭连接。
 */
@Singleton
class MetricsHub @Inject constructor(
    private val sessionManager: SessionManager,
    private val dashboardRepository: DashboardRepository,
) {
    companion object {
        const val MAX_SAMPLES_PER_SERIES = 900
        const val WINDOW_MS = 5 * 60 * 1000L
        const val BACKOFF_MIN_MS = 1_000L
        const val BACKOFF_MAX_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _metrics = MutableStateFlow(MetricsData())
    private val _channelState = MutableStateFlow(MetricsChannelState.IDLE)
    private var bufferServerId: String? = null

    /**
     * 实时指标序列。以激活服务器为生命周期：切换服务器立即清空缓冲，
     * 同一服务器重新订阅（页面回到前台）时保留已有历史并继续追加。
     */
    val metrics: StateFlow<MetricsData> = sessionManager.activeServer
        .flatMapLatest { server ->
            if (server == null) {
                flow {
                    bufferServerId = null
                    _metrics.value = MetricsData()
                    _channelState.value = MetricsChannelState.IDLE
                    emit(_metrics.value)
                }
            } else {
                flow {
                    if (bufferServerId != server.id) {
                        bufferServerId = server.id
                        _metrics.value = MetricsData()
                    }
                    _channelState.value = MetricsChannelState.CONNECTING
                    emit(_metrics.value)
                    collectSystemMetrics()
                }
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), MetricsData())

    /** 实时指标订阅通道状态（页面隐藏后保留最后状态，重新可见时立即置为连接中）。 */
    val channelState: StateFlow<MetricsChannelState> = _channelState.asStateFlow()

    /** 实时指标订阅通道收集：连续失败 3 次后停止自动重连，页面重新打开时随订阅重建恢复。 */
    private suspend fun FlowCollector<MetricsData>.collectSystemMetrics() {
        var backoff = BACKOFF_MIN_MS
        var failures = 0
        while (true) {
            try {
                _channelState.value = MetricsChannelState.CONNECTING
                // 握手完成后首个采样到达才置为已连接，避免“假连接”状态闪烁
                var connected = false
                dashboardRepository.observeSystemMetrics().collect { sample ->
                    if (!connected) {
                        connected = true
                        _channelState.value = MetricsChannelState.CONNECTED
                    }
                    _metrics.update { it.append(sample) }
                    emit(_metrics.value)
                }
                backoff = BACKOFF_MIN_MS
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _channelState.value = MetricsChannelState.RECONNECTING
                if (++failures >= 3) break
                // 断开期间降级轮询补采样，曲线不因断流而冻结
                runCatching { dashboardRepository.fetchDashboard() }
                    .onSuccess { dash ->
                        _metrics.update { it.append(dash) }
                        emit(_metrics.value)
                    }
            }
            delay(backoff)
            backoff = min(backoff * 2, BACKOFF_MAX_MS)
        }
    }

    private fun MetricsData.append(sample: MetricSample): MetricsData {
        var next = this
        sample.cpuPercent?.let { next = next.copy(cpu = next.cpu.appendPoint(sample.timestamp, it)) }
        sample.memPercent?.let { next = next.copy(memory = next.memory.appendPoint(sample.timestamp, it)) }
        sample.tempCelsius?.let { next = next.copy(temperature = next.temperature.appendPoint(sample.timestamp, it)) }
        return next
    }

    private fun MetricsData.append(dashboard: DashboardData): MetricsData {
        val now = System.currentTimeMillis()
        var next = this
        dashboard.cpuPercent?.let { next = next.copy(cpu = next.cpu.appendPoint(now, it)) }
        dashboard.memPercent?.let { next = next.copy(memory = next.memory.appendPoint(now, it)) }
        dashboard.tempAverage?.let { next = next.copy(temperature = next.temperature.appendPoint(now, it)) }
        return next
    }

    private fun List<SamplePoint>.appendPoint(timestamp: Long, value: Double): List<SamplePoint> {
        val windowStart = timestamp - WINDOW_MS
        val retained = if (isEmpty() || first().timestamp >= windowStart) {
            this
        } else {
            dropWhile { it.timestamp < windowStart }
        }
        return (retained + SamplePoint(timestamp, value)).takeLast(MAX_SAMPLES_PER_SERIES)
    }
}
