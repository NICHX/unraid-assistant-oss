package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette
import com.nichx.unraidassistant.ui.theme.ObsidianCorner

/**
 * Obsidian 风格搜索框：胶囊圆角 + 玻璃底色 + 前置搜索图标 + 一键清空。
 * 统一全 App 搜索入口样式（应用市场、模板列表、图标选择器等），
 * 聚焦时青色描边与图标呼应品牌强调色。
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    onClear: (() -> Unit)? = null,
) {
    val obsidian = LocalObsidianPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        shape = ObsidianCorner.pill,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onClear?.invoke() ?: onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "清空搜索",
                    )
                }
            }
        } else {
            null
        },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = obsidian.TextPrimary,
            unfocusedTextColor = obsidian.TextPrimary,
            disabledTextColor = obsidian.TextSecondary.copy(alpha = 0.5f),
            focusedContainerColor = obsidian.Glass,
            unfocusedContainerColor = obsidian.Glass,
            disabledContainerColor = obsidian.Glass.copy(alpha = 0.5f),
            focusedBorderColor = obsidian.Cyan,
            unfocusedBorderColor = obsidian.Border,
            disabledBorderColor = obsidian.Border.copy(alpha = 0.4f),
            focusedLabelColor = obsidian.Cyan,
            unfocusedLabelColor = obsidian.TextSecondary,
            disabledLabelColor = obsidian.TextSecondary.copy(alpha = 0.6f),
            cursorColor = obsidian.Cyan,
            focusedLeadingIconColor = obsidian.Cyan,
            unfocusedLeadingIconColor = obsidian.TextSecondary,
            focusedTrailingIconColor = obsidian.TextSecondary,
            unfocusedTrailingIconColor = obsidian.TextSecondary,
            focusedPlaceholderColor = obsidian.TextSecondary,
            unfocusedPlaceholderColor = obsidian.TextSecondary,
            focusedSupportingTextColor = obsidian.TextSecondary,
            unfocusedSupportingTextColor = obsidian.TextSecondary,
        ),
    )
}
