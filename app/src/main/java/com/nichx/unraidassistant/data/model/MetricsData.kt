package com.nichx.unraidassistant.data.model

/**
 * 实时指标序列中的单个采样点。
 * [timestamp] 为采样点到达客户端的时刻（epoch millis），用于曲线 x 轴。
 */
data class SamplePoint(
    val timestamp: Long,
    val value: Double,
)

/**
 * Dashboard 实时曲线数据：CPU / 内存 / 温度三条独立时间序列。
 * 各序列按字段独立追加，同一时刻可能仅某一字段有新采样（多根字段订阅）。
 */
data class MetricsData(
    val cpu: List<SamplePoint> = emptyList(),
    val memory: List<SamplePoint> = emptyList(),
    val temperature: List<SamplePoint> = emptyList(),
) {
    val latestCpu: Double? get() = cpu.lastOrNull()?.value
    val latestMemory: Double? get() = memory.lastOrNull()?.value
    val latestTemperature: Double? get() = temperature.lastOrNull()?.value
}

/**
 * 实时订阅推送的单个事件。多根字段订阅中，服务端按字段分别推送，
 * 每次事件通常仅一个字段非空，其余为 null。
 */
data class MetricSample(
    val timestamp: Long,
    val cpuPercent: Double? = null,
    val memTotalBytes: Long? = null,
    val memUsedBytes: Long? = null,
    val memPercent: Double? = null,
    val tempCelsius: Double? = null,
)
