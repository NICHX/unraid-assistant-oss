package com.nichx.unraidassistant.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.data.model.NotificationImportance
import com.nichx.unraidassistant.data.model.NotificationItem
import com.nichx.unraidassistant.ui.components.ConfirmDialog
import com.nichx.unraidassistant.ui.components.ErrorBanner
import com.nichx.unraidassistant.ui.components.ErrorState
import com.nichx.unraidassistant.ui.components.FullPageState
import com.nichx.unraidassistant.ui.components.GlassCard
import com.nichx.unraidassistant.ui.components.GlowDot
import com.nichx.unraidassistant.ui.components.LoadingSkeleton
import com.nichx.unraidassistant.ui.components.NoServerState
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * 通知列表页：未读/归档双视图，支持按级别筛选、下拉刷新与增删操作。
 * - 未读视图：单条「标记已读 / 删除」，顶栏「全部已读」；
 * - 归档视图：单条「标记未读 / 删除」，顶栏「清空归档」（确认后执行）；
 * - 删除与清空归档不可恢复，统一走 [ConfirmDialog] 二次确认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenWebView: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val showArchive by viewModel.showArchive.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<NotificationItem?>(null) }
    var confirmClearArchive by remember { mutableStateOf(false) }

    ObsidianScreenScaffold(
        title = "通知",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = obsidian.TextSecondary,
                )
            }
        },
        actions = {
            val success = uiState as? NotificationsUiState.Success
            val bulkEnabled = success != null &&
                !success.bulkBusy &&
                success.items.isNotEmpty()
            if (showArchive) {
                IconButton(
                    onClick = { confirmClearArchive = true },
                    enabled = bulkEnabled,
                ) {
                    if (success?.bulkBusy == true) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = obsidian.Cyan,
                        )
                    } else {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "清空归档",
                            tint = obsidian.TextSecondary,
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = viewModel::archiveAll,
                    enabled = bulkEnabled,
                ) {
                    if (success?.bulkBusy == true) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = obsidian.Cyan,
                        )
                    } else {
                        Icon(
                            Icons.Filled.DoneAll,
                            contentDescription = "全部已读",
                            tint = obsidian.TextSecondary,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TypeToggleRow(showArchive = showArchive, onShowArchive = viewModel::setShowArchive)
            FilterChipsRow(filter = filter, onFilterChange = viewModel::setFilter)
            when (val state = uiState) {
                NotificationsUiState.Loading ->
                    LoadingSkeleton(
                        modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.md),
                        itemCount = 5,
                    )
                NotificationsUiState.NoServer -> NoServerState()
                is NotificationsUiState.Error ->
                    ErrorState(message = state.message, onRetry = state.retry)
                is NotificationsUiState.Success -> {
                    if (state.transientError != null) {
                        ErrorBanner(
                            message = state.transientError,
                            onDismiss = viewModel::clearTransientError,
                            modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xs),
                        )
                    }
                    if (state.items.isEmpty()) {
                        FullPageState(
                            icon = Icons.Filled.NotificationsNone,
                            iconTint = obsidian.TextSecondary,
                            title = if (showArchive) "暂无归档通知" else "暂无未读通知",
                            subtitle = "下拉刷新，或切换到其他视图查看",
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = viewModel::refresh,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(Spacing.xxl),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                            ) {
                                items(state.items, key = { it.id }) { item ->
                                    NotificationListItem(
                                        item = item,
                                        busy = item.id in state.busyIds,
                                        onOpen = onOpenWebView,
                                        onMarkRead = viewModel::markRead,
                                        onMarkUnread = viewModel::markUnread,
                                        onDelete = { deleteTarget = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "删除通知",
            message = "确定删除这条通知吗？此操作不可恢复。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                viewModel.delete(item)
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (confirmClearArchive) {
        ConfirmDialog(
            title = "清空归档",
            message = "将删除全部已归档通知，此操作不可恢复。",
            confirmText = "清空",
            danger = true,
            onConfirm = {
                confirmClearArchive = false
                viewModel.clearArchive()
            },
            onDismiss = { confirmClearArchive = false },
        )
    }
}

/** 未读 / 归档视图切换行。 */
@Composable
private fun TypeToggleRow(
    showArchive: Boolean,
    onShowArchive: (Boolean) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val options = listOf(false to "未读", true to "归档")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        options.forEach { (archive, label) ->
            val selected = showArchive == archive
            FilterChip(
                selected = selected,
                onClick = { onShowArchive(archive) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) obsidian.TextPrimary else obsidian.TextSecondary,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = obsidian.TextSecondary,
                    selectedContainerColor = obsidian.Cyan.copy(alpha = 0.14f),
                    selectedLabelColor = obsidian.TextPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = obsidian.Border,
                    selectedBorderColor = obsidian.Cyan.copy(alpha = 0.40f),
                ),
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    filter: NotificationImportance?,
    onFilterChange: (NotificationImportance?) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val chips = listOf(
        null to "全部",
        NotificationImportance.INFO to "信息",
        NotificationImportance.WARNING to "警告",
        NotificationImportance.ALERT to "告警",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        chips.forEach { (importance, label) ->
            val selected = filter == importance
            val accent = importance?.let { it.accent(obsidian) } ?: obsidian.TextSecondary
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(importance) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) obsidian.TextPrimary else obsidian.TextSecondary,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = obsidian.TextSecondary,
                    selectedContainerColor = accent.copy(alpha = 0.14f),
                    selectedLabelColor = obsidian.TextPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = obsidian.Border,
                    selectedBorderColor = accent.copy(alpha = 0.40f),
                ),
            )
        }
    }
}

@Composable
private fun NotificationListItem(
    item: NotificationItem,
    busy: Boolean,
    onOpen: (String) -> Unit,
    onMarkRead: (NotificationItem) -> Unit,
    onMarkUnread: (NotificationItem) -> Unit,
    onDelete: (NotificationItem) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val accent = item.importance.accent(obsidian)
    val mainTitle = item.subject.ifBlank { item.title }
    val secondary = item.title.ifBlank { item.description }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (item.link != null) it.clickable { onOpen(item.link) } else it },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            GlowDot(color = accent, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.width(Spacing.xl))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mainTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = obsidian.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.link != null) {
                        Spacer(Modifier.width(Spacing.sm))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "点击打开链接",
                            tint = obsidian.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                if (secondary.isNotBlank() && secondary != mainTitle) {
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = obsidian.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!item.timestamp.isNullOrBlank()) {
                    Spacer(Modifier.size(Spacing.sm))
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = obsidian.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.xs))
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = Spacing.md)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                    color = obsidian.Cyan,
                )
            } else {
                ItemActionMenu(
                    item = item,
                    onMarkRead = onMarkRead,
                    onMarkUnread = onMarkUnread,
                    onDelete = onDelete,
                )
            }
        }
    }
}

