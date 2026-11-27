package com.aedev.aetranzit.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncPreferencesRepository {
    val isCloudSyncEnabled: Flow<Boolean>
    val lastSyncedAt: Flow<Long?>
    val sessionUserId: Flow<String?>
    val hasCompletedOnboarding: Flow<Boolean>

    suspend fun setCloudSyncEnabled(enabled: Boolean)
    suspend fun updateLastSyncedAt(timestamp: Long)
    suspend fun setSessionUserId(userId: String?)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
