package com.nichx.unraidassistant.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.data.repository.StorageRepository
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

sealed interface StorageUiState {
    data object Loading : StorageUiState
    data object NoServer : StorageUiState
    data class Success(
        val data: StorageData,
        val isRefreshing: Boolean = false,
        val transientError: String? = null,
        val busyAction: StorageActionType? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    ) : StorageUiState
    data class Error(val message: String, val retry: () -> Unit) : StorageUiState
}

/** 存储页可执行的操作类型，用于操作期间禁用重复点击。 */
enum class StorageActionType {
    ARRAY_START,
    ARRAY_STOP,
    PARITY_START,
    PARITY_PAUSE,
    PARITY_RESUME,
    PARITY_CANCEL,
}

/**
 * 存储数据统一由 [SessionDataHub] 轮询提供，本 VM 负责状态映射与操作：
 * - 轮询随页面可见性自动启停，页面隐藏后不再后台请求；
 * - 已有数据时出错转为 [Success.transientError] 横幅提示，保留旧数据；
 * - 无数据时首次失败进入 [Error]，等待用户重试或下一轮自动恢复。
 * - 阵列启停 / 奇偶校验等控制操作成功后触发一次立即刷新，操作期间禁止重复点击。
 */
@HiltViewModel
class StorageViewModel @Inject constructor(
    private val dataHub: SessionDataHub,
    private val storageRepository: StorageRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private data class ActionState(
        val busyAction: StorageActionType? = null,
        val actionError: String? = null,
        val actionMessage: String? = null,
    )

    private val _actionState = MutableStateFlow(ActionState())
    private val actionState = _actionState.asStateFlow()

    val uiState: StateFlow<StorageUiState> = combine(
        dataHub.storageState,
        sessionManager.activeServer,
        actionState,
    ) { state, server, action ->
        when {
            server == null -> StorageUiState.NoServer
            state.data != null && state.error != null -> StorageUiState.Success(
                data = state.data,
                isRefreshing = state.isRefreshing,
                transientError = state.error.message,
                busyAction = action.busyAction,
                actionError = action.actionError,
                actionMessage = action.actionMessage,
            )
            state.data != null -> StorageUiState.Success(
                data = state.data,
                isRefreshing = state.isRefreshing,
                busyAction = action.busyAction,
                actionError = action.actionError,
                actionMessage = action.actionMessage,
            )
            state.error != null -> StorageUiState.Error(state.error.message ?: "未知错误") { retry() }
            else -> StorageUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StorageUiState.Loading,
    )

    /** 从错误态重试：唤醒轮询立即拉取一次。 */
    fun retry() {
        dataHub.refreshStorage()
    }

    /** 手动刷新：标记 refreshing 并立即拉取一次，不中断轮询节奏。 */
    fun refresh() {
        if (uiState.value !is StorageUiState.Success) return
        dataHub.refreshStorage()
    }

    fun dismissError() {
        dataHub.dismissStorageError()
    }

    fun dismissActionError() {
        _actionState.update { it.copy(actionError = null) }
    }

    fun dismissActionMessage() {
        _actionState.update { it.copy(actionMessage = null) }
    }

    fun startArray() = runAction(StorageActionType.ARRAY_START) { storageRepository.startArray() }
    fun stopArray() = runAction(StorageActionType.ARRAY_STOP) { storageRepository.stopArray() }
    fun startParityCheck() = runAction(StorageActionType.PARITY_START) {
        storageRepository.startParityCheck(correct = false)
    }
    fun pauseParityCheck() = runAction(StorageActionType.PARITY_PAUSE) {
        storageRepository.pauseParityCheck()
    }
    fun resumeParityCheck() = runAction(StorageActionType.PARITY_RESUME) {
        storageRepository.resumeParityCheck()
    }
    fun cancelParityCheck() = runAction(StorageActionType.PARITY_CANCEL) {
        storageRepository.cancelParityCheck()
    }

    private fun runAction(type: StorageActionType, block: suspend () -> Unit) {
        if (uiState.value !is StorageUiState.Success) return
        _actionState.update { ActionState(busyAction = type) }
        viewModelScope.launch {
            try {
                block()
                _actionState.update {
                    it.copy(
                        busyAction = null,
                        actionMessage = type.successMessage(),
                    )
                }
                dataHub.refreshStorage()
            } catch (e: ApiException) {
                _actionState.update { ActionState(actionError = e.message ?: "操作失败") }
            }
        }
    }

    private fun StorageActionType.successMessage(): String = when (this) {
        StorageActionType.ARRAY_START -> "阵列已启动"
        StorageActionType.ARRAY_STOP -> "阵列已停止"
        StorageActionType.PARITY_START -> "奇偶校验已开始"
        StorageActionType.PARITY_PAUSE -> "奇偶校验已暂停"
        StorageActionType.PARITY_RESUME -> "奇偶校验已恢复"
        StorageActionType.PARITY_CANCEL -> "奇偶校验已取消"
    }
}
