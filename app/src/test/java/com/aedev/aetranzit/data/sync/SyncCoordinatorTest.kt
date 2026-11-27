package com.aedev.aetranzit.data.sync

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.remote.AuthSyncRemoteDataSource
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.data.remote.UserProfileRemote
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.sync.ConnectivityObserver
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.domain.sync.TransientSyncException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {

    private lateinit var companyDao: FakeCompanyDao
    private lateinit var userDao: FakeUserDao
    private lateinit var busDao: FakeBusDao
    private lateinit var routeDao: FakeRouteDao
    private lateinit var tripDao: FakeTripDao
    private lateinit var ticketDao: FakeTicketDao
    private lateinit var syncPrefs: FakeSyncPrefs
    private lateinit var authRemote: RecordingAuthRemote
    private lateinit var fleetRemote: RecordingFleetRemote
    private lateinit var connectivity: FakeConnectivityObserver
    private lateinit var scheduler: RecordingScheduler
    private lateinit var callOrderLog: MutableList<String>
    private lateinit var coordinator: SyncCoordinator

    @Before
    fun setUp() {
        companyDao = FakeCompanyDao()
        userDao = FakeUserDao()
        busDao = FakeBusDao()
        routeDao = FakeRouteDao()
        tripDao = FakeTripDao()
        ticketDao = FakeTicketDao()
        syncPrefs = FakeSyncPrefs()
        callOrderLog = mutableListOf()
        authRemote = RecordingAuthRemote(callOrderLog)
        fleetRemote = RecordingFleetRemote(callOrderLog)
        connectivity = FakeConnectivityObserver(initiallyOnline = true)
        scheduler = RecordingScheduler()

        coordinator = SyncCoordinator(
            companyDao = companyDao,
            userDao = userDao,
            busDao = busDao,
            routeDao = routeDao,
            tripDao = tripDao,
            ticketDao = ticketDao,
            syncPreferencesRepository = syncPrefs,
            authSyncDataSource = authRemote,
            fleetSyncDataSource = fleetRemote,
            connectivityObserver = connectivity,
            syncScheduler = scheduler,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `syncAll pushes pending records in strict dependency order before pulling cloud updates`() = runTest {
        val companyId = "company-victory"
        val admin = UserEntity(
            id = "admin-1",
            name = "Maria Santos",
            email = "maria@victory.ph",
            passwordHash = "hash",
            role = UserRole.ADMIN.name,
            companyId = companyId,
            isActive = true,
            isSynced = false,
            updatedAt = 100L
        )
        companyDao.upsert(
            CompanyEntity(
                id = companyId,
                name = "Victory Liner",
                joinCode = "VIC-4821",
                createdAt = 100L,
                isSynced = false,
                updatedAt = 100L
            )
        )
        userDao.upsert(admin)
        busDao.upsert(
            BusEntity(
                id = "bus-1",
                busNumber = "101",
                plateNumber = "NCA-4821",
                totalSeats = 45,
                columns = 4,
                companyId = companyId,
                isSynced = false,
                updatedAt = 100L
            )
        )
        routeDao.upsert(
            RouteEntity(
                id = "route-1",
                origin = "Cubao",
                destination = "Dau",
                baseFare = 195.0,
                stopsJson = "[]",
                companyId = companyId,
                isSynced = false,
                updatedAt = 100L
            )
        )
        tripDao.upsert(
            TripEntity(
                id = "trip-1",
                busId = "bus-1",
                routeId = "route-1",
                conductorId = admin.id,
                companyId = companyId,
                status = "ACTIVE",
                startedAt = 100L,
                endedAt = null,
                isSynced = false,
                updatedAt = 100L
            )
        )
        ticketDao.upsert(
            TicketEntity(
                id = "ticket-1",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = admin.id,
                companyId = companyId,
                seatNumber = "5",
                destination = "Dau",
                passengerType = "REGULAR",
                fareAmount = 195.0,
                cashTendered = 200.0,
                changeAmount = 5.0,
                status = "ACTIVE",
                createdAt = 100L,
                isSynced = false,
                updatedAt = 100L
            )
        )
        syncPrefs.setSessionUserId(admin.id)

        val summary = coordinator.syncAll().getOrThrow()

        assertEquals(6, summary.pushedCount)
        assertEquals(SyncStatus.Synced, coordinator.syncState.value)
        assertNotNull(syncPrefs.lastSyncedAt.first())

        assertEquals(
            listOf(
                "push:company:company-victory",
                "push:user:admin-1",
                "push:bus:bus-1",
                "push:route:route-1",
                "push:trip:trip-1",
                "push:ticket:ticket-1",
                "pull:company:company-victory",
                "pull:users:company-victory",
                "pull:buses:company-victory",
                "pull:routes:company-victory",
                "pull:trips:company-victory:ALL",
                "pull:tickets:company-victory:ALL"
            ),
            callOrderLog
        )

        assertTrue(companyDao.getById(companyId)!!.isSynced)
        assertTrue(userDao.getById(admin.id)!!.isSynced)
        assertTrue(busDao.getByIdAndCompany("bus-1", companyId)!!.isSynced)
        assertTrue(routeDao.getByIdAndCompany("route-1", companyId)!!.isSynced)
        assertTrue(tripDao.getByIdAndCompany("trip-1", companyId)!!.isSynced)
        assertTrue(ticketDao.getByIdAndCompany("ticket-1", companyId)!!.isSynced)
    }

    @Test
    fun `syncAll updates synced local rows when cloud is newer and never overwrites unsynced local writes`() = runTest {
        val companyId = "company-alpha"
        val conductor = UserEntity(
            id = "cond-1",
            name = "Ramon Magsaysay",
            email = "ramon@alpha.ph",
            passwordHash = "hash",
            role = UserRole.CONDUCTOR.name,
            companyId = companyId,
            isActive = true,
            isSynced = true,
            updatedAt = 1_000L
        )
        userDao.upsert(conductor)
        syncPrefs.setSessionUserId(conductor.id)

        // Bus A is already synced locally at t=1000; cloud has newer t=2000 -> should update!
        busDao.upsert(
            BusEntity(
                id = "bus-synced",
                busNumber = "101",
                plateNumber = "OLD-111",
                totalSeats = 45,
                columns = 4,
                companyId = companyId,
                isSynced = true,
                updatedAt = 1_000L
            )
        )
        // Bus B has an unsynced local edit (isSynced = false) at t=1500; cloud has t=5000 -> MUST NOT overwrite!
        // Note: Conductor doesn't push buses, so bus-unsynced stays isSynced = false during pull phase.
        busDao.upsert(
            BusEntity(
                id = "bus-unsynced",
                busNumber = "202-LOCAL-PENDING",
                plateNumber = "LOC-222",
                totalSeats = 50,
                columns = 5,
                companyId = companyId,
                isSynced = false,
                updatedAt = 1_500L
            )
        )

        fleetRemote.remoteBuses = listOf(
            BusEntity(
                id = "bus-synced",
                busNumber = "101-UPDATED",
                plateNumber = "NEW-999",
                totalSeats = 49,
                columns = 5,
                companyId = companyId,
                isSynced = true,
                updatedAt = 2_000L
            ),
            BusEntity(
                id = "bus-unsynced",
                busNumber = "202-STALE-CLOUD-OVERWRITE",
                plateNumber = "CLD-888",
                totalSeats = 40,
                columns = 4,
                companyId = companyId,
                isSynced = true,
                updatedAt = 5_000L
            ),
            BusEntity(
                id = "bus-new-from-cloud",
                busNumber = "303",
                plateNumber = "CLD-303",
                totalSeats = 45,
                columns = 4,
                companyId = companyId,
                isSynced = true,
                updatedAt = 3_000L
            )
        )

        val summary = coordinator.syncAll().getOrThrow()

        // Only bus-synced (newer) and bus-new-from-cloud (missing locally) should be pulled (2 records)
        assertEquals(2, summary.pulledCount)
        assertEquals("NEW-999", busDao.getByIdAndCompany("bus-synced", companyId)?.plateNumber)

        val preservedUnsynced = busDao.getByIdAndCompany("bus-unsynced", companyId)!!
        assertEquals("202-LOCAL-PENDING", preservedUnsynced.busNumber)
        assertFalse(preservedUnsynced.isSynced)

        assertNotNull(busDao.getByIdAndCompany("bus-new-from-cloud", companyId))
    }

    @Test
    fun `syncAll scopes Conductor pull queries to own conductorId and skips Admin-only users list`() = runTest {
        val companyId = "company-beta"
        val conductor = UserEntity(
            id = "cond-42",
            name = "Juan Luna",
            email = "juan@beta.ph",
            passwordHash = "hash",
            role = UserRole.CONDUCTOR.name,
            companyId = companyId,
            isActive = true,
            isSynced = true,
            updatedAt = 500L
        )
        userDao.upsert(conductor)
        syncPrefs.setSessionUserId(conductor.id)

        coordinator.syncAll().getOrThrow()

        assertFalse(callOrderLog.any { it.startsWith("pull:users:") })
        assertTrue(callOrderLog.contains("pull:trips:company-beta:cond-42"))
        assertTrue(callOrderLog.contains("pull:tickets:company-beta:cond-42"))
    }

    @Test
    fun `syncAll returns TransientSyncException and sets Offline state when device has no internet`() = runTest {
        connectivity.setOnline(false)
        syncPrefs.setSessionUserId("admin-1")

        val result = coordinator.syncAll()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TransientSyncException)
        assertEquals(SyncStatus.Offline, coordinator.syncState.value)
        assertTrue(callOrderLog.isEmpty())
    }

    @Test
    fun `startAutoSyncMonitor enqueues immediate sync when connectivity recovers from offline to online`() = runTest {
        connectivity.setOnline(false)
        syncPrefs.setSessionUserId("cond-1")

        coordinator.startAutoSyncMonitor(backgroundScope)
        testScheduler.runCurrent()
        assertEquals(SyncStatus.Offline, coordinator.syncState.value)
        assertEquals(0, scheduler.immediateCount)

        connectivity.setOnline(true)
        testScheduler.runCurrent()
        assertTrue(scheduler.immediateCount >= 1)
    }
}

private class FakeConnectivityObserver(initiallyOnline: Boolean) : ConnectivityObserver {
    private val state = MutableStateFlow(initiallyOnline)
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()
    override fun isCurrentlyOnline(): Boolean = state.value
    fun setOnline(online: Boolean) {
        state.value = online
    }
}

private class RecordingScheduler : SyncScheduler {
    var immediateCount = 0
    var periodicCount = 0
    var cancelledCount = 0
    override fun enqueueImmediateSync() {
        immediateCount++
    }
    override fun schedulePeriodicSync() {
        periodicCount++
    }
    override fun cancelAllSync() {
        cancelledCount++
    }
}

private class FakeSyncPrefs : SyncPreferencesRepository {
    private val enabled = MutableStateFlow(true)
    private val lastSynced = MutableStateFlow<Long?>(null)
    private val sessionUser = MutableStateFlow<String?>(null)
    private val onboardingCompleted = MutableStateFlow(false)

    override val isCloudSyncEnabled: Flow<Boolean> = enabled
    override val lastSyncedAt: Flow<Long?> = lastSynced
    override val sessionUserId: Flow<String?> = sessionUser
    override val hasCompletedOnboarding: Flow<Boolean> = onboardingCompleted

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
    override suspend fun updateLastSyncedAt(timestamp: Long) {
        lastSynced.value = timestamp
    }
    override suspend fun setSessionUserId(userId: String?) {
        sessionUser.value = userId
    }
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted.value = completed
    }
}

