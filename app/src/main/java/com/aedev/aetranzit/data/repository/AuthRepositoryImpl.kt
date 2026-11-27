package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.AuthSyncRemoteDataSource
import com.aedev.aetranzit.data.security.PasswordHasher
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.sync.NoOpSyncScheduler
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.sync.TransientSyncException
import com.aedev.aetranzit.domain.usecase.CompanyJoinCodeGenerator
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
    private val companyDao: CompanyDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val transactionRunner: DatabaseTransactionRunner,
    private val passwordHasher: PasswordHasher,
    private val joinCodeGenerator: CompanyJoinCodeGenerator,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val authSyncDataSource: AuthSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> =
        syncPreferencesRepository.sessionUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(null)
            } else {
                userDao.observeById(userId).map { entity ->
                    entity?.takeIf { it.isActive }?.toDomain()
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllUsers(): Flow<List<User>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                userDao.observeByCompany(companyId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                userDao.observeUnsyncedCountByCompany(companyId)
            }
        }

    override suspend fun getCurrentUser(): User? = withContext(ioDispatcher) {
        val userId = syncPreferencesRepository.sessionUserId.first() ?: return@withContext null
        userDao.getById(userId)?.takeIf { it.isActive }?.toDomain()
    }

    override suspend fun login(email: String, password: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                val normalizedEmail = email.trim().lowercase()
                val localUser = userDao.getByEmail(normalizedEmail)
                val isCloudEnabled = syncPreferencesRepository.isCloudSyncEnabled.first()

                if (localUser != null && localUser.passwordHash.isNotBlank()) {
                    if (!passwordHasher.verify(password, localUser.passwordHash)) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Incorrect email or password.")
                        )
                    }
                    if (!localUser.isActive) {
                        return@withContext Result.failure(
                            IllegalStateException("This account is deactivated. Contact an Admin.")
                        )
                    }
                    syncPreferencesRepository.setSessionUserId(localUser.id)
                    if (isCloudEnabled) {
                        val company = localUser.companyId
                            .takeIf { it.isNotBlank() }
                            ?.let { companyDao.getById(it) }
                        authSyncDataSource.ensureFirebaseAuthSession(
                            user = localUser,
                            company = company,
                            rawPassword = password
                        )
                        if (localUser.companyId.isNotBlank()) {
                            syncScheduler.enqueueImmediateSync()
                        }
                    }
                    return@withContext Result.success(localUser.toDomain())
                }

                if (!isCloudEnabled) {
                    val message = if (localUser != null) {
                        "Enable Cloud Sync and sign in while online once to activate this staff account on this device."
                    } else {
                        "No local account found for $normalizedEmail."
                    }
                    return@withContext Result.failure(IllegalArgumentException(message))
                }

                val remoteProfile = authSyncDataSource
                    .signInAndFetchProfile(normalizedEmail, password)
                    .getOrElse { error ->
                        return@withContext Result.failure(
                            IllegalStateException(error.message ?: "Cloud sign-in failed.")
                        )
                    } ?: return@withContext Result.failure(
                    IllegalStateException("Account profile not found in cloud.")
                )

                if (!remoteProfile.isActive) {
                    return@withContext Result.failure(
                        IllegalStateException("This account is deactivated. Contact an Admin.")
                    )
                }

                remoteProfile.company?.let { remoteCompany ->
                    companyDao.upsert(remoteCompany)
                }

                val cachedEntity = UserEntity(
                    id = localUser?.id ?: remoteProfile.id,
                    name = remoteProfile.name.ifBlank { normalizedEmail.substringBefore('@') },
                    email = remoteProfile.email.lowercase(),
                    passwordHash = passwordHasher.hash(password),
                    role = UserRole.fromRaw(remoteProfile.role).name,
                    companyId = remoteProfile.companyId,
                    isActive = remoteProfile.isActive,
                    mustChangePassword = remoteProfile.mustChangePassword,
                    isSynced = true,
                    updatedAt = remoteProfile.updatedAt
                )
                userDao.upsert(cachedEntity)
                syncPreferencesRepository.setSessionUserId(cachedEntity.id)
                if (cachedEntity.companyId.isNotBlank()) {
                    syncScheduler.enqueueImmediateSync()
                }
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
        password: String
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val trimmedName = name.trim()
            val normalizedEmail = email.trim().lowercase()
            if (trimmedName.length < 2) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter your full name.")
                )
            }
            if (!EMAIL_REGEX.matches(normalizedEmail)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter a valid email address.")
                )
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters.")
                )
            }
            if (userDao.getByEmail(normalizedEmail) != null) {
                return@withContext Result.failure(
                    IllegalStateException("An account with this email already exists.")
                )
            }

            val now = System.currentTimeMillis()
            var createdUser = UserEntity(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                email = normalizedEmail,
                passwordHash = passwordHasher.hash(password),
                role = UserRole.ADMIN.name,
                companyId = "",
                isActive = true,
                mustChangePassword = false,
                isSynced = false,
                updatedAt = now
            )
            userDao.insert(createdUser)
            syncPreferencesRepository.setSessionUserId(createdUser.id)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                val syncResult = authSyncDataSource.pushUserProfile(createdUser, rawPassword = password)
                if (syncResult.isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    userDao.markSynced(createdUser.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    createdUser = createdUser.copy(isSynced = true, updatedAt = syncedAt)
                }
            }

            Result.success(createdUser.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeCompanySetup(companyName: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                val trimmedCompanyName = companyName.trim()
                if (trimmedCompanyName.length < 2) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Bus company / business name must be at least 2 characters.")
                    )
                }

                val sessionUserId = syncPreferencesRepository.sessionUserId.first()
                    ?: return@withContext Result.failure(
                        IllegalStateException("No signed-in Admin session found.")
                    )

                val adminEntity = userDao.getById(sessionUserId)
                    ?.takeIf { it.isActive }
                    ?: return@withContext Result.failure(
                        IllegalStateException("Admin session is no longer active.")
                    )

                if (UserRole.fromRaw(adminEntity.role) != UserRole.ADMIN) {
                    return@withContext Result.failure(
                        IllegalStateException("Only Admin accounts can set up a bus company.")
                    )
                }

                if (adminEntity.companyId.isNotBlank()) {
                    return@withContext Result.success(adminEntity.toDomain())
                }

                val generatedCode = generateUniqueLocalJoinCode(trimmedCompanyName)
                val now = System.currentTimeMillis()
                val companyId = UUID.randomUUID().toString()

                val (createdCompany, updatedAdmin) = transactionRunner.runInTransaction {
                    val companyEntity = CompanyEntity(
                        id = companyId,
                        name = trimmedCompanyName,
                        joinCode = generatedCode,
                        createdAt = now,
                        isSynced = false,
                        updatedAt = now
                    )
                    companyDao.insert(companyEntity)
                    userDao.updateCompanyId(
                        userId = adminEntity.id,
                        companyId = companyId,
                        updatedAt = now
                    )
                    val updatedUser = adminEntity.copy(
                        companyId = companyId,
                        isSynced = false,
                        updatedAt = now
                    )
                    companyEntity to updatedUser
                }

                var finalUser = updatedAdmin
                if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                    val syncResult = authSyncDataSource.pushCompanySetup(
                        company = createdCompany,
                        user = updatedAdmin
                    )
                    if (syncResult.isSuccess) {
                        val syncedAt = System.currentTimeMillis()
                        companyDao.markSynced(createdCompany.id, syncedAt)
                        userDao.markSynced(updatedAdmin.id, syncedAt)
                        syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                        finalUser = updatedAdmin.copy(isSynced = true, updatedAt = syncedAt)
                    }
                    syncScheduler.enqueueImmediateSync()
                }

                Result.success(finalUser.toDomain())
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createStaffAccount(
        name: String,
        email: String,
        temporaryPassword: String,
        role: UserRole
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val trimmedName = name.trim()
            val normalizedEmail = email.trim().lowercase()
            if (trimmedName.length < 2) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter the staff member's full name.")
                )
            }
            if (!EMAIL_REGEX.matches(normalizedEmail)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter a valid email address.")
                )
            }
            if (temporaryPassword.length < MIN_PASSWORD_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException("Temporary password must be at least $MIN_PASSWORD_LENGTH characters.")
                )
            }

            val sessionUserId = syncPreferencesRepository.sessionUserId.first()
                ?: return@withContext Result.failure(
                    IllegalStateException("Admin session required to add staff.")
                )
            val callerAdmin = userDao.getById(sessionUserId)
                ?.takeIf { it.isActive }
                ?: return@withContext Result.failure(
                    IllegalStateException("Active Admin account required.")
                )
            if (UserRole.fromRaw(callerAdmin.role) != UserRole.ADMIN) {
                return@withContext Result.failure(
                    IllegalStateException("Only Admins can provision staff accounts.")
                )
            }
            val adminCompanyId = callerAdmin.companyId.trim()
            if (adminCompanyId.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Complete your Business Setup before adding staff.")
                )
            }

            if (userDao.getByEmail(normalizedEmail) != null) {
                return@withContext Result.failure(
                    IllegalStateException("A staff account with email $normalizedEmail already exists.")
                )
            }

            val now = System.currentTimeMillis()
            var staffEntity = UserEntity(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                email = normalizedEmail,
                passwordHash = passwordHasher.hash(temporaryPassword),
                role = role.name,
                companyId = adminCompanyId,
                isActive = true,
                mustChangePassword = true,
                isSynced = false,
                updatedAt = now
            )
            userDao.insert(staffEntity)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                val provisionResult = authSyncDataSource.provisionStaffAccount(
                    user = staffEntity,
                    temporaryPassword = temporaryPassword
                )
                if (provisionResult.isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    userDao.markSynced(staffEntity.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    staffEntity = staffEntity.copy(isSynced = true, updatedAt = syncedAt)
                } else {
                    val error = provisionResult.exceptionOrNull()
                    if (error != null && error !is TransientSyncException) {
                        userDao.deleteById(staffEntity.id)
                        return@withContext Result.failure(error)
                    }
                }
                syncScheduler.enqueueImmediateSync()
            }

            Result.success(staffEntity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeForcedPasswordChange(newPassword: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                if (newPassword.length < MIN_PASSWORD_LENGTH) {
                    return@withContext Result.failure(
                        IllegalArgumentException("New password must be at least $MIN_PASSWORD_LENGTH characters.")
                    )
                }

                val sessionUserId = syncPreferencesRepository.sessionUserId.first()
                    ?: return@withContext Result.failure(
                        IllegalStateException("No signed-in session found.")
                    )
                val existing = userDao.getById(sessionUserId)
                    ?.takeIf { it.isActive }
                    ?: return@withContext Result.failure(
                        IllegalStateException("Account is not active.")
                    )

                if (existing.passwordHash.isNotBlank() &&
                    passwordHasher.verify(newPassword, existing.passwordHash)
                ) {
                    return@withContext Result.failure(
                        IllegalArgumentException("New password must be different from your temporary password.")
                    )
                }

                val now = System.currentTimeMillis()
                val newHash = passwordHasher.hash(newPassword)
                userDao.updatePasswordAndClearForceChange(
                    userId = existing.id,
                    passwordHash = newHash,
                    updatedAt = now
                )

                var updatedEntity = existing.copy(
                    passwordHash = newHash,
                    mustChangePassword = false,
                    isSynced = false,
                    updatedAt = now
                )

                if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                    val remoteResult = authSyncDataSource.updateCurrentUserPasswordAndProfile(
                        user = updatedEntity,
                        newPassword = newPassword
                    )
                    if (remoteResult.isSuccess) {
                        val syncedAt = System.currentTimeMillis()
                        userDao.markSynced(updatedEntity.id, syncedAt)
                        syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                        updatedEntity = updatedEntity.copy(isSynced = true, updatedAt = syncedAt)
                    }
                    if (updatedEntity.companyId.isNotBlank()) {
                        syncScheduler.enqueueImmediateSync()
                    }
                }

                Result.success(updatedEntity.toDomain())
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun setUserActiveStatus(
        userId: String,
        isActive: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val cleanUserId = userId.trim()
            if (cleanUserId.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("User ID is required.")
                )
            }
            val activeCompanyId = activeCompanyProvider.requireActiveCompanyId()
            val existing = userDao.getById(cleanUserId)
                ?.takeIf { it.companyId == activeCompanyId }
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Staff account not found in your company.")
                )

            val activeSessionId = syncPreferencesRepository.sessionUserId.first()
            if (!isActive && activeSessionId == existing.id) {
                return@withContext Result.failure(
                    IllegalStateException("You cannot deactivate your own currently signed-in Admin account.")
                )
            }

            val now = System.currentTimeMillis()
            userDao.updateActiveStatus(
                userId = existing.id,
                companyId = activeCompanyId,
                isActive = isActive,
                updatedAt = now
            )

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                val updatedEntity = existing.copy(
                    isActive = isActive,
                    isSynced = false,
                    updatedAt = now
                )
                if (authSyncDataSource.pushUserProfile(updatedEntity).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    userDao.markSynced(existing.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                }
                syncScheduler.enqueueImmediateSync()
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            syncPreferencesRepository.setSessionUserId(null)
            authSyncDataSource.signOut()
        }
    }

    private suspend fun generateUniqueLocalJoinCode(companyName: String): String {
        repeat(MAX_JOIN_CODE_ATTEMPTS) {
            val candidate = joinCodeGenerator.generate(companyName)
            if (companyDao.getByJoinCode(candidate) == null) {
                return candidate
            }
        }
        return joinCodeGenerator.generate(companyName)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_JOIN_CODE_ATTEMPTS = 12
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
