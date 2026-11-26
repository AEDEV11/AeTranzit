package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FirebaseAuthSyncDataSource
import com.aedev.aetranzit.data.security.PasswordHasher
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val firebaseAuthSyncDataSource: FirebaseAuthSyncDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> =
        syncPreferencesRepository.sessionUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(null)
            } else {
                userDao.observeById(userId).map { it?.takeIf { entity -> entity.isActive }?.toDomain() }
            }
        }

    override suspend fun getCurrentUser(): User? = withContext(ioDispatcher) {
        val userId = syncPreferencesRepository.sessionUserId.first() ?: return@withContext null
        userDao.getById(userId)?.takeIf { it.isActive }?.toDomain()
    }

    override suspend fun login(email: String, password: String): Result<User> = withContext(ioDispatcher) {
        try {
            val normalizedEmail = email.trim().lowercase()
            val localUser = userDao.getByEmail(normalizedEmail)
            if (localUser != null) {
                if (!passwordHasher.verify(password, localUser.passwordHash)) {
                    return@withContext Result.failure(IllegalArgumentException("Incorrect email or password."))
                }
                if (!localUser.isActive) {
                    return@withContext Result.failure(IllegalStateException("This account is deactivated. Contact an Admin."))
                }
                syncPreferencesRepository.setSessionUserId(localUser.id)
                return@withContext Result.success(localUser.toDomain())
            }

            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(IllegalArgumentException("No local account found for $normalizedEmail."))
            }

            val remoteProfile = firebaseAuthSyncDataSource
                .signInAndFetchProfile(normalizedEmail, password)
                .getOrElse { error ->
                    return@withContext Result.failure(
                        IllegalStateException(error.message ?: "Cloud sign-in failed.")
                    )
                } ?: return@withContext Result.failure(
                IllegalStateException("Account profile not found in cloud.")
            )

            if (!remoteProfile.isActive) {
                return@withContext Result.failure(IllegalStateException("This account is deactivated. Contact an Admin."))
            }

            val cachedEntity = UserEntity(
                id = remoteProfile.id,
                name = remoteProfile.name.ifBlank { normalizedEmail.substringBefore('@') },
                email = remoteProfile.email.lowercase(),
                passwordHash = passwordHasher.hash(password),
                role = UserRole.fromRaw(remoteProfile.role).name,
                isActive = remoteProfile.isActive,
                isSynced = true,
                updatedAt = remoteProfile.updatedAt
            )
            userDao.upsert(cachedEntity)
            syncPreferencesRepository.setSessionUserId(cachedEntity.id)
            Result.success(cachedEntity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val trimmedName = name.trim()
            val normalizedEmail = email.trim().lowercase()
            if (userDao.getByEmail(normalizedEmail) != null) {
                return@withContext Result.failure(IllegalStateException("An account with this email already exists."))
            }

            val now = System.currentTimeMillis()
            var entity = UserEntity(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                email = normalizedEmail,
                passwordHash = passwordHasher.hash(password),
                role = role.name,
                isActive = true,
                isSynced = false,
                updatedAt = now
            )
            userDao.insert(entity)
            syncPreferencesRepository.setSessionUserId(entity.id)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                val syncResult = firebaseAuthSyncDataSource.pushUserProfile(entity, rawPassword = password)
                if (syncResult.isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    userDao.markSynced(entity.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    entity = entity.copy(isSynced = true, updatedAt = syncedAt)
                }
            }
            Result.success(entity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingUsers(): Result<Int> = withContext(ioDispatcher) {
        try {
            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(IllegalStateException("Enable Cloud Sync in Settings before syncing."))
            }
            val unsynced = userDao.getUnsynced()
            var syncedCount = 0
            for (user in unsynced) {
                val result = firebaseAuthSyncDataSource.pushUserProfile(user)
                if (result.isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    userDao.markSynced(user.id, syncedAt)
                    syncedCount++
                }
            }
            if (syncedCount > 0 || unsynced.isEmpty()) {
                syncPreferencesRepository.updateLastSyncedAt(System.currentTimeMillis())
            }
            Result.success(syncedCount)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            syncPreferencesRepository.setSessionUserId(null)
            firebaseAuthSyncDataSource.signOut()
        }
    }
}
