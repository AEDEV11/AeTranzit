package com.aedev.aetranzit.data.sync

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.remote.AuthSyncRemoteDataSource
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.sync.ConnectivityObserver
import com.aedev.aetranzit.domain.sync.SyncManager
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.domain.sync.SyncSummary
import com.aedev.aetranzit.domain.sync.TransientSyncException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val companyDao: CompanyDao,
    private val userDao: UserDao,
    private val busDao: BusDao,
    private val routeDao: RouteDao,
    private val tripDao: TripDao,
    private val ticketDao: TicketDao,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val authSyncDataSource: AuthSyncRemoteDataSource,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    private val connectivityObserver: ConnectivityObserver,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SyncManager {

    private val syncMutex = Mutex()
    private val monitorStarted = AtomicBoolean(false)

    private val _syncState = MutableStateFlow<SyncStatus>(
        if (connectivityObserver.isCurrentlyOnline()) SyncStatus.Idle else SyncStatus.Offline
    )
    override val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    override val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    fun startAutoSyncMonitor(scope: CoroutineScope) {
        if (!monitorStarted.compareAndSet(false, true)) return
        scope.launch {
            var wasOnline = connectivityObserver.isCurrentlyOnline()
            combine(
                connectivityObserver.isOnline,
                syncPreferencesRepository.isCloudSyncEnabled,
                syncPreferencesRepository.sessionUserId
            ) { online, cloudEnabled, sessionUserId ->
                Triple(online, cloudEnabled, sessionUserId)
            }.collect { (online, cloudEnabled, sessionUserId) ->
                if (!cloudEnabled) {
                    _syncState.value = SyncStatus.Idle
                } else if (!online) {
                    if (_syncState.value !is SyncStatus.Syncing) {
                        _syncState.value = SyncStatus.Offline
                    }
                } else {
                    if (_syncState.value is SyncStatus.Offline) {
                        _syncState.value = SyncStatus.Idle
                    }
                    if (!sessionUserId.isNullOrBlank() && (!wasOnline || _syncState.value is SyncStatus.Idle)) {
                        syncScheduler.enqueueImmediateSync()
                    }
                }
                wasOnline = online
            }
        }
    }

    override suspend fun syncAll(): Result<SyncSummary> = withContext(ioDispatcher) {
        syncMutex.withLock {
            try {
                if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                    _syncState.value = SyncStatus.Idle
                    return@withLock Result.failure(
                        IllegalStateException("Cloud Sync is disabled in Settings.")
                    )
                }

                if (!connectivityObserver.isCurrentlyOnline()) {
                    _syncState.value = SyncStatus.Offline
                    return@withLock Result.failure(
                        TransientSyncException(
                            "Device is currently offline. Pending records remain safely queued in Room."
                        )
                    )
                }

                val sessionUserId = syncPreferencesRepository.sessionUserId.first()
                if (sessionUserId.isNullOrBlank()) {
                    _syncState.value = SyncStatus.Idle
                    return@withLock Result.success(
                        SyncSummary(pushedCount = 0, pulledCount = 0, syncedAt = System.currentTimeMillis())
                    )
                }

                val activeUser = userDao.getById(sessionUserId)
                if (activeUser == null || !activeUser.isActive || activeUser.companyId.isBlank()) {
                    _syncState.value = SyncStatus.Idle
                    return@withLock Result.success(
                        SyncSummary(pushedCount = 0, pulledCount = 0, syncedAt = System.currentTimeMillis())
                    )
                }

                _syncState.value = SyncStatus.Syncing
                val companyId = activeUser.companyId
                val isAdmin = UserRole.fromRaw(activeUser.role) == UserRole.ADMIN

                var pushedCount = 0
                var pulledCount = 0

                // =========================================================
                // 1. PUSH PHASE (Strict Dependency Order):
                //    Company -> Users -> Buses -> Routes -> Trips -> Tickets
                // =========================================================

                if (isAdmin) {
                    val unsyncedCompanies = companyDao.getUnsyncedByCompany(companyId)
                    for (company in unsyncedCompanies) {
                        fleetSyncDataSource.pushCompany(company).getOrThrow()
                        companyDao.markSynced(company.id, System.currentTimeMillis())
                        pushedCount++
                    }
                }

                val unsyncedUsers = userDao.getUnsyncedByCompany(companyId).let { list ->
                    if (isAdmin) list else list.filter { it.id == activeUser.id }
                }
                for (user in unsyncedUsers) {
                    authSyncDataSource.pushUserProfile(user).getOrThrow()
                    userDao.markSynced(user.id, System.currentTimeMillis())
                    pushedCount++
                }

                if (isAdmin) {
                    val unsyncedBuses = busDao.getUnsyncedByCompany(companyId)
                    for (bus in unsyncedBuses) {
                        fleetSyncDataSource.pushBus(bus).getOrThrow()
                        busDao.markSynced(bus.id, System.currentTimeMillis())
                        pushedCount++
                    }

                    val unsyncedRoutes = routeDao.getUnsyncedByCompany(companyId)
                    for (route in unsyncedRoutes) {
                        fleetSyncDataSource.pushRoute(route).getOrThrow()
                        routeDao.markSynced(route.id, System.currentTimeMillis())
                        pushedCount++
                    }
                }

                val unsyncedTrips = tripDao.getUnsyncedByCompany(companyId).let { list ->
                    if (isAdmin) list else list.filter { it.conductorId == activeUser.id }
                }
                for (trip in unsyncedTrips) {
                    fleetSyncDataSource.pushTrip(trip).getOrThrow()
                    tripDao.markSynced(trip.id, System.currentTimeMillis())
                    pushedCount++
                }

                val unsyncedTickets = ticketDao.getUnsyncedByCompany(companyId).let { list ->
                    if (isAdmin) list else list.filter { it.conductorId == activeUser.id }
                }
                val occupiedSeatsCache = mutableMapOf<String, List<String>>()
                for (ticket in unsyncedTickets) {
                    val occupiedSeats = occupiedSeatsCache.getOrPut(ticket.tripId) {
                        currentOccupiedSeats(ticket.tripId, companyId)
                    }
                    fleetSyncDataSource.pushTicketAndOccupiedSeats(ticket, occupiedSeats).getOrThrow()
                    ticketDao.markSynced(ticket.id, System.currentTimeMillis())
                    pushedCount++
                }

                // =========================================================
                // 2. PULL PHASE (Firebase Cloud SSOT -> Room Cache):
                //    Upsert ONLY if local == null || (local.isSynced && remote.updatedAt > local.updatedAt)
                // =========================================================

                val remoteCompany = fleetSyncDataSource.fetchCompanyById(companyId).getOrThrow()
                if (remoteCompany != null) {
                    val localCompany = companyDao.getById(remoteCompany.id)
                    if (localCompany == null ||
                        (localCompany.isSynced && remoteCompany.updatedAt > localCompany.updatedAt)
                    ) {
                        companyDao.upsert(remoteCompany.copy(isSynced = true))
                        pulledCount++
                    }
                }

                if (isAdmin) {
                    val remoteUsers = fleetSyncDataSource.fetchUsersByCompany(companyId).getOrThrow()
                    for (remoteUser in remoteUsers) {
                        val localUser = userDao.getById(remoteUser.id)
                            ?: userDao.getByEmail(remoteUser.email)
                        if (localUser == null) {
                            userDao.upsert(remoteUser.copy(isSynced = true))
                            pulledCount++
                        } else if (localUser.isSynced && remoteUser.updatedAt > localUser.updatedAt) {
                            userDao.upsert(
                                remoteUser.copy(
                                    id = localUser.id,
                                    passwordHash = localUser.passwordHash,
                                    isSynced = true
                                )
                            )
                            pulledCount++
                        }
                    }
                }

                val remoteBuses = fleetSyncDataSource.fetchBusesByCompany(companyId).getOrThrow()
                for (remoteBus in remoteBuses) {
                    val localBus = busDao.getByIdAndCompany(remoteBus.id, companyId)
                    if (localBus == null ||
                        (localBus.isSynced && remoteBus.updatedAt > localBus.updatedAt)
                    ) {
                        busDao.upsert(remoteBus.copy(isSynced = true))
                        pulledCount++
                    }
                }

                val remoteRoutes = fleetSyncDataSource.fetchRoutesByCompany(companyId).getOrThrow()
                for (remoteRoute in remoteRoutes) {
                    val localRoute = routeDao.getByIdAndCompany(remoteRoute.id, companyId)
                    if (localRoute == null ||
                        (localRoute.isSynced && remoteRoute.updatedAt > localRoute.updatedAt)
                    ) {
                        routeDao.upsert(remoteRoute.copy(isSynced = true))
                        pulledCount++
                    }
                }

                val conductorScopeId = if (isAdmin) null else activeUser.id
                val remoteTrips = fleetSyncDataSource
                    .fetchTripsByCompany(companyId = companyId, conductorId = conductorScopeId)
                    .getOrThrow()
                for (remoteTrip in remoteTrips) {
                    val localTrip = tripDao.getByIdAndCompany(remoteTrip.id, companyId)
                    if (localTrip == null ||
                        (localTrip.isSynced && remoteTrip.updatedAt > localTrip.updatedAt)
                    ) {
                        tripDao.upsert(remoteTrip.copy(isSynced = true))
                        pulledCount++
                    }
                }

                val remoteTickets = fleetSyncDataSource
                    .fetchTicketsByCompany(companyId = companyId, conductorId = conductorScopeId)
                    .getOrThrow()
                for (remoteTicket in remoteTickets) {
                    val localTicket = ticketDao.getByIdAndCompany(remoteTicket.id, companyId)
                    if (localTicket == null ||
                        (localTicket.isSynced && remoteTicket.updatedAt > localTicket.updatedAt)
                    ) {
                        ticketDao.upsert(remoteTicket.copy(isSynced = true))
                        pulledCount++
                    }
                }

                val syncedAt = System.currentTimeMillis()
                syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                _syncState.value = SyncStatus.Synced
                Result.success(
                    SyncSummary(
                        pushedCount = pushedCount,
                        pulledCount = pulledCount,
                        syncedAt = syncedAt
                    )
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                val isCurrentlyOnline = connectivityObserver.isCurrentlyOnline()
                _syncState.value = when {
                    !isCurrentlyOnline || e is TransientSyncException -> SyncStatus.Offline
                    else -> SyncStatus.Error(e.message ?: "Cloud sync failed.")
                }
                Result.failure(e)
            }
        }
    }

    private suspend fun currentOccupiedSeats(tripId: String, companyId: String): List<String> =
        ticketDao.getActiveTicketsByTrip(tripId, companyId)
            .map { it.seatNumber }
            .distinct()
            .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
}
