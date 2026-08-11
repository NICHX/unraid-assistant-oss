package com.nichx.unraidassistant.session

import com.apollographql.apollo.ApolloClient
import com.nichx.unraidassistant.core.datastore.CleartextWhitelistDataStore
import com.nichx.unraidassistant.core.network.ApolloClientFactory
import com.nichx.unraidassistant.core.network.CleartextPolicy
import com.nichx.unraidassistant.core.network.CleartextRuleType
import com.nichx.unraidassistant.core.security.ApiKeyStore
import com.nichx.unraidassistant.data.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多服务器会话管理。unRAID 助手的核心特性：同一时刻仅一个服务器处于"激活"状态，
 * 切换时取消旧会话的所有协程与订阅，并重建 ApolloClient。
 *
 * - [activeServer]：UI 与 ViewModel 据此感知切换并重新加载。
 * - [sessionScope]：会话级协程作用域，挂载所有 per-server 的订阅 Job，切换时统一取消，避免数据串台。
 * - API Key 仅在 [switchTo] 时从 [ApiKeyStore] 读入内存构建 Client，不作为字段常驻。
 */
@Singleton
class SessionManager @Inject constructor(
    private val apolloClientFactory: ApolloClientFactory,
    private val apiKeyStore: ApiKeyStore,
    private val cleartextWhitelistDataStore: CleartextWhitelistDataStore,
) {
    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer = _activeServer.asStateFlow()

    private val _sessionScope = MutableStateFlow<CoroutineScope?>(null)
    val sessionScope = _sessionScope.asStateFlow()

    private var apolloClient: ApolloClient? = null

    /**
     * 激活指定服务器会话。
     * 缺少 API Key 时返回 false（不切换、不抛异常、不破坏旧会话），由调用方引导用户补充 Key。
     *
     * Keystore 解密与 ApolloClient 构建是重活，统一在 IO 线程执行，
     * 避免冷启动恢复或页面切换时阻塞主线程造成卡顿。
     */
    suspend fun switchTo(server: ServerConfig): Boolean = withContext(Dispatchers.IO) {
        // 1. 先读入 API Key；缺失则不切换，避免破坏旧会话
        val apiKey = apiKeyStore.load(server.id)
        if (apiKey.isNullOrBlank()) return@withContext false
        // 2. 关闭旧会话
        _sessionScope.value?.cancel()
        apolloClient?.close()
        // 3. 构建新 Client 并激活新会话作用域
        apolloClient = apolloClientFactory.create(server, apiKey)
        _sessionScope.value = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        _activeServer.value = server
        autoWhitelistServerHost(server)
        true
    }

    /**
     * 将激活服务器的主机名自动加入明文白名单（HOST 规则，幂等写入），
     * 保证容器 WebUI（http://主机:端口）在应用内打开时不被明文拦截。
     */
    private suspend fun autoWhitelistServerHost(server: ServerConfig) {
        val rule = CleartextPolicy.parseRule(server.host) ?: return
        if (rule.type != CleartextRuleType.HOST) return
        cleartextWhitelistDataStore.addRule(rule)
    }

    fun apolloClient(): ApolloClient = apolloClient ?: error("无激活服务器")

    /** 注销当前会话（删除服务器或退出时调用）。 */
    fun clear() {
        _sessionScope.value?.cancel()
        apolloClient?.close()
        apolloClient = null
        _sessionScope.value = null
        _activeServer.value = null
    }
}
