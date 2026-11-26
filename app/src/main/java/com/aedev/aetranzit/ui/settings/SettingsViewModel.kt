package com.aedev.aetranzit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val syncOpState = MutableStateFlow(SyncOperationState())

    val uiState: StateFlow<SettingsUiState> = combine(
        syncPreferencesRepository.isCloudSyncEnabled,
        syncPreferencesRepository.lastSyncedAt,
        syncOpState
    ) { enabled, lastSyncedAt, op ->
        SettingsUiState(
            isCloudSyncEnabled = enabled,
            lastSyncedAt = lastSyncedAt,
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
            authRepository.syncPendingUsers()
                .onSuccess { syncedCount ->
                    syncOpState.value = SyncOperationState(
                        isSyncing = false,
                        message = if (syncedCount == 0) {
                            "Everything is up to date."
                        } else {
                            "Synced $syncedCount record(s) to cloud."
                        },
                        isError = false
                    )
                }
                .onFailure { error ->
                    syncOpState.value = SyncOperationState(
                        isSyncing = false,
                        message = error.message ?: "Cloud sync failed.",
                        isError = true
                    )
                }
        }
    }
}
