package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.repository.CompanyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val companyDao: CompanyDao,
    private val activeCompanyProvider: ActiveCompanyProvider
) : CompanyRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveCompany(): Flow<Company?> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(null)
            } else {
                companyDao.observeById(companyId).map { it?.toDomain() }
            }
        }

    override fun observeCompanyById(companyId: String): Flow<Company?> {
        val cleanId = companyId.trim()
        if (cleanId.isBlank()) return flowOf(null)
        return companyDao.observeById(cleanId).map { it?.toDomain() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnsyncedCount(): Flow<Int> =
        activeCompanyProvider.observeActiveCompanyId().flatMapLatest { companyId ->
            if (companyId.isNullOrBlank()) {
                flowOf(0)
            } else {
                companyDao.observeUnsyncedCountByCompany(companyId)
            }
        }
}
