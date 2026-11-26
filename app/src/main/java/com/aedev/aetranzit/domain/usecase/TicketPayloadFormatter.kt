package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class TicketPayloadFormatter @Inject constructor() {

    fun buildQrPayload(
        ticket: Ticket,
        busNumber: String,
        routeLabel: String
    ): String {
        val safeBus = sanitizeSegment(busNumber).ifBlank { "NA" }
        val safeRoute = sanitizeSegment(routeLabel).ifBlank { "NA" }
        val safeSeat = sanitizeSegment(ticket.seatNumber).ifBlank { "NA" }
        val safeDestination = sanitizeSegment(ticket.destination).ifBlank { "NA" }
        val fareFixed = String.format(Locale.US, "%.2f", ticket.fareAmount)

        return listOf(
            PAYLOAD_PREFIX,
            PAYLOAD_VERSION,
            ticket.referenceCode,
            sanitizeSegment(ticket.tripId),
            "B:$safeBus",
            "R:$safeRoute",
            "S$safeSeat",
            safeDestination,
            ticket.passengerType.name,
            fareFixed,
            ticket.createdAt.toString()
        ).joinToString(separator = "|")
    }

    fun buildSmsReceiptText(
        ticket: Ticket,
        busNumber: String,
        plateNumber: String,
        routeLabel: String,
        conductorName: String,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val resolvedBus = busNumber.trim().ifBlank { "N/A" }
        val resolvedPlate = plateNumber.trim().ifBlank { "N/A" }
        val resolvedRoute = routeLabel.trim().ifBlank { "N/A" }
        val resolvedConductor = conductorName.trim().ifBlank { "Conductor" }
        val passengerLine = if (ticket.passengerType.isDiscounted) {
            "${ticket.passengerType.displayName} (-20% Discount)"
        } else {
            ticket.passengerType.displayName
        }
        val formattedTime = formatTimestamp(ticket.createdAt, timeZone)

        return buildString {
            appendLine("AeTranzit Digital Ticket")
            appendLine("Ref #: #${ticket.referenceCode} (${ticket.status.displayName.uppercase(Locale.US)})")
            appendLine("Bus: $resolvedBus ($resolvedPlate)")
            appendLine("Route: $resolvedRoute")
            appendLine("Seat: ${ticket.seatNumber} -> ${ticket.destination}")
            appendLine("Passenger: $passengerLine")
            appendLine("Fare: ${formatCurrency(ticket.fareAmount)}")
            appendLine("Cash: ${formatCurrency(ticket.cashTendered)} | Change: ${formatCurrency(ticket.changeAmount)}")
            appendLine("Conductor: $resolvedConductor")
            append("Issued: $formattedTime")
        }
    }

    fun formatTimestamp(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
        }
        return formatter.format(Date(epochMillis))
    }

    private fun formatCurrency(amount: Double): String =
        String.format(Locale.US, "₱%,.2f", amount)

    private fun sanitizeSegment(raw: String): String =
        raw.replace('|', '-')
            .replace(WHITESPACE_REGEX, " ")
            .trim()

    companion object {
        const val PAYLOAD_PREFIX = "AETRANZIT"
        const val PAYLOAD_VERSION = "v1"
        private const val TIMESTAMP_PATTERN = "MMM dd, yyyy hh:mm a"
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
