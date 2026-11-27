package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.remote.AuthSyncRemoteDataSource
import com.aedev.aetranzit.data.remote.UserProfileRemote
import com.aedev.aetranzit.data.security.PasswordHasher
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.sync.TransientSyncException
import com.aedev.aetranzit.domain.usecase.CompanyJoinCodeGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var userDao: InMemoryAuthUserDao
    private lateinit var companyDao: InMemoryCompanyDao
    private lateinit var syncPrefs: FakeAuthSyncPrefs
    private lateinit var activeCompanyProvider: RoomActiveCompanyProvider
    private lateinit var remoteSync: FakeAuthSyncRemoteDataSource
    private lateinit var syncScheduler: RecordingSyncScheduler
    private lateinit var joinCodeGenerator: CompanyJoinCodeGenerator
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        userDao = InMemoryAuthUserDao()
        companyDao = InMemoryCompanyDao()
        syncPrefs = FakeAuthSyncPrefs()
        activeCompanyProvider = RoomActiveCompanyProvider(syncPrefs, userDao)
        remoteSync = FakeAuthSyncRemoteDataSource()
        syncScheduler = RecordingSyncScheduler()
        joinCodeGenerator = CompanyJoinCodeGenerator()
        passwordHasher = PasswordHasher()

        repository = AuthRepositoryImpl(
            userDao = userDao,
            companyDao = companyDao,
            activeCompanyProvider = activeCompanyProvider,
            transactionRunner = DirectTransactionRunner(),
            passwordHasher = passwordHasher,
            joinCodeGenerator = joinCodeGenerator,
            syncPreferencesRepository = syncPrefs,
            authSyncDataSource = remoteSync,
            ioDispatcher = UnconfinedTestDispatcher(),
            syncScheduler = syncScheduler
        )
    }

    @Test
    fun `CompanyJoinCodeGenerator formats 3-letter prefix and 4-digit code cleanly`() {
        assertEquals("VIC-4821", joinCodeGenerator.generate("Victory Liner") { 4821 })
        assertEquals("ABT-0042", joinCodeGenerator.generate("AB Transit") { 42 })
        assertEquals("ABX-0042", joinCodeGenerator.generate("AB") { 42 })
        assertEquals("BUS-9999", joinCodeGenerator.generate("12345 !!!") { 9999 })
        assertTrue(joinCodeGenerator.isValidFormat("vic-4821"))
        assertTrue(joinCodeGenerator.isValidFormat("AET-0001"))
    }

    @Test
    fun `register creates Admin-only account with empty companyId and mustChangePassword false`() = runTest {
        syncPrefs.setCloudSyncEnabled(true)

        val admin = repository.register(
            name = "Maria Santos",
            email = "maria@victory.ph",
            password = "password123"
        ).getOrThrow()

        assertEquals(UserRole.ADMIN, admin.role)
        assertEquals("", admin.companyId)
        assertFalse(admin.mustChangePassword)
        assertTrue(admin.isSynced)
        assertEquals("maria@victory.ph", remoteSync.lastPushedUser?.email)
        assertEquals("", remoteSync.lastPushedUser?.companyId)
    }

    @Test
    fun `completeCompanySetup creates CompanyEntity and assigns companyId to Admin`() = runTest {
        syncPrefs.setCloudSyncEnabled(true)

        val registeredAdmin = repository.register(
            name = "Maria Santos",
            email = "maria@victory.ph",
            password = "password123"
        ).getOrThrow()
        assertEquals("", registeredAdmin.companyId)

        val configuredAdmin = repository.completeCompanySetup("Victory Liner").getOrThrow()

        assertTrue(configuredAdmin.companyId.isNotBlank())
        assertTrue(configuredAdmin.isSynced)

        val createdCompany = companyDao.getById(configuredAdmin.companyId)
        assertNotNull(createdCompany)
        assertEquals("Victory Liner", createdCompany?.name)
        assertTrue(createdCompany!!.joinCode.startsWith("VIC-"))
        assertTrue(createdCompany.isSynced)
        assertEquals(createdCompany.id, remoteSync.lastPushedSetupCompany?.id)
        assertTrue(syncScheduler.immediateSyncCount >= 1)
    }

    @Test
    fun `createStaffAccount provisions Conductor under Admin company with mustChangePassword true`() = runTest {
        syncPrefs.setCloudSyncEnabled(true)

        repository.register(
            name = "Maria Santos",
            email = "maria@victory.ph",
            password = "password123"
        ).getOrThrow()

        // Cannot create staff before completing Business Setup
        val prematureAttempt = repository.createStaffAccount(
            name = "Ramon Magsaysay",
            email = "ramon@victory.ph",
            temporaryPassword = "Temp-4821",
            role = UserRole.CONDUCTOR
        )
        assertTrue(prematureAttempt.isFailure)

        val admin = repository.completeCompanySetup("Victory Liner").getOrThrow()

        val staff = repository.createStaffAccount(
            name = "Ramon Magsaysay",
            email = "ramon@victory.ph",
            temporaryPassword = "Temp-4821",
            role = UserRole.CONDUCTOR
        ).getOrThrow()

        assertEquals(UserRole.CONDUCTOR, staff.role)
        assertEquals(admin.companyId, staff.companyId)
        assertTrue(staff.mustChangePassword)
        assertTrue(staff.isSynced)
        assertEquals("ramon@victory.ph", remoteSync.lastProvisionedStaff?.email)
        assertTrue(remoteSync.lastProvisionedStaff?.mustChangePassword == true)
        // Admin session remains unchanged
        assertEquals(admin.id, repository.getCurrentUser()?.id)
    }

    @Test
    fun `completeForcedPasswordChange rejects reusing temporary password and clears mustChangePassword`() = runTest {
        syncPrefs.setCloudSyncEnabled(true)

        repository.register(
            name = "Maria Santos",
            email = "maria@victory.ph",
            password = "password123"
        ).getOrThrow()
        repository.completeCompanySetup("Victory Liner").getOrThrow()

        repository.createStaffAccount(
            name = "Ramon Magsaysay",
            email = "ramon@victory.ph",
            temporaryPassword = "Temp-4821",
            role = UserRole.CONDUCTOR
        ).getOrThrow()
        repository.logout()

        // Staff logs in with temporary password -> mustChangePassword == true
        val firstLogin = repository.login("ramon@victory.ph", "Temp-4821").getOrThrow()
        assertTrue(firstLogin.mustChangePassword)

        // Reusing exact temporary password fails
        val samePassAttempt = repository.completeForcedPasswordChange("Temp-4821")
        assertTrue(samePassAttempt.isFailure)

        // Setting a brand-new password clears mustChangePassword and updates passwordHash
        val updatedStaff = repository.completeForcedPasswordChange("NewSecurePass99").getOrThrow()
        assertFalse(updatedStaff.mustChangePassword)
        assertTrue(updatedStaff.isSynced)
        assertEquals("NewSecurePass99", remoteSync.lastUpdatedPassword)
        assertFalse(remoteSync.lastPasswordUpdatedProfile?.mustChangePassword ?: true)

        // Old temporary password no longer works; new password succeeds
        repository.logout()
        assertTrue(repository.login("ramon@victory.ph", "Temp-4821").isFailure)
        val relogin = repository.login("ramon@victory.ph", "NewSecurePass99").getOrThrow()
        assertFalse(relogin.mustChangePassword)
    }

    @Test
    fun `observeAllUsers strictly isolates staff lists between Company A and Company B`() = runTest {
        repository.register(
            name = "Admin Alpha",
            email = "alpha@bus.ph",
            password = "password123"
        ).getOrThrow()
        val adminA = repository.completeCompanySetup("Alpha Liner").getOrThrow()

        repository.createStaffAccount(
            name = "Conductor Alpha",
            email = "cond.alpha@bus.ph",
            temporaryPassword = "Temp-1111",
            role = UserRole.CONDUCTOR
        ).getOrThrow()
        repository.logout()

        repository.register(
            name = "Admin Beta",
            email = "beta@bus.ph",
            password = "password123"
        ).getOrThrow()
        val adminB = repository.completeCompanySetup("Beta Express").getOrThrow()
        assertNotEquals(adminA.companyId, adminB.companyId)

        // Currently signed in as Admin Beta -> only sees Beta staff
        val betaStaff = repository.observeAllUsers().first()
        assertEquals(listOf("Admin Beta"), betaStaff.map { it.name })

        // Log back in as Admin Alpha -> only sees Alpha staff (never Beta)
        repository.login("alpha@bus.ph", "password123").getOrThrow()
        val alphaStaff = repository.observeAllUsers().first()
        assertEquals(setOf("Admin Alpha", "Conductor Alpha"), alphaStaff.map { it.name }.toSet())
    }

    @Test
    fun `login links FirebaseAuth session when Cloud Sync is enabled without blocking offline login`() = runTest {
        syncPrefs.setCloudSyncEnabled(false)
        repository.register(
            name = "Offline Founder",
            email = "founder@offline.ph",
            password = "password123"
        ).getOrThrow()
        val offlineAdmin = repository.completeCompanySetup("Offline Transit").getOrThrow()
        repository.logout()

        // Enable cloud sync, simulate offline network error during session link
        syncPrefs.setCloudSyncEnabled(true)
        remoteSync.sessionLinkFailure = TransientSyncException("Network unreachable")
        val offlineLogin = repository.login("founder@offline.ph", "password123").getOrThrow()
        assertEquals(offlineAdmin.id, offlineLogin.id)
        assertEquals("founder@offline.ph", remoteSync.lastSessionLinkedEmail)
        assertTrue(syncScheduler.immediateSyncCount >= 1)

        // Now simulate online session link succeeding
        remoteSync.sessionLinkFailure = null
        val onlineLogin = repository.login("founder@offline.ph", "password123").getOrThrow()
        assertEquals(offlineAdmin.id, onlineLogin.id)
        assertEquals(offlineAdmin.companyId, remoteSync.lastSessionLinkedCompany?.id)
    }
}

