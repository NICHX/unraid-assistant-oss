package com.nichx.unraidassistant.core.util

/**
 * 统一 API 异常分类。Repository 层捕获底层异常并映射到此 sealed 层级，
 * UI 层据此渲染对应的错误文案与重试按钮。
 */
sealed class ApiException(message: String) : Exception(message) {
    data object NetworkUnreachable : ApiException("网络不可达")
    data class NetworkError(val detail: String) : ApiException(detail)
    data object Unauthorized : ApiException("API Key 无效或权限不足")
    data object Forbidden : ApiException("权限不足")
    data object RateLimited : ApiException("请求过于频繁，请稍后重试")
    data class ServerError(val code: Int) : ApiException("服务器错误 ($code)")
    data object Timeout : ApiException("请求超时")
    data class GraphQLError(val errors: List<String>) : ApiException(errors.joinToString())
    data class CleartextBlocked(val host: String) : ApiException("明文访问被白名单拦截：$host")
}
