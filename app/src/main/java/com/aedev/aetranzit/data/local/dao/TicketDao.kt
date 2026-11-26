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

    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeByTrip(tripId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActiveTicketsByTrip(tripId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId AND status = 'ACTIVE'")
    suspend fun getActiveTicketsByTrip(tripId: String): List<TicketEntity>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId AND seatNumber = :seatNumber AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveTicketForSeat(tripId: String, seatNumber: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TicketEntity?>

    @Query("UPDATE tickets SET status = :status, isSynced = 0, updatedAt = :updatedAt WHERE id = :ticketId")
    suspend fun updateStatus(ticketId: String, status: String, updatedAt: Long)

    @Query("UPDATE tickets SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM tickets WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TicketEntity>

    @Query("SELECT COUNT(*) FROM tickets WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
