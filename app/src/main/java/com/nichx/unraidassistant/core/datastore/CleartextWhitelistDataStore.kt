package com.nichx.unraidassistant.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nichx.unraidassistant.core.network.CleartextPolicy
import com.nichx.unraidassistant.core.network.CleartextRule
import com.nichx.unraidassistant.core.network.CleartextWhitelist
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 明文白名单持久化。与 [SettingsDataStore] 共用 "unraid_settings" DataStore 文件。
 * 自定义规则以原始字符串存储（如 "192.168.0.0/16"），读取时经 [CleartextPolicy.parseRule] 重新解析，
 * 非法条目自动丢弃，避免存储坏数据。
 */
@Singleton
class CleartextWhitelistDataStore @Inject constructor(
    private val appDataStore: AppDataStore,
) {

    private object Keys {
        val INTERNAL_ENABLED = booleanPreferencesKey("cleartext_internal_enabled")
        val CUSTOM_RULES = stringSetPreferencesKey("cleartext_custom_rules")
    }

    val whitelist: Flow<CleartextWhitelist> = appDataStore.dataStore.data.map { prefs ->
        CleartextWhitelist(
            internalEnabled = prefs[Keys.INTERNAL_ENABLED] ?: true,
            customRules = (prefs[Keys.CUSTOM_RULES] ?: emptySet())
                .mapNotNull { CleartextPolicy.parseRule(it) },
        )
    }

    suspend fun setInternalEnabled(enabled: Boolean) {
        appDataStore.dataStore.edit { it[Keys.INTERNAL_ENABLED] = enabled }
    }

    suspend fun addRule(rule: CleartextRule) {
        appDataStore.dataStore.edit { prefs ->
            val rules = (prefs[Keys.CUSTOM_RULES] ?: emptySet()).toMutableSet()
            rules.add(rule.value)
            prefs[Keys.CUSTOM_RULES] = rules
        }
    }

    suspend fun removeRule(value: String) {
        appDataStore.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_RULES] = (prefs[Keys.CUSTOM_RULES] ?: emptySet())
                .filterNot { it == value }
                .toSet()
        }
    }
}