/** 条目操作菜单：未读条目提供「标记已读」，归档条目提供「标记未读」，均含「删除」。 */
@Composable
private fun ItemActionMenu(
    item: NotificationItem,
    onMarkRead: (NotificationItem) -> Unit,
    onMarkUnread: (NotificationItem) -> Unit,
    onDelete: (NotificationItem) -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "操作",
                tint = obsidian.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = obsidian.MenuContainer,
        ) {
            if (item.isUnread) {
                DropdownMenuItem(
                    text = { Text("标记已读", color = obsidian.TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = obsidian.Cyan,
                        )
                    },
                    onClick = {
                        expanded = false
                        onMarkRead(item)
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("标记未读", color = obsidian.TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = null,
                            tint = obsidian.Amber,
                        )
                    },
                    onClick = {
                        expanded = false
                        onMarkUnread(item)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("删除", color = obsidian.Red) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = obsidian.Red,
                    )
                },
                onClick = {
                    expanded = false
                    onDelete(item)
                },
            )
        }
    }
}

private fun NotificationImportance.accent(obsidian: ObsidianPalette): Color = when (this) {
    NotificationImportance.INFO -> obsidian.Cyan
    NotificationImportance.WARNING -> obsidian.Amber
    NotificationImportance.ALERT -> obsidian.Red
}
