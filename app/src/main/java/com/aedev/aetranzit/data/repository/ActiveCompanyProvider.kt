package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ActiveCompanyProvider {
    fun observeActiveCompanyId(): Flow<String?>
    suspend fun getActiveCompanyId(): String?

    suspend fun requireActiveCompanyId(): String =
        getActiveCompanyId()
            ?: throw IllegalStateException("No active company session found. Please sign in again.")
}

@Singleton
class RoomActiveCompanyProvider @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val userDao: UserDao
) : ActiveCompanyProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveCompanyId(): Flow<String?> =
        syncPreferencesRepository.sessionUserId
            .flatMapLatest { userId ->
                if (userId.isNullOrBlank()) {
                    flowOf(null)
                } else {
                    userDao.observeById(userId).map { entity ->
                        entity?.takeIf { it.isActive }?.companyId?.takeIf { it.isNotBlank() }
                    }
                }
            }
            .distinctUntilChanged()

    override suspend fun getActiveCompanyId(): String? {
        val userId = syncPreferencesRepository.sessionUserId.first()
        if (userId.isNullOrBlank()) return null
        return userDao.getById(userId)
            ?.takeIf { it.isActive }
            ?.companyId
            ?.takeIf { it.isNotBlank() }
    }
}
