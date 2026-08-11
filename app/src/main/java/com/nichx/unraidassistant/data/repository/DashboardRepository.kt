package com.nichx.unraidassistant.data.repository

import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.nichx.unraidassistant.core.network.CleartextBlockedException
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.DashboardData
import com.nichx.unraidassistant.data.model.MetricSample
import com.nichx.unraidassistant.data.model.ServerStatusEnum
import com.nichx.unraidassistant.data.model.TemperatureStatusEnum
import com.nichx.unraidassistant.data.remote.graphql.GetDashboardQuery
import com.nichx.unraidassistant.data.remote.graphql.SystemMetricsCpuSubscription
import com.nichx.unraidassistant.data.remote.graphql.SystemMetricsMemorySubscription
import com.nichx.unraidassistant.data.remote.graphql.SystemMetricsTemperatureSubscription
import com.nichx.unraidassistant.data.remote.graphql.type.ServerStatus
import com.nichx.unraidassistant.data.remote.graphql.type.TemperatureStatus as GqlTemperatureStatus
import com.nichx.unraidassistant.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

interface DashboardRepository {
    suspend fun fetchDashboard(): DashboardData

    /**
     * 实时订阅系统指标（CPU / 内存 / 温度，WebSocket）。
     * 三者是独立的单根字段订阅（GraphQL 规范要求订阅操作只能有一个根字段，
     * 多根字段会被服务端校验拒绝），在共享连接上并行合并，每次事件仅一个字段非空。
     * 流在连接断开时以异常完成，由调用方重连降级。
     */
    fun observeSystemMetrics(): Flow<MetricSample>
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : DashboardRepository {

    override suspend fun fetchDashboard(): DashboardData {
        val response = try {
            sessionManager.apolloClient().query(GetDashboardQuery()).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        if (response.hasErrors()) {
            throw ApiException.GraphQLError(response.errors?.map { it.message } ?: emptyList())
        }
        val data = response.data ?: throw ApiException.ServerError(500)
        val server = data.server
        val metrics = data.metrics
        val mem = metrics.memory
        val tempSummary = metrics.temperature?.summary
        val hottest = tempSummary?.hottest
        val unread = data.notifications.overview.unread
        val versions = data.info.versions.core

        return DashboardData(
            serverName = server?.name ?: "未知服务器",
            serverComment = server?.comment,
            serverStatus = server?.status.toDomain(ServerStatusEnum.OFFLINE),
            lanIp = server?.lanip ?: "—",
            wanIp = server?.wanip ?: "—",
            osVersion = data.vars.version,
            hostname = data.info.os.hostname,
            uptime = data.info.os.uptime,
            kernel = data.info.os.kernel,
            arch = data.info.os.arch,
            cpuBrand = data.info.cpu.brand,
            cpuCores = data.info.cpu.cores,
            cpuThreads = data.info.cpu.threads,
            cpuSpeedGhz = data.info.cpu.speed,
            systemManufacturer = data.info.system.manufacturer,
            systemModel = data.info.system.model,
            isVirtual = data.info.system.virtual,
            unraidVersion = versions.unraid,
            apiVersion = versions.api,
            kernelVersion = versions.kernel,
            cpuPercent = metrics.cpu?.percentTotal,
            memTotalBytes = mem?.total,
            memUsedBytes = mem?.used,
            memFreeBytes = mem?.free,
            memPercent = mem?.percentTotal,
            tempAverage = tempSummary?.average,
            hottestSensorName = hottest?.name,
            hottestTempValue = hottest?.current?.value,
            hottestTempStatus = hottest?.current?.status.toDomain(TemperatureStatusEnum.UNKNOWN),
            tempWarningCount = tempSummary?.warningCount,
            tempCriticalCount = tempSummary?.criticalCount,
            unreadInfo = unread.info,
            unreadWarning = unread.warning,
            unreadAlert = unread.alert,
            unreadTotal = unread.total,
        )
    }

    override fun observeSystemMetrics(): Flow<MetricSample> = merge(
        sessionManager.apolloClient()
            .subscription(SystemMetricsCpuSubscription())
            .toFlow()
            .map { response ->
                val data = response.data ?: throw response.exception.toApiException()
                MetricSample(
                    timestamp = System.currentTimeMillis(),
                    cpuPercent = data.systemMetricsCpu.percentTotal,
                )
            },
        sessionManager.apolloClient()
            .subscription(SystemMetricsMemorySubscription())
            .toFlow()
            .map { response ->
                val data = response.data ?: throw response.exception.toApiException()
                MetricSample(
                    timestamp = System.currentTimeMillis(),
                    memTotalBytes = data.systemMetricsMemory.total,
                    memUsedBytes = data.systemMetricsMemory.used,
                    memPercent = data.systemMetricsMemory.percentTotal,
                )
            },
        sessionManager.apolloClient()
            .subscription(SystemMetricsTemperatureSubscription())
            .toFlow()
            .map { response ->
                val data = response.data ?: throw response.exception.toApiException()
                MetricSample(
                    timestamp = System.currentTimeMillis(),
                    tempCelsius = data.systemMetricsTemperature?.summary?.average,
                )
            },
    )

    private inline fun <reified G : Enum<G>, reified D : Enum<D>> G?.toDomain(default: D): D =
        this?.let { runCatching { enumValueOf<D>(it.name) }.getOrDefault(default) } ?: default

    private fun ApolloException?.toApiException(): ApiException = when (this) {
        is ApolloHttpException -> when (statusCode) {
            401 -> ApiException.Unauthorized
            403 -> ApiException.Forbidden
            429 -> ApiException.RateLimited
            in 500..599 -> ApiException.ServerError(statusCode)
            else -> ApiException.ServerError(statusCode)
        }
        is ApolloNetworkException -> (this.cause as? CleartextBlockedException)
            ?.let { ApiException.CleartextBlocked(it.host) }
            ?: ApiException.NetworkUnreachable
        null -> ApiException.GraphQLError(listOf("订阅数据为空"))
        else -> ApiException.GraphQLError(listOf(message ?: "未知 GraphQL 错误"))
    }
}
