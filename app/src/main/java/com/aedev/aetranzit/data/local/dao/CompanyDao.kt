package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(company: CompanyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(company: CompanyEntity)

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CompanyEntity?

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE UPPER(joinCode) = UPPER(:joinCode) LIMIT 1")
    suspend fun getByJoinCode(joinCode: String): CompanyEntity?

    @Query("SELECT * FROM companies WHERE id = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<CompanyEntity>

    @Query("SELECT * FROM companies WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CompanyEntity>

    @Query("UPDATE companies SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT COUNT(*) FROM companies WHERE id = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
