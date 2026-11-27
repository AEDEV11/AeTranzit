package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aedev.aetranzit.domain.model.Company

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["companyId"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String, // "ADMIN" or "CONDUCTOR"
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val isActive: Boolean = true,
    val mustChangePassword: Boolean = false,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
