package com.aedev.aetranzit.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aedev.aetranzit.data.local.AeTranzitDatabase
import com.aedev.aetranzit.data.local.DatabaseTransactionRunner
import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.domain.model.Company
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val migratedAt = System.currentTimeMillis()
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `companies` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `joinCode` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_companies_joinCode` ON `companies` (`joinCode`)"
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO `companies` (`id`, `name`, `joinCode`, `createdAt`, `isSynced`, `updatedAt`)
                VALUES (
                    '${Company.DEFAULT_COMPANY_ID}',
                    '${Company.DEFAULT_COMPANY_NAME}',
                    '${Company.DEFAULT_JOIN_CODE}',
                    $migratedAt,
                    0,
                    $migratedAt
                )
                """.trimIndent()
            )

            db.execSQL(
                "ALTER TABLE `users` ADD COLUMN `companyId` TEXT NOT NULL DEFAULT '${Company.DEFAULT_COMPANY_ID}'"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_companyId` ON `users` (`companyId`)")

            db.execSQL(
                "ALTER TABLE `buses` ADD COLUMN `companyId` TEXT NOT NULL DEFAULT '${Company.DEFAULT_COMPANY_ID}'"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_buses_companyId` ON `buses` (`companyId`)")

            db.execSQL(
                "ALTER TABLE `routes` ADD COLUMN `companyId` TEXT NOT NULL DEFAULT '${Company.DEFAULT_COMPANY_ID}'"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_routes_companyId` ON `routes` (`companyId`)")

            db.execSQL(
                "ALTER TABLE `trips` ADD COLUMN `companyId` TEXT NOT NULL DEFAULT '${Company.DEFAULT_COMPANY_ID}'"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_trips_companyId` ON `trips` (`companyId`)")

            db.execSQL(
                "ALTER TABLE `tickets` ADD COLUMN `companyId` TEXT NOT NULL DEFAULT '${Company.DEFAULT_COMPANY_ID}'"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tickets_companyId` ON `tickets` (`companyId`)")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `users` ADD COLUMN `mustChangePassword` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    @Provides
    @Singleton
    fun provideAeTranzitDatabase(
        @ApplicationContext context: Context
    ): AeTranzitDatabase = Room.databaseBuilder(
        context,
        AeTranzitDatabase::class.java,
        "aetranzit.db"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    @Provides
    @Singleton
    fun provideDatabaseTransactionRunner(
        database: AeTranzitDatabase
    ): DatabaseTransactionRunner = object : DatabaseTransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T =
            database.withTransaction { block() }
    }

    @Provides
    fun provideCompanyDao(database: AeTranzitDatabase): CompanyDao = database.companyDao()

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
