package com.nichx.unraidassistant.data.local

import com.nichx.unraidassistant.core.db.entity.ServerEntity
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.model.ServerProtocol

fun ServerEntity.toModel(): ServerConfig = ServerConfig(
    id = id,
    name = name,
    protocol = ServerProtocol.fromValue(protocol),
    host = host,
    port = port,
    apiPath = apiPath,
    insecureSkipVerify = insecureSkipVerify,
)

fun ServerConfig.toEntity(createdAt: Long, updatedAt: Long): ServerEntity = ServerEntity(
    id = id,
    name = name,
    protocol = protocol.value,
    host = host,
    port = port,
    apiPath = apiPath,
    insecureSkipVerify = insecureSkipVerify,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
