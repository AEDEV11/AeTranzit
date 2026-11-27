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

    @Query("SELECT * FROM buses WHERE companyId = :companyId ORDER BY busNumber ASC")
    fun observeByCompany(companyId: String): Flow<List<BusEntity>>

    @Query("SELECT * FROM buses WHERE id = :id AND companyId = :companyId LIMIT 1")
    fun observeByIdAndCompany(id: String, companyId: String): Flow<BusEntity?>

    @Query("SELECT * FROM buses WHERE id = :id AND companyId = :companyId LIMIT 1")
    suspend fun getByIdAndCompany(id: String, companyId: String): BusEntity?

    @Query("DELETE FROM buses WHERE id = :id AND companyId = :companyId")
    suspend fun deleteByIdAndCompany(id: String, companyId: String)

    @Query("UPDATE buses SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM buses WHERE companyId = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<BusEntity>

    @Query("SELECT COUNT(*) FROM buses WHERE companyId = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
