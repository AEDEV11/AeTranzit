package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.BusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bus: BusEntity)

    @Query("SELECT * FROM buses ORDER BY busNumber ASC")
    fun observeAll(): Flow<List<BusEntity>>

    @Query("SELECT * FROM buses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BusEntity?

    @Query("SELECT * FROM buses WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BusEntity>

    @Query("SELECT COUNT(*) FROM buses WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
