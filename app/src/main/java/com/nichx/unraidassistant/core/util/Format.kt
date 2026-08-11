package com.nichx.unraidassistant.core.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * 容量 / 温度 / 时长等通用格式化工具。无状态，纯函数。
 */
object Format {

    private val BYTE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    fun bytes(bytes: Long, decimals: Int = 1): String {
        if (bytes <= 0L) return "0 B"
        val size = bytes.toDouble()
        val exp = (ln(size) / ln(1024.0)).toInt().coerceIn(0, BYTE_UNITS.lastIndex)
        val value = size / 1024.0.pow(exp.toDouble())
        return String.format(Locale.ROOT, "%.${decimals}f %s", value, BYTE_UNITS[exp])
    }

    fun temperature(celsius: Double): String =
        String.format(Locale.ROOT, "%.0f°C", celsius)

    /** 秒级时长 → "1d 02h 15m" 形式。 */
    fun uptime(seconds: Long): String {
        if (seconds <= 0L) return "—"
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        return buildString {
            if (d > 0) append("${d}d ")
            if (h > 0 || d > 0) append("${h.toString().padStart(2, '0')}h ")
            append("${m}m")
        }.trim()
    }

    fun percentage(value: Double, decimals: Int = 1): String =
        String.format(Locale.ROOT, "%.${decimals}f%%", value)

    fun ratio(numerator: Long, denominator: Long): String {
        if (denominator <= 0L) return "—"
        return "${bytes(numerator)} / ${bytes(denominator)}"
    }

    /** epoch 秒 → "yyyy-MM-dd HH:mm" 本地时间。 */
    fun epochSeconds(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "—"
        val instant = java.time.Instant.ofEpochSecond(epochSeconds)
        val time = java.time.LocalDateTime.ofInstant(
            instant,
            java.time.ZoneId.systemDefault(),
        )
        return time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}
