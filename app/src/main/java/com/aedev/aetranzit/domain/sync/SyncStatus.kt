package com.aedev.aetranzit.domain.sync

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object Synced : SyncStatus
    data object Offline : SyncStatus
    data class Error(val message: String) : SyncStatus
}

data class SyncSummary(
    val pushedCount: Int,
    val pulledCount: Int,
    val syncedAt: Long
)

class TransientSyncException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
