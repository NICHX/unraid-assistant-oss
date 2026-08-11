package com.nichx.unraidassistant.data.repository

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.nichx.unraidassistant.core.network.CleartextBlockedException
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.VmData
import com.nichx.unraidassistant.data.model.VmInfo
import com.nichx.unraidassistant.data.model.VmStateEnum
import com.nichx.unraidassistant.data.remote.graphql.ForceStopVmMutation
import com.nichx.unraidassistant.data.remote.graphql.GetVmsQuery
import com.nichx.unraidassistant.data.remote.graphql.PauseVmMutation
import com.nichx.unraidassistant.data.remote.graphql.RebootVmMutation
import com.nichx.unraidassistant.data.remote.graphql.ResumeVmMutation
import com.nichx.unraidassistant.data.remote.graphql.StartVmMutation
import com.nichx.unraidassistant.data.remote.graphql.StopVmMutation
import com.nichx.unraidassistant.data.remote.graphql.type.VmState
import com.nichx.unraidassistant.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

interface VmRepository {
    suspend fun fetchVms(): VmData
    suspend fun startVm(id: String)
    suspend fun stopVm(id: String)
    suspend fun forceStopVm(id: String)
    suspend fun pauseVm(id: String)
    suspend fun resumeVm(id: String)
    suspend fun rebootVm(id: String)
}

@Singleton
class VmRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : VmRepository {

    override suspend fun fetchVms(): VmData {
        val response = try {
            sessionManager.apolloClient().query(GetVmsQuery()).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
        val data = response.data ?: throw ApiException.ServerError(500)
        return VmData(
            vms = data.vms.domains.orEmpty()
                .filter { it.name != null }
                .map { it.toVmInfo() },
        )
    }

    override suspend fun startVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(StartVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    override suspend fun stopVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(StopVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    override suspend fun forceStopVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(ForceStopVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    override suspend fun pauseVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(PauseVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    override suspend fun resumeVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(ResumeVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    override suspend fun rebootVm(id: String) {
        val response = try {
            sessionManager.apolloClient().mutation(RebootVmMutation(id = id)).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        response.throwIfHasErrors()
    }

    private fun GetVmsQuery.Domain.toVmInfo() = VmInfo(
        id = id,
        name = name ?: "未命名",
        state = state.toDomain(VmStateEnum.SHUTOFF),
    )

    private fun ApolloResponse<*>.throwIfHasErrors() {
        if (hasErrors()) {
            throw ApiException.GraphQLError(errors?.map { it.message } ?: emptyList())
        }
    }

    private inline fun <reified G : Enum<G>, reified D : Enum<D>> G?.toDomain(default: D): D =
        this?.let { runCatching { enumValueOf<D>(it.name) }.getOrDefault(default) } ?: default

    private fun ApolloException.toApiException(): ApiException = when (this) {
        is ApolloHttpException -> when (statusCode) {
            401 -> ApiException.Unauthorized
            403 -> ApiException.Forbidden
            429 -> ApiException.RateLimited
            else -> ApiException.ServerError(statusCode)
        }
        is ApolloNetworkException -> (this.cause as? CleartextBlockedException)
            ?.let { ApiException.CleartextBlocked(it.host) }
            ?: ApiException.NetworkUnreachable
        else -> ApiException.GraphQLError(listOf(message ?: "未知 GraphQL 错误"))
    }
}
