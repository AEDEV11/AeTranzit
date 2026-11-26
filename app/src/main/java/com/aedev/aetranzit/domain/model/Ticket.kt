package com.aedev.aetranzit.domain.model

import java.util.Locale

data class Ticket(
    val id: String,
    val tripId: String,
    val busId: String,
    val conductorId: String,
    val seatNumber: String,
    val destination: String,
    val passengerType: PassengerType,
    val fareAmount: Double,
    val cashTendered: Double,
    val changeAmount: Double,
    val status: TicketStatus,
    val createdAt: Long,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    val referenceCode: String
        get() = id.take(8).uppercase(Locale.US)
}
