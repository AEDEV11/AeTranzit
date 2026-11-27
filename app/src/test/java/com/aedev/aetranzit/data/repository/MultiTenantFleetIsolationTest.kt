package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultiTenantFleetIsolationTest {

    private lateinit var busDao: TenantBusDao
    private lateinit var routeDao: TenantRouteDao
    private lateinit var tripDao: TenantTripDao
    private lateinit var companyProvider: TenantSwitchProvider
    private lateinit var syncPrefs: TenantSyncPrefs
    private lateinit var remoteSync: NoOpFleetSyncDataSource
    private lateinit var busRepository: BusRepositoryImpl
    private lateinit var routeRepository: RouteRepositoryImpl

    @Before
    fun setUp() {
        busDao = TenantBusDao()
        routeDao = TenantRouteDao()
        tripDao = TenantTripDao()
        companyProvider = TenantSwitchProvider("company-alpha")
        syncPrefs = TenantSyncPrefs()
        remoteSync = NoOpFleetSyncDataSource()

        val dispatcher = UnconfinedTestDispatcher()
        busRepository = BusRepositoryImpl(
            busDao = busDao,
            tripDao = tripDao,
            activeCompanyProvider = companyProvider,
            syncPreferencesRepository = syncPrefs,
            fleetSyncDataSource = remoteSync,
            ioDispatcher = dispatcher
        )
        routeRepository = RouteRepositoryImpl(
            routeDao = routeDao,
            tripDao = tripDao,
            activeCompanyProvider = companyProvider,
            syncPreferencesRepository = syncPrefs,
            fleetSyncDataSource = remoteSync,
            ioDispatcher = dispatcher
        )
    }

    @Test
    fun `Company A queries and deletes never return or affect Company B buses and routes`() = runTest {
        // Seed Company Alpha fleet & routes
        companyProvider.setCompany("company-alpha")
        val alphaBus = busRepository.saveBus(
            id = null,
            busNumber = "ALP-101",
            plateNumber = "NAA-1010",
            totalSeats = 45,
            columns = 4
        ).getOrThrow()
        val alphaRoute = routeRepository.saveRoute(
            id = null,
            origin = "Cubao",
            destination = "Baguio",
            baseFare = 450.0,
            stops = listOf(RouteStop(name = "Dau", fare = 180.0))
        ).getOrThrow()

        // Seed Company Beta fleet & routes
        companyProvider.setCompany("company-beta")
        val betaBus = busRepository.saveBus(
            id = null,
            busNumber = "BET-202",
            plateNumber = "NBB-2020",
            totalSeats = 50,
            columns = 5
        ).getOrThrow()
        val betaRoute = routeRepository.saveRoute(
            id = null,
            origin = "Pasay",
            destination = "Batangas",
            baseFare = 210.0,
            stops = emptyList()
        ).getOrThrow()

        // Switch back to Company Alpha: verify strict tenant scoping
        companyProvider.setCompany("company-alpha")
        assertEquals(listOf("ALP-101"), busRepository.observeAllBuses().first().map { it.busNumber })
        assertEquals(
            listOf("Cubao → Baguio"),
            routeRepository.observeAllRoutes().first().map { it.displayName }
        )
        assertNull(busRepository.observeBusById(betaBus.id).first())
        assertNull(routeRepository.observeRouteById(betaRoute.id).first())

        // Attempting to delete Company Beta's bus/route while signed into Company Alpha does nothing
        busRepository.deleteBus(betaBus.id)
        routeRepository.deleteRoute(betaRoute.id)

        companyProvider.setCompany("company-beta")
        assertEquals(listOf("BET-202"), busRepository.observeAllBuses().first().map { it.busNumber })
        assertEquals(
            listOf("Pasay → Batangas"),
            routeRepository.observeAllRoutes().first().map { it.displayName }
        )
        assertNull(busRepository.observeBusById(alphaBus.id).first())
        assertNull(routeRepository.observeRouteById(alphaRoute.id).first())
    }
}

private class TenantSwitchProvider(initial: String?) : ActiveCompanyProvider {
    private val current = MutableStateFlow(initial)
    fun setCompany(id: String?) {
        current.value = id
    }
    override fun observeActiveCompanyId(): Flow<String?> = current
    override suspend fun getActiveCompanyId(): String? = current.value
}

private class TenantBusDao : BusDao {
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

private class TenantRouteDao : RouteDao {
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

private class TenantTripDao : TripDao {
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
    override suspend fun markSynced(id: String, syncedAt: Long) = Unit
    override suspend fun getUnsyncedByCompany(companyId: String): List<TripEntity> = emptyList()
    override fun observeUnsyncedCountByCompany(companyId: String): Flow<Int> = MutableStateFlow(0)
}

private class TenantSyncPrefs : SyncPreferencesRepository {
    override val isCloudSyncEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val lastSyncedAt: Flow<Long?> = MutableStateFlow(null)
    override val sessionUserId: Flow<String?> = MutableStateFlow(null)
    override val hasCompletedOnboarding: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun setCloudSyncEnabled(enabled: Boolean) = Unit
    override suspend fun updateLastSyncedAt(timestamp: Long) = Unit
    override suspend fun setSessionUserId(userId: String?) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
}

private class NoOpFleetSyncDataSource : FleetSyncRemoteDataSource {
    override suspend fun pushCompany(company: CompanyEntity): Result<Unit> = Result.success(Unit)
    override suspend fun fetchCompanyById(companyId: String): Result<CompanyEntity?> =
        Result.success(null)
    override suspend fun fetchUsersByCompany(companyId: String): Result<List<UserEntity>> =
        Result.success(emptyList())
    override suspend fun fetchBusesByCompany(companyId: String): Result<List<BusEntity>> =
        Result.success(emptyList())
    override suspend fun fetchRoutesByCompany(companyId: String): Result<List<RouteEntity>> =
        Result.success(emptyList())
    override suspend fun fetchTripsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TripEntity>> = Result.success(emptyList())
    override suspend fun fetchTicketsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TicketEntity>> = Result.success(emptyList())
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
