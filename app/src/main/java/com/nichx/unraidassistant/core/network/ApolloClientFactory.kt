package com.nichx.unraidassistant.core.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.apollographql.apollo.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.data.model.ServerProtocol
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds

/**
 * 按服务器会话构建 ApolloClient。
 *
 * 自签证书支持分两层：
 * 1. 默认路径：network_security_config 信任用户安装的 CA，系统校验链即可通过，无需任何开关；
 * 2. 兜底路径：服务器开启 [ServerConfig.insecureSkipVerify] 后，为 GraphQL API 构建
 *    跳过证书链与主机名校验的 OkHttpClient 副本（连接池/拦截器与共享客户端一致）。
 */
@Singleton
class ApolloClientFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    fun create(server: ServerConfig, apiKey: String): ApolloClient {
        val client = if (server.insecureSkipVerify && server.protocol == ServerProtocol.HTTPS) {
            lenientTlsClient()
        } else {
            okHttpClient
        }
        return ApolloClient.Builder()
            .serverUrl(server.graphQlUrl)
            .okHttpClient(client)
            .addHttpHeader("x-api-key", apiKey)
            .subscriptionNetworkTransport(
                WebSocketNetworkTransport.Builder()
                    .serverUrl(server.wsUrl)
                    .webSocketEngine(WebSocketEngine { client })
                    // unraid-api 的 WS 认证契约：API key 必须通过 connection_init 的 payload（connectionParams）传递，
                    // 服务端 AuthenticationGuard 会把 connectionParams 合并进请求头再做 x-api-key 校验
                    // （见 unraid/api authentication.guard.ts 与 internal-graphql-client.factory.ts）。
                    .wsProtocol(
                        GraphQLWsProtocol {
                            mapOf("x-api-key" to apiKey)
                        }
                    )
                    // 服务器 keepalive（graphql-ws 默认 12s）已能维持连接活跃，
                    // 客户端 ping 放宽到 60s 仅作 NAT/防火墙保活兜底，减少无线电唤醒频率。
                    .pingInterval(60.seconds)
                    // 空闲超时放宽到 5 分钟：熄屏/Doze 下网络可能短暂挂起，
                    // 过短的超时会误断连接并触发重连（重握手比保活更耗电）。
                    .idleTimeout(300.seconds)
                    .build()
            )
            .build()
    }

    /**
     * 基于共享客户端派生跳过证书校验的副本：仅替换 sslSocketFactory 与 hostnameVerifier，
     * 连接池/调度器/拦截器（明文白名单、日志）保持一致。
     */
    private fun lenientTlsClient(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        return okHttpClient.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
