package com.aedev.aetranzit.di

import com.aedev.aetranzit.data.remote.AuthSyncRemoteDataSource
import com.aedev.aetranzit.data.remote.FirebaseAuthSyncDataSource
import com.aedev.aetranzit.data.remote.FirebaseFleetSyncDataSource
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.data.repository.ActiveCompanyProvider
import com.aedev.aetranzit.data.repository.AuthRepositoryImpl
import com.aedev.aetranzit.data.repository.BusRepositoryImpl
import com.aedev.aetranzit.data.repository.CompanyRepositoryImpl
import com.aedev.aetranzit.data.repository.RoomActiveCompanyProvider
import com.aedev.aetranzit.data.repository.RouteRepositoryImpl
import com.aedev.aetranzit.data.repository.SyncPreferencesRepositoryImpl
import com.aedev.aetranzit.data.repository.TicketRepositoryImpl
import com.aedev.aetranzit.data.repository.TripRepositoryImpl
import com.aedev.aetranzit.data.sync.NetworkConnectivityObserver
import com.aedev.aetranzit.data.sync.SyncCoordinator
import com.aedev.aetranzit.data.sync.WorkManagerSyncScheduler
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.sync.ConnectivityObserver
import com.aedev.aetranzit.domain.sync.SyncManager
import com.aedev.aetranzit.domain.sync.SyncScheduler
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
    abstract fun bindActiveCompanyProvider(
        impl: RoomActiveCompanyProvider
    ): ActiveCompanyProvider

    @Binds
    @Singleton
    abstract fun bindCompanyRepository(
        impl: CompanyRepositoryImpl
    ): CompanyRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncPreferencesRepository(
        impl: SyncPreferencesRepositoryImpl
    ): SyncPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAuthSyncRemoteDataSource(
        impl: FirebaseAuthSyncDataSource
    ): AuthSyncRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindFleetSyncRemoteDataSource(
        impl: FirebaseFleetSyncDataSource
    ): FleetSyncRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(
        impl: NetworkConnectivityObserver
    ): ConnectivityObserver

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        impl: WorkManagerSyncScheduler
    ): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindSyncManager(
        impl: SyncCoordinator
    ): SyncManager

    @Binds
    @Singleton
    abstract fun bindBusRepository(impl: BusRepositoryImpl): BusRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(impl: TicketRepositoryImpl): TicketRepository
}
