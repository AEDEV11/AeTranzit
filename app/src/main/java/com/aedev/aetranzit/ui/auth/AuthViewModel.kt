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

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)

sealed interface AuthUiEvent {
    data class NameChanged(val value: String) : AuthUiEvent
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent
    data object TogglePasswordVisibility : AuthUiEvent
    data object SubmitLogin : AuthUiEvent
    data object SubmitRegister : AuthUiEvent
    data object ClearErrors : AuthUiEvent
}

sealed interface AuthEffect {
    data class Authenticated(val user: User) : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.NameChanged -> _uiState.update {
                it.copy(name = event.value, nameError = null, generalError = null)
            }
            is AuthUiEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailError = null, generalError = null)
            }
            is AuthUiEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, passwordError = null, generalError = null)
            }
            is AuthUiEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordError = null, generalError = null)
            }
            AuthUiEvent.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            AuthUiEvent.ClearErrors -> _uiState.update {
                it.copy(
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    generalError = null
                )
            }
            AuthUiEvent.SubmitLogin -> submitLogin()
            AuthUiEvent.SubmitRegister -> submitRegister()
        }
    }

    private fun submitLogin() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, generalError = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.login(current.email, current.password)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.send(AuthEffect.Authenticated(user))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = error.message ?: "Unable to sign in."
                        )
                    }
                }
        }
    }

    private fun submitRegister() {
        val current = _uiState.value
        if (current.isLoading) return

        val nameError = if (current.name.trim().length < 2) "Enter your full name." else null
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        val confirmError = if (current.confirmPassword != current.password) {
            "Passwords do not match."
        } else {
            null
        }

        if (
            nameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, generalError = null)
            }
            authRepository.register(
                name = current.name,
                email = current.email,
                password = current.password
            ).onSuccess { user ->
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(AuthEffect.Authenticated(user))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = error.message ?: "Unable to create Admin account."
                    )
                }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email is required."
            !EMAIL_REGEX.matches(trimmed) -> "Enter a valid email address."
            else -> null
        }
    }

    private fun validatePassword(password: String): String? = when {
        password.isEmpty() -> "Password is required."
        password.length < MIN_PASSWORD_LENGTH ->
            "Password must be at least $MIN_PASSWORD_LENGTH characters."
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
