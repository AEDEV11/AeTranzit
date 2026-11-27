package com.aedev.aetranzit.ui.auth

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.ui.navigation.AeTranzitRoutes
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
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitLogin rejects empty email and short password`() {
        viewModel.onEvent(AuthUiEvent.EmailChanged(""))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("123"))
        viewModel.onEvent(AuthUiEvent.SubmitLogin)

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
    }

    @Test
    fun `submitRegister rejects mismatched passwords`() {
        viewModel.onEvent(AuthUiEvent.NameChanged("Maria Santos"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("maria@aetranzit.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged("different123"))
        viewModel.onEvent(AuthUiEvent.SubmitRegister)

        assertEquals("Passwords do not match.", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `submitRegister creates ADMIN with empty companyId and routes to COMPANY_SETUP`() = runTest {
        viewModel.onEvent(AuthUiEvent.NameChanged("Maria Santos"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("maria@aetranzit.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.SubmitRegister)
        advanceUntilIdle()

        val effect = viewModel.effects.first() as AuthEffect.Authenticated
        assertEquals(UserRole.ADMIN, effect.user.role)
        assertEquals("", effect.user.companyId)
        assertFalse(effect.user.mustChangePassword)
        assertEquals(AeTranzitRoutes.COMPANY_SETUP, AeTranzitRoutes.postAuthRouteFor(effect.user))
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `postAuthRouteFor enforces FORCE_CHANGE_PASSWORD and COMPANY_SETUP before home`() {
        val tempStaff = User(
            id = "u-cond",
            name = "Ramon",
            email = "ramon@bus.ph",
            role = UserRole.CONDUCTOR,
            companyId = "comp-1",
            isActive = true,
            mustChangePassword = true,
            isSynced = true,
            updatedAt = 1L
        )
        assertEquals(AeTranzitRoutes.FORCE_CHANGE_PASSWORD, AeTranzitRoutes.postAuthRouteFor(tempStaff))

        val readyStaff = tempStaff.copy(mustChangePassword = false)
        assertEquals(AeTranzitRoutes.CONDUCTOR_HOME, AeTranzitRoutes.postAuthRouteFor(readyStaff))

        val unconfiguredAdmin = User(
            id = "u-admin",
            name = "Maria",
            email = "maria@bus.ph",
            role = UserRole.ADMIN,
            companyId = "",
            isActive = true,
            mustChangePassword = false,
            isSynced = true,
            updatedAt = 1L
        )
        assertEquals(AeTranzitRoutes.COMPANY_SETUP, AeTranzitRoutes.postAuthRouteFor(unconfiguredAdmin))

        val configuredAdmin = unconfiguredAdmin.copy(companyId = "comp-1")
        assertEquals(AeTranzitRoutes.ADMIN_DASHBOARD, AeTranzitRoutes.postAuthRouteFor(configuredAdmin))
    }
}

private class FakeAuthRepository : AuthRepository {
    private val currentUser = MutableStateFlow<User?>(null)

    override fun observeCurrentUser(): Flow<User?> = currentUser
    override fun observeAllUsers(): Flow<List<User>> =
        MutableStateFlow(listOfNotNull(currentUser.value))
    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun getCurrentUser(): User? = currentUser.value

    override suspend fun login(email: String, password: String): Result<User> {
        val user = currentUser.value ?: return Result.failure(IllegalArgumentException("No user"))
        return Result.success(user)
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<User> {
        val user = User(
            id = "u-1",
            name = name,
            email = email,
            role = UserRole.ADMIN,
            companyId = "",
            isActive = true,
            mustChangePassword = false,
            isSynced = false,
            updatedAt = 1L
        )
        currentUser.value = user
        return Result.success(user)
    }

    override suspend fun completeCompanySetup(companyName: String): Result<User> {
        val updated = (currentUser.value ?: return Result.failure(IllegalStateException()))
            .copy(companyId = "company-new")
        currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun createStaffAccount(
        name: String,
        email: String,
        temporaryPassword: String,
        role: UserRole
    ): Result<User> = Result.failure(UnsupportedOperationException())

    override suspend fun completeForcedPasswordChange(newPassword: String): Result<User> =
        Result.failure(UnsupportedOperationException())

    override suspend fun setUserActiveStatus(userId: String, isActive: Boolean): Result<Unit> =
        Result.success(Unit)

    override suspend fun logout() {
        currentUser.value = null
    }
}
