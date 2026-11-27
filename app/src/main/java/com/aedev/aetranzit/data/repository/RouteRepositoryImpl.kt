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
class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao,
    private val tripDao: TripDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : RouteRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllRoutes(): Flow<List<Route>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                routeDao.observeByCompany(companyId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRouteById(id: String): Flow<Route?> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                routeDao.observeByIdAndCompany(id, companyId).map { it?.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                routeDao.observeUnsyncedCountByCompany(companyId)
            }
        }

    override suspend fun saveRoute(
        id: String?,
        origin: String,
        destination: String,
        baseFare: Double,
        stops: List<RouteStop>
    ): Result<Route> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
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

            val resolvedId = if (!id.isNullOrBlank()) {
                val existing = routeDao.getByIdAndCompany(id, companyId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Route not found in your company.")
                    )
                existing.id
            } else {
                UUID.randomUUID().toString()
            }

            val now = System.currentTimeMillis()
            var entity = RouteEntity(
                id = resolvedId,
                origin = trimmedOrigin,
                destination = trimmedDest,
                baseFare = baseFare,
                stopsJson = RouteStopsJsonCodec.encode(cleanedStops),
                companyId = companyId,
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
                syncScheduler.enqueueImmediateSync()
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
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            val existing = routeDao.getByIdAndCompany(id, companyId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Route not found in your company.")
                )
            if (tripDao.countActiveTripsByRoute(existing.id, companyId) > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Cannot delete a route while an active trip is running on it.")
                )
            }
            routeDao.deleteByIdAndCompany(existing.id, companyId)
            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                fleetSyncDataSource.deleteRoute(existing.id)
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
