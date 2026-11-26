package com.aedev.aetranzit.ui.admin.staff

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageStaffViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeStaffAuthRepository
    private lateinit var viewModel: ManageStaffViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeStaffAuthRepository()
        viewModel = ManageStaffViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onToggleUserActive blocks deactivating currently logged-in Admin account`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onToggleUserActive(userId = "admin-1", targetActive = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val adminItem = state.staffMembers.first { it.id == "admin-1" }
        assertTrue(adminItem.isActive)
        assertTrue(state.isErrorMessage)
        assertEquals(ManageStaffViewModel.SELF_DEACTIVATION_MESSAGE, state.statusMessage)
        assertEquals(0, fakeAuthRepository.setStatusCallCount)
    }

    @Test
    fun `onToggleUserActive deactivates and reactivates conductor account`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onToggleUserActive(userId = "cond-1", targetActive = false)
        advanceUntilIdle()

        val afterDeactivate = viewModel.uiState.value
        val deactivatedConductor = afterDeactivate.staffMembers.first { it.id == "cond-1" }
        assertFalse(deactivatedConductor.isActive)
        assertFalse(afterDeactivate.isErrorMessage)
        assertTrue(afterDeactivate.statusMessage.orEmpty().contains("deactivated"))

        viewModel.onToggleUserActive(userId = "cond-1", targetActive = true)
        advanceUntilIdle()

        val afterReactivate = viewModel.uiState.value
        val reactivatedConductor = afterReactivate.staffMembers.first { it.id == "cond-1" }
        assertTrue(reactivatedConductor.isActive)
        assertFalse(afterReactivate.isErrorMessage)
        assertTrue(afterReactivate.statusMessage.orEmpty().contains("activated"))
    }
}

private class FakeStaffAuthRepository : AuthRepository {
    private val adminUser = User(
        id = "admin-1",
        name = "Maria Santos",
        email = "maria@aetranzit.ph",
        role = UserRole.ADMIN,
        isActive = true,
        isSynced = true,
        updatedAt = 1_000L
    )

    private val conductorUser = User(
        id = "cond-1",
        name = "Ramon Magsaysay",
        email = "ramon@aetranzit.ph",
        role = UserRole.CONDUCTOR,
        isActive = true,
        isSynced = true,
        updatedAt = 1_000L
    )

    private val currentUserFlow = MutableStateFlow<User?>(adminUser)
    private val allUsersFlow = MutableStateFlow(listOf(adminUser, conductorUser))

    var setStatusCallCount: Int = 0
        private set

    override fun observeCurrentUser(): Flow<User?> = currentUserFlow

    override fun observeAllUsers(): Flow<List<User>> = allUsersFlow

    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun getCurrentUser(): User? = currentUserFlow.value

    override suspend fun login(email: String, password: String): Result<User> =
        Result.success(adminUser)

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> = Result.success(adminUser)

    override suspend fun setUserActiveStatus(userId: String, isActive: Boolean): Result<Unit> {
        setStatusCallCount++
        if (!isActive && userId == currentUserFlow.value?.id) {
            return Result.failure(IllegalStateException("Self deactivation blocked"))
        }
        allUsersFlow.value = allUsersFlow.value.map { user ->
            if (user.id == userId) {
                user.copy(isActive = isActive, isSynced = false)
            } else {
                user
            }
        }
        return Result.success(Unit)
    }

    override suspend fun syncPendingUsers(): Result<Int> = Result.success(0)

    override suspend fun logout() {
        currentUserFlow.value = null
    }
}
