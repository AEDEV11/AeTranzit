package com.aedev.aetranzit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aedev.aetranzit.data.local.dao.BusDao
import com.aedev.aetranzit.data.local.dao.CompanyDao
import com.aedev.aetranzit.data.local.dao.RouteDao
import com.aedev.aetranzit.data.local.dao.TicketDao
import com.aedev.aetranzit.data.local.dao.TripDao
import com.aedev.aetranzit.data.local.dao.UserDao
import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.local.entity.UserEntity

@Database(
    entities = [
        CompanyEntity::class,
        UserEntity::class,
        BusEntity::class,
        RouteEntity::class,
        TripEntity::class,
        TicketEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AeTranzitDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun userDao(): UserDao
    abstract fun busDao(): BusDao
    abstract fun routeDao(): RouteDao
    abstract fun tripDao(): TripDao
    abstract fun ticketDao(): TicketDao
}
