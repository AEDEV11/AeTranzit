package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeAllTrips(): Flow<List<Trip>>
    fun observeTripById(tripId: String): Flow<Trip?>
    fun observeActiveTrip(conductorId: String): Flow<Trip?>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun startTrip(conductorId: String, busId: String, routeId: String): Result<Trip>
    suspend fun endTrip(tripId: String): Result<Trip>
}
