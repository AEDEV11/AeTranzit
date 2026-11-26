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
    fun `buildQrPayload produces deterministic pipe-delimited v1 verification string`() {
        val payload = formatter.buildQrPayload(
            ticket = sampleTicket,
            busNumber = "AE-108",
            routeLabel = "Cubao → Baguio"
        )

        assertEquals(
            "AETRANZIT|v1|F8A1BC23|trip-501|B:AE-108|R:Cubao → Baguio|S12|Dau Terminal|STUDENT|128.00|1764144000000",
            payload
        )
    }

    @Test
    fun `buildQrPayload sanitizes pipe delimiters and falls back when metadata blank`() {
        val ticketWithPipe = sampleTicket.copy(destination = "San Fernando | North\nExit")
        val payload = formatter.buildQrPayload(
            ticket = ticketWithPipe,
            busNumber = "  ",
            routeLabel = ""
        )

        val parts = payload.split("|")
        assertEquals(11, parts.size)
        assertEquals("AETRANZIT", parts[0])
        assertEquals("v1", parts[1])
        assertEquals("B:NA", parts[4])
        assertEquals("R:NA", parts[5])
        assertEquals("San Fernando - North Exit", parts[7])
    }

    @Test
    fun `buildSmsReceiptText includes reference bus route seat discount fare cash change and timestamp`() {
        val utcZone = TimeZone.getTimeZone("UTC")
        val sms = formatter.buildSmsReceiptText(
            ticket = sampleTicket,
            busNumber = "AE-108",
            plateNumber = "NAA 1080",
            routeLabel = "Cubao → Baguio",
            conductorName = "Maria Santos",
            timeZone = utcZone
        )

        assertTrue(sms.contains("AeTranzit Digital Ticket"))
        assertTrue(sms.contains("Ref #: #F8A1BC23 (ACTIVE)"))
        assertTrue(sms.contains("Bus: AE-108 (NAA 1080)"))
        assertTrue(sms.contains("Route: Cubao → Baguio"))
        assertTrue(sms.contains("Seat: 12 -> Dau Terminal"))
        assertTrue(sms.contains("Passenger: Student (-20% Discount)"))
        assertTrue(sms.contains("Fare: ₱128.00"))
        assertTrue(sms.contains("Cash: ₱200.00 | Change: ₱72.00"))
        assertTrue(sms.contains("Conductor: Maria Santos"))
        assertTrue(sms.contains("Issued: "))
    }

    @Test
    fun `buildSmsReceiptText omits discount suffix for regular passenger`() {
        val regularTicket = sampleTicket.copy(
            passengerType = PassengerType.REGULAR,
            fareAmount = 160.0,
            cashTendered = 160.0,
            changeAmount = 0.0,
            status = TicketStatus.DROPPED_OFF
        )

        val sms = formatter.buildSmsReceiptText(
            ticket = regularTicket,
            busNumber = "AE-108",
            plateNumber = "NAA 1080",
            routeLabel = "Cubao → Baguio",
            conductorName = "Maria Santos",
            timeZone = TimeZone.getTimeZone("UTC")
        )

        assertTrue(sms.contains("Ref #: #F8A1BC23 (DROPPED OFF)"))
        assertTrue(sms.contains("Passenger: Regular"))
        assertFalse(sms.contains("-20% Discount"))
    }
}
