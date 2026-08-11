package com.nichx.unraidassistant.core.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 容器/虚拟机页的内容展示模式。 */
enum class ContentViewMode { GRID, LIST }

/**
 * 应用设置持久化。存储主题模式、上次激活服务器、Dashboard 轮询间隔、各页视图偏好等非敏感设置。
 *
 * 启动时读取 [lastServerId] 以恢复上次会话（满足"记住最后视图"的用户偏好）。
 * 数据经 [AppDataStore] 共享唯一实例，避免多 DataStore 同文件冲突。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val appDataStore: AppDataStore,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SERVER_ID = stringPreferencesKey("last_server_id")
        val POLL_INTERVAL_SECONDS = intPreferencesKey("poll_interval_seconds")
        val LAST_USED_TAB = stringPreferencesKey("last_used_tab")
        val DOCKER_VIEW_MODE = stringPreferencesKey("docker_view_mode")
        val VM_VIEW_MODE = stringPreferencesKey("vm_view_mode")
        val PLUGINS_VIEW_MODE = stringPreferencesKey("plugins_view_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val DND_START_MINUTES = intPreferencesKey("dnd_start_minutes")
        val DND_END_MINUTES = intPreferencesKey("dnd_end_minutes")
        val ICON_LIBRARY = stringPreferencesKey("icon_library")
        val ICON_API_BASE = stringPreferencesKey("icon_api_base")
        val ICON_RAW_BASE = stringPreferencesKey("icon_raw_base")
    }

    /** 默认免打扰时段：23:00 – 07:00。 */
    companion object {
        const val DEFAULT_DND_START_MINUTES = 23 * 60
        const val DEFAULT_DND_END_MINUTES = 7 * 60
    }

    private fun readViewMode(prefs: Preferences, key: Preferences.Key<String>): ContentViewMode =
        prefs[key]?.let { runCatching { ContentViewMode.valueOf(it) }.getOrNull() } ?: ContentViewMode.GRID

    val themeMode: Flow<ThemeMode> = appDataStore.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val lastServerId: Flow<String?> = appDataStore.dataStore.data.map { it[Keys.LAST_SERVER_ID] }

    val pollIntervalSeconds: Flow<Int> = appDataStore.dataStore.data.map { it[Keys.POLL_INTERVAL_SECONDS] ?: 30 }

    val lastUsedTab: Flow<String?> = appDataStore.dataStore.data.map { it[Keys.LAST_USED_TAB] }

    val dockerViewMode: Flow<ContentViewMode> =
        appDataStore.dataStore.data.map { readViewMode(it, Keys.DOCKER_VIEW_MODE) }

    val vmViewMode: Flow<ContentViewMode> =
        appDataStore.dataStore.data.map { readViewMode(it, Keys.VM_VIEW_MODE) }

    val pluginsViewMode: Flow<ContentViewMode> =
        appDataStore.dataStore.data.map { readViewMode(it, Keys.PLUGINS_VIEW_MODE) }

    /** 系统通知总开关（默认关闭，避免未经用户确认即推送）。 */
    val notificationsEnabled: Flow<Boolean> =
        appDataStore.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }

    val dndEnabled: Flow<Boolean> =
        appDataStore.dataStore.data.map { it[Keys.DND_ENABLED] ?: false }

    /** 免打扰起始时刻（自 00:00 起的分钟数，默认 23:00）。 */
    val dndStartMinutes: Flow<Int> =
        appDataStore.dataStore.data.map { it[Keys.DND_START_MINUTES] ?: DEFAULT_DND_START_MINUTES }

    /** 免打扰结束时刻（自 00:00 起的分钟数，默认 07:00）。 */
    val dndEndMinutes: Flow<Int> =
        appDataStore.dataStore.data.map { it[Keys.DND_END_MINUTES] ?: DEFAULT_DND_END_MINUTES }

    suspend fun setThemeMode(mode: ThemeMode) {
        appDataStore.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDockerViewMode(mode: ContentViewMode) {
        appDataStore.dataStore.edit { it[Keys.DOCKER_VIEW_MODE] = mode.name }
    }

    suspend fun setVmViewMode(mode: ContentViewMode) {
        appDataStore.dataStore.edit { it[Keys.VM_VIEW_MODE] = mode.name }
    }

    suspend fun setPluginsViewMode(mode: ContentViewMode) {
        appDataStore.dataStore.edit { it[Keys.PLUGINS_VIEW_MODE] = mode.name }
    }

    suspend fun setLastServerId(id: String?) {
        appDataStore.dataStore.edit {
            if (id == null) it.remove(Keys.LAST_SERVER_ID) else it[Keys.LAST_SERVER_ID] = id
        }
    }

    suspend fun setPollIntervalSeconds(seconds: Int) {
        appDataStore.dataStore.edit { it[Keys.POLL_INTERVAL_SECONDS] = seconds.coerceIn(5, 300) }
    }

    suspend fun setLastUsedTab(route: String) {
        appDataStore.dataStore.edit { it[Keys.LAST_USED_TAB] = route }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appDataStore.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        appDataStore.dataStore.edit { it[Keys.DND_ENABLED] = enabled }
    }

    suspend fun setDndStartMinutes(minutes: Int) {
        appDataStore.dataStore.edit { it[Keys.DND_START_MINUTES] = minutes.coerceIn(0, 24 * 60 - 1) }
    }

    suspend fun setDndEndMinutes(minutes: Int) {
        appDataStore.dataStore.edit { it[Keys.DND_END_MINUTES] = minutes.coerceIn(0, 24 * 60 - 1) }
    }

    /** 选中的内置图标库 key（空 = linuxserver）。 */
    val iconLibrary: Flow<String?> =
        appDataStore.dataStore.data.map { it[Keys.ICON_LIBRARY] }

    /** 自定义图标库的列表 API 地址（空 = 官方 GitHub API）。 */
    val iconApiBase: Flow<String?> =
        appDataStore.dataStore.data.map { it[Keys.ICON_API_BASE] }

    /** 自定义图标库的图标文件下载前缀（空 = 官方 raw.githubusercontent.com）。 */
    val iconRawBase: Flow<String?> =
        appDataStore.dataStore.data.map { it[Keys.ICON_RAW_BASE] }

    suspend fun setIconLibrary(key: String) {
        appDataStore.dataStore.edit { it[Keys.ICON_LIBRARY] = key }
    }

    suspend fun setIconApiBase(url: String) {
        appDataStore.dataStore.edit { it[Keys.ICON_API_BASE] = url }
    }

    suspend fun setIconRawBase(url: String) {
        appDataStore.dataStore.edit { it[Keys.ICON_RAW_BASE] = url }
    }
}
