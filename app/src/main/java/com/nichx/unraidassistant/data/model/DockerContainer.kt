package com.nichx.unraidassistant.data.model

enum class ContainerStateEnum { RUNNING, PAUSED, EXITED }

data class DockerContainerInfo(
    val id: String,
    val name: String,
    val image: String,
    val state: ContainerStateEnum,
    val status: String,
    val autoStart: Boolean,
    val autoStartWait: Int?,
    val iconUrl: String?,
    val webUiUrl: String?,
    val templatePath: String?,
    val isUpdateAvailable: Boolean?,
    val lanIpPorts: List<String>?,
    val sizeRootFs: Long?,
    val created: Int,
)

data class DockerData(
    val containers: List<DockerContainerInfo>,
)

/** 容器日志单行：timestamp 为 RFC3339（UTC）字符串，可直接按字典序比较。 */
data class DockerLogLine(
    val timestamp: String,
    val message: String,
)

/**
 * 一次日志拉取的结果。[cursor] 可原样传回 `since` 参数继续增量拉取；
 * 为空表示服务端已无更多日志（容器被删除等场景）。
 */
data class DockerLogsData(
    val containerId: String,
    val lines: List<DockerLogLine>,
    val cursor: String?,
)
