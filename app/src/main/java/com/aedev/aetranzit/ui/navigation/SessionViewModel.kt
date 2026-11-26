package com.aedev.aetranzit.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val currentUser: User? = null,
    val isCloudSyncEnabled: Boolean = false
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    syncPreferencesRepository: SyncPreferencesRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionUiState> = combine(
        authRepository.observeCurrentUser(),
        syncPreferencesRepository.isCloudSyncEnabled
    ) { user, cloudSyncEnabled ->
        SessionUiState(
            currentUser = user,
            isCloudSyncEnabled = cloudSyncEnabled
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
