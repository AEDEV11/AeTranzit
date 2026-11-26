package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun observeAllRoutes(): Flow<List<Route>>
    fun observeRouteById(id: String): Flow<Route?>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun saveRoute(
        id: String? = null,
        origin: String,
        destination: String,
        baseFare: Double,
        stops: List<RouteStop>
    ): Result<Route>
    suspend fun deleteRoute(id: String): Result<Unit>
    suspend fun syncPendingRoutes(): Result<Int>
}
