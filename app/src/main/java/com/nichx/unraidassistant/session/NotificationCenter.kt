package com.nichx.unraidassistant.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.notification.NotificationForwardService
import com.nichx.unraidassistant.core.notification.NotificationPoster
import com.nichx.unraidassistant.data.model.NotificationOverviewData
import com.nichx.unraidassistant.data.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** 订阅通道连接状态，供设置页/调试展示。 */
enum class NotificationChannelState { IDLE, CONNECTED, CONNECTING, RECONNECTING }

/**
 * 通知实时通道中枢：随会话启停，负责把 unRAID 的实时通知事件（WebSocket 订阅）
 * 转投为 Android 系统通知，并维护未读概览计数供 UI 消费。
 *
 * - 订阅生命周期挂在 [SessionManager.sessionScope] 下：服务器切换时随旧会话统一取消，
 *   新会话激活后自动重建订阅，避免数据串台；
 * - 指数退避自动重连：订阅流断开（WebSocket 掉线/服务器重启）后按 1s→2s→…→30s
 *   递增退避重试，连续失败 3 次后停止重连（避免后台无限空转），期间以轮询兜底刷新概览计数；
 * - 免打扰过滤与通知总开关在 [NotificationPoster] 内完成，本类不感知 UI 状态。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val notificationRepository: NotificationRepository,
    private val notificationPoster: NotificationPoster,
    private val settingsDataStore: SettingsDataStore,
) {
    private companion object {
        const val BACKOFF_MIN_MS = 1_000L
        const val BACKOFF_MAX_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _overview = MutableStateFlow(NotificationOverviewData())
    val overview: StateFlow<NotificationOverviewData> = _overview.asStateFlow()

    private val _channelState = MutableStateFlow(NotificationChannelState.IDLE)
    val channelState: StateFlow<NotificationChannelState> = _channelState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * 由操作 mutation（归档/删除等）的返回概览直接刷新本地计数。
     * 订阅推送可能滞后或通道断开，操作成功后以服务端返回为准即时校正。
     */
    fun updateOverview(data: NotificationOverviewData) {
        _overview.value = data
    }

    init {
        // 会话存在且通知总开关开启时才建立订阅通道
        // 任一条件变化都取消旧通道（coroutineScope 级联取消子协程），再按新状态重建。
        combine(
            sessionManager.sessionScope,
            settingsDataStore.notificationsEnabled,
        ) { sessionScope, enabled -> sessionScope to enabled }
            .flatMapLatest<Pair<CoroutineScope?, Boolean>, Unit> { (sessionScope, enabled) ->
                if (sessionScope == null || !enabled) {
                    flow {
                        _overview.value = NotificationOverviewData()
                        _channelState.value = NotificationChannelState.IDLE
                        _lastError.value = null
                        awaitCancellation()
                    }
                } else {
                    flow {
                        _channelState.value = NotificationChannelState.CONNECTING
                        _lastError.value = null
                        coroutineScope {
                            launch { collectAdded() }
                            launch { collectOverview() }
                        }
                    }
                }
            }
            .catch { }
            .launchIn(scope)

        // 通道活跃期间保持前台服务提升进程优先级，防止后台被杀中断订阅；
        // 回到 IDLE（开关关闭/无会话）时停止，不驻留。
        // StateFlow 语义下状态变化即发射，无需 distinctUntilChanged。
        _channelState
            .onEach { state ->
                if (state == NotificationChannelState.IDLE) {
                    context.stopService(Intent(context, NotificationForwardService::class.java))
                } else {
                    startForegroundServiceSafely()
                }
            }
            .launchIn(scope)
    }

    /** 启动前台保活服务；Android 12+ 后台启动限制等异常静默忽略（订阅不受影响）。 */
    private fun startForegroundServiceSafely() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NotificationForwardService::class.java),
            )
        }
    }

    /**
     * 实时通知订阅：收到事件即转投系统通知（含 DND 过滤）。
     * 流断开后按指数退避自动重连，连续失败 3 次后停止。
     */
    private suspend fun collectAdded() {
        var backoff = BACKOFF_MIN_MS
        var failures = 0
        while (true) {
            try {
                _channelState.value = NotificationChannelState.CONNECTED
                notificationRepository.observeNotificationAdded()
                    .collect { item -> notificationPoster.post(item) }
                // 流正常完成也视为断开（服务端关闭连接），继续重连
                backoff = BACKOFF_MIN_MS
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _channelState.value = NotificationChannelState.RECONNECTING
                _lastError.value = e.message ?: e::class.simpleName
                if (++failures >= 3) break
            }
            delay(backoff)
            backoff = min(backoff * 2, BACKOFF_MAX_MS)
        }
    }

    /**
     * 概览计数订阅：优先走 WebSocket 实时通道；
     * 断开期间降级为轮询查询，连续失败 3 次后停止重连。
     */
    private suspend fun collectOverview() {
        var backoff = BACKOFF_MIN_MS
        var failures = 0
        while (true) {
            try {
                notificationRepository.observeNotificationsOverview()
                    .collect { _overview.value = it }
                backoff = BACKOFF_MIN_MS
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 降级：以轮询间隔兜底刷新一次，再继续退避重连
                runCatching { notificationRepository.fetchNotificationsOverview() }
                    .onSuccess { _overview.value = it }
                if (++failures >= 3) break
                delay(backoff)
                backoff = min(backoff * 2, BACKOFF_MAX_MS)
                continue
            }
            delay(backoff)
            backoff = min(backoff * 2, BACKOFF_MAX_MS)
        }
    }
}
