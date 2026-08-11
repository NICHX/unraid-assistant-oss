package com.nichx.unraidassistant.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.model.ServerProtocol
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddServerForm(
    val name: String = "",
    val protocol: ServerProtocol = ServerProtocol.HTTPS,
    val host: String = "",
    val port: String = "443",
    val apiKey: String = "",
    val insecureSkipVerify: Boolean = false,
)

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val formState = MutableStateFlow(AddServerForm())

    /** 编辑模式下的服务器 id；null 表示新增。 */
    private val editingId = MutableStateFlow<String?>(null)

    val isFormValid: StateFlow<Boolean> = formState.map { f ->
        f.name.isNotBlank() &&
            f.host.isNotBlank() &&
            f.port.toIntOrNull()?.let { it in 1..65535 } == true &&
            (f.apiKey.isNotBlank() || editingId.value != null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    fun updateName(value: String) = formState.update { it.copy(name = value) }

    fun updateProtocol(value: ServerProtocol) = formState.update {
        it.copy(protocol = value, port = if (value == ServerProtocol.HTTPS) "443" else "80")
    }

    fun updateHost(value: String) = formState.update { it.copy(host = value) }

    fun updatePort(value: String) = formState.update {
        it.copy(port = value.filter { c -> c.isDigit() }.take(5))
    }

    fun updateApiKey(value: String) = formState.update { it.copy(apiKey = value) }

    fun updateInsecureSkipVerify(enabled: Boolean) = formState.update {
        it.copy(insecureSkipVerify = enabled)
    }

    /** 编辑模式：加载服务器配置预填表单（API Key 留空，不修改则保持原 Key）。 */
    fun load(serverId: String) {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            editingId.value = server.id
            formState.value = AddServerForm(
                name = server.name,
                protocol = server.protocol,
                host = server.host,
                port = server.port.toString(),
                insecureSkipVerify = server.insecureSkipVerify,
            )
        }
    }

    /**
     * 保存服务器配置与 API Key；新增时若尚无激活服务器则自动激活首个服务器。
     * 单服务器上限（硬限制）：已有服务器时拒绝新增第 2 台，经 [onBlocked] 提示。
     * 编辑模式下 API Key 留空表示不修改原 Key。
     */
    fun save(onSuccess: () -> Unit, onBlocked: (String) -> Unit = {}) {
        val form = formState.value
        val port = form.port.toIntOrNull() ?: return
        viewModelScope.launch {
            // 单服务器上限：仅允许保存首台服务器
            if (editingId.value == null && serverRepository.count() >= 1) {
                onBlocked("最多可管理 1 台服务器")
                return@launch
            }
            val server = ServerConfig(
                id = editingId.value ?: UUID.randomUUID().toString(),
                name = form.name.trim(),
                protocol = form.protocol,
                host = form.host.trim(),
                port = port,
                insecureSkipVerify = form.insecureSkipVerify,
            )
            serverRepository.save(server, form.apiKey.trim())
            if (editingId.value == null && sessionManager.activeServer.value == null) {
                sessionManager.switchTo(server)
                settingsDataStore.setLastServerId(server.id)
            }
            onSuccess()
        }
    }
}
