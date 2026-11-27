package com.aedev.aetranzit.ui.admin.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.usecase.CompanyJoinCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanySetupUiState(
    val adminName: String = "",
    val companyName: String = "",
    val companyCodePreview: String = "",
    val isSubmitting: Boolean = false,
    val companyNameError: String? = null,
    val generalError: String? = null
)

sealed interface CompanySetupEffect {
    data class SetupCompleted(val user: User) : CompanySetupEffect
    data object LoggedOut : CompanySetupEffect
}

@HiltViewModel
class CompanySetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val joinCodeGenerator: CompanyJoinCodeGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanySetupUiState())
    val uiState: StateFlow<CompanySetupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CompanySetupEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                _uiState.update { it.copy(adminName = currentUser.name) }
            }
        }
    }

    fun onCompanyNameChanged(value: String) {
        val trimmed = value.trim()
        val preview = if (trimmed.length >= 2) {
            val sampleCode = joinCodeGenerator.generate(trimmed) { 0 }
            val prefix = sampleCode.substringBefore('-')
            "$prefix-XXXX"
        } else {
            ""
        }
        _uiState.update {
            it.copy(
                companyName = value,
                companyCodePreview = preview,
                companyNameError = null,
                generalError = null
            )
        }
    }

    fun onSubmitSetup() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val trimmed = current.companyName.trim()
        if (trimmed.length < MIN_COMPANY_NAME_LENGTH) {
            _uiState.update {
                it.copy(
                    companyNameError = "Bus company / business name must be at least $MIN_COMPANY_NAME_LENGTH characters.",
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, companyNameError = null, generalError = null)
            }
            authRepository.completeCompanySetup(trimmed)
                .onSuccess { updatedUser ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(CompanySetupEffect.SetupCompleted(updatedUser))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            generalError = error.message ?: "Unable to complete business setup."
                        )
                    }
                }
        }
    }

    fun onLogout() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(CompanySetupEffect.LoggedOut)
        }
    }

    private companion object {
        const val MIN_COMPANY_NAME_LENGTH = 2
    }
}
