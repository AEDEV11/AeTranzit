package com.aedev.aetranzit.domain.sync

import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val syncState: StateFlow<SyncStatus>
    val isOnline: StateFlow<Boolean>
    suspend fun syncAll(): Result<SyncSummary>
}
