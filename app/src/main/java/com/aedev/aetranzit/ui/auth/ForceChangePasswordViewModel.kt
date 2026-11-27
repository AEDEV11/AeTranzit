package com.aedev.aetranzit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForceChangePasswordUiState(
    val userName: String = "",
    val userEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)

sealed interface ForceChangePasswordEffect {
    data class PasswordUpdated(val user: User) : ForceChangePasswordEffect
    data object LoggedOut : ForceChangePasswordEffect
}

@HiltViewModel
class ForceChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForceChangePasswordUiState())
    val uiState: StateFlow<ForceChangePasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ForceChangePasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        userName = user.name,
                        userEmail = user.email
                    )
                }
            }
        }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = null,
                generalError = null
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = null,
                generalError = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmitNewPassword() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val passwordError = when {
            current.newPassword.isEmpty() -> "New password is required."
            current.newPassword.length < MIN_PASSWORD_LENGTH ->
                "New password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }
        val confirmError = when {
            current.confirmPassword.isEmpty() -> "Confirm your new password."
            current.confirmPassword != current.newPassword -> "Passwords do not match."
            else -> null
        }

        if (passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    newPasswordError = passwordError,
                    confirmPasswordError = confirmError,
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    newPasswordError = null,
                    confirmPasswordError = null,
                    generalError = null
                )
            }
            authRepository.completeForcedPasswordChange(current.newPassword)
                .onSuccess { updatedUser ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(ForceChangePasswordEffect.PasswordUpdated(updatedUser))
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to update password."
                    val isDifferentTempError = message.contains("temporary password", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            newPasswordError = if (isDifferentTempError) message else null,
                            generalError = if (isDifferentTempError) null else message
                        )
                    }
                }
        }
    }

    fun onLogout() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(ForceChangePasswordEffect.LoggedOut)
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
