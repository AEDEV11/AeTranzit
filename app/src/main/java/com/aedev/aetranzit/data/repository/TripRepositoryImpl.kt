package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.sync.NoOpSyncScheduler
import com.aedev.aetranzit.domain.sync.SyncScheduler
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
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val busDao: BusDao,
    private val routeDao: RouteDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : TripRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllTrips(): Flow<List<Trip>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                tripDao.observeByCompany(companyId).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTripById(tripId: String): Flow<Trip?> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                tripDao.observeByIdAndCompany(tripId, companyId).map { it?.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveTrip(conductorId: String): Flow<Trip?> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                tripDao.observeActiveTripByConductor(conductorId, companyId).map { it?.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                tripDao.observeUnsyncedCountByCompany(companyId)
            }
        }

    override suspend fun startTrip(
        conductorId: String,
        busId: String,
        routeId: String
    ): Result<Trip> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            if (conductorId.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("No authenticated conductor found.")
                )
            }
            if (busId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please select a bus."))
            }
            if (routeId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please select a route."))
            }

            if (tripDao.getActiveTripByConductor(conductorId, companyId) != null) {
                return@withContext Result.failure(
                    IllegalStateException("You already have an active trip. End it before starting a new trip.")
                )
            }
            if (busDao.getByIdAndCompany(busId, companyId) == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selected bus does not exist in your company.")
                )
            }
            if (routeDao.getByIdAndCompany(routeId, companyId) == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selected route does not exist in your company.")
                )
            }
            if (tripDao.countActiveTripsByBus(busId, companyId) > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Selected bus already has an active trip in progress.")
                )
            }

            val now = System.currentTimeMillis()
            var entity = TripEntity(
                id = UUID.randomUUID().toString(),
                busId = busId,
                routeId = routeId,
                conductorId = conductorId,
                companyId = companyId,
                status = TripStatus.ACTIVE.name,
                startedAt = now,
                endedAt = null,
                isSynced = false,
                updatedAt = now
            )
            tripDao.upsert(entity)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                if (fleetSyncDataSource.pushTrip(entity).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    tripDao.markSynced(entity.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    entity = entity.copy(isSynced = true, updatedAt = syncedAt)
                }
                syncScheduler.enqueueImmediateSync()
            }
            Result.success(entity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endTrip(tripId: String): Result<Trip> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            val existing = tripDao.getByIdAndCompany(tripId, companyId)
                ?: return@withContext Result.failure(IllegalArgumentException("Active trip not found."))
            if (TripStatus.fromRaw(existing.status) == TripStatus.COMPLETED) {
                return@withContext Result.failure(IllegalStateException("Trip is already completed."))
            }

            val now = System.currentTimeMillis()
            var updated = existing.copy(
                status = TripStatus.COMPLETED.name,
                endedAt = now,
                isSynced = false,
                updatedAt = now
            )
            tripDao.upsert(updated)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                if (fleetSyncDataSource.pushTrip(updated).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    tripDao.markSynced(updated.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    updated = updated.copy(isSynced = true, updatedAt = syncedAt)
                }
                syncScheduler.enqueueImmediateSync()
            }
            Result.success(updated.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
