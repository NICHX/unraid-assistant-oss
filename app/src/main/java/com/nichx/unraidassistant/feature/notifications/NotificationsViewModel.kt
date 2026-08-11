package com.nichx.unraidassistant.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.NotificationImportance
import com.nichx.unraidassistant.data.model.NotificationItem
import com.nichx.unraidassistant.data.model.NotificationOverviewData
import com.nichx.unraidassistant.data.repository.NotificationRepository
import com.nichx.unraidassistant.session.NotificationCenter
import com.nichx.unraidassistant.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data object NoServer : NotificationsUiState
    data class Success(
        val items: List<NotificationItem>,
        val isRefreshing: Boolean = false,
        val transientError: String? = null,
        /** 正在执行操作的条目 id（防重复提交）。 */
        val busyIds: Set<String> = emptySet(),
        /** 全部已读 / 清空归档等批量操作执行中。 */
        val bulkBusy: Boolean = false,
    ) : NotificationsUiState
    data class Error(val message: String, val retry: () -> Unit) : NotificationsUiState
}

/**
 * 通知列表页：未读/归档双视图，按重要性筛选，支持下拉刷新与增删操作。
 * - 无服务器时进入 [NotificationsUiState.NoServer]；
 * - 已有数据时出错转为 [Success.transientError] 横幅提示，保留旧列表；
 * - 无数据时首次失败进入 [Error]，等待用户重试；
 * - 操作（标记已读/未读、删除、全部已读、清空归档）成功后以服务端返回的
 *   概览校正角标（经 [NotificationCenter.updateOverview]），再刷新列表。
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val sessionManager: SessionManager,
    private val notificationCenter: NotificationCenter,
) : ViewModel() {

    private val _filter = MutableStateFlow<NotificationImportance?>(null)
    val filter: StateFlow<NotificationImportance?> = _filter.asStateFlow()

    /** 当前视图：false = 未读，true = 归档。 */
    private val _showArchive = MutableStateFlow(false)
    val showArchive: StateFlow<Boolean> = _showArchive.asStateFlow()

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** 切换重要性筛选：null 为全部级别，切换后重新加载。 */
    fun setFilter(importance: NotificationImportance?) {
        if (_filter.value == importance) return
        _filter.value = importance
        _uiState.value = NotificationsUiState.Loading
        load()
    }

    /** 切换未读/归档视图，切换后重新加载。 */
    fun setShowArchive(archive: Boolean) {
        if (_showArchive.value == archive) return
        _showArchive.value = archive
        _uiState.value = NotificationsUiState.Loading
        load()
    }

    /** 下拉刷新：保留旧列表并标记刷新中，完成后静默替换。 */
    fun refresh() {
        if (_uiState.value is NotificationsUiState.Success) {
            _uiState.update { (it as NotificationsUiState.Success).copy(isRefreshing = true) }
        }
        load()
    }

    /** 从错误态重试。 */
    fun retry() {
        load()
    }

    /** 关闭刷新失败横幅。 */
    fun clearTransientError() {
        _uiState.update {
            if (it is NotificationsUiState.Success && it.transientError != null) {
                it.copy(transientError = null)
            } else {
                it
            }
        }
    }

    /** 标记已读（归档单条）。 */
    fun markRead(item: NotificationItem) {
        runSingleAction(item) { repository.archiveNotifications(listOf(item.id)) }
    }

    /** 标记未读（取消归档单条）。 */
    fun markUnread(item: NotificationItem) {
        runSingleAction(item) { repository.unarchiveNotifications(listOf(item.id)) }
    }

    /** 删除单条通知（不可恢复，UI 需确认对话框）。 */
    fun delete(item: NotificationItem) {
        runSingleAction(item) { repository.deleteNotification(item.id, item.isUnread) }
    }

    /** 全部已读：归档当前筛选下的所有未读通知。 */
    fun archiveAll() {
        if (bulkBusy()) return
        _uiState.update { (it as NotificationsUiState.Success).copy(bulkBusy = true) }
        runBulkAction { repository.archiveAll(_filter.value) }
    }

    /** 清空全部归档通知（不可恢复，UI 需确认对话框）。 */
    fun clearArchive() {
        if (bulkBusy()) return
        _uiState.update { (it as NotificationsUiState.Success).copy(bulkBusy = true) }
        runBulkAction { repository.deleteArchivedNotifications() }
    }

    private fun runSingleAction(
        item: NotificationItem,
        action: suspend () -> NotificationOverviewData,
    ) {
        val state = _uiState.value
        if (state !is NotificationsUiState.Success || item.id in state.busyIds) return
        _uiState.update {
            (it as NotificationsUiState.Success).copy(busyIds = it.busyIds + item.id)
        }
        viewModelScope.launch {
            try {
                notificationCenter.updateOverview(action())
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _uiState.update {
                    (it as? NotificationsUiState.Success)?.copy(
                        busyIds = it.busyIds - item.id,
                        transientError = e.message ?: "操作失败",
                    ) ?: it
                }
            }
        }
    }

    private fun runBulkAction(action: suspend () -> NotificationOverviewData) {
        viewModelScope.launch {
            try {
                notificationCenter.updateOverview(action())
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _uiState.update {
                    (it as? NotificationsUiState.Success)?.copy(
                        bulkBusy = false,
                        transientError = e.message ?: "操作失败",
                    ) ?: it
                }
            }
        }
    }

    private fun bulkBusy(): Boolean =
        (_uiState.value as? NotificationsUiState.Success)?.bulkBusy == true

    private fun load() {
        if (sessionManager.activeServer.value == null) {
            _uiState.value = NotificationsUiState.NoServer
            return
        }
        viewModelScope.launch {
            try {
                val items = repository.fetchNotifications(
                    isUnread = !_showArchive.value,
                    importance = _filter.value,
                    offset = 0,
                    limit = 200,
                )
                _uiState.value = NotificationsUiState.Success(items = items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _uiState.value = when (val current = _uiState.value) {
                    is NotificationsUiState.Success ->
                        current.copy(isRefreshing = false, transientError = e.message ?: "加载失败")
                    else -> NotificationsUiState.Error(e.message ?: "未知错误") { load() }
                }
            }
        }
    }
}
