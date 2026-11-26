package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ticket: TicketEntity)

    @Query("SELECT * FROM tickets WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeByTrip(tripId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TicketEntity>

    @Query("SELECT COUNT(*) FROM tickets WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
