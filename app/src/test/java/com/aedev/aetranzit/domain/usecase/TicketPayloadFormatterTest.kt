package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class TicketPayloadFormatterTest {

    private val formatter = TicketPayloadFormatter()

    private val sampleTicket = Ticket(
        id = "f8a1bc23-9876-4321-abcd-112233445566",
        tripId = "trip-501",
        busId = "bus-10",
        conductorId = "cond-9",
        companyId = "company-genesis",
        seatNumber = "12",
        destination = "Dau Terminal",
        passengerType = PassengerType.STUDENT,
        fareAmount = 128.0,
        cashTendered = 200.0,
        changeAmount = 72.0,
        status = TicketStatus.ACTIVE,
        createdAt = 1_764_144_000_000L,
        isSynced = false,
        updatedAt = 1_764_144_000_000L
    )

    @Test
    fun `buildQrPayload returns direct web verification URL for ticket id`() {
        val payload = formatter.buildQrPayload(ticket = sampleTicket)

        assertTrue(TicketPayloadFormatter.WEB_TICKET_BASE_URL.startsWith("https://"))
        assertTrue(TicketPayloadFormatter.WEB_TICKET_BASE_URL.endsWith("/t/"))
        assertEquals(
            "${TicketPayloadFormatter.WEB_TICKET_BASE_URL}f8a1bc23-9876-4321-abcd-112233445566",
            payload
        )
    }

    @Test
    fun `buildVerificationUrl trims surrounding whitespace on ticket id`() {
        val url = formatter.buildVerificationUrl("  f8a1bc23-9876-4321-abcd-112233445566  ")

        assertEquals(
            "${TicketPayloadFormatter.WEB_TICKET_BASE_URL}f8a1bc23-9876-4321-abcd-112233445566",
            url
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildVerificationUrl rejects blank ticket id`() {
        formatter.buildVerificationUrl("   ")
    }

    @Test
    fun `buildSmsReceiptText brands header with bus company name and Powered by AeTranzit footer`() {
        val utcZone = TimeZone.getTimeZone("UTC")
        val sms = formatter.buildSmsReceiptText(
            ticket = sampleTicket,
            companyName = "Genesis Transport",
            busNumber = "AE-108",
            plateNumber = "NAA 1080",
            routeLabel = "Cubao → Baguio",
            conductorName = "Maria Santos",
            timeZone = utcZone
        )

        assertTrue(sms.startsWith("Genesis Transport Digital Ticket"))
        assertTrue(sms.contains("Ref #: #F8A1BC23 (ACTIVE)"))
        assertTrue(sms.contains("Bus: AE-108 (NAA 1080)"))
        assertTrue(sms.contains("Route: Cubao → Baguio"))
        assertTrue(sms.contains("Seat: 12 -> Dau Terminal"))
        assertTrue(sms.contains("Passenger: Student (-20% Discount)"))
        assertTrue(sms.contains("Fare: ₱128.00"))
        assertTrue(sms.contains("Cash: ₱200.00 | Change: ₱72.00"))
        assertTrue(sms.contains("Conductor: Maria Santos"))
        assertTrue(sms.contains("Issued: "))
        assertTrue(sms.contains("Powered by AeTranzit"))
        assertTrue(
            sms.endsWith(
                "Verify/View Ticket: ${TicketPayloadFormatter.WEB_TICKET_BASE_URL}f8a1bc23-9876-4321-abcd-112233445566"
            )
        )
    }

    @Test
    fun `buildSmsReceiptText falls back to AeTranzit when companyName is blank and omits regular discount suffix`() {
        val regularTicket = sampleTicket.copy(
            passengerType = PassengerType.REGULAR,
            fareAmount = 160.0,
            cashTendered = 160.0,
            changeAmount = 0.0,
            status = TicketStatus.DROPPED_OFF
        )

        val sms = formatter.buildSmsReceiptText(
            ticket = regularTicket,
            companyName = "   ",
            busNumber = "AE-108",
            plateNumber = "NAA 1080",
            routeLabel = "Cubao → Baguio",
            conductorName = "Maria Santos",
            timeZone = TimeZone.getTimeZone("UTC")
        )

        assertTrue(sms.startsWith("AeTranzit Digital Ticket"))
        assertTrue(sms.contains("Ref #: #F8A1BC23 (DROPPED OFF)"))
        assertTrue(sms.contains("Passenger: Regular"))
        assertFalse(sms.contains("-20% Discount"))
        assertTrue(sms.contains("Powered by AeTranzit"))
        assertTrue(
            sms.endsWith(
                "Verify/View Ticket: ${TicketPayloadFormatter.WEB_TICKET_BASE_URL}f8a1bc23-9876-4321-abcd-112233445566"
            )
        )
    }
}
