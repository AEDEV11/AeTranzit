package com.aedev.aetranzit.ui.auth

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForceChangePasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeForcePasswordAuthRepository
    private lateinit var viewModel: ForceChangePasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeForcePasswordAuthRepository()
        viewModel = ForceChangePasswordViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rejects short or mismatched passwords client-side`() = runTest {
        advanceUntilIdle()

        viewModel.onNewPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        viewModel.onSubmitNewPassword()

        assertNotNull(viewModel.uiState.value.newPasswordError)

        viewModel.onNewPasswordChanged("NewPass123")
        viewModel.onConfirmPasswordChanged("DifferentPass123")
        viewModel.onSubmitNewPassword()

        assertNull(viewModel.uiState.value.newPasswordError)
        assertEquals("Passwords do not match.", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `rejects reusing temporary password and emits PasswordUpdated on valid password`() = runTest {
        advanceUntilIdle()

        // Attempt reusing temporary password -> surfaces inline on newPasswordError
        viewModel.onNewPasswordChanged("Temp-4821")
        viewModel.onConfirmPasswordChanged("Temp-4821")
        viewModel.onSubmitNewPassword()
        advanceUntilIdle()

        assertEquals(
            "New password must be different from your temporary password.",
            viewModel.uiState.value.newPasswordError
        )

        // Submit valid new password -> emits PasswordUpdated with mustChangePassword == false
        viewModel.onNewPasswordChanged("SecureBus99")
        viewModel.onConfirmPasswordChanged("SecureBus99")
        viewModel.onSubmitNewPassword()
        advanceUntilIdle()

        val effect = viewModel.effects.first() as ForceChangePasswordEffect.PasswordUpdated
        assertFalse(effect.user.mustChangePassword)
        assertNull(viewModel.uiState.value.newPasswordError)
        assertNull(viewModel.uiState.value.generalError)
    }
}

private class FakeForcePasswordAuthRepository : AuthRepository {
    private val staffUser = MutableStateFlow<User?>(
        User(
            id = "cond-1",
            name = "Ramon Magsaysay",
            email = "ramon@victory.ph",
            role = UserRole.CONDUCTOR,
            companyId = "company-1",
            isActive = true,
            mustChangePassword = true,
            isSynced = true,
            updatedAt = 1_000L
        )
    )

    override fun observeCurrentUser(): Flow<User?> = staffUser
    override fun observeAllUsers(): Flow<List<User>> =
        MutableStateFlow(listOfNotNull(staffUser.value))
    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun getCurrentUser(): User? = staffUser.value
    override suspend fun login(email: String, password: String): Result<User> =
        Result.failure(UnsupportedOperationException())
    override suspend fun register(name: String, email: String, password: String): Result<User> =
        Result.failure(UnsupportedOperationException())
    override suspend fun completeCompanySetup(companyName: String): Result<User> =
        Result.failure(UnsupportedOperationException())
    override suspend fun createStaffAccount(
        name: String,
        email: String,
        temporaryPassword: String,
        role: UserRole
    ): Result<User> = Result.failure(UnsupportedOperationException())

    override suspend fun completeForcedPasswordChange(newPassword: String): Result<User> {
        if (newPassword == "Temp-4821") {
            return Result.failure(
                IllegalArgumentException("New password must be different from your temporary password.")
            )
        }
        val updated = staffUser.value!!.copy(mustChangePassword = false)
        staffUser.value = updated
        return Result.success(updated)
    }

    override suspend fun setUserActiveStatus(userId: String, isActive: Boolean): Result<Unit> =
        Result.success(Unit)
    override suspend fun logout() {
        staffUser.value = null
    }
}
