package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.Bus
import kotlinx.coroutines.flow.Flow

interface BusRepository {
    fun observeAllBuses(): Flow<List<Bus>>
    fun observeBusById(id: String): Flow<Bus?>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun saveBus(
        id: String? = null,
        busNumber: String,
        plateNumber: String,
        totalSeats: Int,
        columns: Int
    ): Result<Bus>
    suspend fun deleteBus(id: String): Result<Unit>
    suspend fun syncPendingBuses(): Result<Int>
}
