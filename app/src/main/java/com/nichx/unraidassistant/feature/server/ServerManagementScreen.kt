package com.nichx.unraidassistant.feature.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.data.model.ServerConfig
import com.nichx.unraidassistant.ui.components.ConfirmDialog
import com.nichx.unraidassistant.ui.components.FullPageState
import com.nichx.unraidassistant.ui.components.GlassCard
import com.nichx.unraidassistant.ui.components.LoadingSkeleton
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.ObsidianSnackbarHost
import com.nichx.unraidassistant.ui.components.StatusPill
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import com.nichx.unraidassistant.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagementScreen(
    onBack: () -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (String) -> Unit,
    viewModel: ServerManagementViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val switchError by viewModel.switchError.collectAsStateWithLifecycle()
    val canAddServer by viewModel.canAddServer.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ServerConfig?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(switchError) {
        if (switchError != null) {
            snackbarHostState.showSnackbar(switchError!!)
            viewModel.consumeSwitchError()
        }
    }

    ObsidianScreenScaffold(
        title = "服务器管理",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = obsidian.TextPrimary,
                )
            }
        },
        snackbarHost = { ObsidianSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 单服务器上限：已有服务器时提示，不可新增
                    if (canAddServer) onAddServer() else showLimitDialog = true
                },
                containerColor = obsidian.Indigo,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加服务器")
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is ServerManagementUiState.Loading -> LoadingSkeleton(
                Modifier
                    .padding(padding)
                    .padding(Spacing.xxl),
            )
            is ServerManagementUiState.Empty -> FullPageState(
                icon = Icons.Filled.Dns,
                iconTint = obsidian.TextSecondary,
                title = "还没有服务器",
                subtitle = "点击右下角按钮添加第一台 Unraid 服务器",
                modifier = Modifier.padding(padding),
            )
            is ServerManagementUiState.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                items(state.servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        isActive = server.id == state.activeServerId,
                        onActivate = { viewModel.activate(server) },
                        onEdit = { onEditServer(server.id) },
                        onDelete = { deleteTarget = server },
                    )
                }
            }
        }
    }

    deleteTarget?.let { server ->
        ConfirmDialog(
            title = "删除服务器",
            message = "确定删除「${server.name}」吗？此操作不可撤销。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                viewModel.delete(server)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            containerColor = obsidian.Background,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("已到服务器数量上限", color = obsidian.TextPrimary) },
            text = {
                Text(
                    text = "最多可管理 1 台服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = obsidian.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("知道了", color = obsidian.Cyan)
                }
            },
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val iconShape = MaterialTheme.shapes.medium
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(obsidian.Glass, iconShape)
                    .border(1.dp, obsidian.Border, iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = null,
                    tint = if (isActive) obsidian.Cyan else obsidian.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(Spacing.xl))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = obsidian.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isActive) {
                        Spacer(Modifier.size(Spacing.md))
                        StatusPill("当前", StatusColors.Running)
                    }
                }
                Spacer(Modifier.size(Spacing.xxs))
                Text(
                    text = "${server.protocol.value}://${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = obsidian.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(Spacing.xs))
            IconButton(onClick = onActivate) {
                Icon(
                    imageVector = Icons.Filled.WifiTethering,
                    contentDescription = "激活",
                    tint = if (isActive) obsidian.Cyan else obsidian.TextSecondary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.ModeEdit,
                    contentDescription = "编辑",
                    tint = obsidian.TextSecondary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "删除",
                    tint = obsidian.TextSecondary,
                )
            }
        }
    }
}
