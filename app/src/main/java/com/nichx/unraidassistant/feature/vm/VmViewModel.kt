package com.nichx.unraidassistant.feature.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.ContentViewMode
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.VmData
import com.nichx.unraidassistant.data.repository.VmRepository
import com.nichx.unraidassistant.session.SessionDataHub
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface VmUiState {
    data object Loading : VmUiState
    data object NoServer : VmUiState
    data class Success(
        val data: VmData,
        val isRefreshing: Boolean = false,
        val transientError: String? = null,
        val busyVmId: String? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    ) : VmUiState
    data class Error(val message: String, val retry: () -> Unit) : VmUiState
}

/**
 * 虚拟机数据统一由 [SessionDataHub] 轮询提供，本 VM 负责状态映射与操作：
 * - 轮询随页面可见性自动启停，页面隐藏后不再后台请求；
 * - 操作（启动/停止等）成功后触发一次立即刷新；操作期间禁止重复点击。
 */
@HiltViewModel
class VmViewModel @Inject constructor(
    private val dataHub: SessionDataHub,
    private val vmRepository: VmRepository,
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private data class ActionState(
        val busyVmId: String? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    )

    private val _actionState = MutableStateFlow(ActionState())
    private val actionState = _actionState.asStateFlow()

    /** 虚拟机页视图偏好：网格/列表，跨应用重启持久化。 */
    val viewMode: StateFlow<ContentViewMode> = settingsDataStore.vmViewMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContentViewMode.GRID,
    )

    val uiState: StateFlow<VmUiState> = combine(
        dataHub.vmsState,
        sessionManager.activeServer,
        actionState,
    ) { state, server, action ->
        when {
            server == null -> VmUiState.NoServer
            state.data != null && state.error != null -> VmUiState.Success(
                data = state.data,
                isRefreshing = state.isRefreshing,
                transientError = state.error.message,
                busyVmId = action.busyVmId,
                actionError = action.actionError,
                actionMessage = action.actionMessage,
            )
            state.data != null -> VmUiState.Success(
                data = state.data,
                isRefreshing = state.isRefreshing,
                busyVmId = action.busyVmId,
                actionError = action.actionError,
                actionMessage = action.actionMessage,
            )
            state.error != null -> VmUiState.Error(state.error.message ?: "未知错误") { retry() }
            else -> VmUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VmUiState.Loading,
    )

    /** 从错误态重试：唤醒轮询立即拉取一次。 */
    fun retry() {
        dataHub.refreshVms()
    }

    /** 手动刷新：标记 refreshing 并立即拉取一次，不中断轮询节奏。 */
    fun refresh() {
        if (uiState.value !is VmUiState.Success) return
        dataHub.refreshVms()
    }

    fun dismissError() {
        dataHub.dismissVmsError()
    }

    fun dismissActionError() {
        _actionState.update { it.copy(actionError = null) }
    }

    fun dismissActionMessage() {
        _actionState.update { it.copy(actionMessage = null) }
    }

    /** 切换虚拟机页视图模式并持久化。 */
    fun setViewMode(mode: ContentViewMode) {
        viewModelScope.launch { settingsDataStore.setVmViewMode(mode) }
    }

    /**
     * 虚拟机 WebUI 编辑页深链：`/UpdateVM?uuid=<标准 UUID>`。
     *
     * unraid 7 GraphQL 的 `id` 是 PrefixedID，形如 `<64位hash>:<标准UUID>`；
     * 而 VMedit.php 的 domain_get_domain_by_uuid() 只认冒号后那段标准 libvirt UUID，
     * 直接拼 PrefixedID 会导致服务端找不到虚拟机、页面渲染不出编辑表单。
     */
    fun startVm(id: String) = runAction(id, "虚拟机已启动") { vmRepository.startVm(id) }
    fun stopVm(id: String) = runAction(id, "已发送关机请求") { vmRepository.stopVm(id) }
    fun forceStopVm(id: String) = runAction(id, "虚拟机已强制停止") { vmRepository.forceStopVm(id) }
    fun pauseVm(id: String) = runAction(id, "虚拟机已暂停") { vmRepository.pauseVm(id) }
    fun resumeVm(id: String) = runAction(id, "虚拟机已恢复") { vmRepository.resumeVm(id) }
    fun rebootVm(id: String) = runAction(id, "虚拟机已重启") { vmRepository.rebootVm(id) }

    private fun runAction(id: String, successMessage: String, block: suspend () -> Unit) {
        if (uiState.value !is VmUiState.Success) return
        _actionState.update { ActionState(busyVmId = id) }
        viewModelScope.launch {
            try {
                block()
                _actionState.update {
                    it.copy(
                        busyVmId = null,
                        actionMessage = successMessage,
                    )
                }
                dataHub.refreshVms()
            } catch (e: ApiException) {
                _actionState.update { ActionState(actionError = e.message ?: "操作失败") }
            }
        }
    }
}