private class DirectTransactionRunner : DatabaseTransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}

private class InMemoryAuthUserDao : UserDao {
    private val users = MutableStateFlow<List<UserEntity>>(emptyList())

    override suspend fun insert(user: UserEntity) {
        check(users.value.none { it.id == user.id || it.email.equals(user.email, ignoreCase = true) }) {
            "Duplicate user"
        }
        users.value = users.value + user
    }

    override suspend fun upsert(user: UserEntity) {
        users.value = users.value.filterNot { it.id == user.id } + user
    }

    override suspend fun getByEmail(email: String): UserEntity? =
        users.value.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override suspend fun getById(id: String): UserEntity? =
        users.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<UserEntity?> =
        users.map { list -> list.firstOrNull { it.id == id } }

    override fun observeByCompany(companyId: String): Flow<List<UserEntity>> =
        users.map { list -> list.filter { it.companyId == companyId } }

    override suspend fun updateActiveStatus(
        userId: String,
        companyId: String,
        isActive: Boolean,
        updatedAt: Long
    ) {
        users.value = users.value.map {
            if (it.id == userId && it.companyId == companyId) {
                it.copy(isActive = isActive, isSynced = false, updatedAt = updatedAt)
            } else {
                it
            }
        }
    }

    override suspend fun updateCompanyId(userId: String, companyId: String, updatedAt: Long) {
        users.value = users.value.map {
            if (it.id == userId) {
                it.copy(companyId = companyId, isSynced = false, updatedAt = updatedAt)
            } else {
                it
            }
        }
    }

