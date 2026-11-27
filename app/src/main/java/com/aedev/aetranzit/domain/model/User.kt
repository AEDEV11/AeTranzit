package com.aedev.aetranzit.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val companyId: String = Company.DEFAULT_COMPANY_ID,
    val isActive: Boolean,
    val mustChangePassword: Boolean = false,
    val isSynced: Boolean,
    val updatedAt: Long
)
