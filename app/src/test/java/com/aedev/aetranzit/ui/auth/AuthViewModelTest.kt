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
        viewModel.onEvent(AuthUiEvent.NameChanged("Juan"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("juan@aetranzit.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged("different123"))
        viewModel.onEvent(AuthUiEvent.SubmitRegister)

        assertEquals("Passwords do not match.", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `submitRegister emits Authenticated effect with selected role`() = runTest {
        viewModel.onEvent(AuthUiEvent.RoleSelected(UserRole.ADMIN))
        viewModel.onEvent(AuthUiEvent.NameChanged("Maria Santos"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("maria@aetranzit.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.SubmitRegister)
        advanceUntilIdle()

        val effect = viewModel.effects.first()
        assertEquals(UserRole.ADMIN, (effect as AuthEffect.Authenticated).user.role)
        assertNull(viewModel.uiState.value.generalError)
    }
}

private class FakeAuthRepository : AuthRepository {
    private val currentUser = MutableStateFlow<User?>(null)

    override fun observeCurrentUser(): Flow<User?> = currentUser
    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun getCurrentUser(): User? = currentUser.value

    override suspend fun login(email: String, password: String): Result<User> {
        val user = currentUser.value ?: return Result.failure(IllegalArgumentException("No user"))
        return Result.success(user)
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> {
        val user = User(
            id = "u-1",
            name = name,
            email = email,
            role = role,
            isActive = true,
            isSynced = false,
            updatedAt = 1L
        )
        currentUser.value = user
        return Result.success(user)
    }

    override suspend fun syncPendingUsers(): Result<Int> = Result.success(0)

    override suspend fun logout() {
        currentUser.value = null
    }
}