    override suspend fun updatePasswordAndClearForceChange(
        userId: String,
        passwordHash: String,
        updatedAt: Long
    ) {
        users.value = users.value.map {
            if (it.id == userId) {
                it.copy(
                    passwordHash = passwordHash,
                    mustChangePassword = false,
                    isSynced = false,
                    updatedAt = updatedAt
                )
            } else {
                it
            }
        }
    }

    override suspend fun deleteById(id: String) {
        users.value = users.value.filterNot { it.id == id }
    }

    override suspend fun getUnsyncedByCompany(companyId: String): List<UserEntity> =
        users.value.filter { it.companyId == companyId && !it.isSynced }

    override suspend fun markSynced(id: String, syncedAt: Long) {
        users.value = users.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }

    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        users.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}

private class InMemoryCompanyDao : CompanyDao {
    private val companies = MutableStateFlow<List<CompanyEntity>>(emptyList())

    override suspend fun insert(company: CompanyEntity) {
        check(
            companies.value.none {
                it.id == company.id || it.joinCode.equals(company.joinCode, ignoreCase = true)
            }
        ) { "Duplicate company" }
        companies.value = companies.value + company
    }

    override suspend fun upsert(company: CompanyEntity) {
        companies.value = companies.value.filterNot { it.id == company.id } + company
    }

