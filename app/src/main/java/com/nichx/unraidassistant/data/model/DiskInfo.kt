package com.nichx.unraidassistant.data.model

enum class ArrayStateEnum {
    STARTED, STOPPED, NEW_ARRAY, RECON_DISK, DISABLE_DISK, SWAP_DSBL,
    INVALID_EXPANSION, PARITY_NOT_BIGGEST, TOO_MANY_MISSING_DISKS,
    NEW_DISK_TOO_SMALL, NO_DATA_DISKS,
}

enum class ArrayDiskStatusEnum {
    DISK_NP, DISK_OK, DISK_NP_MISSING, DISK_INVALID, DISK_WRONG,
    DISK_DSBL, DISK_NP_DSBL, DISK_DSBL_NEW, DISK_NEW,
}

enum class ArrayDiskTypeEnum { DATA, PARITY, BOOT, FLASH, CACHE }

data class DiskInfo(
    val id: String,
    val name: String?,
    val device: String?,
    val type: ArrayDiskTypeEnum,
    val status: ArrayDiskStatusEnum?,
    val tempCelsius: Int?,
    val isRotational: Boolean?,
    val isSpinning: Boolean?,
    val sizeKb: Long?,
    val fsSizeKb: Long?,
    val fsFreeKb: Long?,
    val fsUsedKb: Long?,
    val numReads: Long?,
    val numWrites: Long?,
    val numErrors: Long?,
)

data class ArrayCapacityInfo(
    val freeKb: String,
    val usedKb: String,
    val totalKb: String,
)

data class StorageData(
    val arrayState: ArrayStateEnum,
    val capacity: ArrayCapacityInfo?,
    val parityCheck: ParityCheckInfo?,
    val parities: List<DiskInfo>,
    val dataDisks: List<DiskInfo>,
    val cacheDisks: List<DiskInfo>,
    val bootDisk: DiskInfo?,
    val shares: List<ShareInfo>,
)

enum class ParityCheckStatusEnum {
    NEVER_RUN, RUNNING, PAUSED, COMPLETED, CANCELLED, FAILED,
}

data class ParityCheckInfo(
    val status: ParityCheckStatusEnum,
    val progress: Int?,
    val speed: String?,
    val errors: Int?,
    val correcting: Boolean?,
    val paused: Boolean?,
    val running: Boolean?,
    val durationSeconds: Int?,
)

data class ShareInfo(
    val id: String,
    val name: String?,
    val freeKb: Long?,
    val usedKb: Long?,
    val sizeKb: Long?,
    val cache: Boolean?,
    val comment: String?,
)
