package com.aedev.aetranzit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Onboarding : SplashDestination
    data object Login : SplashDestination
    data class Authenticated(val user: User) : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncPreferencesRepository: SyncPreferencesRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        resolveSession()
    }

    private fun resolveSession() {
        viewModelScope.launch {
            val resolvedDestination = coroutineScope {
                val minDisplayJob = launch { delay(MIN_SPLASH_DURATION_MS.milliseconds) }
                val nextDestination = resolveNextDestination()
                minDisplayJob.join()
                nextDestination
            }
            _destination.value = resolvedDestination
        }
    }

    private suspend fun resolveNextDestination(): SplashDestination {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            return SplashDestination.Authenticated(user)
        }
        val hasCompletedOnboarding = syncPreferencesRepository.hasCompletedOnboarding.first()
        return if (hasCompletedOnboarding) {
            SplashDestination.Login
        } else {
            SplashDestination.Onboarding
        }
    }

    companion object {
        internal const val MIN_SPLASH_DURATION_MS = 1_200L
    }
}