    override suspend fun getById(id: String): CompanyEntity? =
        companies.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<CompanyEntity?> =
        companies.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getByJoinCode(joinCode: String): CompanyEntity? =
        companies.value.firstOrNull { it.joinCode.equals(joinCode, ignoreCase = true) }

    override suspend fun getUnsyncedByCompany(companyId: String): List<CompanyEntity> =
        companies.value.filter { it.id == companyId && !it.isSynced }

    override suspend fun getUnsynced(): List<CompanyEntity> =
        companies.value.filter { !it.isSynced }

    override suspend fun markSynced(id: String, syncedAt: Long) {
        companies.value = companies.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }

    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        companies.map { list -> list.count { it.id == companyId && !it.isSynced } }
}

private class FakeAuthSyncPrefs : SyncPreferencesRepository {
    private val enabled = MutableStateFlow(false)
    private val lastSynced = MutableStateFlow<Long?>(null)
    private val sessionUser = MutableStateFlow<String?>(null)

    override val isCloudSyncEnabled: Flow<Boolean> = enabled
    override val lastSyncedAt: Flow<Long?> = lastSynced
    override val sessionUserId: Flow<String?> = sessionUser

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    override suspend fun updateLastSyncedAt(timestamp: Long) {
        lastSynced.value = timestamp
    }

    override suspend fun setSessionUserId(userId: String?) {
        sessionUser.value = userId
    }
}

private class RecordingSyncScheduler : SyncScheduler {
    var immediateSyncCount: Int = 0
    var periodicSyncCount: Int = 0
    var cancelledCount: Int = 0

    override fun enqueueImmediateSync() {
        immediateSyncCount++
    }

    override fun schedulePeriodicSync() {
        periodicSyncCount++
    }

    override fun cancelAllSync() {
        cancelledCount++
    }
}

private class FakeAuthSyncRemoteDataSource : AuthSyncRemoteDataSource {
    var lastPushedSetupCompany: CompanyEntity? = null
    var lastPushedUser: UserEntity? = null
    var lastProvisionedStaff: UserEntity? = null
    var lastUpdatedPassword: String? = null
    var lastPasswordUpdatedProfile: UserEntity? = null
    var lastSessionLinkedEmail: String? = null
    var lastSessionLinkedCompany: CompanyEntity? = null
    var sessionLinkFailure: Throwable? = null

    override suspend fun pushCompanySetup(
        company: CompanyEntity,
        user: UserEntity
    ): Result<Unit> {
        lastPushedSetupCompany = company
        lastPushedUser = user
        return Result.success(Unit)
    }

    override suspend fun provisionStaffAccount(
        user: UserEntity,
        temporaryPassword: String
    ): Result<Unit> {
        lastProvisionedStaff = user
        return Result.success(Unit)
    }

    override suspend fun updateCurrentUserPasswordAndProfile(
        user: UserEntity,
        newPassword: String
    ): Result<Unit> {
        lastPasswordUpdatedProfile = user
        lastUpdatedPassword = newPassword
        return Result.success(Unit)
    }

    override suspend fun ensureFirebaseAuthSession(
        user: UserEntity,
        company: CompanyEntity?,
        rawPassword: String
    ): Result<Unit> {
        lastSessionLinkedEmail = user.email
        lastSessionLinkedCompany = company
        sessionLinkFailure?.let { return Result.failure(it) }
        return Result.success(Unit)
    }

    override suspend fun pushUserProfile(user: UserEntity, rawPassword: String?): Result<Unit> {
        lastPushedUser = user
        return Result.success(Unit)
    }

    override suspend fun signInAndFetchProfile(
        email: String,
        password: String
    ): Result<UserProfileRemote?> = Result.success(null)

    override fun signOut() = Unit
}
