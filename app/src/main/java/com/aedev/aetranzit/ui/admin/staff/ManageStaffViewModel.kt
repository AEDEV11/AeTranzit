package com.aedev.aetranzit.ui.admin.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

data class StaffMemberItem(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val mustChangePassword: Boolean = false,
    val isSynced: Boolean,
    val isCurrentUser: Boolean
)

data class AddStaffFormState(
    val isSheetVisible: Boolean = false,
    val name: String = "",
    val email: String = "",
    val selectedRole: UserRole = UserRole.CONDUCTOR,
    val temporaryPassword: String = "",
    val isPasswordVisible: Boolean = true,
    val isCreating: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

data class ProvisionedStaffCredentials(
    val name: String,
    val email: String,
    val role: UserRole,
    val temporaryPassword: String,
    val companyName: String
) {
    fun toClipboardSummary(): String = buildString {
        appendLine("$companyName — AeTranzit Staff Login")
        appendLine("Name: $name (${role.displayName})")
        appendLine("Email: $email")
        appendLine("Temporary Password: $temporaryPassword")
        append("Note: You will be prompted to set your own password on first sign-in.")
    }
}

data class ManageStaffUiState(
    val companyName: String = Company.DEFAULT_COMPANY_NAME,
    val staffMembers: List<StaffMemberItem> = emptyList(),
    val currentAdminId: String? = null,
    val totalAdmins: Int = 0,
    val totalConductors: Int = 0,
    val activeCount: Int = 0,
    val updatingUserId: String? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val addStaffForm: AddStaffFormState = AddStaffFormState(),
    val createdCredentials: ProvisionedStaffCredentials? = null
)

private data class StaffActionState(
    val updatingUserId: String? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val addStaffForm: AddStaffFormState = AddStaffFormState(),
    val createdCredentials: ProvisionedStaffCredentials? = null
)

@HiltViewModel
class ManageStaffViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    companyRepository: CompanyRepository
) : ViewModel() {

    private val secureRandom = SecureRandom()
    private val actionState = MutableStateFlow(StaffActionState())

    val uiState: StateFlow<ManageStaffUiState> = combine(
        authRepository.observeAllUsers(),
        authRepository.observeCurrentUser(),
        companyRepository.observeActiveCompany(),
        actionState
    ) { users, currentUser, company, action ->
        val currentId = currentUser?.id
        val items = users
            .sortedWith(
                compareBy<com.aedev.aetranzit.domain.model.User> {
                    if (it.role == UserRole.ADMIN) 0 else 1
                }.thenBy { it.name.lowercase() }
            )
            .map { user ->
                StaffMemberItem(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    role = user.role,
                    isActive = user.isActive,
                    mustChangePassword = user.mustChangePassword,
                    isSynced = user.isSynced,
                    isCurrentUser = user.id == currentId
                )
            }

        ManageStaffUiState(
            companyName = company?.name?.ifBlank { Company.DEFAULT_COMPANY_NAME }
                ?: Company.DEFAULT_COMPANY_NAME,
            staffMembers = items,
            currentAdminId = currentId,
            totalAdmins = items.count { it.role == UserRole.ADMIN },
            totalConductors = items.count { it.role == UserRole.CONDUCTOR },
            activeCount = items.count { it.isActive },
            updatingUserId = action.updatingUserId,
            statusMessage = action.statusMessage,
            isErrorMessage = action.isErrorMessage,
            addStaffForm = action.addStaffForm,
            createdCredentials = action.createdCredentials
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageStaffUiState()
    )

    fun onOpenAddStaffSheet() {
        actionState.update { current ->
            current.copy(
                addStaffForm = AddStaffFormState(
                    isSheetVisible = true,
                    temporaryPassword = generateTemporaryPassword()
                ),
                statusMessage = null,
                isErrorMessage = false
            )
        }
    }

    fun onDismissAddStaffSheet() {
        if (actionState.value.addStaffForm.isCreating) return
        actionState.update { current ->
            current.copy(addStaffForm = AddStaffFormState(isSheetVisible = false))
        }
    }

    fun onStaffNameChanged(value: String) {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    name = value,
                    nameError = null,
                    generalError = null
                )
            )
        }
    }

    fun onStaffEmailChanged(value: String) {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    email = value,
                    emailError = null,
                    generalError = null
                )
            )
        }
    }

    fun onStaffRoleSelected(role: UserRole) {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    selectedRole = role,
                    generalError = null
                )
            )
        }
    }

    fun onTemporaryPasswordChanged(value: String) {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    temporaryPassword = value,
                    passwordError = null,
                    generalError = null
                )
            )
        }
    }

    fun onGenerateTemporaryPassword() {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    temporaryPassword = generateTemporaryPassword(),
                    isPasswordVisible = true,
                    passwordError = null,
                    generalError = null
                )
            )
        }
    }

    fun onToggleTempPasswordVisibility() {
        actionState.update { current ->
            current.copy(
                addStaffForm = current.addStaffForm.copy(
                    isPasswordVisible = !current.addStaffForm.isPasswordVisible
                )
            )
        }
    }

    fun onSubmitCreateStaff() {
        val form = actionState.value.addStaffForm
        if (form.isCreating) return

        val trimmedName = form.name.trim()
        val trimmedEmail = form.email.trim()
        val nameError = if (trimmedName.length < 2) "Full name is required." else null
        val emailError = when {
            trimmedEmail.isEmpty() -> "Email address is required."
            !EMAIL_REGEX.matches(trimmedEmail) -> "Enter a valid email address."
            else -> null
        }
        val passwordError = when {
            form.temporaryPassword.isEmpty() -> "Temporary password is required."
            form.temporaryPassword.length < MIN_PASSWORD_LENGTH ->
                "Temporary password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }

        if (nameError != null || emailError != null || passwordError != null) {
            actionState.update { current ->
                current.copy(
                    addStaffForm = current.addStaffForm.copy(
                        nameError = nameError,
                        emailError = emailError,
                        passwordError = passwordError,
                        generalError = null
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            actionState.update { current ->
                current.copy(
                    addStaffForm = current.addStaffForm.copy(
                        isCreating = true,
                        nameError = null,
                        emailError = null,
                        passwordError = null,
                        generalError = null
                    )
                )
            }

            val result = authRepository.createStaffAccount(
                name = trimmedName,
                email = trimmedEmail,
                temporaryPassword = form.temporaryPassword,
                role = form.selectedRole
            )

            val resolvedCompanyName = uiState.value.companyName
            actionState.update { current ->
                result.fold(
                    onSuccess = { createdUser ->
                        current.copy(
                            addStaffForm = AddStaffFormState(isSheetVisible = false),
                            createdCredentials = ProvisionedStaffCredentials(
                                name = createdUser.name,
                                email = createdUser.email,
                                role = createdUser.role,
                                temporaryPassword = form.temporaryPassword,
                                companyName = resolvedCompanyName
                            ),
                            statusMessage = "Provisioned ${createdUser.role.displayName} account for ${createdUser.name}.",
                            isErrorMessage = false
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            addStaffForm = current.addStaffForm.copy(
                                isCreating = false,
                                generalError = error.message ?: "Unable to create staff account."
                            )
                        )
                    }
                )
            }
        }
    }

    fun onCredentialsCopied(staffName: String) {
        actionState.update { current ->
            current.copy(
                statusMessage = "Copied temporary login credentials for $staffName.",
                isErrorMessage = false
            )
        }
    }

    fun onDismissCreatedCredentials() {
        actionState.update { current ->
            current.copy(createdCredentials = null)
        }
    }

    fun onToggleUserActive(userId: String, targetActive: Boolean) {
        if (actionState.value.updatingUserId != null) return

        viewModelScope.launch {
            val currentAdminId = uiState.value.currentAdminId ?: authRepository.getCurrentUser()?.id
            if (!targetActive && currentAdminId != null && userId == currentAdminId) {
                actionState.update {
                    it.copy(
                        statusMessage = SELF_DEACTIVATION_MESSAGE,
                        isErrorMessage = true
                    )
                }
                return@launch
            }

            actionState.update {
                it.copy(updatingUserId = userId, statusMessage = null, isErrorMessage = false)
            }

            val targetName = uiState.value.staffMembers
                .firstOrNull { it.id == userId }
                ?.name
                ?: "Staff account"

            val result = authRepository.setUserActiveStatus(userId = userId, isActive = targetActive)
            actionState.update { current ->
                result.fold(
                    onSuccess = {
                        val verb = if (targetActive) "activated" else "deactivated"
                        current.copy(
                            updatingUserId = null,
                            statusMessage = "$targetName has been $verb.",
                            isErrorMessage = false
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            updatingUserId = null,
                            statusMessage = error.message ?: "Unable to update staff account status.",
                            isErrorMessage = true
                        )
                    }
                )
            }
        }
    }

    fun onDismissMessage() {
        actionState.update { it.copy(statusMessage = null, isErrorMessage = false) }
    }

    private fun generateTemporaryPassword(): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val prefix = buildString {
            repeat(4) { append(letters[secureRandom.nextInt(letters.length)]) }
        }
        val suffix = buildString {
            repeat(4) { append(digits[secureRandom.nextInt(digits.length)]) }
        }
        return "$prefix-$suffix"
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        const val SELF_DEACTIVATION_MESSAGE =
            "You cannot deactivate your own currently signed-in Admin account."
    }
}
