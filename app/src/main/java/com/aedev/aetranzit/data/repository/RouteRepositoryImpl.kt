package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.mapper.RouteStopsJsonCodec
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
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
class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao,
    private val tripDao: TripDao,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RouteRepository {

    override fun observeAllRoutes(): Flow<List<Route>> =
        routeDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeRouteById(id: String): Flow<Route?> =
        routeDao.observeById(id).map { it?.toDomain() }

    override fun observeUnsyncedCount(): Flow<Int> =
        routeDao.observeUnsyncedCount()

    override suspend fun saveRoute(
        id: String?,
        origin: String,
        destination: String,
        baseFare: Double,
        stops: List<RouteStop>
    ): Result<Route> = withContext(ioDispatcher) {
        try {
            val trimmedOrigin = origin.trim()
            val trimmedDest = destination.trim()
            if (trimmedOrigin.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Origin is required."))
            }
            if (trimmedDest.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Destination is required."))
            }
            if (trimmedOrigin.equals(trimmedDest, ignoreCase = true)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Origin and destination must be different.")
                )
            }
            if (baseFare <= 0.0 || baseFare.isNaN() || baseFare.isInfinite()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Base fare must be greater than zero.")
                )
            }

            val cleanedStops = stops.map { stop ->
                val stopName = stop.name.trim()
                if (stopName.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Every intermediate stop must have a name.")
                    )
                }
                if (stop.fare <= 0.0 || stop.fare.isNaN() || stop.fare.isInfinite()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Stop fare for '$stopName' must be greater than zero.")
                    )
                }
                RouteStop(name = stopName, fare = stop.fare)
            }

            val now = System.currentTimeMillis()
            var entity = RouteEntity(
                id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                origin = trimmedOrigin,
                destination = trimmedDest,
                baseFare = baseFare,
                stopsJson = RouteStopsJsonCodec.encode(cleanedStops),
                isSynced = false,
                updatedAt = now
            )
            routeDao.upsert(entity)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                if (fleetSyncDataSource.pushRoute(entity).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    routeDao.markSynced(entity.id, syncedAt)
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

    override suspend fun deleteRoute(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (tripDao.countActiveTripsByRoute(id) > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Cannot delete a route while an active trip is running on it.")
                )
            }
            routeDao.deleteById(id)
            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                fleetSyncDataSource.deleteRoute(id)
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingRoutes(): Result<Int> = withContext(ioDispatcher) {
        try {
            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(
                    IllegalStateException("Enable Cloud Sync in Settings before syncing.")
                )
            }
            val unsynced = routeDao.getUnsynced()
            var syncedCount = 0
            for (route in unsynced) {
                if (fleetSyncDataSource.pushRoute(route).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    routeDao.markSynced(route.id, syncedAt)
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
