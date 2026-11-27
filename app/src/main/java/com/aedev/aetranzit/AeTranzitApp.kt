package com.aedev.aetranzit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aedev.aetranzit.data.sync.SyncCoordinator
import com.aedev.aetranzit.di.ApplicationScope
import com.aedev.aetranzit.domain.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class AeTranzitApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncCoordinator.startAutoSyncMonitor(applicationScope)
        syncScheduler.schedulePeriodicSync()
    }
}
