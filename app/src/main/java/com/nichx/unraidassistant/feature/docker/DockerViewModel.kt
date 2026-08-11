package com.nichx.unraidassistant.feature.docker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.ContentViewMode
import com.nichx.unraidassistant.core.datastore.MirrorSettingsDataStore
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.network.MirrorConfig
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.DockerData
import com.nichx.unraidassistant.data.model.DockerLogLine
import com.nichx.unraidassistant.data.repository.DockerRepository
import com.nichx.unraidassistant.session.SessionDataHub
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DockerUiState {
    data object Loading : DockerUiState
    data object NoServer : DockerUiState
    data object Empty : DockerUiState
    data class Success(
        val data: DockerData,
        val isRefreshing: Boolean = false,
        val transientError: String? = null,
        val busyContainerId: String? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    ) : DockerUiState
    data class Error(val message: String, val retry: () -> Unit) : DockerUiState
}

/** 容器日志查看器状态：lines 为已累积的行，streaming 表示正在轮询增量。 */
data class LogUiState(
    val containerId: String? = null,
    val lines: List<DockerLogLine> = emptyList(),
    val loading: Boolean = false,
    val streaming: Boolean = false,
    val error: String? = null,
)

/**
 * 容器数据统一由 [SessionDataHub] 轮询提供，本 VM 仅做状态映射：
 * - 轮询随页面可见性自动启停，页面隐藏后不再后台请求；
 * - 已有数据时出错转为 [Success.transientError] 横幅提示，保留旧数据；
 * - 无数据时首次失败进入 [Error]，等待用户重试或下一轮自动恢复；
 * - 容器为空时展示 [Empty]。
 * - 控制操作（启停/重启/暂停/更新）成功后触发一次立即刷新，操作期间禁止重复点击。
 */
