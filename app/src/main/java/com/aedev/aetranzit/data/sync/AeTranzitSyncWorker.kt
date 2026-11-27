package com.aedev.aetranzit.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aedev.aetranzit.domain.sync.SyncManager
import com.aedev.aetranzit.domain.sync.TransientSyncException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class AeTranzitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val outcome = syncManager.syncAll()
            if (outcome.isSuccess) {
                Result.success()
            } else {
                val error = outcome.exceptionOrNull()
                if (error is TransientSyncException && runAttemptCount < MAX_AUTO_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            if (runAttemptCount < MAX_AUTO_RETRIES) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_AUTO_RETRIES = 6
    }
}
