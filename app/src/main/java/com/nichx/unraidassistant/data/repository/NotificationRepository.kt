package com.nichx.unraidassistant.data.repository

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import com.nichx.unraidassistant.core.util.ApiException
import com.nichx.unraidassistant.data.model.NotificationCounts
import com.nichx.unraidassistant.data.model.NotificationImportance
import com.nichx.unraidassistant.data.model.NotificationItem
import com.nichx.unraidassistant.data.model.NotificationOverviewData
import com.nichx.unraidassistant.data.remote.graphql.ArchiveAllNotificationsMutation
import com.nichx.unraidassistant.data.remote.graphql.ArchiveNotificationsMutation
import com.nichx.unraidassistant.data.remote.graphql.DeleteArchivedNotificationsMutation
import com.nichx.unraidassistant.data.remote.graphql.DeleteNotificationMutation
import com.nichx.unraidassistant.data.remote.graphql.GetNotificationsOverviewQuery
import com.nichx.unraidassistant.data.remote.graphql.GetNotificationsQuery
import com.nichx.unraidassistant.data.remote.graphql.NotificationAddedSubscription
import com.nichx.unraidassistant.data.remote.graphql.NotificationsOverviewSubscription
import com.nichx.unraidassistant.data.remote.graphql.UnarchiveNotificationsMutation
import com.nichx.unraidassistant.data.remote.graphql.type.NotificationFilter
import com.nichx.unraidassistant.data.remote.graphql.type.NotificationImportance as GqlImportance
import com.nichx.unraidassistant.data.remote.graphql.type.NotificationType as GqlType
import com.nichx.unraidassistant.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NotificationRepository {

    /**
     * 实时订阅新通知（WebSocket）。发射每个通知事件对应的 [NotificationItem]。
     * 流在订阅终止或连接断开时会以异常完成，由调用方负责重连与降级。
     */
    fun observeNotificationAdded(): Flow<NotificationItem>

    /** 实时订阅通知概览计数（WebSocket）。 */
    fun observeNotificationsOverview(): Flow<NotificationOverviewData>

    /** 单次查询通知概览计数（降级轮询通道）。 */
    suspend fun fetchNotificationsOverview(): NotificationOverviewData

    /**
     * 分页拉取通知列表。[isUnread] 决定取未读还是归档；[importance] 为 null 时不过滤级别，
     * 返回按服务端排序的最新通知，列表页用于展示与下拉刷新。
     */
    suspend fun fetchNotifications(
        isUnread: Boolean,
        importance: NotificationImportance?,
        offset: Int,
        limit: Int,
    ): List<NotificationItem>

    /** 批量归档（标记已读）。返回操作后的概览计数。 */
    suspend fun archiveNotifications(ids: List<String>): NotificationOverviewData

    /** 批量取消归档（标记未读）。返回操作后的概览计数。 */
    suspend fun unarchiveNotifications(ids: List<String>): NotificationOverviewData

    /** 全部标记已读（可按重要性限定，null = 全部级别）。 */
    suspend fun archiveAll(importance: NotificationImportance?): NotificationOverviewData

    /** 删除单条通知。 */
    suspend fun deleteNotification(id: String, isUnread: Boolean): NotificationOverviewData

    /** 清空全部归档通知。 */
    suspend fun deleteArchivedNotifications(): NotificationOverviewData
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : NotificationRepository {

    override fun observeNotificationAdded(): Flow<NotificationItem> =
        sessionManager.apolloClient()
            .subscription(NotificationAddedSubscription())
            .toFlow()
            .map { response ->
                val data = response.data ?: throw response.exception.toApiException()
                data.notificationAdded.toDomain()
            }

    override fun observeNotificationsOverview(): Flow<NotificationOverviewData> =
        sessionManager.apolloClient()
            .subscription(NotificationsOverviewSubscription())
            .toFlow()
            .map { response ->
                val data = response.data ?: throw response.exception.toApiException()
                data.notificationsOverview.toDomain()
            }

    override suspend fun fetchNotificationsOverview(): NotificationOverviewData {
        val response = try {
            sessionManager.apolloClient().query(GetNotificationsOverviewQuery()).execute()
        } catch (e: ApolloException) {
            throw ApiException.NetworkUnreachable
        }
        if (response.hasErrors()) {
            throw ApiException.GraphQLError(response.errors?.map { it.message } ?: emptyList())
        }
        val overview = response.data?.notifications?.overview
            ?: throw ApiException.ServerError(500)
        return overview.toDomain()
    }

    override suspend fun fetchNotifications(
        isUnread: Boolean,
        importance: NotificationImportance?,
        offset: Int,
        limit: Int,
    ): List<NotificationItem> {
        val response = try {
            sessionManager.apolloClient().query(
                GetNotificationsQuery(
                    filter = NotificationFilter(
                        importance = Optional.presentIfNotNull(importance?.toGql()),
                        type = if (isUnread) GqlType.UNREAD else GqlType.ARCHIVE,
                        offset = offset,
                        limit = limit,
                    ),
                ),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        if (response.hasErrors()) {
            throw ApiException.GraphQLError(response.errors?.map { it.message } ?: emptyList())
        }
        return response.data?.notifications?.list?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun archiveNotifications(ids: List<String>): NotificationOverviewData {
        if (ids.isEmpty()) return NotificationOverviewData()
        val response = try {
            sessionManager.apolloClient().mutation(
                ArchiveNotificationsMutation(ids = ids),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        return response.data?.archiveNotifications?.toOverview() ?: throw response.exception.toApiException()
    }

    override suspend fun unarchiveNotifications(ids: List<String>): NotificationOverviewData {
        if (ids.isEmpty()) return NotificationOverviewData()
        val response = try {
            sessionManager.apolloClient().mutation(
                UnarchiveNotificationsMutation(ids = ids),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        return response.data?.unarchiveNotifications?.toOverview() ?: throw response.exception.toApiException()
    }

    override suspend fun archiveAll(importance: NotificationImportance?): NotificationOverviewData {
        val response = try {
            sessionManager.apolloClient().mutation(
                ArchiveAllNotificationsMutation(importance = Optional.presentIfNotNull(importance?.toGql())),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        return response.data?.archiveAll?.toOverview() ?: throw response.exception.toApiException()
    }

    override suspend fun deleteNotification(id: String, isUnread: Boolean): NotificationOverviewData {
        val response = try {
            sessionManager.apolloClient().mutation(
                DeleteNotificationMutation(
                    id = id,
                    type = if (isUnread) GqlType.UNREAD else GqlType.ARCHIVE,
                ),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        return response.data?.deleteNotification?.toOverview() ?: throw response.exception.toApiException()
    }

    override suspend fun deleteArchivedNotifications(): NotificationOverviewData {
        val response = try {
            sessionManager.apolloClient().mutation(
                DeleteArchivedNotificationsMutation(),
            ).execute()
        } catch (e: ApolloException) {
            throw e.toApiException()
        }
        return response.data?.deleteArchivedNotifications?.toOverview() ?: throw response.exception.toApiException()
    }

    private fun NotificationAddedSubscription.NotificationAdded.toDomain(): NotificationItem =
        NotificationItem(
            id = id,
            title = title,
            subject = subject,
            description = description,
            importance = importance.toDomain(),
            link = link,
            isUnread = type == GqlType.UNREAD,
            timestamp = timestamp,
        )

    private fun GetNotificationsQuery.List.toDomain(): NotificationItem =
        NotificationItem(
            id = id,
            title = title,
            subject = subject,
            description = description,
            importance = importance.toDomain(),
            link = link,
            isUnread = type == GqlType.UNREAD,
            timestamp = formattedTimestamp ?: timestamp,
        )

    private fun NotificationImportance.toGql(): GqlImportance = when (this) {
        NotificationImportance.ALERT -> GqlImportance.ALERT
        NotificationImportance.WARNING -> GqlImportance.WARNING
        NotificationImportance.INFO -> GqlImportance.INFO
    }

    private fun NotificationsOverviewSubscription.NotificationsOverview.toDomain(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toDomain(),
            archive = archive.toDomain(),
        )

    private fun GetNotificationsOverviewQuery.Overview.toDomain(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toDomain(),
            archive = archive.toDomain(),
        )

    private fun GqlImportance.toDomain(): NotificationImportance = when (this) {
        GqlImportance.ALERT -> NotificationImportance.ALERT
        GqlImportance.WARNING -> NotificationImportance.WARNING
        GqlImportance.INFO -> NotificationImportance.INFO
        GqlImportance.UNKNOWN__ -> NotificationImportance.INFO
    }

    private fun NotificationsOverviewSubscription.Unread.toDomain(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun NotificationsOverviewSubscription.Archive.toDomain(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun GetNotificationsOverviewQuery.Unread.toDomain(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun GetNotificationsOverviewQuery.Archive.toDomain(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun ArchiveNotificationsMutation.ArchiveNotifications.toOverview(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toCounts(),
            archive = archive.toCounts(),
        )

    private fun ArchiveNotificationsMutation.Unread.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun ArchiveNotificationsMutation.Archive.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun UnarchiveNotificationsMutation.UnarchiveNotifications.toOverview(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toCounts(),
            archive = archive.toCounts(),
        )

    private fun UnarchiveNotificationsMutation.Unread.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun UnarchiveNotificationsMutation.Archive.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun ArchiveAllNotificationsMutation.ArchiveAll.toOverview(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toCounts(),
            archive = archive.toCounts(),
        )

    private fun ArchiveAllNotificationsMutation.Unread.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun ArchiveAllNotificationsMutation.Archive.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun DeleteNotificationMutation.DeleteNotification.toOverview(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toCounts(),
            archive = archive.toCounts(),
        )

    private fun DeleteNotificationMutation.Unread.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun DeleteNotificationMutation.Archive.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun DeleteArchivedNotificationsMutation.DeleteArchivedNotifications.toOverview(): NotificationOverviewData =
        NotificationOverviewData(
            unread = unread.toCounts(),
            archive = archive.toCounts(),
        )

    private fun DeleteArchivedNotificationsMutation.Unread.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun DeleteArchivedNotificationsMutation.Archive.toCounts(): NotificationCounts =
        NotificationCounts(info = info, warning = warning, alert = alert, total = total)

    private fun Throwable?.toApiException(): ApiException = when (this) {
        is ApiException -> this
        // 保留 Apollo 原始错误信息（如 HTTP 401、连接被拒、GraphQL 拒绝原因），供设置页诊断展示
        is ApolloException -> ApiException.NetworkError(message ?: "网络不可达")
        null -> ApiException.GraphQLError(listOf("订阅数据为空"))
        else -> ApiException.GraphQLError(listOf(message ?: "未知订阅错误"))
    }
}
