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

    @Query("SELECT * FROM routes ORDER BY origin ASC, destination ASC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE isSynced = 0")
    suspend fun getUnsynced(): List<RouteEntity>

    @Query("SELECT COUNT(*) FROM routes WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
