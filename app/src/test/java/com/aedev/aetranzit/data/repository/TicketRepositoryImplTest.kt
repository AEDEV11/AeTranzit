package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketRepositoryImplTest {

    private lateinit var ticketDao: FakeTicketDao
    private lateinit var tripDao: FakeTripDaoForTickets
    private lateinit var transactionRunner: MutexTransactionRunner
    private lateinit var syncPrefs: FakeTicketSyncPrefs
    private lateinit var remoteSync: RecordingFleetSyncDataSource
    private lateinit var repository: TicketRepositoryImpl

    @Before
    fun setUp() = runTest {
        ticketDao = FakeTicketDao()
        tripDao = FakeTripDaoForTickets()
        transactionRunner = MutexTransactionRunner()
        syncPrefs = FakeTicketSyncPrefs()
        remoteSync = RecordingFleetSyncDataSource()

        tripDao.upsert(
            TripEntity(
                id = "trip-active-1",
                busId = "bus-101",
                routeId = "route-1",
                conductorId = "cond-1",
                status = TripStatus.ACTIVE.name,
                startedAt = 1_000L,
                endedAt = null,
                isSynced = false,
                updatedAt = 1_000L
            )
        )

        repository = TicketRepositoryImpl(
            ticketDao = ticketDao,
            tripDao = tripDao,
            transactionRunner = transactionRunner,
            syncPreferencesRepository = syncPrefs,
            fleetSyncDataSource = remoteSync,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `issueTicket atomically rejects duplicate booking on the same active seat`() = runTest {
        val concurrentAttempts = listOf(
            async {
                repository.issueTicket(
                    tripId = "trip-active-1",
                    seatNumber = "12",
                    destination = "Dau",
                    passengerType = PassengerType.REGULAR,
                    fareAmount = 195.0,
                    cashTendered = 200.0
                )
            },
            async {
                repository.issueTicket(
                    tripId = "trip-active-1",
                    seatNumber = "12",
                    destination = "San Fernando",
                    passengerType = PassengerType.STUDENT,
                    fareAmount = 112.0,
                    cashTendered = 150.0
                )
            }
        ).awaitAll()

        val successes = concurrentAttempts.filter { it.isSuccess }
        val failures = concurrentAttempts.filter { it.isFailure }

        assertEquals(1, successes.size)
        assertEquals(1, failures.size)
        assertEquals(1, repository.observeActiveTicketsByTrip("trip-active-1").first().size)
    }

    @Test
    fun `markPassengerDroppedOff frees seat so a new ticket can be issued for the same seat`() = runTest {
        val firstTicket = repository.issueTicket(
            tripId = "trip-active-1",
            seatNumber = "5",
            destination = "Bocaue",
            passengerType = PassengerType.REGULAR,
            fareAmount = 65.0,
            cashTendered = 100.0
        ).getOrThrow()

        assertEquals(35.0, firstTicket.changeAmount, 0.001)
        assertEquals(1, repository.observeActiveTicketsByTrip("trip-active-1").first().size)

        val dropped = repository.markPassengerDroppedOff(firstTicket.id).getOrThrow()
        assertEquals(TicketStatus.DROPPED_OFF, dropped.status)
        assertTrue(repository.observeActiveTicketsByTrip("trip-active-1").first().isEmpty())

        val reissued = repository.issueTicket(
            tripId = "trip-active-1",
            seatNumber = "5",
            destination = "Dau",
            passengerType = PassengerType.SENIOR,
            fareAmount = 156.0,
            cashTendered = 200.0
        )
        assertTrue(reissued.isSuccess)
        assertEquals(1, repository.observeActiveTicketsByTrip("trip-active-1").first().size)
        assertEquals(2, repository.observeTicketsByTrip("trip-active-1").first().size)
    }

    @Test
    fun `cloud sync pushes ticket and live occupiedSeats atomically`() = runTest {
        syncPrefs.setCloudSyncEnabled(true)

        val t1 = repository.issueTicket(
            tripId = "trip-active-1",
            seatNumber = "3",
            destination = "Dau",
            passengerType = PassengerType.REGULAR,
            fareAmount = 195.0,
            cashTendered = 200.0
        ).getOrThrow()
        assertTrue(t1.isSynced)
        assertEquals(listOf("3"), remoteSync.lastOccupiedSeats)

        val t2 = repository.issueTicket(
            tripId = "trip-active-1",
            seatNumber = "1",
            destination = "Bocaue",
            passengerType = PassengerType.STUDENT,
            fareAmount = 52.0,
            cashTendered = 100.0
        ).getOrThrow()
        assertTrue(t2.isSynced)
        assertEquals(listOf("1", "3"), remoteSync.lastOccupiedSeats)

        repository.markPassengerDroppedOff(t2.id).getOrThrow()
        assertEquals(listOf("3"), remoteSync.lastOccupiedSeats)
        assertEquals(0, repository.observeUnsyncedCount().first())
    }

    @Test
    fun `observeTicketById emits live ticket and null for unknown id`() = runTest {
        val issued = repository.issueTicket(
            tripId = "trip-active-1",
            seatNumber = "7",
            destination = "Dau",
            passengerType = PassengerType.SENIOR,
            fareAmount = 96.0,
            cashTendered = 100.0
        ).getOrThrow()

        val observed = repository.observeTicketById(issued.id).first()
        assertEquals(issued.id, observed?.id)
        assertEquals(TicketStatus.ACTIVE, observed?.status)

        repository.markPassengerDroppedOff(issued.id).getOrThrow()
        val afterDropOff = repository.observeTicketById(issued.id).first()
        assertEquals(TicketStatus.DROPPED_OFF, afterDropOff?.status)

        val missing = repository.observeTicketById("unknown-ticket").first()
        assertEquals(null, missing)
    }
}

private class MutexTransactionRunner : DatabaseTransactionRunner {
    private val mutex = Mutex()

    override suspend fun <T> runInTransaction(block: suspend () -> T): T =
        mutex.withLock { block() }
}

private class FakeTicketDao : TicketDao {
    private val items = MutableStateFlow<List<TicketEntity>>(emptyList())

    override suspend fun upsert(ticket: TicketEntity) {
        items.value = items.value.filterNot { it.id == ticket.id } + ticket
    }

    override fun observeByTrip(tripId: String): Flow<List<TicketEntity>> =
        items.map { list -> list.filter { it.tripId == tripId } }

    override fun observeActiveTicketsByTrip(tripId: String): Flow<List<TicketEntity>> =
        items.map { list ->
            list.filter { it.tripId == tripId && it.status == TicketStatus.ACTIVE.name }
        }

    override suspend fun getActiveTicketsByTrip(tripId: String): List<TicketEntity> =
        items.value.filter { it.tripId == tripId && it.status == TicketStatus.ACTIVE.name }

    override suspend fun getActiveTicketForSeat(tripId: String, seatNumber: String): TicketEntity? =
        items.value.firstOrNull {
            it.tripId == tripId &&
                it.seatNumber == seatNumber &&
                it.status == TicketStatus.ACTIVE.name
        }

    override suspend fun getById(id: String): TicketEntity? =
        items.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<TicketEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun updateStatus(ticketId: String, status: String, updatedAt: Long) {
        items.value = items.value.map {
            if (it.id == ticketId) {
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

    override suspend fun getUnsynced(): List<TicketEntity> =
        items.value.filter { !it.isSynced }

    override fun observeUnsyncedCount(): Flow<Int> =
        items.map { list -> list.count { !it.isSynced } }
}

private class FakeTripDaoForTickets : TripDao {
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

private class FakeTicketSyncPrefs : SyncPreferencesRepository {
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

private class RecordingFleetSyncDataSource : FleetSyncRemoteDataSource {
    var lastOccupiedSeats: List<String> = emptyList()

    override suspend fun pushBus(bus: BusEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBus(busId: String): Result<Unit> = Result.success(Unit)
    override suspend fun pushRoute(route: RouteEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRoute(routeId: String): Result<Unit> = Result.success(Unit)
    override suspend fun pushTrip(trip: TripEntity): Result<Unit> = Result.success(Unit)
    override suspend fun pushTicketAndOccupiedSeats(
        ticket: TicketEntity,
        occupiedSeats: List<String>
    ): Result<Unit> {
        lastOccupiedSeats = occupiedSeats
        return Result.success(Unit)
    }
}
