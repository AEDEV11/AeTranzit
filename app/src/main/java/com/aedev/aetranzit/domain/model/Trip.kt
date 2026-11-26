package com.aedev.aetranzit.domain.model

data class Trip(
    val id: String,
    val busId: String,
    val routeId: String,
    val conductorId: String,
    val status: TripStatus,
    val startedAt: Long,
    val endedAt: Long? = null,
    val isSynced: Boolean,
    val updatedAt: Long
)
