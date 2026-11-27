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

    @Query("SELECT * FROM trips WHERE companyId = :companyId ORDER BY startedAt DESC")
    fun observeByCompany(companyId: String): Flow<List<TripEntity>>

    @Query(
        "SELECT * FROM trips WHERE conductorId = :conductorId AND companyId = :companyId " +
            "AND status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1"
    )
    fun observeActiveTripByConductor(conductorId: String, companyId: String): Flow<TripEntity?>

    @Query(
        "SELECT * FROM trips WHERE conductorId = :conductorId AND companyId = :companyId " +
            "AND status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1"
    )
    suspend fun getActiveTripByConductor(conductorId: String, companyId: String): TripEntity?

    @Query(
        "SELECT COUNT(*) FROM trips WHERE busId = :busId AND companyId = :companyId " +
            "AND status = 'ACTIVE'"
    )
    suspend fun countActiveTripsByBus(busId: String, companyId: String): Int

    @Query(
        "SELECT COUNT(*) FROM trips WHERE routeId = :routeId AND companyId = :companyId " +
            "AND status = 'ACTIVE'"
    )
    suspend fun countActiveTripsByRoute(routeId: String, companyId: String): Int

    @Query("SELECT * FROM trips WHERE id = :id AND companyId = :companyId LIMIT 1")
    fun observeByIdAndCompany(id: String, companyId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id AND companyId = :companyId LIMIT 1")
    suspend fun getByIdAndCompany(id: String, companyId: String): TripEntity?

    @Query("UPDATE trips SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM trips WHERE companyId = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<TripEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE companyId = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
