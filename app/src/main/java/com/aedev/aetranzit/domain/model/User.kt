package com.aedev.aetranzit.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val isSynced: Boolean,
    val updatedAt: Long
)
