package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    fun observeAllTickets(): Flow<List<Ticket>>
    fun observeTicketById(ticketId: String): Flow<Ticket?>
    fun observeTicketsByTrip(tripId: String): Flow<List<Ticket>>
    fun observeActiveTicketsByTrip(tripId: String): Flow<List<Ticket>>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun issueTicket(
        tripId: String,
        seatNumber: String,
        destination: String,
        passengerType: PassengerType,
        fareAmount: Double,
        cashTendered: Double
    ): Result<Ticket>
    suspend fun markPassengerDroppedOff(ticketId: String): Result<Ticket>
    suspend fun syncPendingTickets(): Result<Int>
}
