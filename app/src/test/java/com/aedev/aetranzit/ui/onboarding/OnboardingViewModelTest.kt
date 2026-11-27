package com.aedev.aetranzit.ui.onboarding

import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSyncPreferencesRepository: FakeOnboardingSyncPreferencesRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSyncPreferencesRepository = FakeOnboardingSyncPreferencesRepository()
        viewModel = OnboardingViewModel(fakeSyncPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding sets onboarding completed to true and triggers callback`() = runTest {
        var callbackExecuted = false

        viewModel.completeOnboarding {
            callbackExecuted = true
        }
        advanceUntilIdle()

        assertTrue(callbackExecuted)
        assertTrue(fakeSyncPreferencesRepository.hasCompletedOnboarding.first())
    }
}

private class FakeOnboardingSyncPreferencesRepository : SyncPreferencesRepository {
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
