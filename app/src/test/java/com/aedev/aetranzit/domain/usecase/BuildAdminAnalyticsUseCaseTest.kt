package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BuildAdminAnalyticsUseCaseTest {

    private val useCase = BuildAdminAnalyticsUseCase()
    private val manilaTz: TimeZone = TimeZone.getTimeZone("Asia/Manila")

    private fun epochAt(year: Int, monthOneBased: Int, day: Int, hour: Int = 10): Long {
        return Calendar.getInstance(manilaTz).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `execute returns empty chart points and zero KPIs when no tickets exist`() {
        val now = epochAt(2026, 11, 26, hour = 16)

        val summary = useCase.execute(
            tickets = emptyList(),
            trips = emptyList(),
            buses = emptyList(),
            routes = emptyList(),
            users = emptyList(),
            dateRange = AnalyticsDateRange.TODAY,
            nowMillis = now,
            timeZone = manilaTz
        )

        assertEquals(0.0, summary.totalRevenue, 0.001)
        assertEquals(0, summary.totalTicketsSold)
        assertEquals(0, summary.discountedTicketsCount)
        assertEquals(0.0, summary.discountedSharePercent, 0.001)
        assertEquals(0, summary.activeTripsCount)
        assertEquals(0.0, summary.averageLoadFactorPercent, 0.001)
        assertTrue(summary.dailyRevenuePoints.isEmpty())
        assertTrue(summary.topDestinations.isEmpty())
        assertTrue(summary.conductorSummaries.isEmpty())
    }

    @Test
    fun `execute filters tickets across TODAY LAST_7_DAYS LAST_30_DAYS and ALL_TIME`() {
        val now = epochAt(2026, 11, 26, hour = 17)
        val todayTicket = sampleTicket(
            id = "t-today",
            fare = 200.0,
            passengerType = PassengerType.REGULAR,
            createdAt = epochAt(2026, 11, 26, hour = 9)
        )
        val fiveDaysAgoTicket = sampleTicket(
            id = "t-5d",
            fare = 160.0,
            passengerType = PassengerType.STUDENT,
            createdAt = epochAt(2026, 11, 21, hour = 14)
        )
        val twentyDaysAgoTicket = sampleTicket(
            id = "t-20d",
            fare = 300.0,
            passengerType = PassengerType.SENIOR,
            createdAt = epochAt(2026, 11, 6, hour = 11)
        )
        val sixtyDaysAgoTicket = sampleTicket(
            id = "t-60d",
            fare = 450.0,
            passengerType = PassengerType.REGULAR,
            createdAt = epochAt(2026, 9, 15, hour = 8)
        )

        val tickets = listOf(todayTicket, fiveDaysAgoTicket, twentyDaysAgoTicket, sixtyDaysAgoTicket)

        val todaySummary = useCase.execute(
            tickets = tickets,
            trips = emptyList(),
            buses = emptyList(),
            routes = emptyList(),
            users = emptyList(),
            dateRange = AnalyticsDateRange.TODAY,
            nowMillis = now,
            timeZone = manilaTz
        )
        assertEquals(1, todaySummary.totalTicketsSold)
        assertEquals(200.0, todaySummary.totalRevenue, 0.001)

        val weekSummary = useCase.execute(
            tickets = tickets,
            trips = emptyList(),
            buses = emptyList(),
            routes = emptyList(),
            users = emptyList(),
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            nowMillis = now,
            timeZone = manilaTz
        )
        assertEquals(2, weekSummary.totalTicketsSold)
        assertEquals(360.0, weekSummary.totalRevenue, 0.001)
        assertEquals(1, weekSummary.discountedTicketsCount)
        assertEquals(50.0, weekSummary.discountedSharePercent, 0.001)
        assertEquals(2, weekSummary.dailyRevenuePoints.size)
        assertTrue(
            weekSummary.dailyRevenuePoints[0].dayStartMillis <
                weekSummary.dailyRevenuePoints[1].dayStartMillis
        )

        val monthSummary = useCase.execute(
            tickets = tickets,
            trips = emptyList(),
            buses = emptyList(),
            routes = emptyList(),
            users = emptyList(),
            dateRange = AnalyticsDateRange.LAST_30_DAYS,
            nowMillis = now,
            timeZone = manilaTz
        )
        assertEquals(3, monthSummary.totalTicketsSold)
        assertEquals(660.0, monthSummary.totalRevenue, 0.001)

        val allTimeSummary = useCase.execute(
            tickets = tickets,
            trips = emptyList(),
            buses = emptyList(),
            routes = emptyList(),
            users = emptyList(),
            dateRange = AnalyticsDateRange.ALL_TIME,
            nowMillis = now,
            timeZone = manilaTz
        )
        assertEquals(4, allTimeSummary.totalTicketsSold)
        assertEquals(1_110.0, allTimeSummary.totalRevenue, 0.001)
    }

    @Test
    fun `execute ranks top destinations computes load factor and summarizes conductors`() {
        val now = epochAt(2026, 11, 26, hour = 16)
        val buses = listOf(
            Bus(
                id = "bus-1",
                busNumber = "AE-101",
                plateNumber = "NAA-1010",
                totalSeats = 40,
                columns = 4,
                isSynced = true,
                updatedAt = now
            )
        )
        val routes = listOf(
            Route(
                id = "route-1",
                origin = "Cubao",
                destination = "Baguio",
                baseFare = 450.0,
                stops = listOf(RouteStop("Tarlac Junction", 200.0)),
                isSynced = true,
                updatedAt = now
            )
        )
        val users = listOf(
            User(
                id = "cond-1",
                name = "Ramon Magsaysay",
                email = "ramon@aetranzit.ph",
                role = UserRole.CONDUCTOR,
                isActive = true,
                isSynced = true,
                updatedAt = now
            ),
            User(
                id = "cond-2",
                name = "Juan Luna",
                email = "juan@aetranzit.ph",
                role = UserRole.CONDUCTOR,
                isActive = true,
                isSynced = true,
                updatedAt = now
            )
        )
        val trips = listOf(
            Trip(
                id = "trip-1",
                busId = "bus-1",
                routeId = "route-1",
                conductorId = "cond-1",
                status = TripStatus.ACTIVE,
                startedAt = epochAt(2026, 11, 26, hour = 8),
                endedAt = null,
                isSynced = true,
                updatedAt = now
            )
        )
        val tickets = listOf(
            sampleTicket(
                id = "t1",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = "cond-1",
                destination = "Baguio",
                fare = 450.0,
                passengerType = PassengerType.REGULAR,
                status = TicketStatus.ACTIVE,
                createdAt = epochAt(2026, 11, 26, hour = 9)
            ),
            sampleTicket(
                id = "t2",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = "cond-1",
                destination = "Baguio",
                fare = 360.0,
                passengerType = PassengerType.PWD,
                status = TicketStatus.ACTIVE,
                createdAt = epochAt(2026, 11, 26, hour = 10)
            ),
            sampleTicket(
                id = "t3",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = "cond-2",
                destination = "Tarlac Junction",
                fare = 200.0,
                passengerType = PassengerType.REGULAR,
                status = TicketStatus.DROPPED_OFF,
                createdAt = epochAt(2026, 11, 26, hour = 11)
            )
        )

        val summary = useCase.execute(
            tickets = tickets,
            trips = trips,
            buses = buses,
            routes = routes,
            users = users,
            dateRange = AnalyticsDateRange.TODAY,
            nowMillis = now,
            timeZone = manilaTz
        )

        assertEquals(1_010.0, summary.totalRevenue, 0.001)
        assertEquals(3, summary.totalTicketsSold)
        assertEquals(1, summary.discountedTicketsCount)
        assertEquals(33.3, summary.discountedSharePercent, 0.001)
        assertEquals(1, summary.activeTripsCount)
        // 2 ACTIVE tickets on a 40-seat bus = 5.0%
        assertEquals(5.0, summary.averageLoadFactorPercent, 0.001)

        assertEquals(2, summary.topDestinations.size)
        assertEquals("Baguio", summary.topDestinations[0].destination)
        assertEquals(810.0, summary.topDestinations[0].totalRevenue, 0.001)
        assertEquals("Tarlac Junction", summary.topDestinations[1].destination)
        assertEquals(200.0, summary.topDestinations[1].totalRevenue, 0.001)

        assertEquals(2, summary.conductorSummaries.size)
        assertEquals("Ramon Magsaysay", summary.conductorSummaries[0].conductorName)
        assertEquals(810.0, summary.conductorSummaries[0].totalRevenue, 0.001)
        assertEquals(2, summary.conductorSummaries[0].ticketsIssued)
        assertEquals("Juan Luna", summary.conductorSummaries[1].conductorName)
        assertEquals(200.0, summary.conductorSummaries[1].totalRevenue, 0.001)
    }

    private fun sampleTicket(
        id: String,
        tripId: String = "trip-default",
        busId: String = "bus-default",
        conductorId: String = "cond-default",
        destination: String = "Baguio",
        fare: Double,
        passengerType: PassengerType,
        status: TicketStatus = TicketStatus.ACTIVE,
        createdAt: Long
    ): Ticket = Ticket(
        id = id,
        tripId = tripId,
        busId = busId,
        conductorId = conductorId,
        seatNumber = "1",
        destination = destination,
        passengerType = passengerType,
        fareAmount = fare,
        cashTendered = fare,
        changeAmount = 0.0,
        status = status,
        createdAt = createdAt,
        isSynced = true,
        updatedAt = createdAt
    )
}
