package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE conductorId = :conductorId ORDER BY startedAt DESC")
    fun observeByConductor(conductorId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE conductorId = :conductorId AND status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveTripByConductor(conductorId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE conductorId = :conductorId AND status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveTripByConductor(conductorId: String): TripEntity?

    @Query("SELECT COUNT(*) FROM trips WHERE busId = :busId AND status = 'ACTIVE'")
    suspend fun countActiveTripsByBus(busId: String): Int

    @Query("SELECT COUNT(*) FROM trips WHERE routeId = :routeId AND status = 'ACTIVE'")
    suspend fun countActiveTripsByRoute(routeId: String): Int

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TripEntity?

    @Query("UPDATE trips SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
