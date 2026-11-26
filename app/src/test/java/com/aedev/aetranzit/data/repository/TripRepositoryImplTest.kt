package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripRepositoryImplTest {

    private lateinit var tripDao: InMemoryTripDao
    private lateinit var busDao: InMemoryBusDao
    private lateinit var routeDao: InMemoryRouteDao
    private lateinit var syncPrefs: FakeSyncPreferencesRepository
    private lateinit var remoteSync: FakeFleetSyncRemoteDataSource
    private lateinit var repository: TripRepositoryImpl

    @Before
    fun setUp() = runTest {
        tripDao = InMemoryTripDao()
        busDao = InMemoryBusDao()
        routeDao = InMemoryRouteDao()
        syncPrefs = FakeSyncPreferencesRepository()
        remoteSync = FakeFleetSyncRemoteDataSource()

        busDao.upsert(
            BusEntity(
                id = "bus-1",
                busNumber = "101",
                plateNumber = "NCA-4821",
                totalSeats = 45,
                columns = 4,
                isSynced = false,
                updatedAt = 1L
            )
        )
        busDao.upsert(
            BusEntity(
                id = "bus-2",
                busNumber = "204",
                plateNumber = "NDB-9910",
                totalSeats = 55,
                columns = 5,
                isSynced = false,
                updatedAt = 1L
            )
        )
        routeDao.upsert(
            RouteEntity(
                id = "route-1",
                origin = "Cubao",
                destination = "Dau",
                baseFare = 195.0,
                stopsJson = "[]",
                isSynced = false,
                updatedAt = 1L
            )
        )

        repository = TripRepositoryImpl(
            tripDao = tripDao,
            busDao = busDao,
            routeDao = routeDao,
            syncPreferencesRepository = syncPrefs,
            fleetSyncDataSource = remoteSync,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `startTrip creates active trip and blocks starting a second active trip for same conductor`() = runTest {
        val firstResult = repository.startTrip(
            conductorId = "cond-1",
            busId = "bus-1",
            routeId = "route-1"
        )
        assertTrue(firstResult.isSuccess)
        val startedTrip = firstResult.getOrThrow()
        assertEquals(TripStatus.ACTIVE, startedTrip.status)
        assertFalse(startedTrip.isSynced)
        assertEquals(startedTrip.id, repository.observeActiveTrip("cond-1").first()?.id)

        val secondResult = repository.startTrip(
            conductorId = "cond-1",
            busId = "bus-2",
            routeId = "route-1"
        )
        assertTrue(secondResult.isFailure)
    }

    @Test
    fun `endTrip marks active trip COMPLETED and allows starting a subsequent trip`() = runTest {
        val trip = repository.startTrip(
            conductorId = "cond-1",
            busId = "bus-1",
            routeId = "route-1"
        ).getOrThrow()

        val ended = repository.endTrip(trip.id).getOrThrow()
        assertEquals(TripStatus.COMPLETED, ended.status)
        assertNotNull(ended.endedAt)
        assertNull(repository.observeActiveTrip("cond-1").first())

        val nextTripResult = repository.startTrip(
            conductorId = "cond-1",
            busId = "bus-1",
            routeId = "route-1"
        )
        assertTrue(nextTripResult.isSuccess)
    }

    @Test
    fun `syncPendingTrips syncs unsynced trips when cloud sync is enabled`() = runTest {
        repository.startTrip("cond-1", "bus-1", "route-1").getOrThrow()
        assertTrue(repository.syncPendingTrips().isFailure)

        syncPrefs.setCloudSyncEnabled(true)
        val syncedCount = repository.syncPendingTrips().getOrThrow()
        assertEquals(1, syncedCount)
        assertEquals(0, repository.observeUnsyncedCount().first())
    }
}

private class InMemoryTripDao : TripDao {
    private val items = MutableStateFlow<List<TripEntity>>(emptyList())

    override suspend fun upsert(trip: TripEntity) {
        items.value = items.value.filterNot { it.id == trip.id } + trip
    }

    override fun observeByConductor(conductorId: String): Flow<List<TripEntity>> =
        items.map { list -> list.filter { it.conductorId == conductorId } }

    override fun observeActiveTripByConductor(conductorId: String): Flow<TripEntity?> =
        items.map { list ->
            list.firstOrNull { it.conductorId == conductorId && it.status == "ACTIVE" }
        }

    override suspend fun getActiveTripByConductor(conductorId: String): TripEntity? =
        items.value.firstOrNull { it.conductorId == conductorId && it.status == "ACTIVE" }

    override suspend fun countActiveTripsByBus(busId: String): Int =
        items.value.count { it.busId == busId && it.status == "ACTIVE" }

    override suspend fun countActiveTripsByRoute(routeId: String): Int =
        items.value.count { it.routeId == routeId && it.status == "ACTIVE" }

    override fun observeById(id: String): Flow<TripEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: String): TripEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }

    override suspend fun getUnsynced(): List<TripEntity> =
        items.value.filter { !it.isSynced }

    override fun observeUnsyncedCount(): Flow<Int> =
        items.map { list -> list.count { !it.isSynced } }
}

private class InMemoryBusDao : BusDao {
    private val items = MutableStateFlow<List<BusEntity>>(emptyList())

    override suspend fun upsert(bus: BusEntity) {
        items.value = items.value.filterNot { it.id == bus.id } + bus
    }

    override fun observeAll(): Flow<List<BusEntity>> = items

    override fun observeById(id: String): Flow<BusEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: String): BusEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }

    override suspend fun getUnsynced(): List<BusEntity> = items.value.filter { !it.isSynced }

    override fun observeUnsyncedCount(): Flow<Int> =
        items.map { list -> list.count { !it.isSynced } }
}

private class InMemoryRouteDao : RouteDao {
    private val items = MutableStateFlow<List<RouteEntity>>(emptyList())

    override suspend fun upsert(route: RouteEntity) {
        items.value = items.value.filterNot { it.id == route.id } + route
    }

    override fun observeAll(): Flow<List<RouteEntity>> = items

    override fun observeById(id: String): Flow<RouteEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: String): RouteEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun markSynced(id: String, syncedAt: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isSynced = true, updatedAt = syncedAt) else it
        }
    }

    override suspend fun getUnsynced(): List<RouteEntity> = items.value.filter { !it.isSynced }

    override fun observeUnsyncedCount(): Flow<Int> =
        items.map { list -> list.count { !it.isSynced } }
}

private class FakeSyncPreferencesRepository : SyncPreferencesRepository {
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

private class FakeFleetSyncRemoteDataSource : FleetSyncRemoteDataSource {
    override suspend fun pushBus(bus: BusEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBus(busId: String): Result<Unit> = Result.success(Unit)
    override suspend fun pushRoute(route: RouteEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRoute(routeId: String): Result<Unit> = Result.success(Unit)
    override suspend fun pushTrip(trip: TripEntity): Result<Unit> = Result.success(Unit)
    override suspend fun pushTicketAndOccupiedSeats(
        ticket: TicketEntity,
        occupiedSeats: List<String>
    ): Result<Unit> = Result.success(Unit)
}
