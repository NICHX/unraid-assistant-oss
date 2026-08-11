package com.nichx.unraidassistant.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.data.model.DashboardData
import com.nichx.unraidassistant.data.model.MetricsData
import com.nichx.unraidassistant.data.model.NotificationOverviewData
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.session.MetricsChannelState
import com.nichx.unraidassistant.session.MetricsHub
import com.nichx.unraidassistant.session.NotificationCenter
import com.nichx.unraidassistant.session.NotificationChannelState
import com.nichx.unraidassistant.session.SessionDataHub
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object NoServer : DashboardUiState
    data class Success(
        val data: DashboardData,
        val isRefreshing: Boolean = false,
        val transientError: String? = null,
        val storage: StorageData? = null,
    ) : DashboardUiState
    data class Error(val message: String, val retry: () -> Unit) : DashboardUiState
}

/**
 * Dashboard 数据统一由 [SessionDataHub] 轮询提供，本 VM 仅做状态映射与交互：
 * - 轮询随页面可见性自动启停，页面隐藏后不再后台请求；
 * - 已有数据时出错转为 [Success.transientError] 横幅提示，保留旧数据；
 * - 无数据时首次失败进入 [Error]，等待用户重试或下一轮自动恢复。
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dataHub: SessionDataHub,
    private val serverRepository: ServerRepository,
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
    private val notificationCenter: NotificationCenter,
    private val metricsHub: MetricsHub,
) : ViewModel() {

    /** 服务器切换失败提示（如缺少 API Key），一次性事件由 UI 消费后清空。 */
    private val _switchError = MutableStateFlow<String?>(null)
    val switchError = _switchError.asStateFlow()

    val servers: StateFlow<List<ServerConfig>> = serverRepository.observeServers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val activeServer: StateFlow<ServerConfig?> = sessionManager.activeServer

    /** 实时订阅通道的未读概览（WebSocket 推送驱动，比轮询查询更新更及时）。 */
    val notificationOverview: StateFlow<NotificationOverviewData> = notificationCenter.overview

    /** 订阅通道连接状态，用于判断实时概览数据是否可信。 */
    val notificationChannelState: StateFlow<NotificationChannelState> = notificationCenter.channelState

    /** 实时指标序列（Dashboard 实时曲线），随页面可见性自动启停订阅。 */
    val metrics: StateFlow<MetricsData> = metricsHub.metrics

    /** 实时指标订阅通道状态。 */
    val metricsChannelState: StateFlow<MetricsChannelState> = metricsHub.channelState

    val uiState: StateFlow<DashboardUiState> = combine(
        dataHub.dashboardState,
        dataHub.storageState,
        sessionManager.activeServer,
    ) { dash, store, server ->
        when {
            server == null -> DashboardUiState.NoServer
            dash.data != null && dash.error != null -> DashboardUiState.Success(
                data = dash.data,
                isRefreshing = dash.isRefreshing,
                transientError = dash.error.message,
                storage = store.data,
            )
            dash.data != null -> DashboardUiState.Success(
                data = dash.data,
                isRefreshing = dash.isRefreshing,
                storage = store.data,
            )
            dash.error != null -> DashboardUiState.Error(dash.error.message ?: "未知错误") { retry() }
            else -> DashboardUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading,
    )

    /** 从错误态重试：唤醒各领域轮询立即拉取一次。 */
    fun retry() {
        dataHub.refreshDashboard()
        dataHub.refreshStorage()
    }

    /** 手动刷新：标记 refreshing 并立即拉取一次，不中断轮询节奏。 */
    fun refresh() {
        if (uiState.value !is DashboardUiState.Success) return
        dataHub.refreshDashboard()
        dataHub.refreshStorage()
    }

    fun dismissError() {
        dataHub.dismissDashboardError()
    }

    /** 切换激活服务器（Dashboard 顶栏服务器选择器入口）。缺少 API Key 时置错误提示，引导用户补 Key。 */
    fun activate(server: ServerConfig) {
        viewModelScope.launch {
            if (sessionManager.switchTo(server)) {
                settingsDataStore.setLastServerId(server.id)
                _switchError.value = null
            } else {
                _switchError.value = "服务器「${server.name}」缺少 API Key，请在服务器管理中编辑补充"
            }
        }
    }

    fun consumeSwitchError() {
        _switchError.value = null
    }
}
