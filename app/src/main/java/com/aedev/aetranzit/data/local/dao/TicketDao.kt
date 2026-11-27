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

    @Query("SELECT * FROM tickets WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun observeByCompany(companyId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId AND companyId = :companyId ORDER BY createdAt DESC")
    fun observeByTrip(tripId: String, companyId: String): Flow<List<TicketEntity>>

    @Query(
        "SELECT * FROM tickets WHERE tripId = :tripId AND companyId = :companyId " +
            "AND status = 'ACTIVE' ORDER BY createdAt DESC"
    )
    fun observeActiveTicketsByTrip(tripId: String, companyId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tripId = :tripId AND companyId = :companyId AND status = 'ACTIVE'")
    suspend fun getActiveTicketsByTrip(tripId: String, companyId: String): List<TicketEntity>

    @Query(
        "SELECT * FROM tickets WHERE tripId = :tripId AND seatNumber = :seatNumber " +
            "AND companyId = :companyId AND status = 'ACTIVE' LIMIT 1"
    )
    suspend fun getActiveTicketForSeat(
        tripId: String,
        seatNumber: String,
        companyId: String
    ): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id AND companyId = :companyId LIMIT 1")
    suspend fun getByIdAndCompany(id: String, companyId: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id AND companyId = :companyId LIMIT 1")
    fun observeByIdAndCompany(id: String, companyId: String): Flow<TicketEntity?>

    @Query(
        "UPDATE tickets SET status = :status, isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :ticketId AND companyId = :companyId"
    )
    suspend fun updateStatus(
        ticketId: String,
        companyId: String,
        status: String,
        updatedAt: Long
    )

    @Query("UPDATE tickets SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM tickets WHERE companyId = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<TicketEntity>

    @Query("SELECT COUNT(*) FROM tickets WHERE companyId = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
