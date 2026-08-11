package com.nichx.unraidassistant.data.model

enum class VmStateEnum {
    NOSTATE,
    RUNNING,
    IDLE,
    PAUSED,
    SHUTDOWN,
    SHUTOFF,
    CRASHED,
    PMSUSPENDED,
}

data class VmInfo(
    val id: String,
    val name: String,
    val state: VmStateEnum,
)

data class VmData(
    val vms: List<VmInfo>,
)
