package com.aedev.aetranzit.ui.auth

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeSplashAuthRepository
    private lateinit var fakeSyncPreferencesRepository: FakeSplashSyncPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeSplashAuthRepository()
        fakeSyncPreferencesRepository = FakeSplashSyncPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resolveSession holds Loading until minimum splash duration elapses`() = runTest {
        fakeAuthRepository.currentUser = null
        fakeSyncPreferencesRepository.setOnboardingCompleted(true)

        val viewModel = SplashViewModel(fakeAuthRepository, fakeSyncPreferencesRepository)

        advanceTimeBy((SplashViewModel.MIN_SPLASH_DURATION_MS - 1L).milliseconds)
        runCurrent()
        assertEquals(SplashDestination.Loading, viewModel.destination.value)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(SplashDestination.Login, viewModel.destination.value)
    }

    @Test
    fun `resolveSession routes to Authenticated when user is logged in`() = runTest {
        val user = User(
            id = "user-1",
            name = "Test Admin",
            email = "admin@aetranzit.ph",
            role = UserRole.ADMIN,
            companyId = "comp-1",
            isActive = true,
            mustChangePassword = false,
            isSynced = true,
            updatedAt = 1_000L
        )
        fakeAuthRepository.currentUser = user

        val viewModel = SplashViewModel(fakeAuthRepository, fakeSyncPreferencesRepository)
        advanceUntilIdle()

        val destination = viewModel.destination.value
        assertTrue(destination is SplashDestination.Authenticated)
        assertEquals(user, (destination as SplashDestination.Authenticated).user)
    }

    @Test
    fun `resolveSession routes to Onboarding when unauthenticated and onboarding not completed`() = runTest {
        fakeAuthRepository.currentUser = null
        fakeSyncPreferencesRepository.setOnboardingCompleted(false)

        val viewModel = SplashViewModel(fakeAuthRepository, fakeSyncPreferencesRepository)
        advanceUntilIdle()

        assertEquals(SplashDestination.Onboarding, viewModel.destination.value)
    }

    @Test
    fun `resolveSession routes to Login when unauthenticated and onboarding completed`() = runTest {
        fakeAuthRepository.currentUser = null
        fakeSyncPreferencesRepository.setOnboardingCompleted(true)

        val viewModel = SplashViewModel(fakeAuthRepository, fakeSyncPreferencesRepository)
        advanceUntilIdle()

        assertEquals(SplashDestination.Login, viewModel.destination.value)
    }
}

private class FakeSplashAuthRepository : AuthRepository {
    var currentUser: User? = null

    override fun observeCurrentUser(): Flow<User?> = MutableStateFlow(currentUser)
    override fun observeAllUsers(): Flow<List<User>> = MutableStateFlow(listOfNotNull(currentUser))
    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun getCurrentUser(): User? = currentUser
    override suspend fun login(email: String, password: String) = Result.failure<User>(UnsupportedOperationException())
    override suspend fun register(name: String, email: String, password: String) = Result.failure<User>(UnsupportedOperationException())
    override suspend fun completeCompanySetup(companyName: String) = Result.failure<User>(UnsupportedOperationException())
    override suspend fun createStaffAccount(name: String, email: String, temporaryPassword: String, role: UserRole) = Result.failure<User>(UnsupportedOperationException())
    override suspend fun completeForcedPasswordChange(newPassword: String) = Result.failure<User>(UnsupportedOperationException())
    override suspend fun setUserActiveStatus(userId: String, isActive: Boolean) = Result.success(Unit)
    override suspend fun logout() { currentUser = null }
}

private class FakeSplashSyncPreferencesRepository : SyncPreferencesRepository {
    private val _hasCompletedOnboarding = MutableStateFlow(false)

    override val isCloudSyncEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val lastSyncedAt: Flow<Long?> = MutableStateFlow(null)
    override val sessionUserId: Flow<String?> = MutableStateFlow(null)
    override val hasCompletedOnboarding: Flow<Boolean> = _hasCompletedOnboarding

    override suspend fun setCloudSyncEnabled(enabled: Boolean) = Unit
    override suspend fun updateLastSyncedAt(timestamp: Long) = Unit
    override suspend fun setSessionUserId(userId: String?) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _hasCompletedOnboarding.value = completed
    }
}
