package com.nichx.unraidassistant.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServerManagementUiState {
    data object Loading : ServerManagementUiState
    data object Empty : ServerManagementUiState
    data class Success(
        val servers: List<ServerConfig>,
        val activeServerId: String?,
    ) : ServerManagementUiState
}

/**
 * 服务器管理（二级页面）：查看/激活/删除服务器。
 * 激活时持久化 lastServerId，删除激活中的服务器会同步注销会话。
 * 单服务器限制：已有服务器时不允许新增（硬限制）。
 */
@HiltViewModel
class ServerManagementViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<ServerManagementUiState> = combine(
        serverRepository.observeServers(),
        sessionManager.activeServer,
    ) { servers, active ->
        if (servers.isEmpty()) {
            ServerManagementUiState.Empty
        } else {
            ServerManagementUiState.Success(servers, active?.id)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ServerManagementUiState.Loading,
    )

    /** 激活失败提示（如缺少 API Key），一次性事件由 UI 消费后清空。 */
    private val _switchError = MutableStateFlow<String?>(null)
    val switchError = _switchError.asStateFlow()

    /** 是否允许新增服务器：最多 1 台，已有服务器则不可新增。 */
    val canAddServer: StateFlow<Boolean> = serverRepository.observeServers()
        .map { servers -> servers.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    /** 激活服务器会话并持久化 lastServerId。缺少 API Key 时置错误提示。 */
    fun activate(server: ServerConfig) {
        viewModelScope.launch {
            if (sessionManager.switchTo(server)) {
                settingsDataStore.setLastServerId(server.id)
                _switchError.value = null
            } else {
                _switchError.value = "服务器「${server.name}」缺少 API Key，请编辑后重新激活"
            }
        }
    }

    fun consumeSwitchError() {
        _switchError.value = null
    }

    /** 删除服务器；若删除的是当前激活服务器，同步注销会话。 */
    fun delete(server: ServerConfig) {
        viewModelScope.launch {
            if (sessionManager.activeServer.value?.id == server.id) {
                sessionManager.clear()
                settingsDataStore.setLastServerId(null)
            }
            serverRepository.delete(server.id)
        }
    }
}
