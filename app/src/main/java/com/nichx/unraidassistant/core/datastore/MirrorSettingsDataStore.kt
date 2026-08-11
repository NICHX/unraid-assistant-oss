package com.nichx.unraidassistant.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nichx.unraidassistant.core.network.MirrorConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 镜像加速配置持久化。与 [SettingsDataStore] 共用 "unraid_settings" DataStore 文件。
 *
 * 存储总开关、被禁用的内置源 id 集合与自定义前缀代理列表；
 * 市场模板下载、应用图标加载等场景统一经 [MirrorConfig] 生成候选地址。
 */
@Singleton
class MirrorSettingsDataStore @Inject constructor(
    private val appDataStore: AppDataStore,
) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("mirror_enabled")
        val DISABLED_BUILT_INS = stringSetPreferencesKey("mirror_disabled_built_ins")
        val CUSTOM_PROXIES = stringSetPreferencesKey("mirror_custom_proxies")
    }

    val config: Flow<MirrorConfig> = appDataStore.dataStore.data.map { prefs ->
        MirrorConfig(
            enabled = prefs[Keys.ENABLED] ?: true,
            disabledBuiltIns = prefs[Keys.DISABLED_BUILT_INS] ?: emptySet(),
            customProxies = (prefs[Keys.CUSTOM_PROXIES] ?: emptySet()).toList(),
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        appDataStore.dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    /** 启用/禁用内置源（id 见 [com.nichx.unraidassistant.core.network.BuiltInMirrorSource]）。 */
    suspend fun setBuiltInEnabled(sourceId: String, enabled: Boolean) {
        appDataStore.dataStore.edit { prefs ->
            val disabled = (prefs[Keys.DISABLED_BUILT_INS] ?: emptySet()).toMutableSet()
            if (enabled) disabled.remove(sourceId) else disabled.add(sourceId)
            prefs[Keys.DISABLED_BUILT_INS] = disabled
        }
    }

    suspend fun addCustomProxy(prefix: String) {
        val normalized = prefix.trim().trimEnd('/')
        if (normalized.isEmpty()) return
        appDataStore.dataStore.edit { prefs ->
            val proxies = (prefs[Keys.CUSTOM_PROXIES] ?: emptySet()).toMutableSet()
            proxies.add(normalized)
            prefs[Keys.CUSTOM_PROXIES] = proxies
        }
    }

    suspend fun removeCustomProxy(prefix: String) {
        appDataStore.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PROXIES] = (prefs[Keys.CUSTOM_PROXIES] ?: emptySet())
                .filterNot { it == prefix.trim().trimEnd('/') }
                .toSet()
        }
    }
}
