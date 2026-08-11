package com.nichx.unraidassistant.data.model

import kotlinx.serialization.Serializable

/**
 * unRAID 服务器连接配置。
 *
 * API Key 不在此模型中——它等同服务器 admin 权限，单独经 [com.nichx.unraidassistant.core.security.ApiKeyStore]
 * 加密存储，仅在实际构建 ApolloClient 时按 serverId 从 Keystore 读入内存。
 */
@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val protocol: ServerProtocol = ServerProtocol.HTTPS,
    val host: String,
    val port: Int = 443,
    val apiPath: String = "/graphql",
    /**
     * 跳过 TLS 证书校验（信任任意自签证书）。
     *
     * 仅为用户无法安装自建 CA 的场景提供的兜底开关：开启后 GraphQL API（OkHttp）
     * 与 WebGUI 深链（WebView）对该服务器不再校验证书链与主机名。
     * 推荐做法仍是让用户把自建 CA 安装进系统（见 network_security_config），
     * 此开关仅作明确告知风险后的逃生通道。
     */
    val insecureSkipVerify: Boolean = false,
) {
    val baseUrl: String get() = "${protocol.value}://$host:$port"
    val graphQlUrl: String get() = "$baseUrl$apiPath"
    val wsUrl: String get() = baseUrl.replaceFirst("http", "ws") + apiPath
}

enum class ServerProtocol(val value: String) {
    HTTP("http"),
    HTTPS("https");

    companion object {
        fun fromValue(value: String): ServerProtocol =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: HTTPS
    }
}
