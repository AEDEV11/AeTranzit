package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class DailyRevenuePoint(
    val dayStartMillis: Long,
    val label: String,
    val revenue: Double,
    val ticketCount: Int
)

data class DestinationRevenueStat(
    val destination: String,
    val ticketCount: Int,
    val totalRevenue: Double,
    val revenueSharePercent: Double
)

data class ConductorPerformanceStat(
    val conductorId: String,
    val conductorName: String,
    val ticketsIssued: Int,
    val totalRevenue: Double,
    val tripsCount: Int
)

data class AdminAnalyticsSummary(
    val dateRange: AnalyticsDateRange = AnalyticsDateRange.LAST_7_DAYS,
    val totalRevenue: Double = 0.0,
    val totalTicketsSold: Int = 0,
    val regularTicketsCount: Int = 0,
    val discountedTicketsCount: Int = 0,
    val discountedSharePercent: Double = 0.0,
    val activeTripsCount: Int = 0,
    val averageLoadFactorPercent: Double = 0.0,
    val dailyRevenuePoints: List<DailyRevenuePoint> = emptyList(),
    val topDestinations: List<DestinationRevenueStat> = emptyList(),
    val conductorSummaries: List<ConductorPerformanceStat> = emptyList()
)

class BuildAdminAnalyticsUseCase @Inject constructor() {

    fun execute(
        tickets: List<Ticket>,
        trips: List<Trip>,
        buses: List<Bus>,
        routes: List<Route>,
        users: List<User>,
        dateRange: AnalyticsDateRange,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        maxTopDestinations: Int = DEFAULT_MAX_TOP_DESTINATIONS
    ): AdminAnalyticsSummary {
        val filteredTickets = tickets.filter { ticket ->
            dateRange.contains(ticket.createdAt, nowMillis, timeZone)
        }

        val totalRevenue = CalculateFareUseCase.roundCurrency(
            filteredTickets.sumOf { it.fareAmount }
        )
        val totalTicketsSold = filteredTickets.size
        val discountedTicketsCount = filteredTickets.count { it.passengerType.isDiscounted }
        val regularTicketsCount = totalTicketsSold - discountedTicketsCount
        val discountedSharePercent = if (totalTicketsSold == 0) {
            0.0
        } else {
            roundOneDecimal((discountedTicketsCount.toDouble() * 100.0) / totalTicketsSold.toDouble())
        }

        val activeTrips = trips.filter { it.status == TripStatus.ACTIVE }
        val activeTripsCount = activeTrips.size

        val averageLoadFactorPercent = computeAverageLoadFactorPercent(
            activeTrips = activeTrips,
            allTrips = trips,
            allTickets = tickets,
            filteredTickets = filteredTickets,
            buses = buses,
            dateRange = dateRange,
            nowMillis = nowMillis,
            timeZone = timeZone
        )

        val dailyRevenuePoints = buildDailyRevenuePoints(
            filteredTickets = filteredTickets,
            timeZone = timeZone
        )

        val topDestinations = buildTopDestinations(
            filteredTickets = filteredTickets,
            routes = routes,
            totalRevenue = totalRevenue,
            maxTopDestinations = maxTopDestinations
        )

        val conductorSummaries = buildConductorSummaries(
            filteredTickets = filteredTickets,
            trips = trips,
            users = users,
            dateRange = dateRange,
            nowMillis = nowMillis,
            timeZone = timeZone
        )

        return AdminAnalyticsSummary(
            dateRange = dateRange,
            totalRevenue = totalRevenue,
            totalTicketsSold = totalTicketsSold,
            regularTicketsCount = regularTicketsCount,
            discountedTicketsCount = discountedTicketsCount,
            discountedSharePercent = discountedSharePercent,
            activeTripsCount = activeTripsCount,
            averageLoadFactorPercent = averageLoadFactorPercent,
            dailyRevenuePoints = dailyRevenuePoints,
            topDestinations = topDestinations,
            conductorSummaries = conductorSummaries
        )
    }

