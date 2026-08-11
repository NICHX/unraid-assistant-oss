package com.nichx.unraidassistant.core.network

/**
 * 明文（HTTP）白名单策略。
 *
 * unRAID WebGUI / 容器 WebUI 常为明文 HTTP，且服务器地址可能是任意内网 IP 或主机名。
 * Android 的 network_security_config 是编译期静态配置，且 <domain> 不支持 CIDR 网段，
 * 因此真正的白名单判定放在应用层：OkHttp 拦截器与 WebView 导航守卫统一调用 [CleartextWhitelist.allows]。
 */
enum class CleartextRuleType { CIDR, HOST, WILDCARD }

data class CleartextRule(val type: CleartextRuleType, val value: String) {
    val display: String
        get() = value

    fun matches(host: String): Boolean = when (type) {
        CleartextRuleType.HOST -> host == value
        CleartextRuleType.WILDCARD -> {
            val base = value.removePrefix("*.")
            host == base || host.endsWith(".$base")
        }
        CleartextRuleType.CIDR -> {
            val ip = CleartextPolicy.parseIpv4(host) ?: return false
            CleartextPolicy.ipInCidr(ip, value)
        }
    }
}

/**
 * 白名单快照：默认内网网段开关 + 用户自定义规则。
 * [allows] 对传入主机名做小写归一化后匹配。
 */
data class CleartextWhitelist(
    val internalEnabled: Boolean = true,
    val customRules: List<CleartextRule> = emptyList(),
) {
    val allRules: List<CleartextRule>
        get() = (if (internalEnabled) CleartextPolicy.DEFAULT_INTERNAL_RULES else emptyList()) + customRules

    fun allows(host: String): Boolean {
        val normalized = host.trim().lowercase()
        if (normalized.isEmpty()) return false
        return allRules.any { it.matches(normalized) }
    }
}

object CleartextPolicy {

    /**
     * 默认放行规则：RFC1918 内网网段（家庭/局域网直连 unRAID 的常见场景）+ localhost。
     */
    val DEFAULT_INTERNAL_RULES: List<CleartextRule> = listOf(
        CleartextRule(CleartextRuleType.CIDR, "192.168.0.0/16"),
        CleartextRule(CleartextRuleType.CIDR, "10.0.0.0/8"),
        CleartextRule(CleartextRuleType.CIDR, "172.16.0.0/12"),
        CleartextRule(CleartextRuleType.HOST, "localhost"),
    )

    private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val HOSTNAME_REGEX = Regex(
        "^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)" +
            "(\\.([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*$",
    )

    /**
     * 解析并校验一条用户输入规则，非法输入返回 null。
     * 支持：CIDR 网段（192.168.0.0/16）、IPv4（192.168.1.5）、主机名（nas.local）、
     * 通配符（*.example.com，含域名本身及所有子域）。
     */
    fun parseRule(input: String): CleartextRule? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith("*.") && isValidHostname(trimmed.substring(2)) ->
                CleartextRule(CleartextRuleType.WILDCARD, trimmed.lowercase())
            trimmed.contains('/') -> parseCidr(trimmed)
            isValidIpv4(trimmed) -> CleartextRule(CleartextRuleType.HOST, trimmed)
            isValidHostname(trimmed) -> CleartextRule(CleartextRuleType.HOST, trimmed.lowercase())
            else -> null
        }
    }

    fun validate(input: String): Boolean = parseRule(input) != null

    fun isValidIpv4(host: String): Boolean {
        if (!IPV4_REGEX.matches(host)) return false
        return host.split('.').all { octet ->
            val n = octet.toIntOrNull() ?: return false
            n in 0..255
        }
    }

    private fun isValidHostname(host: String): Boolean {
        if (host == "localhost") return true
        if (host.isBlank() || host.length > 253) return false
        return HOSTNAME_REGEX.matches(host)
    }

    private fun parseCidr(input: String): CleartextRule? {
        val parts = input.split('/')
        if (parts.size != 2) return null
        val prefix = parts[1].toIntOrNull() ?: return null
        if (prefix !in 0..32 || !isValidIpv4(parts[0])) return null
        return CleartextRule(CleartextRuleType.CIDR, input)
    }

    internal fun parseIpv4(host: String): Int? {
        if (!isValidIpv4(host)) return null
        return host.split('.').fold(0) { acc, octet -> (acc shl 8) or octet.toInt() }
    }

    internal fun ipInCidr(ip: Int, cidr: String): Boolean {
        val parts = cidr.split('/')
        if (parts.size != 2) return false
        val network = parseIpv4(parts[0]) ?: return false
        val prefix = parts[1].toIntOrNull() ?: return false
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        return (ip and mask) == (network and mask)
    }
}
