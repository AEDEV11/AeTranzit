package com.aedev.aetranzit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val origin: String,
    val destination: String,
    val baseFare: Double,
    val stopsJson: String,
    val isSynced: Boolean = false,
    val updatedAt: Long
)
