package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class ReportTicketRow(
    val ticketId: String,
    val referenceCode: String,
    val formattedDateTime: String,
    val busNumber: String,
    val routeLabel: String,
    val seatNumber: String,
    val destination: String,
    val passengerTypeLabel: String,
    val isDiscounted: Boolean,
    val fareAmount: Double,
    val cashTendered: Double,
    val changeAmount: Double,
    val statusLabel: String,
    val conductorName: String,
    val createdAt: Long
)

class ReportExportFormatter @Inject constructor() {

    fun buildReportRows(
        tickets: List<Ticket>,
        trips: List<Trip>,
        buses: List<Bus>,
        routes: List<Route>,
        users: List<User>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<ReportTicketRow> {
        val tripsById = trips.associateBy { it.id }
        val busesById = buses.associateBy { it.id }
        val routesById = routes.associateBy { it.id }
        val usersById = users.associateBy { it.id }

        val dateFormatter = SimpleDateFormat(REPORT_DATE_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
        }

        return tickets
            .sortedByDescending { it.createdAt }
            .map { ticket ->
                val trip = tripsById[ticket.tripId]
                val bus = busesById[ticket.busId] ?: trip?.let { tripsBus -> busesById[tripsBus.busId] }
                val route = trip?.let { routesById[it.routeId] }
                val conductor = usersById[ticket.conductorId]

                ReportTicketRow(
                    ticketId = ticket.id,
                    referenceCode = ticket.referenceCode,
                    formattedDateTime = dateFormatter.format(Date(ticket.createdAt)),
                    busNumber = bus?.busNumber?.ifBlank { "N/A" } ?: "N/A",
                    routeLabel = route?.displayName ?: "Unassigned Route",
                    seatNumber = ticket.seatNumber.ifBlank { "N/A" },
                    destination = ticket.destination.ifBlank { "N/A" },
                    passengerTypeLabel = ticket.passengerType.displayName,
                    isDiscounted = ticket.passengerType.isDiscounted,
                    fareAmount = ticket.fareAmount,
                    cashTendered = ticket.cashTendered,
                    changeAmount = ticket.changeAmount,
                    statusLabel = ticket.status.displayName,
                    conductorName = conductor?.name?.ifBlank { "Conductor" } ?: "Conductor",
                    createdAt = ticket.createdAt
                )
            }
    }

    fun buildCsvContent(
        tickets: List<Ticket>,
        trips: List<Trip>,
        buses: List<Bus>,
        routes: List<Route>,
        users: List<User>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val rows = buildReportRows(
            tickets = tickets,
            trips = trips,
            buses = buses,
            routes = routes,
            users = users,
            timeZone = timeZone
        )
        return buildCsvContent(rows)
    }

    fun buildCsvContent(rows: List<ReportTicketRow>): String {
        val lines = ArrayList<String>(rows.size + 1)
        lines.add(CSV_HEADERS.joinToString(separator = ",") { escapeCsvField(it) })

        for (row in rows) {
            val fields = listOf(
                row.formattedDateTime,
                "#${row.referenceCode}",
                row.busNumber,
                row.routeLabel,
                row.seatNumber,
                row.destination,
                row.passengerTypeLabel,
                formatFixedAmount(row.fareAmount),
                formatFixedAmount(row.cashTendered),
                formatFixedAmount(row.changeAmount),
                row.statusLabel,
                row.conductorName
            )
            lines.add(fields.joinToString(separator = ",") { escapeCsvField(it) })
        }

        return lines.joinToString(separator = CRLF)
    }

    fun escapeCsvField(value: String): String {
        val requiresQuoting = value.any { ch ->
            ch == ',' || ch == '"' || ch == '\n' || ch == '\r'
        }
        return if (requiresQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatFixedAmount(amount: Double): String =
        String.format(Locale.US, "%.2f", amount)

    companion object {
        val CSV_HEADERS: List<String> = listOf(
            "Date/Time",
            "Ticket Ref #",
            "Bus #",
            "Route",
            "Seat #",
            "Destination",
            "Passenger Type",
            "Fare",
            "Cash",
            "Change",
            "Status",
            "Conductor Name"
        )

        private const val REPORT_DATE_PATTERN = "yyyy-MM-dd HH:mm"
        private const val CRLF = "\r\n"
    }
}
