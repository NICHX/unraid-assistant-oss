package com.nichx.unraidassistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichx.unraidassistant.core.datastore.ThemeMode

/**
 * 品牌语义状态色（"控制台"式色板，浅/深色通用）。
 * 运行/警告/停止/告警 分别对应 绿/琥珀/灰蓝/红。
 */
object StatusColors {
    val Running = Color(0xFF2FBF71)
    val Paused = Color(0xFFF2A93B)
    val Stopped = Color(0xFF8A94A0)
    val Alert = Color(0xFFF05A5A)
}

/**
 * 品牌间距 token（Obsidian 设计体系）。
 * 覆盖代码库高频使用的 2/4/6/8/10/12/16/20/24/32dp，页面与组件统一经此取间距。
 * 尺寸类（图标大小、卡片高度等）不属于间距节奏，保持字面值。
 */
object Spacing {
    val xxs = 2.dp    // 行内细缝：标签间距
    val xs = 4.dp     // 微间距：小图标间隔
    val sm = 6.dp     // 图标与文本间隔
    val md = 8.dp     // 次级间距：图标标题行、列表项内距
    val lg = 10.dp    // 列表/网格间隔
    val xl = 12.dp    // 常用间距：分区之间、spacedBy
    val xxl = 16.dp   // 页面边距、卡片内边距
    val xxxl = 20.dp  // 大间距：弹窗内边距
    val xxxxl = 24.dp // 特大间距
    val xxxxxl = 32.dp // 全屏状态页内边距
}

/** 品牌非标圆角（不在标准 Shapes 五级刻度内的常用形状，随主题恒定的几何值）。 */
object ObsidianCorner {
    val skeletonBlock = RoundedCornerShape(8.dp)
    val listItem = RoundedCornerShape(12.dp)
    val banner = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(22.dp)
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(50)
}

/**
 * Obsidian 设计体系色板：玻璃拟态，随主题提供深色/浅色两套实例。
 * 深色版是「深空黑曜石」，浅色版是「白昼玻璃」。
 * 强调色统一使用渐变组（青→靛 / 琥珀→橙 / 绿→青 / 红→橙）。
 */
class ObsidianPalette(
    val isDark: Boolean,
    val Background: Color,
    val Glass: Color,
    val Border: Color,
    val Highlight: Color,
    val Track: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Cyan: Color,
    val Indigo: Color,
    val Violet: Color,
    val Amber: Color,
    val Orange: Color,
    val Green: Color,
    val Red: Color,
    val GlowCyan: Color,
    val GlowIndigo: Color,
    val GlowAmber: Color,
    val MenuContainer: Color,
    val OnAccent: Color,
    val BarContainer: Color,
    val BarIndicator: Color,
)

/** 深色：深空黑曜石。 */
val DarkObsidian = ObsidianPalette(
    isDark = true,
    Background = Color(0xFF05060A),
    Glass = Color(0x14FFFFFF),
    Border = Color(0x1FFFFFFF),
    Highlight = Color(0x12FFFFFF),
    Track = Color(0x1AFFFFFF),
    TextPrimary = Color(0xFFF2F5FA),
    TextSecondary = Color(0xFF8B93A3),
    Cyan = Color(0xFF22D3EE),
    Indigo = Color(0xFF6366F1),
    Violet = Color(0xFFA78BFA),
    Amber = Color(0xFFF59E0B),
    Orange = Color(0xFFF97316),
    Green = Color(0xFF34D399),
    Red = Color(0xFFF87171),
    GlowCyan = Color(0x2422D3EE),
    GlowIndigo = Color(0x216366F1),
    GlowAmber = Color(0x12F59E0B),
    MenuContainer = Color(0xFF10131C),
    OnAccent = Color(0xFF00332B),
    BarContainer = Color(0xFF1C1C2E),
    BarIndicator = Color(0x1AFFFFFF),
)

