package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE companyId = :companyId ORDER BY origin ASC, destination ASC")
    fun observeByCompany(companyId: String): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id AND companyId = :companyId LIMIT 1")
    fun observeByIdAndCompany(id: String, companyId: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE id = :id AND companyId = :companyId LIMIT 1")
    suspend fun getByIdAndCompany(id: String, companyId: String): RouteEntity?

    @Query("DELETE FROM routes WHERE id = :id AND companyId = :companyId")
    suspend fun deleteByIdAndCompany(id: String, companyId: String)

    @Query("UPDATE routes SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM routes WHERE companyId = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<RouteEntity>

    @Query("SELECT COUNT(*) FROM routes WHERE companyId = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
