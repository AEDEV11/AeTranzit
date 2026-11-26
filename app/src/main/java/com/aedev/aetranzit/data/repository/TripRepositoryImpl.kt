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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TripRepository {

    override fun observeTripById(tripId: String): Flow<Trip?> =
        tripDao.observeById(tripId).map { it?.toDomain() }

    override fun observeActiveTrip(conductorId: String): Flow<Trip?> =
        tripDao.observeActiveTripByConductor(conductorId).map { it?.toDomain() }

    override fun observeTripsByConductor(conductorId: String): Flow<List<Trip>> =
        tripDao.observeByConductor(conductorId).map { list -> list.map { it.toDomain() } }

    override fun observeUnsyncedCount(): Flow<Int> =
        tripDao.observeUnsyncedCount()

    override suspend fun startTrip(
        conductorId: String,
        busId: String,
        routeId: String
    ): Result<Trip> = withContext(ioDispatcher) {
        try {
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

            if (tripDao.getActiveTripByConductor(conductorId) != null) {
                return@withContext Result.failure(
                    IllegalStateException("You already have an active trip. End it before starting a new trip.")
                )
            }
            if (tripDao.countActiveTripsByBus(busId) > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Selected bus already has an active trip in progress.")
                )
            }
            if (busDao.getById(busId) == null) {
                return@withContext Result.failure(IllegalArgumentException("Selected bus no longer exists."))
            }
            if (routeDao.getById(routeId) == null) {
                return@withContext Result.failure(IllegalArgumentException("Selected route no longer exists."))
            }

            val now = System.currentTimeMillis()
            var entity = TripEntity(
                id = UUID.randomUUID().toString(),
                busId = busId,
                routeId = routeId,
                conductorId = conductorId,
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
            val existing = tripDao.getById(tripId)
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
            }
            Result.success(updated.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingTrips(): Result<Int> = withContext(ioDispatcher) {
        try {
            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(
                    IllegalStateException("Enable Cloud Sync in Settings before syncing.")
                )
            }
            val unsynced = tripDao.getUnsynced()
            var syncedCount = 0
            for (trip in unsynced) {
                if (fleetSyncDataSource.pushTrip(trip).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    tripDao.markSynced(trip.id, syncedAt)
                    syncedCount++
                }
            }
            if (syncedCount > 0) {
                syncPreferencesRepository.updateLastSyncedAt(System.currentTimeMillis())
            }
            Result.success(syncedCount)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
