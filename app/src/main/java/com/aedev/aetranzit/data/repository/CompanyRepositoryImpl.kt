package com.aedev.aetranzit.data.repository

import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.mapper.toDomain
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.usecase.CompanyJoinCodeGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val companyDao: CompanyDao,
    private val activeCompanyProvider: ActiveCompanyProvider,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val fleetSyncDataSource: FleetSyncRemoteDataSource,
    private val joinCodeGenerator: CompanyJoinCodeGenerator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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

    override suspend fun getActiveCompany(): Company? = withContext(ioDispatcher) {
        val companyId = activeCompanyProvider.getActiveCompanyId() ?: return@withContext null
        companyDao.getById(companyId)?.toDomain()
    }

    override suspend fun getCompanyById(companyId: String): Company? = withContext(ioDispatcher) {
        val cleanId = companyId.trim()
        if (cleanId.isBlank()) return@withContext null
        companyDao.getById(cleanId)?.toDomain()
    }

    override suspend fun findByJoinCode(joinCode: String): Result<Company?> =
        withContext(ioDispatcher) {
            try {
                val normalized = joinCodeGenerator.normalize(joinCode)
                if (normalized.isBlank()) {
                    return@withContext Result.success(null)
                }

                val local = companyDao.getByJoinCode(normalized)
                if (local != null) {
                    return@withContext Result.success(local.toDomain())
                }

                if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                    return@withContext Result.success(null)
                }

                val remote = fleetSyncDataSource.fetchCompanyByJoinCode(normalized).getOrThrow()
                if (remote != null) {
                    companyDao.upsert(remote)
                }
                Result.success(remote?.toDomain())
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun syncPendingCompanies(): Result<Int> = withContext(ioDispatcher) {
        try {
            if (!syncPreferencesRepository.isCloudSyncEnabled.first()) {
                return@withContext Result.failure(
                    IllegalStateException("Enable Cloud Sync in Settings before syncing.")
                )
            }
            val companyId = activeCompanyProvider.getActiveCompanyId()
                ?: return@withContext Result.success(0)

            val unsynced = companyDao.getUnsyncedByCompany(companyId)
            var syncedCount = 0
            for (company in unsynced) {
                if (fleetSyncDataSource.pushCompany(company).isSuccess) {
                    val syncedAt = System.currentTimeMillis()
                    companyDao.markSynced(company.id, syncedAt)
                    syncedCount++
                }
            }
            if (syncedCount > 0) {
                syncPreferencesRepository.updateLastSyncedAt(System.currentTimeMillis())
            }
            Result.success(syncedCount)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
