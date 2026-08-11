package com.nichx.unraidassistant.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 服务器连接配置持久化实体。与 [com.nichx.unraidassistant.data.model.ServerConfig] 一一映射。
 * API Key 不在此存储，见 [com.nichx.unraidassistant.core.security.ApiKeyStore]。
 */
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val apiPath: String,
    val insecureSkipVerify: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
