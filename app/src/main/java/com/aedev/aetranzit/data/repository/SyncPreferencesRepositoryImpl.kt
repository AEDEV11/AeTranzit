package com.aedev.aetranzit.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SyncPreferencesRepository {

    override val isCloudSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CLOUD_SYNC_ENABLED] ?: true
    }

    override val lastSyncedAt: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNCED_AT]
    }

    override val sessionUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SESSION_USER_ID]
    }

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CLOUD_SYNC_ENABLED] = enabled
        }
    }

    override suspend fun updateLastSyncedAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNCED_AT] = timestamp
        }
    }

    override suspend fun setSessionUserId(userId: String?) {
        dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(KEY_SESSION_USER_ID)
            } else {
                prefs[KEY_SESSION_USER_ID] = userId
            }
        }
    }

    private companion object {
        val KEY_CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        val KEY_SESSION_USER_ID = stringPreferencesKey("session_user_id")
    }
}
