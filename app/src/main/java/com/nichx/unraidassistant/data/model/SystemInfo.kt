package com.nichx.unraidassistant.data.model

enum class ServerStatusEnum { ONLINE, OFFLINE, NEVER_CONNECTED }
enum class TemperatureStatusEnum { NORMAL, WARNING, CRITICAL, UNKNOWN }

/**
 * Dashboard 聚合数据。单次 GetDashboard Query 的映射结果，覆盖服务器身份、
 * 系统/CPU 信息、实时 CPU/内存/温度指标、未读通知计数。
 */
data class DashboardData(
    val serverName: String,
    val serverComment: String?,
    val serverStatus: ServerStatusEnum,
    val lanIp: String,
    val wanIp: String,
    val osVersion: String?,
    val hostname: String?,
    val uptime: String?,
    val kernel: String?,
    val arch: String?,
    val cpuBrand: String?,
    val cpuCores: Int?,
    val cpuThreads: Int?,
    val cpuSpeedGhz: Double?,
    val systemManufacturer: String?,
    val systemModel: String?,
    val isVirtual: Boolean?,
    val unraidVersion: String?,
    val apiVersion: String?,
    val kernelVersion: String?,
    val cpuPercent: Double?,
    val memTotalBytes: Long?,
    val memUsedBytes: Long?,
    val memFreeBytes: Long?,
    val memPercent: Double?,
    val tempAverage: Double?,
    val hottestSensorName: String?,
    val hottestTempValue: Double?,
    val hottestTempStatus: TemperatureStatusEnum?,
    val tempWarningCount: Int?,
    val tempCriticalCount: Int?,
    val unreadInfo: Int,
    val unreadWarning: Int,
    val unreadAlert: Int,
    val unreadTotal: Int,
)
