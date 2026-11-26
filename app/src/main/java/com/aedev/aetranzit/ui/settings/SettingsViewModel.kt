package com.aedev.aetranzit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
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
    val isCloudSyncEnabled: Boolean = false,
    val lastSyncedAt: Long? = null,
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val isSyncError: Boolean = false
)

sealed interface SettingsUiEvent {
    data class ToggleCloudSync(val enabled: Boolean) : SettingsUiEvent
    data object SyncNow : SettingsUiEvent
}

private data class SyncOperationState(
    val isSyncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val authRepository: AuthRepository,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository,
    private val tripRepository: TripRepository,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val syncOpState = MutableStateFlow(SyncOperationState())

    private val pendingSyncCountFlow = combine(
        authRepository.observeUnsyncedCount(),
        busRepository.observeUnsyncedCount(),
        routeRepository.observeUnsyncedCount(),
        tripRepository.observeUnsyncedCount(),
        ticketRepository.observeUnsyncedCount()
    ) { users, buses, routes, trips, tickets ->
        users + buses + routes + trips + tickets
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        syncPreferencesRepository.isCloudSyncEnabled,
        syncPreferencesRepository.lastSyncedAt,
        pendingSyncCountFlow,
        syncOpState
    ) { enabled, lastSyncedAt, pendingCount, op ->
        SettingsUiState(
            isCloudSyncEnabled = enabled,
            lastSyncedAt = lastSyncedAt,
            pendingSyncCount = pendingCount,
            isSyncing = op.isSyncing,
            syncStatusMessage = op.message,
            isSyncError = op.isError
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
                syncOpState.update {
                    it.copy(
                        message = if (event.enabled) {
                            "Cloud Sync enabled for Firebase project aetranzit."
                        } else {
                            "Cloud Sync disabled. App runs strictly offline on Room."
                        },
                        isError = false
                    )
                }
            }
            SettingsUiEvent.SyncNow -> syncNow()
        }
    }

    private fun syncNow() {
        if (syncOpState.value.isSyncing) return
        viewModelScope.launch {
            syncOpState.value = SyncOperationState(isSyncing = true)

            val results = listOf(
                authRepository.syncPendingUsers(),
                busRepository.syncPendingBuses(),
                routeRepository.syncPendingRoutes(),
                tripRepository.syncPendingTrips(),
                ticketRepository.syncPendingTickets()
            )

            val firstFailure = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            val totalSynced = results.sumOf { it.getOrDefault(0) }

            if (firstFailure != null && totalSynced == 0) {
                syncOpState.value = SyncOperationState(
                    isSyncing = false,
                    message = firstFailure.message ?: "Cloud sync failed.",
                    isError = true
                )
            } else {
                val syncedAt = System.currentTimeMillis()
                syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                syncOpState.value = SyncOperationState(
                    isSyncing = false,
                    message = when {
                        firstFailure != null ->
                            "Partially synced $totalSynced record(s); some items failed (${firstFailure.message})."
                        totalSynced == 0 ->
                            "Everything is up to date (Users, Buses, Routes, Trips & Tickets)."
                        else ->
                            "Synced $totalSynced record(s) to cloud (Users, Buses, Routes, Trips & Tickets)."
                    },
                    isError = firstFailure != null
                )
            }
        }
    }
}
