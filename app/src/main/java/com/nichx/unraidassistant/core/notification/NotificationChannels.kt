package com.nichx.unraidassistant.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知渠道定义。unRAID 通知按严重级别分流：
 * - [ALERT_CHANNEL_ID]：警告与告警（高优先级，默认提醒）
 * - [INFO_CHANNEL_ID]：普通信息（默认优先级）
 * - [FOREGROUND_CHANNEL_ID]：前台服务常驻提示（低优先级，静默不打扰）
 * 渠道在 App 首次注入时创建一次，用户可在系统设置中单独调整各渠道行为。
 */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val ALERT_CHANNEL_ID = "unraid_alerts"
        const val INFO_CHANNEL_ID = "unraid_info"
        const val FOREGROUND_CHANNEL_ID = "unraid_foreground"
    }

    fun create() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "unRAID 告警",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "服务器警告与告警级别通知"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                INFO_CHANNEL_ID,
                "unRAID 信息",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "服务器普通信息通知"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "订阅保活",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "实时通道运行时显示的常驻状态提示"
            }
        )
    }
}
