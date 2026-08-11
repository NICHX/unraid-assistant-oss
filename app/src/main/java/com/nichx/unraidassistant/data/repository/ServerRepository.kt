package com.nichx.unraidassistant.data.repository

import com.nichx.unraidassistant.core.db.dao.ServerDao
import com.nichx.unraidassistant.core.security.ApiKeyStore
import com.nichx.unraidassistant.data.local.toEntity
import com.nichx.unraidassistant.data.local.toModel
import com.nichx.unraidassistant.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器配置仓库。协调 Room（连接配置）与 [ApiKeyStore]（加密 Key）的原子读写。
 */
interface ServerRepository {
    fun observeServers(): Flow<List<ServerConfig>>
    suspend fun getServer(id: String): ServerConfig?
    suspend fun count(): Int
    suspend fun save(server: ServerConfig, apiKey: String)
    suspend fun delete(id: String)
}

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val apiKeyStore: ApiKeyStore,
) : ServerRepository {

    override fun observeServers(): Flow<List<ServerConfig>> =
        serverDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getServer(id: String): ServerConfig? =
        serverDao.getById(id)?.toModel()

    override suspend fun count(): Int = serverDao.count()

    /**
     * 保存服务器配置。apiKey 非空时覆盖加密 Key；为空表示编辑时不修改 Key。
     * 服务器管理页删除某服务器后若其仍为激活会话，调用方负责注销会话。
     */
    override suspend fun save(server: ServerConfig, apiKey: String) {
        val now = System.currentTimeMillis()
        val createdAt = serverDao.getById(server.id)?.createdAt ?: now
        serverDao.upsert(server.toEntity(createdAt = createdAt, updatedAt = now))
        if (apiKey.isNotBlank()) {
            apiKeyStore.save(server.id, apiKey)
        }
    }

    override suspend fun delete(id: String) {
        serverDao.delete(id)
        apiKeyStore.clear(id)
    }
}
