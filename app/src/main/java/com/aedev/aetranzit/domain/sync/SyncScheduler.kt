package com.aedev.aetranzit.domain.sync

interface SyncScheduler {
    fun enqueueImmediateSync()
    fun schedulePeriodicSync()
    fun cancelAllSync()
}

object NoOpSyncScheduler : SyncScheduler {
    override fun enqueueImmediateSync() = Unit
    override fun schedulePeriodicSync() = Unit
    override fun cancelAllSync() = Unit
}
