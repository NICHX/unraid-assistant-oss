package com.nichx.unraidassistant.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 加密存储。API Key 等同服务器 admin 权限，强制走 Android Keystore
 * （AES256-GCM 主密钥）加密，不落明文、不写入日志。
 *
 * 实现：androidx.security:security-crypto 已整体弃用且无官方平替，
 * 改用 Android Keystore 原生 AES/GCM——主密钥不可导出地保存在 Keystore 中，
 * 密文以 "IV 长度 + IV + 密文" 的结构存入普通 SharedPreferences（详见 [KeystoreAesGcm]）。
 *
 * 生命周期：Key 仅在 [com.nichx.unraidassistant.session.SessionManager.switchTo]
 * 调用时按 serverId 读入内存构建 ApolloClient，不作为字段常驻。
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val cipher = KeystoreAesGcm(KEY_ALIAS)

    fun save(serverId: String, apiKey: String) {
        val payload = cipher.encrypt(apiKey.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(serverId, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun load(serverId: String): String? {
        val encoded = prefs.getString(serverId, null) ?: return null
        val payload = try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return cipher.decrypt(payload)?.toString(Charsets.UTF_8)
    }

    fun clear(serverId: String) {
        prefs.edit().remove(serverId).apply()
    }

    private companion object {
        const val PREFS_NAME = "unraid_secure"
        const val KEY_ALIAS = "unraid_api_key"
    }
}
