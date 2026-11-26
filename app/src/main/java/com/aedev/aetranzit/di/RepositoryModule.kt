package com.aedev.aetranzit.di

import com.aedev.aetranzit.data.repository.AuthRepositoryImpl
import com.aedev.aetranzit.data.repository.SyncPreferencesRepositoryImpl
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncPreferencesRepository(
        impl: SyncPreferencesRepositoryImpl
    ): SyncPreferencesRepository
}
