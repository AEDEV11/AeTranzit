package com.aedev.aetranzit.ui.admin.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffMemberItem(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val isSynced: Boolean,
    val isCurrentUser: Boolean
)

data class ManageStaffUiState(
    val staffMembers: List<StaffMemberItem> = emptyList(),
    val currentAdminId: String? = null,
    val totalAdmins: Int = 0,
    val totalConductors: Int = 0,
    val activeCount: Int = 0,
    val updatingUserId: String? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

private data class StaffActionState(
    val updatingUserId: String? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

@HiltViewModel
class ManageStaffViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val actionState = MutableStateFlow(StaffActionState())

    val uiState: StateFlow<ManageStaffUiState> = combine(
        authRepository.observeAllUsers(),
        authRepository.observeCurrentUser(),
        actionState
    ) { users, currentUser, action ->
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
                    isSynced = user.isSynced,
                    isCurrentUser = user.id == currentId
                )
            }

        ManageStaffUiState(
            staffMembers = items,
            currentAdminId = currentId,
            totalAdmins = items.count { it.role == UserRole.ADMIN },
            totalConductors = items.count { it.role == UserRole.CONDUCTOR },
            activeCount = items.count { it.isActive },
            updatingUserId = action.updatingUserId,
            statusMessage = action.statusMessage,
            isErrorMessage = action.isErrorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageStaffUiState()
    )

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

    companion object {
        const val SELF_DEACTIVATION_MESSAGE =
            "You cannot deactivate your own currently signed-in Admin account."
    }
}
