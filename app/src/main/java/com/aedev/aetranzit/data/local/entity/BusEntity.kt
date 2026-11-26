package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey val id: String,
    val busNumber: String,
    val plateNumber: String,
    val totalSeats: Int,
    val columns: Int,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
