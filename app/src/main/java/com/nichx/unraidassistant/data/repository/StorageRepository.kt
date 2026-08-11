package com.nichx.unraidassistant.data.repository

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.nichx.unraidassistant.core.network.CleartextBlockedException
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.ArrayCapacityInfo
import com.nichx.unraidassistant.data.model.ArrayDiskStatusEnum
import com.nichx.unraidassistant.data.model.ArrayDiskTypeEnum
import com.nichx.unraidassistant.data.model.ArrayStateEnum
import com.nichx.unraidassistant.data.model.DiskInfo
import com.nichx.unraidassistant.data.model.ParityCheckInfo
import com.nichx.unraidassistant.data.model.ParityCheckStatusEnum
import com.nichx.unraidassistant.data.model.ShareInfo
import com.nichx.unraidassistant.data.model.StorageData
import com.nichx.unraidassistant.data.remote.graphql.ArrayStartMutation
import com.nichx.unraidassistant.data.remote.graphql.ArrayStopMutation
import com.nichx.unraidassistant.data.remote.graphql.GetStorageQuery
import com.nichx.unraidassistant.data.remote.graphql.ParityCheckCancelMutation
import com.nichx.unraidassistant.data.remote.graphql.ParityCheckPauseMutation
import com.nichx.unraidassistant.data.remote.graphql.ParityCheckResumeMutation
import com.nichx.unraidassistant.data.remote.graphql.ParityCheckStartMutation
import com.nichx.unraidassistant.data.remote.graphql.type.ArrayDiskStatus
import com.nichx.unraidassistant.data.remote.graphql.type.ArrayDiskType
import com.nichx.unraidassistant.data.remote.graphql.type.ArrayState
import com.nichx.unraidassistant.data.remote.graphql.type.ParityCheckStatus
import com.nichx.unraidassistant.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

interface StorageRepository {
    suspend fun fetchStorage(): StorageData
    suspend fun startArray()
    suspend fun stopArray()
    suspend fun startParityCheck(correct: Boolean)
    suspend fun pauseParityCheck()
    suspend fun resumeParityCheck()
    suspend fun cancelParityCheck()
}

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : StorageRepository {

    override suspend fun fetchStorage(): StorageData {
        val response = try {
            sessionManager.apolloClient().query(GetStorageQuery()).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        if (response.hasErrors()) {
            throw ApiException.GraphQLError(response.errors?.map { it.message } ?: emptyList())
        }
        val data = response.data ?: throw ApiException.ServerError(500)
        val array = data.array
        val capacity = array.capacity
        return StorageData(
            arrayState = array.state.toDomain(ArrayStateEnum.STOPPED),
            capacity = capacity.kilobytes.let {
                ArrayCapacityInfo(it.free, it.used, it.total)
            },
            parityCheck = array.parityCheckStatus.toParityCheckInfo(),
            parities = array.parities.map { it.toDiskInfo() },
            dataDisks = array.disks.map { it.toDiskInfo() },
            cacheDisks = array.caches.map { it.toDiskInfo() },
            bootDisk = array.boot?.toDiskInfo(),
            shares = data.shares.map { it.toShareInfo() },
        )
    }

    override suspend fun startArray() {
        executeMutation(ArrayStartMutation())
    }

    override suspend fun stopArray() {
        executeMutation(ArrayStopMutation())
    }

    override suspend fun startParityCheck(correct: Boolean) {
        executeMutation(ParityCheckStartMutation(correct = correct))
    }

    override suspend fun pauseParityCheck() {
        executeMutation(ParityCheckPauseMutation())
    }

    override suspend fun resumeParityCheck() {
        executeMutation(ParityCheckResumeMutation())
    }

    override suspend fun cancelParityCheck() {
        executeMutation(ParityCheckCancelMutation())
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

    private fun GetStorageQuery.ParityCheckStatus.toParityCheckInfo() = ParityCheckInfo(
        status = status.toDomain(ParityCheckStatusEnum.NEVER_RUN),
        progress = progress,
        speed = speed,
        errors = errors,
        correcting = correcting,
        paused = paused,
        running = running,
        durationSeconds = duration,
    )

    private fun GetStorageQuery.Parity.toDiskInfo() = DiskInfo(
        id = id, name = name, device = device,
        type = type.toDomain(ArrayDiskTypeEnum.DATA),
        status = status?.toDomain(ArrayDiskStatusEnum.DISK_NP),
        tempCelsius = temp, isRotational = null, isSpinning = isSpinning,
        sizeKb = size, fsSizeKb = null, fsFreeKb = null, fsUsedKb = null,
        numReads = null, numWrites = null, numErrors = null,
    )

    private fun GetStorageQuery.Disk.toDiskInfo() = DiskInfo(
        id = id, name = name, device = device,
        type = type.toDomain(ArrayDiskTypeEnum.DATA),
        status = status?.toDomain(ArrayDiskStatusEnum.DISK_NP),
        tempCelsius = temp, isRotational = rotational, isSpinning = isSpinning,
        sizeKb = size, fsSizeKb = fsSize, fsFreeKb = fsFree, fsUsedKb = fsUsed,
        numReads = numReads, numWrites = numWrites, numErrors = numErrors,
    )

    private fun GetStorageQuery.Cach.toDiskInfo() = DiskInfo(
        id = id, name = name, device = device,
        type = type.toDomain(ArrayDiskTypeEnum.DATA),
        status = status?.toDomain(ArrayDiskStatusEnum.DISK_NP),
        tempCelsius = temp, isRotational = rotational, isSpinning = isSpinning,
        sizeKb = size, fsSizeKb = fsSize, fsFreeKb = fsFree, fsUsedKb = fsUsed,
        numReads = null, numWrites = null, numErrors = null,
    )

    private fun GetStorageQuery.Boot.toDiskInfo() = DiskInfo(
        id = id, name = name, device = device,
        type = ArrayDiskTypeEnum.BOOT,
        status = null, tempCelsius = null, isRotational = null, isSpinning = null,
        sizeKb = size, fsSizeKb = null, fsFreeKb = null, fsUsedKb = null,
        numReads = null, numWrites = null, numErrors = null,
    )

    private fun GetStorageQuery.Share.toShareInfo() = ShareInfo(
        id = id, name = name, freeKb = free, usedKb = used,
        sizeKb = size, cache = cache, comment = comment,
    )

    private inline fun <reified G : Enum<G>, reified D : Enum<D>> G?.toDomain(default: D): D =
        this?.let { runCatching { enumValueOf<D>(it.name) }.getOrDefault(default) } ?: default

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
