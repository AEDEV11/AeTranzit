package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class TicketPayloadFormatter @Inject constructor() {

    fun buildVerificationUrl(ticketId: String): String {
        val cleanId = ticketId.trim()
        require(cleanId.isNotEmpty()) { "Ticket ID must not be blank for web verification URL." }
        return "$WEB_TICKET_BASE_URL$cleanId"
    }

    @Suppress("UNUSED_PARAMETER")
    fun buildQrPayload(
        ticket: Ticket,
        busNumber: String = "",
        routeLabel: String = ""
    ): String = buildVerificationUrl(ticket.id)

    fun buildSmsReceiptText(
        ticket: Ticket,
        companyName: String = Company.FALLBACK_BRAND_NAME,
        busNumber: String,
        plateNumber: String,
        routeLabel: String,
        conductorName: String,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val resolvedCompany = companyName.trim().ifBlank { Company.FALLBACK_BRAND_NAME }
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
        val verificationUrl = buildVerificationUrl(ticket.id)

        return buildString {
            appendLine("$resolvedCompany Digital Ticket")
            appendLine("Ref #: #${ticket.referenceCode} (${ticket.status.displayName.uppercase(Locale.US)})")
            appendLine("Bus: $resolvedBus ($resolvedPlate)")
            appendLine("Route: $resolvedRoute")
            appendLine("Seat: ${ticket.seatNumber} -> ${ticket.destination}")
            appendLine("Passenger: $passengerLine")
            appendLine("Fare: ${formatCurrency(ticket.fareAmount)}")
            appendLine("Cash: ${formatCurrency(ticket.cashTendered)} | Change: ${formatCurrency(ticket.changeAmount)}")
            appendLine("Conductor: $resolvedConductor")
            appendLine("Issued: $formattedTime")
            appendLine("Powered by AeTranzit")
            append("Verify/View Ticket: $verificationUrl")
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

    companion object {
        /**
         * Active ticket web verification URL prefix (Firebase Hosting / custom domain).
         */
        const val WEB_TICKET_BASE_URL = "https://aetranzit.aesprt.com/t/"
        private const val TIMESTAMP_PATTERN = "MMM dd, yyyy hh:mm a"
    }
}
