package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus

fun TicketEntity.toDomain(): Ticket = Ticket(
    id = id,
    tripId = tripId,
    busId = busId,
    conductorId = conductorId,
    seatNumber = seatNumber,
    destination = destination,
    passengerType = PassengerType.fromRaw(passengerType),
    fareAmount = fareAmount,
    cashTendered = cashTendered,
    changeAmount = changeAmount,
    status = TicketStatus.fromRaw(status),
    createdAt = createdAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)

fun Ticket.toEntity(): TicketEntity = TicketEntity(
    id = id,
    tripId = tripId,
    busId = busId,
    conductorId = conductorId,
    seatNumber = seatNumber,
    destination = destination,
    passengerType = passengerType.name,
    fareAmount = fareAmount,
    cashTendered = cashTendered,
    changeAmount = changeAmount,
    status = status.name,
    createdAt = createdAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)