/** 浅色：白昼玻璃（白色玻璃卡 + 深墨色文字 + 加深的强调色保证对比度）。 */
val LightObsidian = ObsidianPalette(
    isDark = false,
    Background = Color(0xFFF4F6FB),
    Glass = Color(0xCCFFFFFF),
    Border = Color(0x260F172A),
    Highlight = Color(0x66FFFFFF),
    Track = Color(0x140F172A),
    TextPrimary = Color(0xFF1B2233),
    TextSecondary = Color(0xFF6B7488),
    Cyan = Color(0xFF0891B2),
    Indigo = Color(0xFF4F46E5),
    Violet = Color(0xFF7C3AED),
    Amber = Color(0xFFD97706),
    Orange = Color(0xFFEA580C),
    Green = Color(0xFF16A34A),
    Red = Color(0xFFDC2626),
    GlowCyan = Color(0x1A0891B2),
    GlowIndigo = Color(0x174F46E5),
    GlowAmber = Color(0x0DD97706),
    MenuContainer = Color(0xFFFFFFFF),
    OnAccent = Color(0xFF00332B),
    BarContainer = Color(0xFFFFFFFF),
    BarIndicator = Color(0xFFE8E8E8),
)

/** 当前主题下的 Obsidian 色板，由 [UnraidAssistantTheme] 注入。 */
val LocalObsidianPalette = staticCompositionLocalOf { DarkObsidian }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B7A6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA9F0E1),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF0B7A6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8EEE9),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF1F6FB0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E8FF),
    onTertiaryContainer = Color(0xFF001E33),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFBFCFD),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE3E9EE),
    onSurfaceVariant = Color(0xFF454C54),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3F5),
    surfaceContainer = Color(0xFFF4F7F9),
    surfaceContainerHigh = Color(0xFFEDF1F4),
    surfaceContainerHighest = Color(0xFFE7ECEF),
    outline = Color(0xFF73777E),
    outlineVariant = Color(0xFFC2C7CC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF35D1BB),
    onPrimary = Color(0xFF00332B),
    primaryContainer = Color(0xFF0B4A40),
    onPrimaryContainer = Color(0xFFA9F0E1),
    secondary = Color(0xFF2FB8A8),
    onSecondary = Color(0xFF00332B),
    secondaryContainer = Color(0xFF0F4E48),
    onSecondaryContainer = Color(0xFFA9F0E1),
    tertiary = Color(0xFF8CC7FF),
    onTertiary = Color(0xFF003355),
    tertiaryContainer = Color(0xFF0A4A7A),
    onTertiaryContainer = Color(0xFFD3E8FF),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE2E8EF),
    surface = Color(0xFF12171E),
    onSurface = Color(0xFFE2E8EF),
    surfaceVariant = Color(0xFF1B232D),
    onSurfaceVariant = Color(0xFFA4AFBB),
    surfaceContainerLowest = Color(0xFF070A0E),
    surfaceContainerLow = Color(0xFF0E131A),
    surfaceContainer = Color(0xFF12171E),
    surfaceContainerHigh = Color(0xFF1A212B),
    surfaceContainerHighest = Color(0xFF202833),
    outline = Color(0xFF47525F),
    outlineVariant = Color(0xFF26313C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** 品牌排版：标题加重加粗，正文沿用系统字体。 */
private val BrandTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

/** 品牌圆角：卡片 14dp / 控件 20dp。 */
private val BrandShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

/**
 * 应用主题。支持浅色/深色/跟随系统；默认关闭动态取色，所有颜色由品牌色板决定，
 * 换壁纸不会改变 App 外观。如需壁纸取色可传 [dynamicColor] = true（Android 12+ 生效）。
 * [themeMode] 由设置页写入，持久化在 DataStore。
 */
@Composable
fun UnraidAssistantTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    // 强调角色（primary/secondary 组）钉为品牌色：默认 Button/OutlinedButton/FilterChip/Switch 等
    // 会继承这些角色，动态取色（暖色壁纸）或静态色板残留的琥珀/土黄不应出现在这些控件上。
    val brandScheme = if (darkTheme) DarkScheme else LightScheme
    val colorScheme = baseScheme.copy(
        primary = brandScheme.primary,
        onPrimary = brandScheme.onPrimary,
        primaryContainer = brandScheme.primaryContainer,
        onPrimaryContainer = brandScheme.onPrimaryContainer,
        secondary = brandScheme.secondary,
        onSecondary = brandScheme.onSecondary,
        secondaryContainer = brandScheme.secondaryContainer,
        onSecondaryContainer = brandScheme.onSecondaryContainer,
    )
    CompositionLocalProvider(
        LocalObsidianPalette provides (if (darkTheme) DarkObsidian else LightObsidian),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BrandTypography,
            shapes = BrandShapes,
            content = content,
        )
    }
}
