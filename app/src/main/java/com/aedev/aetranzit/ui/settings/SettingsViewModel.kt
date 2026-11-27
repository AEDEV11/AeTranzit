package com.aedev.aetranzit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.sync.SyncManager
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isCloudSyncEnabled: Boolean = true,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val isSyncError: Boolean = false,
    val syncBadgeState: SyncBadgeUiState = SyncBadgeUiState()
)

sealed interface SettingsUiEvent {
    data class ToggleCloudSync(val enabled: Boolean) : SettingsUiEvent
    data object SyncNow : SettingsUiEvent
}

private data class SyncOperationState(
    val isManualSyncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

private data class SyncRuntimeSnapshot(
    val syncStatus: SyncStatus,
    val isOnline: Boolean,
    val op: SyncOperationState
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    companyRepository: CompanyRepository,
    authRepository: AuthRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    tripRepository: TripRepository,
    ticketRepository: TicketRepository
) : ViewModel() {

    private val syncOpState = MutableStateFlow(SyncOperationState())

    private val companyAndUserCountFlow = combine(
        companyRepository.observeUnsyncedCount(),
        authRepository.observeUnsyncedCount()
    ) { companies, users -> companies + users }

    private val pendingSyncCountFlow = combine(
        companyAndUserCountFlow,
        busRepository.observeUnsyncedCount(),
        routeRepository.observeUnsyncedCount(),
        tripRepository.observeUnsyncedCount(),
        ticketRepository.observeUnsyncedCount()
    ) { companiesAndUsers, buses, routes, trips, tickets ->
        companiesAndUsers + buses + routes + trips + tickets
    }

    private val runtimeSnapshotFlow = combine(
        syncManager.syncState,
        syncManager.isOnline,
        syncOpState
    ) { status, isOnline, op ->
        SyncRuntimeSnapshot(
            syncStatus = status,
            isOnline = isOnline,
            op = op
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        syncPreferencesRepository.isCloudSyncEnabled,
        syncPreferencesRepository.lastSyncedAt,
        pendingSyncCountFlow,
        runtimeSnapshotFlow
    ) { enabled, lastSyncedAt, pendingCount, runtime ->
        val backgroundError = (runtime.syncStatus as? SyncStatus.Error)?.message
        val resolvedMessage = runtime.op.message ?: backgroundError
        val resolvedIsError = if (runtime.op.message != null) {
            runtime.op.isError
        } else {
            backgroundError != null
        }

        SettingsUiState(
            isCloudSyncEnabled = enabled,
            isOnline = runtime.isOnline,
            lastSyncedAt = lastSyncedAt,
            pendingSyncCount = pendingCount,
            isSyncing = runtime.op.isManualSyncing || runtime.syncStatus is SyncStatus.Syncing,
            syncStatusMessage = resolvedMessage,
            isSyncError = resolvedIsError,
            syncBadgeState = SyncBadgeUiState(
                syncStatus = runtime.syncStatus,
                isOnline = runtime.isOnline,
                isCloudSyncEnabled = enabled,
                pendingCount = pendingCount
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ToggleCloudSync -> viewModelScope.launch {
                syncPreferencesRepository.setCloudSyncEnabled(event.enabled)
                if (event.enabled) {
                    syncScheduler.schedulePeriodicSync()
                    syncScheduler.enqueueImmediateSync()
                } else {
                    syncScheduler.cancelAllSync()
                }
                syncOpState.update {
                    it.copy(
                        message = if (event.enabled) {
                            "Auto-sync enabled for Firebase project aetranzit."
                        } else {
                            "Auto-sync paused. Local changes remain queued safely in Room."
                        },
                        isError = false
                    )
                }
            }
            SettingsUiEvent.SyncNow -> syncNow()
        }
    }

    private fun syncNow() {
        if (uiState.value.isSyncing) return
        viewModelScope.launch {
            syncOpState.value = SyncOperationState(isManualSyncing = true)

            val result = syncManager.syncAll()
            result.fold(
                onSuccess = { summary ->
                    val message = when {
                        summary.pushedCount == 0 && summary.pulledCount == 0 ->
                            "Everything is in sync between this device and Firebase Cloud."
                        else ->
                            "Two-way sync complete: pushed ${summary.pushedCount}, pulled ${summary.pulledCount} record(s)."
                    }
                    syncOpState.value = SyncOperationState(
                        isManualSyncing = false,
                        message = message,
                        isError = false
                    )
                },
                onFailure = { error ->
                    syncOpState.value = SyncOperationState(
                        isManualSyncing = false,
                        message = error.message ?: "Cloud sync failed.",
                        isError = true
                    )
                }
            )
        }
    }
}
