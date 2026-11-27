package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>
    fun observeAllUsers(): Flow<List<User>>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun getCurrentUser(): User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<User>
    suspend fun completeCompanySetup(companyName: String): Result<User>
    suspend fun createStaffAccount(
        name: String,
        email: String,
        temporaryPassword: String,
        role: UserRole = UserRole.CONDUCTOR
    ): Result<User>
    suspend fun completeForcedPasswordChange(newPassword: String): Result<User>
    suspend fun setUserActiveStatus(userId: String, isActive: Boolean): Result<Unit>
    suspend fun logout()
}
