package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.domain.model.Bus

fun BusEntity.toDomain(): Bus = Bus(
    id = id,
    busNumber = busNumber,
    plateNumber = plateNumber,
    totalSeats = totalSeats,
    columns = columns,
    companyId = companyId,
    isSynced = isSynced,
    updatedAt = updatedAt
)
