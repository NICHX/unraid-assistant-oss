package com.nichx.unraidassistant.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.CleartextWhitelistDataStore
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.datastore.ThemeMode
import com.nichx.unraidassistant.core.network.CleartextPolicy
import com.nichx.unraidassistant.core.network.CleartextWhitelist
import com.nichx.unraidassistant.core.updater.UpdateCheckState
import com.nichx.unraidassistant.core.updater.UpdateChecker
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.session.NotificationCenter
import com.nichx.unraidassistant.session.NotificationChannelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 系统设置页：主题模式与轮询间隔为全局设置（DataStore 持久化），
 * 服务器数量用于展示"服务器管理"入口的次级文案，明文白名单供用户管理 HTTP 放行规则。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val cleartextWhitelistDataStore: CleartextWhitelistDataStore,
    private val notificationCenter: NotificationCenter,
    private val updateChecker: UpdateChecker,
    serverRepository: ServerRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val pollIntervalSeconds: StateFlow<Int> = settingsDataStore.pollIntervalSeconds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 30,
    )

    val serverCount: StateFlow<Int> = serverRepository.observeServers()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /** 订阅通道连接状态，供设置页展示实时通道健康度。 */
    val channelState: StateFlow<NotificationChannelState> = notificationCenter.channelState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationChannelState.IDLE,
    )

    /** 最近一次订阅失败原因，供设置页诊断展示。 */
    val channelError: StateFlow<String?> = notificationCenter.lastError.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** 更新检查状态（与启动自动检查共用同一单例状态流，发现新版本时根组件统一弹窗）。 */
    val updateState: StateFlow<UpdateCheckState> = updateChecker.state

    /** 手动检查版本更新：与 [UpdateChecker.check] 的并发保护配合，避免与启动检查重复请求。 */
    fun checkForUpdates() {
        viewModelScope.launch { updateChecker.check() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setPollIntervalSeconds(seconds: Int) {
        viewModelScope.launch { settingsDataStore.setPollIntervalSeconds(seconds) }
    }

    /** 系统通知总开关。 */
    val notificationsEnabled: StateFlow<Boolean> = settingsDataStore.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val dndEnabled: StateFlow<Boolean> = settingsDataStore.dndEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val dndStartMinutes: StateFlow<Int> = settingsDataStore.dndStartMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsDataStore.DEFAULT_DND_START_MINUTES,
    )

    val dndEndMinutes: StateFlow<Int> = settingsDataStore.dndEndMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsDataStore.DEFAULT_DND_END_MINUTES,
    )

    /** 系统通知总开关。 */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotificationsEnabled(enabled) }
    }

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDndEnabled(enabled) }
    }

    fun setDndStartMinutes(minutes: Int) {
        viewModelScope.launch { settingsDataStore.setDndStartMinutes(minutes) }
    }

    fun setDndEndMinutes(minutes: Int) {
        viewModelScope.launch { settingsDataStore.setDndEndMinutes(minutes) }
    }

    val cleartextWhitelist: StateFlow<CleartextWhitelist> = cleartextWhitelistDataStore.whitelist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CleartextWhitelist(),
        )

    fun setCleartextInternalEnabled(enabled: Boolean) {
        viewModelScope.launch { cleartextWhitelistDataStore.setInternalEnabled(enabled) }
    }

    fun addCleartextRule(input: String): Boolean {
        val rule = CleartextPolicy.parseRule(input) ?: return false
        viewModelScope.launch { cleartextWhitelistDataStore.addRule(rule) }
        return true
    }

    fun removeCleartextRule(value: String) {
        viewModelScope.launch { cleartextWhitelistDataStore.removeRule(value) }
    }
}
