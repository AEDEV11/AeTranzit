package com.aedev.aetranzit.domain.model

data class Route(
    val id: String,
    val origin: String,
    val destination: String,
    val baseFare: Double,
    val stops: List<RouteStop>,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    val displayName: String
        get() = "$origin → $destination"
}
