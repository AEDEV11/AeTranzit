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
    fun observeById(id: String): Flow<BusEntity?>

    @Query("SELECT * FROM buses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BusEntity?

    @Query("DELETE FROM buses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE buses SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM buses WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BusEntity>

    @Query("SELECT COUNT(*) FROM buses WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
