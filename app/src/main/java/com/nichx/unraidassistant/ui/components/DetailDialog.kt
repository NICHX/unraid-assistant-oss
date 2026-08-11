package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.Spacing

/**
 * Obsidian 详情弹窗：承载卡片放不下的完整信息与二级操作。
 * 与 [ConfirmDialog] 同风格（圆角深色玻璃容器），内容可滚动。
 * 敏感操作不直接置于一级页面，统一收敛进此类二级弹窗，避免误触。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val obsidian = LocalObsidianPalette.current
    val shape = MaterialTheme.shapes.extraLarge
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(obsidian.Background, shape)
                .border(1.dp, obsidian.Border, shape)
                .padding(Spacing.xxxl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = obsidian.TextPrimary,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.size(Spacing.xxs))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = obsidian.TextSecondary,
                            maxLines = 2,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = obsidian.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        }
    }
}

/** 详情弹窗内的信息行：等宽值 + 可选强调色。 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color? = null,
    monospace: Boolean = true,
) {
    val obsidian = LocalObsidianPalette.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = obsidian.TextSecondary,
        )
        Spacer(Modifier.width(Spacing.xl))
        if (monospace) {
            MonoValue(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: obsidian.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.size(Spacing.sm))
}
