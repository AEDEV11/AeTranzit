package com.aedev.aetranzit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aedev.aetranzit.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE companyId = :companyId ORDER BY role ASC, name ASC")
    fun observeByCompany(companyId: String): Flow<List<UserEntity>>

    @Query(
        "UPDATE users SET isActive = :isActive, isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :userId AND companyId = :companyId"
    )
    suspend fun updateActiveStatus(
        userId: String,
        companyId: String,
        isActive: Boolean,
        updatedAt: Long
    )

    @Query(
        "UPDATE users SET companyId = :companyId, isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :userId"
    )
    suspend fun updateCompanyId(
        userId: String,
        companyId: String,
        updatedAt: Long
    )

    @Query(
        "UPDATE users SET passwordHash = :passwordHash, mustChangePassword = 0, " +
            "isSynced = 0, updatedAt = :updatedAt WHERE id = :userId"
    )
    suspend fun updatePasswordAndClearForceChange(
        userId: String,
        passwordHash: String,
        updatedAt: Long
    )

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM users WHERE companyId = :companyId AND isSynced = 0")
    suspend fun getUnsyncedByCompany(companyId: String): List<UserEntity>

    @Query("UPDATE users SET isSynced = 1, updatedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT COUNT(*) FROM users WHERE companyId = :companyId AND isSynced = 0")
    fun observeUnsyncedCountByCompany(companyId: String): Flow<Int>
}
