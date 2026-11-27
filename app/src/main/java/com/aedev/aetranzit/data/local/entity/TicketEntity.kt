package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aedev.aetranzit.domain.model.Company

@Entity(
    tableName = "tickets",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["conductorId"]),
        Index(value = ["companyId"])
    ]
)
data class TicketEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val busId: String,
    val conductorId: String,
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val seatNumber: String,
    val destination: String,
    val passengerType: String,
    val fareAmount: Double,
    val cashTendered: Double,
    val changeAmount: Double,
    val status: String,
    val createdAt: Long,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
