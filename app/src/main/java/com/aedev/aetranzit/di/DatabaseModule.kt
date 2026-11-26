package com.aedev.aetranzit.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.aedev.aetranzit.data.local.AeTranzitDatabase
import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAeTranzitDatabase(
        @ApplicationContext context: Context
    ): AeTranzitDatabase = Room.databaseBuilder(
        context,
        AeTranzitDatabase::class.java,
        "aetranzit.db"
    ).build()

    @Provides
    @Singleton
    fun provideDatabaseTransactionRunner(
        database: AeTranzitDatabase
    ): DatabaseTransactionRunner = object : DatabaseTransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T =
            database.withTransaction { block() }
    }

    @Provides
    fun provideUserDao(database: AeTranzitDatabase): UserDao = database.userDao()

    @Provides
    fun provideBusDao(database: AeTranzitDatabase): BusDao = database.busDao()

    @Provides
    fun provideRouteDao(database: AeTranzitDatabase): RouteDao = database.routeDao()

    @Provides
    fun provideTripDao(database: AeTranzitDatabase): TripDao = database.tripDao()

    @Provides
    fun provideTicketDao(database: AeTranzitDatabase): TicketDao = database.ticketDao()
}
