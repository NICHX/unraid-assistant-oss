package com.nichx.unraidassistant.feature.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichx.unraidassistant.data.model.ServerProtocol
import com.nichx.unraidassistant.ui.components.InfoCard
import com.nichx.unraidassistant.ui.components.ObsidianScreenScaffold
import com.nichx.unraidassistant.ui.components.ObsidianSnackbarHost
import com.nichx.unraidassistant.ui.components.ObsidianTextField
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    serverId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddServerViewModel = hiltViewModel(),
) {
    val obsidian = LocalObsidianPalette.current
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val isValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val isEdit = serverId != null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(serverId) {
        serverId?.let(viewModel::load)
    }

    ObsidianScreenScaffold(
        title = if (isEdit) "编辑服务器" else "添加服务器",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Text(
                text = "填写连接信息，保存后即可在 Dashboard 查看服务器状态。",
                style = MaterialTheme.typography.bodySmall,
                color = obsidian.TextSecondary,
            )

            InfoCard(icon = Icons.Filled.Dns, title = "服务器") {
                ObsidianTextField(
                    value = form.name,
                    onValueChange = viewModel::updateName,
                    label = "名称",
                    placeholder = "例如：家用 NAS",
                    singleLine = true,
                )
                Spacer(Modifier.size(Spacing.xl))
                Text(
                    text = "协议",
                    style = MaterialTheme.typography.labelLarge,
                    color = obsidian.TextSecondary,
                )
                Spacer(Modifier.size(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    listOf(ServerProtocol.HTTPS, ServerProtocol.HTTP).forEach { protocol ->
                        val selected = form.protocol == protocol
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateProtocol(protocol) },
                            label = {
                                Text(
                                    text = protocol.name,
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
                Spacer(Modifier.size(Spacing.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    ObsidianTextField(
                        value = form.host,
                        onValueChange = viewModel::updateHost,
                        label = "主机地址",
                        placeholder = "192.168.1.10 或 nas.example.com",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.weight(1f),
                    )
                    ObsidianTextField(
                        value = form.port,
                        onValueChange = viewModel::updatePort,
                        label = "端口",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                    )
                }
            }

            InfoCard(icon = Icons.Filled.Key, title = "API 密钥") {
                ObsidianTextField(
                    value = form.apiKey,
                    onValueChange = viewModel::updateApiKey,
                    label = "API Key",
                    placeholder = if (isEdit) "留空则不修改" else "粘贴 API Key",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (isEdit) {
                        "留空保存将保持原 Key 不变"
                    } else {
                        "在 unRAID WebGUI → Settings → API Keys 生成"
                    },
                )
            }

            if (form.protocol == ServerProtocol.HTTPS) {
                InfoCard(icon = Icons.Filled.Shield, title = "安全") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "跳过证书校验",
                                style = MaterialTheme.typography.bodyLarge,
                                color = obsidian.TextPrimary,
                            )
                            Spacer(Modifier.size(Spacing.xxs))
                            Text(
                                text = "信任自签名/不受信任的证书（请确认服务器可信）",
                                style = MaterialTheme.typography.bodySmall,
                                color = obsidian.TextSecondary,
                            )
                        }
                        Spacer(Modifier.size(Spacing.xxl))
                        Switch(
                            checked = form.insecureSkipVerify,
                            onCheckedChange = viewModel::updateInsecureSkipVerify,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = obsidian.Cyan,
                                checkedTrackColor = obsidian.Cyan.copy(alpha = 0.4f),
                                uncheckedThumbColor = obsidian.TextSecondary,
                                uncheckedTrackColor = obsidian.Glass,
                                uncheckedBorderColor = obsidian.Border,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.size(Spacing.xs))

            Button(
                onClick = {
                    // 单服务器上限：被拒时以 Snackbar 提示
                    viewModel.save(
                        onSuccess = onSaved,
                        onBlocked = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("保存")
            }
        }
    }
}
