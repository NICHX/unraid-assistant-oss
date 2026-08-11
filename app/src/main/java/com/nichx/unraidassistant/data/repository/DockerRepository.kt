package com.nichx.unraidassistant.data.repository

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.nichx.unraidassistant.core.network.CleartextBlockedException
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.ContainerStateEnum
import com.nichx.unraidassistant.data.model.DockerContainerInfo
import com.nichx.unraidassistant.data.model.DockerData
import com.nichx.unraidassistant.data.model.DockerLogLine
import com.nichx.unraidassistant.data.model.DockerLogsData
import com.nichx.unraidassistant.data.remote.graphql.GetContainerLogsQuery
import com.nichx.unraidassistant.data.remote.graphql.GetDockerQuery
import com.nichx.unraidassistant.data.remote.graphql.PauseContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.RestartContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.StartContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.StopContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.UnpauseContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.UpdateAllContainersMutation
import com.nichx.unraidassistant.data.remote.graphql.UpdateAutostartConfigurationMutation
import com.nichx.unraidassistant.data.remote.graphql.UpdateContainerMutation
import com.nichx.unraidassistant.data.remote.graphql.type.ContainerState
import com.nichx.unraidassistant.data.remote.graphql.type.DockerAutostartEntryInput
import com.nichx.unraidassistant.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DockerRepository {
    suspend fun fetchDocker(): DockerData
    suspend fun fetchContainerLogs(id: String, since: String?, tail: Int?): DockerLogsData
    fun observeContainerLogs(id: String, tail: Int = 200, pollIntervalMs: Long = 2_000): Flow<DockerLogsData>
    suspend fun startContainer(id: String)
    suspend fun stopContainer(id: String)
    suspend fun restartContainer(id: String)
    suspend fun pauseContainer(id: String)
    suspend fun unpauseContainer(id: String)
    suspend fun updateContainer(id: String)
    suspend fun updateAllContainers(): Int
    suspend fun updateAutoStart(id: String, autoStart: Boolean, wait: Int?)
}

@Singleton
class DockerRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : DockerRepository {

    override suspend fun fetchDocker(): DockerData {
        val response = try {
            sessionManager.apolloClient().query(GetDockerQuery()).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        if (response.hasErrors()) {
            throw ApiException.GraphQLError(response.errors?.map { it.message } ?: emptyList())
        }
        val data = response.data ?: throw ApiException.ServerError(500)
        return DockerData(
            containers = data.docker.containers.map { it.toInfo() },
        )
    }

    private fun GetDockerQuery.Container.toInfo() = DockerContainerInfo(
        id = id,
        name = names.firstOrNull() ?: "未命名",
        image = image,
        state = state.toDomain(ContainerStateEnum.EXITED),
        status = status,
        autoStart = autoStart,
        autoStartWait = autoStartWait,
        iconUrl = iconUrl,
        webUiUrl = webUiUrl,
        templatePath = templatePath,
        isUpdateAvailable = isUpdateAvailable,
        lanIpPorts = lanIpPorts,
        sizeRootFs = sizeRootFs,
        created = created,
    )

    override suspend fun fetchContainerLogs(id: String, since: String?, tail: Int?): DockerLogsData {
        val response = try {
            sessionManager.apolloClient()
                .query(GetContainerLogsQuery(id = id, since = Optional.presentIfNotNull(since), tail = Optional.presentIfNotNull(tail)))
                .execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
        val logs = response.data?.docker?.logs ?: throw ApiException.ServerError(500)
        return DockerLogsData(
            containerId = logs.containerId,
            lines = logs.lines.map { DockerLogLine(timestamp = it.timestamp, message = it.message) },
            cursor = logs.cursor,
        )
    }

    /**
     * 容器日志增量流：首次拉取 [tail] 行历史，随后将服务端返回的 cursor 原样传回
     * `since` 参数轮询增量日志；无游标（容器被移除等）或订阅取消时结束。
     */
    override fun observeContainerLogs(id: String, tail: Int, pollIntervalMs: Long): Flow<DockerLogsData> = flow {
        var cursor: String? = null
        while (true) {
            val logs = fetchContainerLogs(id, cursor, if (cursor == null) tail else null)
            emit(logs)
            cursor = logs.cursor ?: break
            delay(pollIntervalMs)
        }
    }

    private inline fun <reified G : Enum<G>, reified D : Enum<D>> G.toDomain(default: D): D =
        runCatching { enumValueOf<D>(name) }.getOrDefault(default)

    override suspend fun startContainer(id: String) {
        executeMutation(StartContainerMutation(id = id))
    }

    override suspend fun stopContainer(id: String) {
        executeMutation(StopContainerMutation(id = id))
    }

    override suspend fun restartContainer(id: String) {
        executeMutation(RestartContainerMutation(id = id))
    }

    override suspend fun pauseContainer(id: String) {
        executeMutation(PauseContainerMutation(id = id))
    }

    override suspend fun unpauseContainer(id: String) {
        executeMutation(UnpauseContainerMutation(id = id))
    }

    override suspend fun updateContainer(id: String) {
        executeMutation(UpdateContainerMutation(id = id))
    }

    override suspend fun updateAllContainers(): Int {
        val response = try {
            sessionManager.apolloClient().mutation(UpdateAllContainersMutation()).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
        return response.data?.docker?.updateAllContainers?.size ?: 0
    }

    override suspend fun updateAutoStart(id: String, autoStart: Boolean, wait: Int?) {
        val response = try {
            sessionManager.apolloClient().mutation(
                UpdateAutostartConfigurationMutation(
                    entries = listOf(
                        DockerAutostartEntryInput(
                            id = id,
                            autoStart = autoStart,
                            wait = Optional.presentIfNotNull(wait),
                        ),
                    ),
                    persistUserPreferences = Optional.present(true),
                ),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    private suspend fun executeMutation(mutation: com.apollographql.apollo.api.Mutation<*>) {
        val response = try {
            sessionManager.apolloClient().mutation(mutation).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    private fun ApolloResponse<*>.throwIfHasErrors() {
        if (hasErrors()) {
            throw ApiException.GraphQLError(errors?.map { it.message } ?: emptyList())
        }
    }

    private fun ApolloException.toApiException(): ApiException = when (this) {
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
        else -> ApiException.GraphQLError(listOf(message ?: "未知 GraphQL 错误"))
    }
}
