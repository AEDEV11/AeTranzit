package com.aedev.aetranzit.domain.model

data class Bus(
    val id: String,
    val busNumber: String,
    val plateNumber: String,
    val totalSeats: Int,
    val columns: Int,
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    val layoutLabel: String
        get() = if (columns == 5) "2x3" else "2x2"
}
