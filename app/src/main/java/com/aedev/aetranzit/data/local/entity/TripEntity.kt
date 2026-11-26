package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["conductorId"]),
        Index(value = ["status"])
    ]
)
data class TripEntity(
    @PrimaryKey val id: String,
    val busId: String,
    val routeId: String,
    val conductorId: String,
    val status: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
