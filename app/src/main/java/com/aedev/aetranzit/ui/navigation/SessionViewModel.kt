package com.aedev.aetranzit.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.sync.SyncManager
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val currentUser: User? = null,
    val isCloudSyncEnabled: Boolean = true,
    val syncBadgeState: SyncBadgeUiState = SyncBadgeUiState()
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    syncPreferencesRepository: SyncPreferencesRepository,
    syncManager: SyncManager,
    companyRepository: CompanyRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    tripRepository: TripRepository,
    ticketRepository: TicketRepository
) : ViewModel() {

    private val companyAndUserUnsyncedFlow = combine(
        companyRepository.observeUnsyncedCount(),
        authRepository.observeUnsyncedCount()
    ) { companies, users -> companies + users }

    private val totalPendingSyncFlow = combine(
        companyAndUserUnsyncedFlow,
        busRepository.observeUnsyncedCount(),
        routeRepository.observeUnsyncedCount(),
        tripRepository.observeUnsyncedCount(),
        ticketRepository.observeUnsyncedCount()
    ) { companiesAndUsers, buses, routes, trips, tickets ->
        companiesAndUsers + buses + routes + trips + tickets
    }

    val sessionState: StateFlow<SessionUiState> = combine(
        authRepository.observeCurrentUser(),
        syncPreferencesRepository.isCloudSyncEnabled,
        syncManager.syncState,
        syncManager.isOnline,
        totalPendingSyncFlow
    ) { user, cloudSyncEnabled, syncStatus, isOnline, pendingCount ->
        SessionUiState(
            currentUser = user,
            isCloudSyncEnabled = cloudSyncEnabled,
            syncBadgeState = SyncBadgeUiState(
                syncStatus = syncStatus,
                isOnline = isOnline,
                isCloudSyncEnabled = cloudSyncEnabled,
                pendingCount = pendingCount
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionUiState()
    )

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
