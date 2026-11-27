package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "companies",
    indices = [Index(value = ["joinCode"], unique = true)]
)
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val joinCode: String,
    val createdAt: Long,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
