package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aedev.aetranzit.domain.model.Company

@Entity(
    tableName = "buses",
    indices = [Index(value = ["companyId"])]
)
data class BusEntity(
    @PrimaryKey val id: String,
    val busNumber: String,
    val plateNumber: String,
    val totalSeats: Int,
    val columns: Int,
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
