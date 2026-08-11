package com.nichx.unraidassistant.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局唯一的 Preferences DataStore 持有者。
 *
 * 所有设置类存储（主题、上次会话、明文白名单等）共用 "unraid_settings" 这一个文件。
 * 注意：不同版本 datastore 的 preferencesDataStore 委托对"同名多委托"的收敛行为并不一致
 * （1.3.0-alpha10 下会创建多个实例，导致 IllegalStateException: multiple DataStores active
 * for the same file），因此必须由本单例持有唯一实例，其他模块经 [dataStore] 共享。
 */
@Singleton
class AppDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore("unraid_settings")

    /** 唯一实例：构造时求值一次并被单例强引用，进程内不再重建。 */
    private val store: DataStore<Preferences> = context.appDataStore

    val dataStore: DataStore<Preferences> get() = store
}
