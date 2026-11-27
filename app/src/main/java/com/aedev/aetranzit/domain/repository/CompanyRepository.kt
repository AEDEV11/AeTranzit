package com.aedev.aetranzit.domain.repository

import com.aedev.aetranzit.domain.model.Company
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun observeActiveCompany(): Flow<Company?>
    fun observeCompanyById(companyId: String): Flow<Company?>
    fun observeUnsyncedCount(): Flow<Int>
}
