package com.nichx.unraidassistant.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.datastore.ThemeMode
import com.nichx.unraidassistant.core.updater.UpdateCheckState
import com.nichx.unraidassistant.core.updater.UpdateChecker
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.session.SessionManager
import com.nichx.unraidassistant.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 应用级 ViewModel：负责启动时的"会话 + 视图恢复"与主题模式下发。
 *
 * 启动流程：
 * 1. 读取 lastServerId，若服务器仍存在则自动激活（恢复上次会话）；
 * 2. 读取 lastUsedTab，决定 HomeContainer 初始 tab（恢复上次停留的 tab）；
 * 3. [isReady] 置位后 UI 才进入导航，避免先渲染默认页再跳变。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val serverRepository: ServerRepository,
    private val sessionManager: SessionManager,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM,
    )

    /** HomeContainer 初始 tab：优先恢复上次停留的底部 tab（NavHost startDestination 固定为 HOME）。 */
    val startTab: StateFlow<String> = settingsDataStore.lastUsedTab
        .map { route -> route?.takeIf { it in Routes.bottomBarRoutes } ?: Routes.DASHBOARD }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Routes.DASHBOARD,
        )

    private val restored = MutableStateFlow(false)

    val isReady: StateFlow<Boolean> = combine(restored, startTab) { r, _ -> r }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /** 更新检查状态（单例共享），供根组件发现新版本时统一弹窗。 */
    val updateState: StateFlow<UpdateCheckState> = updateChecker.state

    init {
        viewModelScope.launch {
            // 恢复流程（DataStore 读盘 / Room 查询 / Keystore 解密 / ApolloClient 构建）整体放到 IO，
            // 主线程立即释放，避免冷启动期间卡在 spinner 上。
            withContext(Dispatchers.IO) {
                val lastServerId = settingsDataStore.lastServerId.first()
                if (lastServerId != null) {
                    val server = serverRepository.getServer(lastServerId)
                    if (server != null && sessionManager.activeServer.value?.id != server.id) {
                        runCatching { sessionManager.switchTo(server) }
                    }
                }
            }
            restored.value = true
        }
        // 启动静默检查版本更新（发现新版本才弹窗提示，与手动检查共用同一状态流）
        viewModelScope.launch { updateChecker.check() }
    }

    /** 底部 tab 切换时持久化，供下次启动恢复。 */
    fun saveLastTab(route: String) {
        if (route in Routes.bottomBarRoutes) {
            viewModelScope.launch { settingsDataStore.setLastUsedTab(route) }
        }
    }
}
