package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aedev.aetranzit.domain.model.Company

@Entity(
    tableName = "routes",
    indices = [Index(value = ["companyId"])]
)
data class RouteEntity(
    @PrimaryKey val id: String,
    val origin: String,
    val destination: String,
    val baseFare: Double,
    val stopsJson: String,
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
