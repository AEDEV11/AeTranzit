package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.sync.NoOpSyncScheduler
import com.aedev.aetranzit.domain.sync.SyncScheduler
import com.aedev.aetranzit.domain.usecase.CalculateFareUseCase
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
class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao,
    private val tripDao: TripDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val transactionRunner: DatabaseTransactionRunner,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : TicketRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllTickets(): Flow<List<Ticket>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                ticketDao.observeByCompany(companyId).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTicketById(ticketId: String): Flow<Ticket?> {
        val cleanId = ticketId.trim()
        if (cleanId.isBlank()) return flowOf(null)
        return activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                ticketDao.observeByIdAndCompany(cleanId, companyId).map { it?.toDomain() }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTicketsByTrip(tripId: String): Flow<List<Ticket>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                ticketDao.observeByTrip(tripId, companyId).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveTicketsByTrip(tripId: String): Flow<List<Ticket>> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                ticketDao.observeActiveTicketsByTrip(tripId, companyId).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                ticketDao.observeUnsyncedCountByCompany(companyId)
            }
        }

    override suspend fun issueTicket(
        tripId: String,
        seatNumber: String,
        destination: String,
        passengerType: PassengerType,
        fareAmount: Double,
        cashTendered: Double
    ): Result<Ticket> = withContext(ioDispatcher) {
        try {
            val companyId = activeCompanyProvider.requireActiveCompanyId()
            val cleanTripId = tripId.trim()
            val cleanSeat = seatNumber.trim()
            val cleanDestination = destination.trim()

            if (cleanTripId.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Active trip ID is required.")
                )
            }
            if (cleanSeat.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Please select a valid seat.")
                )
            }
            if (cleanDestination.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Please select a passenger destination.")
                )
            }

            val roundedFare = CalculateFareUseCase.roundCurrency(fareAmount)
            val roundedCash = CalculateFareUseCase.roundCurrency(cashTendered)

            if (roundedFare <= 0.0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Fare amount must be greater than ₱0.00.")
                )
            }
            if (roundedCash < roundedFare) {
                return@withContext Result.failure(
                    IllegalArgumentException("Cash tendered is less than the required fare.")
                )
            }

            val changeAmount = CalculateFareUseCase.roundCurrency(roundedCash - roundedFare)

            val (savedEntity, occupiedSeats) = transactionRunner.runInTransaction {
                val trip = tripDao.getByIdAndCompany(cleanTripId, companyId)
                    ?: throw IllegalArgumentException("Active trip not found in your company.")
                if (TripStatus.fromRaw(trip.status) != TripStatus.ACTIVE) {
                    throw IllegalStateException("Cannot issue tickets on a completed trip.")
                }

                val existingForSeat = ticketDao.getActiveTicketForSeat(
                    tripId = cleanTripId,
                    seatNumber = cleanSeat,
                    companyId = companyId
                )
                if (existingForSeat != null) {
                    throw IllegalStateException("Seat $cleanSeat is already occupied.")
                }

                val now = System.currentTimeMillis()
                val entity = TicketEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = cleanTripId,
                    busId = trip.busId,
                    conductorId = trip.conductorId,
                    companyId = trip.companyId,
                    seatNumber = cleanSeat,
                    destination = cleanDestination,
                    passengerType = passengerType.name,
                    fareAmount = roundedFare,
                    cashTendered = roundedCash,
                    changeAmount = changeAmount,
                    status = TicketStatus.ACTIVE.name,
                    createdAt = now,
                    isSynced = false,
                    updatedAt = now
                )
                ticketDao.upsert(entity)
                val activeSeats = currentOccupiedSeats(cleanTripId, companyId)
                entity to activeSeats
            }

            var finalEntity = savedEntity
            if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                if (fleetSyncDataSource.pushTicketAndOccupiedSeats(savedEntity, occupiedSeats).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    ticketDao.markSynced(savedEntity.id, syncedAt)
                    syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                    finalEntity = savedEntity.copy(isSynced = true, updatedAt = syncedAt)
                }
                syncScheduler.enqueueImmediateSync()
            }

            Result.success(finalEntity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markPassengerDroppedOff(ticketId: String): Result<Ticket> =
        withContext(ioDispatcher) {
            try {
                val companyId = activeCompanyProvider.requireActiveCompanyId()
                val cleanTicketId = ticketId.trim()
                if (cleanTicketId.isBlank()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Ticket ID is required.")
                    )
                }

                val (updatedEntity, occupiedSeats) = transactionRunner.runInTransaction {
                    val existing = ticketDao.getByIdAndCompany(cleanTicketId, companyId)
                        ?: throw IllegalArgumentException("Ticket not found in your company.")
                    if (TicketStatus.fromRaw(existing.status) == TicketStatus.DROPPED_OFF) {
                        throw IllegalStateException("Passenger has already been marked dropped off.")
                    }

                    val now = System.currentTimeMillis()
                    ticketDao.updateStatus(
                        ticketId = existing.id,
                        companyId = companyId,
                        status = TicketStatus.DROPPED_OFF.name,
                        updatedAt = now
                    )
                    val updated = existing.copy(
                        status = TicketStatus.DROPPED_OFF.name,
                        isSynced = false,
                        updatedAt = now
                    )
                    val activeSeats = currentOccupiedSeats(existing.tripId, companyId)
                    updated to activeSeats
                }

                var finalEntity = updatedEntity
                if (syncPreferencesRepository.isCloudSyncEnabled.first()) {
                    if (fleetSyncDataSource.pushTicketAndOccupiedSeats(updatedEntity, occupiedSeats).isSuccess) {
                        val syncedAt = System.currentTimeMillis()
                        ticketDao.markSynced(updatedEntity.id, syncedAt)
                        syncPreferencesRepository.updateLastSyncedAt(syncedAt)
                        finalEntity = updatedEntity.copy(isSynced = true, updatedAt = syncedAt)
                    }
                    syncScheduler.enqueueImmediateSync()
                }

                Result.success(finalEntity.toDomain())
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun currentOccupiedSeats(tripId: String, companyId: String): List<String> =
        ticketDao.getActiveTicketsByTrip(tripId, companyId)
            .map { it.seatNumber }
            .distinct()
            .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
}
