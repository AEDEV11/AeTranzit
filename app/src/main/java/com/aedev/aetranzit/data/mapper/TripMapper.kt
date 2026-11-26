package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus

fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    busId = busId,
    routeId = routeId,
    conductorId = conductorId,
    status = TripStatus.fromRaw(status),
    startedAt = startedAt,
    endedAt = endedAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    busId = busId,
    routeId = routeId,
    conductorId = conductorId,
    status = status.name,
    startedAt = startedAt,
    endedAt = endedAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)
