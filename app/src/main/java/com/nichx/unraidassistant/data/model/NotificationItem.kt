package com.nichx.unraidassistant.data.model

/**
 * unRAID 通知事件。由 WebSocket 订阅 `notificationAdded` 实时推送，
 * 或按需从通知列表 Query 拉取。
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val importance: NotificationImportance,
    val link: String?,
    val isUnread: Boolean,
    val timestamp: String?,
)

enum class NotificationImportance { ALERT, INFO, WARNING }

/** 通知分级计数（信息 / 警告 / 告警 / 总计）。 */
data class NotificationCounts(
    val info: Int = 0,
    val warning: Int = 0,
    val alert: Int = 0,
    val total: Int = 0,
)

/**
 * 通知概览快照。[unread] 为未读计数、[archive] 为已归档计数，
 * 来自 `notifications.overview`（订阅实时推送或降级轮询 Query）。
 */
data class NotificationOverviewData(
    val unread: NotificationCounts = NotificationCounts(),
    val archive: NotificationCounts = NotificationCounts(),
)
