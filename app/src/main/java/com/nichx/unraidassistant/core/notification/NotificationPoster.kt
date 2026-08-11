package com.nichx.unraidassistant.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nichx.unraidassistant.MainActivity
import com.nichx.unraidassistant.R
import com.nichx.unraidassistant.core.datastore.SettingsDataStore
import com.nichx.unraidassistant.data.model.NotificationImportance
import com.nichx.unraidassistant.data.model.NotificationItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 将 unRAID 通知事件投递为 Android 系统通知。
 *
 * - 按严重级别分流到 [NotificationChannels.ALERT_CHANNEL_ID]（警告/告警）与
 *   [NotificationChannels.INFO_CHANNEL_ID]（信息）；
 * - 通知总开关关闭或处于免打扰时段时静默丢弃；
 * - 点击通知通过 [EXTRA_OPEN_NOTIFICATIONS] 深链打开通知列表页。
 */
@Singleton
class NotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val notificationChannels: NotificationChannels,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        /** 点击系统通知跳转通知列表页的 Intent extra（MainActivity/UnraidNavHost 消费）。 */
        const val EXTRA_OPEN_NOTIFICATIONS = "unraid_assistant_open_notifications"
    }

    init {
        notificationChannels.create()
    }

    /** 是否处于免打扰时段：按当前时刻分钟数与 start/end 比较，支持跨午夜区间。 */
    suspend fun isInDnd(): Boolean {
        if (!settingsDataStore.dndEnabled.first()) return false
        val start = settingsDataStore.dndStartMinutes.first()
        val end = settingsDataStore.dndEndMinutes.first()
        if (start == end) return false
        val now = LocalTime.now().hour * 60 + LocalTime.now().minute
        return if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }

    /** 投递通知。总开关关闭 / DND / 无通知权限时返回 false（不推送）。 */
    suspend fun post(item: NotificationItem): Boolean {
        if (!settingsDataStore.notificationsEnabled.first()) return false
        if (isInDnd()) return false
        if (!notificationManager.areNotificationsEnabled()) return false

        val channelId = when (item.importance) {
            NotificationImportance.ALERT, NotificationImportance.WARNING ->
                NotificationChannels.ALERT_CHANNEL_ID
            NotificationImportance.INFO -> NotificationChannels.INFO_CHANNEL_ID
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(item.subject.ifBlank { item.title })
            .setContentText(item.title.ifBlank { item.description })
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.description))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        // 通知 ID 用事件 id 哈希，同一事件重复推送时覆盖而非堆积
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        notificationManager.notify(item.id.hashCode(), notification)
        return true
    }
}