@HiltViewModel
class DockerViewModel @Inject constructor(
    private val dataHub: SessionDataHub,
    private val dockerRepository: DockerRepository,
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
    private val mirrorSettingsDataStore: MirrorSettingsDataStore,
) : ViewModel() {

    private data class ActionState(
        val busyContainerId: String? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    )

    private val _actionState = MutableStateFlow(ActionState())
    private val actionState = _actionState.asStateFlow()

    private val _logState = MutableStateFlow(LogUiState())
    val logState: StateFlow<LogUiState> = _logState.asStateFlow()

    /** 容器页视图偏好：网格/列表，跨应用重启持久化。 */
    val viewMode: StateFlow<ContentViewMode> = settingsDataStore.dockerViewMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContentViewMode.GRID,
    )

    /** 镜像加速配置（容器图标加载 Coil 多源时读取）。 */
    val mirrorConfig: StateFlow<MirrorConfig> = mirrorSettingsDataStore.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MirrorConfig(),
    )

    private var logJob: Job? = null

    val uiState: StateFlow<DockerUiState> = combine(
        dataHub.dockerState,
        sessionManager.activeServer,
        actionState,
    ) { state, server, action ->
        when {
            server == null -> DockerUiState.NoServer
            state.data != null && state.error != null -> DockerUiState.Success(
                data = state.data,
                isRefreshing = state.isRefreshing,
                transientError = state.error.message,
                busyContainerId = action.busyContainerId,
                actionError = action.actionError,
                actionMessage = action.actionMessage,
            )
            state.data != null -> if (state.data.containers.isEmpty()) {
                DockerUiState.Empty
            } else {
                DockerUiState.Success(
                    data = state.data,
                    isRefreshing = state.isRefreshing,
                    busyContainerId = action.busyContainerId,
                    actionError = action.actionError,
                    actionMessage = action.actionMessage,
                )
            }
            state.error != null -> DockerUiState.Error(state.error.message ?: "未知错误") { retry() }
            else -> DockerUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DockerUiState.Loading,
    )

    /** 从错误态重试：唤醒轮询立即拉取一次。 */
    fun retry() {
        dataHub.refreshDocker()
    }

    /** 手动刷新：标记 refreshing 并立即拉取一次，不中断轮询节奏。 */
    fun refresh() {
        if (uiState.value !is DockerUiState.Success) return
        dataHub.refreshDocker()
    }

    fun dismissError() {
        dataHub.dismissDockerError()
    }

    fun dismissActionError() {
        _actionState.update { it.copy(actionError = null) }
    }

    fun dismissActionMessage() {
        _actionState.update { it.copy(actionMessage = null) }
    }

    /** 切换容器页视图模式并持久化。 */
    fun setViewMode(mode: ContentViewMode) {
        viewModelScope.launch { settingsDataStore.setDockerViewMode(mode) }
    }

    /** 打开日志查看器：重置缓存并启动 cursor 增量轮询。 */
    fun openLogs(containerId: String) {
        logJob?.cancel()
        _logState.value = LogUiState(containerId = containerId, loading = true)
        startLogStream()
    }

    /** 关闭日志查看器：取消轮询并清空缓存。 */
    fun closeLogs() {
        logJob?.cancel()
        logJob = null
        _logState.value = LogUiState()
    }

    /** 暂停/恢复日志轮询；暂停保留已拉取的行，恢复时基于既有行去重接入增量流。 */
    fun toggleLogStreaming() {
        val state = _logState.value
        if (state.containerId == null) return
        if (state.streaming) {
            logJob?.cancel()
            _logState.update { it.copy(streaming = false) }
        } else {
            logJob?.cancel()
            _logState.update { it.copy(loading = true, streaming = true, error = null) }
            startLogStream()
        }
    }

    /** 启动日志增量流；已打开过日志时基于既有最后一行时间戳去重。 */
    private fun startLogStream() {
        val containerId = _logState.value.containerId ?: return
        logJob = viewModelScope.launch {
            try {
                dockerRepository.observeContainerLogs(containerId)
                    .retryWhen { cause, attempt ->
                        val message = (cause as? ApiException)?.message ?: cause.message ?: "日志流中断"
                        _logState.update { it.copy(loading = false, error = message) }
                        if (attempt >= 2) {
                            _logState.update { it.copy(streaming = false) }
                            return@retryWhen false
                        }
                        delay(minOf(2_000L * (attempt + 1), 15_000L))
                        true
                    }
                    .collect { logs ->
                        _logState.update { state ->
                            val last = state.lines.lastOrNull()?.timestamp
                            val newLines = if (last == null) {
                                logs.lines
                            } else {
                                logs.lines.filter { it.timestamp > last }
                            }
                            state.copy(
                                lines = state.lines + newLines,
                                loading = false,
                                streaming = true,
                                error = null,
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // retryWhen 重试耗尽后重新抛出上游异常，此处兜底避免未捕获异常导致崩溃。
                _logState.update {
                    it.copy(loading = false, streaming = false, error = e.message ?: "日志流中断")
                }
            }
        }
    }

    fun startContainer(id: String) = runAction(id) { dockerRepository.startContainer(id) }
    fun stopContainer(id: String) = runAction(id) { dockerRepository.stopContainer(id) }
    fun restartContainer(id: String) = runAction(id) { dockerRepository.restartContainer(id) }
    fun pauseContainer(id: String) = runAction(id) { dockerRepository.pauseContainer(id) }
    fun unpauseContainer(id: String) = runAction(id) { dockerRepository.unpauseContainer(id) }
    fun updateContainer(id: String) = runAction(id) { dockerRepository.updateContainer(id) }

    /** 编辑容器自动启动设置（GraphQL 仅支持自动启动配置的修改）。 */
    fun updateAutoStart(id: String, autoStart: Boolean, wait: Int?, onResult: (Boolean) -> Unit) {
        if (uiState.value !is DockerUiState.Success) return
        _actionState.update { ActionState(busyContainerId = id) }
        viewModelScope.launch {
            try {
                dockerRepository.updateAutoStart(id, autoStart, wait)
                _actionState.update {
                    it.copy(
                        busyContainerId = null,
                        actionMessage = if (autoStart) "已开启「自动启动」" else "已关闭「自动启动」",
                    )
                }
                dataHub.refreshDocker()
                onResult(true)
            } catch (e: ApiException) {
                _actionState.update { ActionState(actionError = e.message ?: "操作失败") }
                onResult(false)
            }
        }
    }

    fun updateAllContainers() {
        if (uiState.value !is DockerUiState.Success) return
        _actionState.update { it.copy(busyContainerId = "__ALL__") }
        viewModelScope.launch {
            try {
                val count = dockerRepository.updateAllContainers()
                _actionState.update {
                    it.copy(
                        busyContainerId = null,
                        actionMessage = if (count > 0) "已更新 $count 个容器" else "所有容器已是最新",
                    )
                }
                dataHub.refreshDocker()
            } catch (e: ApiException) {
                _actionState.update { ActionState(actionError = e.message ?: "操作失败") }
            }
        }
    }

    private fun runAction(id: String, block: suspend () -> Unit) {
        if (uiState.value !is DockerUiState.Success) return
        _actionState.update { ActionState(busyContainerId = id) }
        viewModelScope.launch {
            try {
                block()
                _actionState.update { it.copy(busyContainerId = null) }
                dataHub.refreshDocker()
            } catch (e: ApiException) {
                _actionState.update { ActionState(actionError = e.message ?: "操作失败") }
            }
        }
    }
}
