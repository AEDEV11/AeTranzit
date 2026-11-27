package com.aedev.aetranzit.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aedev.aetranzit.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager
) : SyncScheduler {

    private val connectedConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun enqueueImmediateSync() {
        val request = OneTimeWorkRequestBuilder<AeTranzitSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun schedulePeriodicSync() {
        val periodicRequest = PeriodicWorkRequestBuilder<AeTranzitSyncWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    override fun cancelAllSync() {
        workManager.cancelUniqueWork(UNIQUE_IMMEDIATE_SYNC_WORK)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC_WORK)
    }

    companion object {
        const val UNIQUE_IMMEDIATE_SYNC_WORK = "aetranzit_immediate_auto_sync"
        const val UNIQUE_PERIODIC_SYNC_WORK = "aetranzit_periodic_auto_sync"
        private const val PERIODIC_INTERVAL_MINUTES = 15L
        private const val BACKOFF_DELAY_SECONDS = 15L
    }
}
