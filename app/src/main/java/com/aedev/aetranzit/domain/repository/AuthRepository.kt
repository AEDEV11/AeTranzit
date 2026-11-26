package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, role: UserRole): Result<User>
    suspend fun syncPendingUsers(): Result<Int>
    suspend fun logout()
}
