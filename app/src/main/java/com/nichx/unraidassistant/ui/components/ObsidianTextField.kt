package com.nichx.unraidassistant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.nichx.unraidassistant.ui.theme.LocalObsidianPalette

/**
 * Obsidian 风格输入框：聚焦青边框 + 玻璃底色 + 主题文字色。
 * 统一全 App 文本输入的取色，替换各页面内联的默认 OutlinedTextField 颜色样板。
 */
@Composable
fun ObsidianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    isError: Boolean = false,
    enabled: Boolean = true,
    supportingText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val obsidian = LocalObsidianPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = obsidian.TextPrimary,
            unfocusedTextColor = obsidian.TextPrimary,
            disabledTextColor = obsidian.TextSecondary.copy(alpha = 0.5f),
            focusedBorderColor = obsidian.Cyan,
            unfocusedBorderColor = obsidian.Border,
            disabledBorderColor = obsidian.Border.copy(alpha = 0.4f),
            errorBorderColor = obsidian.Red,
            focusedLabelColor = obsidian.Cyan,
            unfocusedLabelColor = obsidian.TextSecondary,
            disabledLabelColor = obsidian.TextSecondary.copy(alpha = 0.6f),
            errorLabelColor = obsidian.Red,
            cursorColor = obsidian.Cyan,
            focusedSupportingTextColor = obsidian.TextSecondary,
            unfocusedSupportingTextColor = obsidian.TextSecondary,
            disabledSupportingTextColor = obsidian.TextSecondary.copy(alpha = 0.6f),
            errorSupportingTextColor = obsidian.Red,
        ),
    )
}
