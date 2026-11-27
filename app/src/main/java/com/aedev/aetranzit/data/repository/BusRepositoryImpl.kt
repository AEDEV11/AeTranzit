package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.repository.BusRepository
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
class BusRepositoryImpl @Inject constructor(
    private val busDao: BusDao,
    private val tripDao: TripDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : BusRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllBuses(): Flow<List<Bus>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                busDao.observeByCompany(companyId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBusById(id: String): Flow<Bus?> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                busDao.observeByIdAndCompany(id, companyId).map { it?.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                busDao.observeUnsyncedCountByCompany(companyId)
            }
        }

    override suspend fun saveBus(
        id: String?,
        busNumber: String,
        plateNumber: String,
        totalSeats: Int,
        columns: Int
    ): Result<Bus> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            val trimmedBusNo = busNumber.trim()
            val trimmedPlate = plateNumber.trim().uppercase()
            if (trimmedBusNo.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Bus number is required."))
            }
            if (trimmedPlate.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Plate number is required."))
            }
            if (totalSeats !in MIN_SEATS..MAX_SEATS) {
                return@withContext Result.failure(
                    IllegalArgumentException("Total seats must be between $MIN_SEATS and $MAX_SEATS.")
                )
            }
            if (columns != 4 && columns != 5) {
                return@withContext Result.failure(
                    IllegalArgumentException("Seat layout must be 4 (2x2) or 5 (2x3) columns.")
                )
            }

            val resolvedId = if (!id.isNullOrBlank()) {
                val existing = busDao.getByIdAndCompany(id, companyId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Bus not found in your company.")
                    )
                existing.id
            } else {
                UUID.randomUUID().toString()
            }

            val now = System.currentTimeMillis()
            var entity = BusEntity(
                id = resolvedId,
                busNumber = trimmedBusNo,
                plateNumber = trimmedPlate,
                totalSeats = totalSeats,
                columns = columns,
                companyId = companyId,
                isSynced = false,
                updatedAt = now
            )
            busDao.upsert(entity)

            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                if (fleetSyncDataSource.pushBus(entity).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    busDao.markSynced(entity.id, syncedAt)
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

    override suspend fun deleteBus(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            val existing = busDao.getByIdAndCompany(id, companyId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Bus not found in your company.")
                )
            if (tripDao.countActiveTripsByBus(existing.id, companyId) > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Cannot delete a bus while it is assigned to an active trip.")
                )
            }
            busDao.deleteByIdAndCompany(existing.id, companyId)
            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                fleetSyncDataSource.deleteBus(existing.id)
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingBuses(): Result<Int> = withContext(ioDispatcher) {
        try {
            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(
                    IllegalStateException("Enable Cloud Sync in Settings before syncing.")
                )
            }
            val companyId = activeCompanyProvider.getActiveCompanyId()
                ?: return@withContext Result.success(0)

            val unsynced = busDao.getUnsyncedByCompany(companyId)
            var syncedCount = 0
            for (bus in unsynced) {
                if (fleetSyncDataSource.pushBus(bus).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    busDao.markSynced(bus.id, syncedAt)
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

    private companion object {
        const val MIN_SEATS = 10
        const val MAX_SEATS = 80
    }
}