private class RecordingAuthRemote(
    private val log: MutableList<String>
) : AuthSyncRemoteDataSource {
    override suspend fun pushCompanySetup(
        company: CompanyEntity,
        user: UserEntity
    ): Result<Unit> = Result.success(Unit)

    override suspend fun provisionStaffAccount(
        user: UserEntity,
        temporaryPassword: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateCurrentUserPasswordAndProfile(
        user: UserEntity,
        newPassword: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun ensureFirebaseAuthSession(
        user: UserEntity,
        company: CompanyEntity?,
        rawPassword: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun pushUserProfile(user: UserEntity, rawPassword: String?): Result<Unit> {
        log += "push:user:${user.id}"
        return Result.success(Unit)
    }

    override suspend fun signInAndFetchProfile(
        email: String,
        password: String
    ): Result<UserProfileRemote?> = Result.success(null)

    override fun signOut() = Unit
}

private class RecordingFleetRemote(
    private val log: MutableList<String>
) : FleetSyncRemoteDataSource {
    var remoteCompany: CompanyEntity? = null
    var remoteUsers: List<UserEntity> = emptyList()
    var remoteBuses: List<BusEntity> = emptyList()
    var remoteRoutes: List<RouteEntity> = emptyList()
    var remoteTrips: List<TripEntity> = emptyList()
    var remoteTickets: List<TicketEntity> = emptyList()

    override suspend fun pushCompany(company: CompanyEntity): Result<Unit> {
        log += "push:company:${company.id}"
        return Result.success(Unit)
    }

    override suspend fun fetchCompanyById(companyId: String): Result<CompanyEntity?> {
        log += "pull:company:$companyId"
        return Result.success(remoteCompany)
    }

    override suspend fun fetchUsersByCompany(companyId: String): Result<List<UserEntity>> {
        log += "pull:users:$companyId"
        return Result.success(remoteUsers)
    }

    override suspend fun fetchBusesByCompany(companyId: String): Result<List<BusEntity>> {
        log += "pull:buses:$companyId"
        return Result.success(remoteBuses)
    }

    override suspend fun fetchRoutesByCompany(companyId: String): Result<List<RouteEntity>> {
        log += "pull:routes:$companyId"
        return Result.success(remoteRoutes)
    }

    override suspend fun fetchTripsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TripEntity>> {
        log += "pull:trips:$companyId:${conductorId ?: "ALL"}"
        return Result.success(remoteTrips)
    }

    override suspend fun fetchTicketsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TicketEntity>> {
        log += "pull:tickets:$companyId:${conductorId ?: "ALL"}"
        return Result.success(remoteTickets)
    }

    override suspend fun pushBus(bus: BusEntity): Result<Unit> {
        log += "push:bus:${bus.id}"
        return Result.success(Unit)
    }

    override suspend fun deleteBus(busId: String): Result<Unit> = Result.success(Unit)

    override suspend fun pushRoute(route: RouteEntity): Result<Unit> {
        log += "push:route:${route.id}"
        return Result.success(Unit)
    }

    override suspend fun deleteRoute(routeId: String): Result<Unit> = Result.success(Unit)

    override suspend fun pushTrip(trip: TripEntity): Result<Unit> {
        log += "push:trip:${trip.id}"
        return Result.success(Unit)
    }

    override suspend fun pushTicketAndOccupiedSeats(
        ticket: TicketEntity,
        occupiedSeats: List<String>
    ): Result<Unit> {
        log += "push:ticket:${ticket.id}"
        return Result.success(Unit)
    }
}

private class FakeCompanyDao : CompanyDao {
    private val items = MutableStateFlow<List<CompanyEntity>>(emptyList())
    override suspend fun insert(company: CompanyEntity) = upsert(company)
    override suspend fun upsert(company: CompanyEntity) {
        items.value = items.value.filterNot { it.id == company.id } + company
    }
    override suspend fun getById(id: String): CompanyEntity? =
        items.value.firstOrNull { it.id == id }
    override fun observeById(id: String): Flow<CompanyEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getByJoinCode(joinCode: String): CompanyEntity? =
        items.value.firstOrNull { it.joinCode.equals(joinCode, ignoreCase = true) }
    override suspend fun getUnsyncedByCompany(companyId: String): List<CompanyEntity> =
        items.value.filter { it.id == companyId && !it.isSynced }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.id == companyId && !it.isSynced } }
}

private class FakeUserDao : UserDao {
    private val items = MutableStateFlow<List<UserEntity>>(emptyList())
    override suspend fun insert(user: UserEntity) = upsert(user)
    override suspend fun upsert(user: UserEntity) {
        items.value = items.value.filterNot { it.id == user.id } + user
    }
    override suspend fun getByEmail(email: String): UserEntity? =
        items.value.firstOrNull { it.email.equals(email, ignoreCase = true) }
    override suspend fun getById(id: String): UserEntity? =
        items.value.firstOrNull { it.id == id }
    override fun observeById(id: String): Flow<UserEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }
    override fun observeByCompany(companyId: String): Flow<List<UserEntity>> =
        items.map { list -> list.filter { it.companyId == companyId } }
    override suspend fun updateActiveStatus(
        userId: String,
        companyId: String,
        isActive: Boolean,
        updatedAt: Long
    ) {
        items.value = items.value.map {
            if (it.id == userId && it.companyId == companyId) {
                it.copy(isActive = isActive, isSynced = false, updatedAt = updatedAt)
            } else {
                it
            }
        }
    }
    override suspend fun updateCompanyId(userId: String, companyId: String, updatedAt: Long) {
        items.value = items.value.map {
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
        items.value = items.value.map {
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
        items.value = items.value.filterNot { it.id == id }
    }
    override suspend fun getUnsyncedByCompany(companyId: String): List<UserEntity> =
        items.value.filter { it.companyId == companyId && !it.isSynced }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}

private class FakeBusDao : BusDao {
    private val items = MutableStateFlow<List<BusEntity>>(emptyList())
    override suspend fun upsert(bus: BusEntity) {
        items.value = items.value.filterNot { it.id == bus.id } + bus
    }
    override fun observeByCompany(companyId: String): Flow<List<BusEntity>> =
        items.map { list -> list.filter { it.companyId == companyId } }
    override fun observeByIdAndCompany(id: String, companyId: String): Flow<BusEntity?> =
        items.map { list -> list.firstOrNull { it.id == id && it.companyId == companyId } }
    override suspend fun getByIdAndCompany(id: String, companyId: String): BusEntity? =
        items.value.firstOrNull { it.id == id && it.companyId == companyId }
    override suspend fun deleteByIdAndCompany(id: String, companyId: String) {
        items.value = items.value.filterNot { it.id == id && it.companyId == companyId }
    }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override suspend fun getUnsyncedByCompany(companyId: String): List<BusEntity> =
        items.value.filter { it.companyId == companyId && !it.isSynced }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}

private class FakeRouteDao : RouteDao {
    private val items = MutableStateFlow<List<RouteEntity>>(emptyList())
    override suspend fun upsert(route: RouteEntity) {
        items.value = items.value.filterNot { it.id == route.id } + route
    }
    override fun observeByCompany(companyId: String): Flow<List<RouteEntity>> =
        items.map { list -> list.filter { it.companyId == companyId } }
    override fun observeByIdAndCompany(id: String, companyId: String): Flow<RouteEntity?> =
        items.map { list -> list.firstOrNull { it.id == id && it.companyId == companyId } }
    override suspend fun getByIdAndCompany(id: String, companyId: String): RouteEntity? =
        items.value.firstOrNull { it.id == id && it.companyId == companyId }
    override suspend fun deleteByIdAndCompany(id: String, companyId: String) {
        items.value = items.value.filterNot { it.id == id && it.companyId == companyId }
    }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override suspend fun getUnsyncedByCompany(companyId: String): List<RouteEntity> =
        items.value.filter { it.companyId == companyId && !it.isSynced }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}

private class FakeTripDao : TripDao {
    private val items = MutableStateFlow<List<TripEntity>>(emptyList())
    override suspend fun upsert(trip: TripEntity) {
        items.value = items.value.filterNot { it.id == trip.id } + trip
    }
    override fun observeByCompany(companyId: String): Flow<List<TripEntity>> =
        items.map { list -> list.filter { it.companyId == companyId } }
    override fun observeActiveTripByConductor(conductorId: String, companyId: String): Flow<TripEntity?> =
        items.map { list ->
            list.firstOrNull { it.conductorId == conductorId && it.companyId == companyId && it.status == "ACTIVE" }
        }
    override suspend fun getActiveTripByConductor(conductorId: String, companyId: String): TripEntity? =
        items.value.firstOrNull { it.conductorId == conductorId && it.companyId == companyId && it.status == "ACTIVE" }
    override suspend fun countActiveTripsByBus(busId: String, companyId: String): Int =
        items.value.count { it.busId == busId && it.companyId == companyId && it.status == "ACTIVE" }
    override suspend fun countActiveTripsByRoute(routeId: String, companyId: String): Int =
        items.value.count { it.routeId == routeId && it.companyId == companyId && it.status == "ACTIVE" }
    override fun observeByIdAndCompany(id: String, companyId: String): Flow<TripEntity?> =
        items.map { list -> list.firstOrNull { it.id == id && it.companyId == companyId } }
    override suspend fun getByIdAndCompany(id: String, companyId: String): TripEntity? =
        items.value.firstOrNull { it.id == id && it.companyId == companyId }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override suspend fun getUnsyncedByCompany(companyId: String): List<TripEntity> =
        items.value.filter { it.companyId == companyId && !it.isSynced }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}

private class FakeTicketDao : TicketDao {
    private val items = MutableStateFlow<List<TicketEntity>>(emptyList())
    override suspend fun upsert(ticket: TicketEntity) {
        items.value = items.value.filterNot { it.id == ticket.id } + ticket
    }
    override fun observeByCompany(companyId: String): Flow<List<TicketEntity>> =
        items.map { list -> list.filter { it.companyId == companyId } }
    override fun observeByTrip(tripId: String, companyId: String): Flow<List<TicketEntity>> =
        items.map { list -> list.filter { it.tripId == tripId && it.companyId == companyId } }
    override fun observeActiveTicketsByTrip(tripId: String, companyId: String): Flow<List<TicketEntity>> =
        items.map { list ->
            list.filter { it.tripId == tripId && it.companyId == companyId && it.status == "ACTIVE" }
        }
    override suspend fun getActiveTicketsByTrip(tripId: String, companyId: String): List<TicketEntity> =
        items.value.filter { it.tripId == tripId && it.companyId == companyId && it.status == "ACTIVE" }
    override suspend fun getActiveTicketForSeat(
        tripId: String,
        seatNumber: String,
        companyId: String
    ): TicketEntity? = items.value.firstOrNull {
        it.tripId == tripId &&
            it.seatNumber == seatNumber &&
            it.companyId == companyId &&
            it.status == "ACTIVE"
    }
    override suspend fun getByIdAndCompany(id: String, companyId: String): TicketEntity? =
        items.value.firstOrNull { it.id == id && it.companyId == companyId }
    override fun observeByIdAndCompany(id: String, companyId: String): Flow<TicketEntity?> =
        items.map { list -> list.firstOrNull { it.id == id && it.companyId == companyId } }
    override suspend fun updateStatus(
        ticketId: String,
        companyId: String,
        status: String,
        updatedAt: Long
    ) {
        items.value = items.value.map {
            if (it.id == ticketId && it.companyId == companyId) {
                it.copy(status = status, isSynced = false, updatedAt = updatedAt)
            } else {
                it
            }
        }
    }
    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }
    override suspend fun getUnsyncedByCompany(companyId: String): List<TicketEntity> =
        items.value.filter { it.companyId == companyId && !it.isSynced }
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> =
        items.map { list -> list.count { it.companyId == companyId && !it.isSynced } }
}
