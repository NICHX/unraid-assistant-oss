package com.nichx.unraidassistant.core.network

/** GitHub raw 加速源（gh-proxy 类前缀代理，用于国内网络直连 raw 超时的兜底）。 */
enum class BuiltInMirrorSource(
    val id: String,
    val displayName: String,
    val description: String,
) {
    DIRECT("direct", "GitHub 直连", "raw.githubusercontent.com 官方直连"),
    JSDELIVR("jsdelivr", "jsDelivr CDN", "cdn.jsdelivr.net 全球 CDN"),
    GHPROXY_NET("ghproxy.net", "ghproxy.net", "gh-proxy 前缀代理"),
    GHPROXY_COM("gh-proxy.com", "gh-proxy.com", "gh-proxy 前缀代理"),
    GHFAST_TOP("ghfast.top", "ghfast.top", "gh-proxy 前缀代理"),
}

/** 镜像加速配置（持久化于 [com.nichx.unraidassistant.core.datastore.MirrorSettingsDataStore]）。 */
data class MirrorConfig(
    /** 镜像加速总开关：关闭时所有请求仅直连。 */
    val enabled: Boolean = true,
    /** 用户禁用的内置源 id（默认全部启用）。 */
    val disabledBuiltIns: Set<String> = emptySet(),
    /** 用户自定义前缀代理（按序追加在内置代理之后尝试）。 */
    val customProxies: List<String> = emptyList(),
)

/** 内置 gh-proxy 前缀代理：id → 前缀。 */
private val GITHUB_RAW_PROXIES = mapOf(
    BuiltInMirrorSource.GHPROXY_NET.id to "https://ghproxy.net/",
    BuiltInMirrorSource.GHPROXY_COM.id to "https://gh-proxy.com/",
    BuiltInMirrorSource.GHFAST_TOP.id to "https://ghfast.top/",
)

/**
 * 按镜像配置对 GitHub raw 链接生成候选加速地址（直连 → jsDelivr CDN → 前缀代理）。
 *
 * - `https://raw.githubusercontent.com/owner/repo/branch/path`
 * - jsDelivr: `https://cdn.jsdelivr.net/gh/owner/repo@branch/path`
 * - 代理: `https://ghproxy.net/https://raw.githubusercontent.com/...`
 *
 * 非 GitHub raw 链接原样返回；总开关关闭时仅返回直连。
 * 可用于 OkHttp 依次尝试或 Coil 多源加载。
 */
fun githubRawMirrorUrls(url: String, config: MirrorConfig = MirrorConfig()): List<String> {
    val rawPrefix = "https://raw.githubusercontent.com/"
    if (!url.startsWith(rawPrefix) || !config.enabled) return listOf(url)
    val parts = url.removePrefix(rawPrefix).split("/", limit = 4)
    if (parts.size < 3) return listOf(url)

    val candidates = mutableListOf<String>()
    if (BuiltInMirrorSource.DIRECT.id !in config.disabledBuiltIns) {
        candidates.add(url)
    }
    if (BuiltInMirrorSource.JSDELIVR.id !in config.disabledBuiltIns) {
        candidates.add(
            "https://cdn.jsdelivr.net/gh/${parts[0]}/${parts[1]}@${parts.drop(2).joinToString("/")}",
        )
    }
    GITHUB_RAW_PROXIES.forEach { (id, prefix) ->
        if (id !in config.disabledBuiltIns) {
            candidates.add(prefix + url)
        }
    }
    config.customProxies.forEach { prefix ->
        candidates.add(prefix.trimEnd('/') + "/" + url)
    }
    return candidates
}
