package com.aedev.aetranzit.di

import com.aedev.aetranzit.data.remote.FirebaseFleetSyncDataSource
import com.aedev.aetranzit.data.remote.FleetSyncRemoteDataSource
import com.aedev.aetranzit.data.repository.AuthRepositoryImpl
import com.aedev.aetranzit.data.repository.BusRepositoryImpl
import com.aedev.aetranzit.data.repository.RouteRepositoryImpl
import com.aedev.aetranzit.data.repository.SyncPreferencesRepositoryImpl
import com.aedev.aetranzit.data.repository.TicketRepositoryImpl
import com.aedev.aetranzit.data.repository.TripRepositoryImpl
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
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

    @Binds
    @Singleton
    abstract fun bindFleetSyncRemoteDataSource(
        impl: FirebaseFleetSyncDataSource
    ): FleetSyncRemoteDataSource

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
