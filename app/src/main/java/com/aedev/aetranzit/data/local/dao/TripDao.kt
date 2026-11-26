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

    @Query("SELECT * FROM trips WHERE conductorId = :conductorId ORDER BY startedAt DESC")
    fun observeByConductor(conductorId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
