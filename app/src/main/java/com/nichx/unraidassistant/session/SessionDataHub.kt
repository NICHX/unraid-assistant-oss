package com.nichx.unraidassistant.session

import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.DashboardData
import com.nichx.unraidassistant.data.model.DockerData
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.data.model.VmData
import com.nichx.unraidassistant.data.repository.DashboardRepository
import com.nichx.unraidassistant.data.repository.DockerRepository
import com.nichx.unraidassistant.data.repository.StorageRepository
import com.nichx.unraidassistant.data.repository.VmRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 领域数据快照。data 为最近一次成功拉取的数据；error 为最近一次失败原因，
 * 二者可同时存在（已有旧数据时失败转为瞬时错误横幅）。
 */
data class DomainState<T>(
    val data: T? = null,
    val error: ApiException? = null,
    val isRefreshing: Boolean = false,
)

/**
 * 单个数据领域的统一轮询器。
 *
 * - 仅在有订阅者时运行：[stateIn] 使用 [SharingStarted.WhileSubscribed]，页面隐藏
 *   （tab 切走 / 盖住二级页 / app 退后台）约 5 秒后自动挂起，页面可见时自动恢复；
 * - 服务器切换时由 [flatMapLatest] 重启轮询并清空旧数据；同一服务器重新订阅则保留
 *   旧数据（stale-while-revalidate），避免切页时闪加载；
 * - [refresh]/[dismissError] 通过信号流唤醒等待中的循环，实现立即拉取与清除错误横幅。
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DomainPoller<T : Any>(
    private val scope: CoroutineScope,
    session: Flow<ServerConfig?>,
    private val intervalProvider: suspend () -> Long,
    private val fetch: suspend () -> T,
) {
    private enum class Signal { Refresh, Dismiss }

    private val mutable = MutableStateFlow(DomainState<T>())
    private val signals = MutableSharedFlow<Signal>(extraBufferCapacity = 1)

    /** 当前数据的归属服务器，用于区分"切服务器清空"与"同一服务器重订阅保留"。 */
    private var fetchedServerId: String? = null

    val state: StateFlow<DomainState<T>> = session
        .flatMapLatest { server ->
            if (server == null) {
                flow {
                    mutable.value = DomainState()
                    emit(mutable.value)
                    awaitCancellation()
                }
            } else {
                flow {
                    if (mutable.value.data != null && fetchedServerId == server.id) {
                        emit(mutable.value)
                    } else {
                        mutable.value = DomainState()
                        emit(mutable.value)
                    }
                    fetchedServerId = server.id
                    while (true) {
                        try {
                            mutable.value = DomainState(data = fetch())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: ApiException) {
                            mutable.value = mutable.value.copy(error = e)
                        } catch (e: Exception) {
                            mutable.value = mutable.value.copy(
                                error = ApiException.GraphQLError(listOf(e.message ?: "未知错误")),
                            )
                        }
                        emit(mutable.value)
                        when (withTimeoutOrNull(intervalProvider()) { signals.first() }) {
                            Signal.Refresh -> {
                                mutable.update { it.copy(isRefreshing = it.data != null, error = null) }
                                emit(mutable.value)
                            }
                            Signal.Dismiss -> {
                                mutable.update { it.copy(error = null) }
                                emit(mutable.value)
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), DomainState())

    /** 立即拉取一次（打断当前轮询等待）。 */
    fun refresh() {
        signals.tryEmit(Signal.Refresh)
    }

    /** 关闭瞬时错误横幅。 */
    fun dismissError() {
        signals.tryEmit(Signal.Dismiss)
    }
}

/**
 * 应用级统一数据中枢：负责各数据领域（首页 / 存储 / 容器 / 虚拟机）的轮询调度与共享缓存。
 *
 * 相比各页面各自发起请求：
 * - 轮询引擎收敛到单点，页面只消费 [DomainState] 快照，消除重复样板代码；
 * - 轮询随页面可见性启停（由 [DomainPoller] 的 WhileSubscribed 实现），
 *   不可见 tab 不再后台空转请求；
 * - 服务器切换时所有领域统一重启，避免切换瞬间多请求风暴。
 */
@Singleton
class SessionDataHub @Inject constructor(
    private val sessionManager: SessionManager,
    private val settingsDataStore: SettingsDataStore,
    private val dashboardRepository: DashboardRepository,
    private val storageRepository: StorageRepository,
    private val dockerRepository: DockerRepository,
    private val vmRepository: VmRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val session: Flow<ServerConfig?> = sessionManager.activeServer

    private val intervalProvider: suspend () -> Long = {
        settingsDataStore.pollIntervalSeconds.first() * 1000L
    }

    private val dashboard = DomainPoller(scope, session, intervalProvider, dashboardRepository::fetchDashboard)
    private val storage = DomainPoller(scope, session, intervalProvider, storageRepository::fetchStorage)
    private val docker = DomainPoller(scope, session, intervalProvider, dockerRepository::fetchDocker)
    private val vms = DomainPoller(scope, session, intervalProvider, vmRepository::fetchVms)

    val dashboardState: StateFlow<DomainState<DashboardData>> = dashboard.state
    val storageState: StateFlow<DomainState<StorageData>> = storage.state
    val dockerState: StateFlow<DomainState<DockerData>> = docker.state
    val vmsState: StateFlow<DomainState<VmData>> = vms.state

    fun refreshDashboard() = dashboard.refresh()
    fun refreshStorage() = storage.refresh()
    fun refreshDocker() = docker.refresh()
    fun refreshVms() = vms.refresh()

    fun dismissDashboardError() = dashboard.dismissError()
    fun dismissStorageError() = storage.dismissError()
    fun dismissDockerError() = docker.dismissError()
    fun dismissVmsError() = vms.dismissError()
}