    private fun computeAverageLoadFactorPercent(
        activeTrips: List<Trip>,
        allTrips: List<Trip>,
        allTickets: List<Ticket>,
        filteredTickets: List<Ticket>,
        buses: List<Bus>,
        dateRange: AnalyticsDateRange,
        nowMillis: Long,
        timeZone: TimeZone
    ): Double {
        val busesById = buses.associateBy { it.id }

        if (activeTrips.isNotEmpty()) {
            val activeTicketsByTrip = allTickets
                .filter { it.status == TicketStatus.ACTIVE }
                .groupBy { it.tripId }

            val occupancies = activeTrips.map { trip ->
                val capacity = busesById[trip.busId]?.totalSeats?.coerceAtLeast(1) ?: DEFAULT_SEAT_CAPACITY
                val occupied = activeTicketsByTrip[trip.id]?.size ?: 0
                ((occupied.toDouble() / capacity.toDouble()) * 100.0).coerceIn(0.0, 100.0)
            }
            return roundOneDecimal(occupancies.average())
        }

        val filteredTrips = allTrips.filter { trip ->
            dateRange.contains(trip.startedAt, nowMillis, timeZone)
        }
        if (filteredTrips.isEmpty()) return 0.0

        val ticketsByTrip = filteredTickets.groupBy { it.tripId }
        val historicalFactors = filteredTrips.map { trip ->
            val capacity = busesById[trip.busId]?.totalSeats?.coerceAtLeast(1) ?: DEFAULT_SEAT_CAPACITY
            val boarded = ticketsByTrip[trip.id]?.size ?: 0
            ((boarded.toDouble() / capacity.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        }
        return roundOneDecimal(historicalFactors.average())
    }

    private fun buildDailyRevenuePoints(
        filteredTickets: List<Ticket>,
        timeZone: TimeZone
    ): List<DailyRevenuePoint> {
        if (filteredTickets.isEmpty()) return emptyList()

        val dayLabelFormatter = SimpleDateFormat(DAY_LABEL_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
        }

        return filteredTickets
            .groupBy { ticket -> AnalyticsDateRange.startOfDayMillis(ticket.createdAt, timeZone) }
            .toSortedMap()
            .map { (dayStartMillis, dayTickets) ->
                DailyRevenuePoint(
                    dayStartMillis = dayStartMillis,
                    label = dayLabelFormatter.format(Date(dayStartMillis)),
                    revenue = CalculateFareUseCase.roundCurrency(dayTickets.sumOf { it.fareAmount }),
                    ticketCount = dayTickets.size
                )
            }
    }

    private fun buildTopDestinations(
        filteredTickets: List<Ticket>,
        routes: List<Route>,
        totalRevenue: Double,
        maxTopDestinations: Int
    ): List<DestinationRevenueStat> {
        if (filteredTickets.isEmpty()) return emptyList()

        val canonicalStopNames = buildMap {
            routes.forEach { route ->
                put(route.destination.lowercase(Locale.US), route.destination)
                route.stops.forEach { stop ->
                    put(stop.name.lowercase(Locale.US), stop.name)
                }
            }
        }

        return filteredTickets
            .groupBy { ticket ->
                val trimmed = ticket.destination.trim().ifBlank { "Unknown" }
                canonicalStopNames[trimmed.lowercase(Locale.US)] ?: trimmed
            }
            .map { (destination, destTickets) ->
                val destRevenue = CalculateFareUseCase.roundCurrency(
                    destTickets.sumOf { it.fareAmount }
                )
                val share = if (totalRevenue <= 0.0) {
                    0.0
                } else {
                    roundOneDecimal((destRevenue * 100.0) / totalRevenue)
                }
                DestinationRevenueStat(
                    destination = destination,
                    ticketCount = destTickets.size,
                    totalRevenue = destRevenue,
                    revenueSharePercent = share
                )
            }
            .sortedWith(
                compareByDescending<DestinationRevenueStat> { it.totalRevenue }
                    .thenByDescending { it.ticketCount }
                    .thenBy { it.destination }
            )
            .take(maxTopDestinations.coerceAtLeast(1))
    }

    private fun buildConductorSummaries(
        filteredTickets: List<Ticket>,
        trips: List<Trip>,
        users: List<User>,
        dateRange: AnalyticsDateRange,
        nowMillis: Long,
        timeZone: TimeZone
    ): List<ConductorPerformanceStat> {
        val usersById = users.associateBy { it.id }
        val filteredTrips = trips.filter { trip ->
            dateRange.contains(trip.startedAt, nowMillis, timeZone)
        }

        val ticketsByConductor = filteredTickets.groupBy { it.conductorId }
        val tripsByConductor = filteredTrips.groupBy { it.conductorId }

        val conductorIds = buildSet {
            users.filter { it.role == UserRole.CONDUCTOR }.forEach { add(it.id) }
            addAll(ticketsByConductor.keys)
            addAll(tripsByConductor.keys)
        }.filter { it.isNotBlank() }

        return conductorIds
            .map { conductorId ->
                val conductorTickets = ticketsByConductor[conductorId].orEmpty()
                val conductorTripsIds = buildSet {
                    tripsByConductor[conductorId].orEmpty().forEach { add(it.id) }
                    conductorTickets.forEach { if (it.tripId.isNotBlank()) add(it.tripId) }
                }
                val resolvedName = usersById[conductorId]?.name?.takeIf { it.isNotBlank() }
                    ?: "Conductor (${conductorId.take(6)})"

                ConductorPerformanceStat(
                    conductorId = conductorId,
                    conductorName = resolvedName,
                    ticketsIssued = conductorTickets.size,
                    totalRevenue = CalculateFareUseCase.roundCurrency(
                        conductorTickets.sumOf { it.fareAmount }
                    ),
                    tripsCount = conductorTripsIds.size
                )
            }
            .filter { stat ->
                stat.ticketsIssued > 0 || stat.tripsCount > 0 || usersById[stat.conductorId]?.isActive == true
            }
            .sortedWith(
                compareByDescending<ConductorPerformanceStat> { it.totalRevenue }
                    .thenByDescending { it.ticketsIssued }
                    .thenBy { it.conductorName }
            )
    }

    private fun roundOneDecimal(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    companion object {
        private const val DAY_LABEL_PATTERN = "MMM dd"
        private const val DEFAULT_SEAT_CAPACITY = 45
        private const val DEFAULT_MAX_TOP_DESTINATIONS = 5
    }
}
