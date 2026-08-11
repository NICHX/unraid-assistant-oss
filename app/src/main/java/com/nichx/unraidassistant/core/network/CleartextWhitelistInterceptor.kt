package com.nichx.unraidassistant.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 明文请求被白名单拒绝时抛出的异常。继承 [IOException] 使 OkHttp/Apollo 按网络层
 * 失败处理（Apollo 会包装为 ApolloNetworkException，仓库层据 cause 还原为 [ApiException.CleartextBlocked]）。
 */
class CleartextBlockedException(val host: String) :
    IOException("明文访问被白名单拦截：$host")

/**
 * 明文白名单拦截器：仅拦截 http:// 请求，主机不在白名单内直接抛出 [CleartextBlockedException]，
 * 不发起任何网络连接。HTTPS 与其他协议不受影响。
 */
class CleartextWhitelistInterceptor(
    private val policyCache: CleartextPolicyCache,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.scheme.equals("http", ignoreCase = true)) {
            val host = request.url.host
            if (!policyCache.isHostAllowed(host)) {
                throw CleartextBlockedException(host)
            }
        }
        return chain.proceed(request)
    }
}
