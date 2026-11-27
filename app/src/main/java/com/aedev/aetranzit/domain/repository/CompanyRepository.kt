package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.Company
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun observeActiveCompany(): Flow<Company?>
    fun observeCompanyById(companyId: String): Flow<Company?>
    fun observeUnsyncedCount(): Flow<Int>
    suspend fun getActiveCompany(): Company?
    suspend fun getCompanyById(companyId: String): Company?
    suspend fun findByJoinCode(joinCode: String): Result<Company?>
    suspend fun syncPendingCompanies(): Result<Int>
}
