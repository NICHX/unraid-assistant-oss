package com.nichx.unraidassistant.core.network

import com.nichx.unraidassistant.core.datastore.CleartextWhitelistDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 明文白名单内存快照。OkHttp 拦截器（网络线程）与 WebView 导航守卫（主线程）均需
 * 在非协程上下文中同步判定，因此以 [StateFlow] 常驻缓存，Eagerly 启动保证
 * [snapshot] 读到的始终是最新已持久化策略（初始值即默认白名单，行为安全）。
 */
@Singleton
class CleartextPolicyCache @Inject constructor(
    whitelistDataStore: CleartextWhitelistDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val snapshot: StateFlow<CleartextWhitelist> = whitelistDataStore.whitelist
        .stateIn(scope, SharingStarted.Eagerly, CleartextWhitelist())

    fun current(): CleartextWhitelist = snapshot.value

    fun isHostAllowed(host: String): Boolean = snapshot.value.allows(host)
}
