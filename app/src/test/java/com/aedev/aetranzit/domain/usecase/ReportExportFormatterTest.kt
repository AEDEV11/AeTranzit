package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ReportExportFormatterTest {

    private val formatter = ReportExportFormatter()
    private val utcTz: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `buildCsvContent outputs exact header and expected row count`() {
        val tickets = listOf(
            Ticket(
                id = "a1b2c3d4-0000-1111-2222-333344445555",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = "cond-1",
                seatNumber = "14",
                destination = "Dau Terminal",
                passengerType = PassengerType.STUDENT,
                fareAmount = 156.0,
                cashTendered = 200.0,
                changeAmount = 44.0,
                status = TicketStatus.ACTIVE,
                createdAt = 1_764_144_000_000L,
                isSynced = true,
                updatedAt = 1_764_144_000_000L
            ),
            Ticket(
                id = "f9e8d7c6-0000-1111-2222-333344445555",
                tripId = "trip-1",
                busId = "bus-1",
                conductorId = "cond-1",
                seatNumber = "15",
                destination = "Baguio",
                passengerType = PassengerType.REGULAR,
                fareAmount = 450.0,
                cashTendered = 500.0,
                changeAmount = 50.0,
                status = TicketStatus.DROPPED_OFF,
                createdAt = 1_764_147_600_000L,
                isSynced = true,
                updatedAt = 1_764_147_600_000L
            )
        )

        val csv = formatter.buildCsvContent(
            tickets = tickets,
            trips = listOf(
                Trip(
                    id = "trip-1",
                    busId = "bus-1",
                    routeId = "route-1",
                    conductorId = "cond-1",
                    status = TripStatus.ACTIVE,
                    startedAt = 1_764_140_000_000L,
                    endedAt = null,
                    isSynced = true,
                    updatedAt = 1_764_140_000_000L
                )
            ),
            buses = listOf(
                Bus(
                    id = "bus-1",
                    busNumber = "AE-101",
                    plateNumber = "NAA-1010",
                    totalSeats = 45,
                    columns = 4,
                    isSynced = true,
                    updatedAt = 1L
                )
            ),
            routes = listOf(
                Route(
                    id = "route-1",
                    origin = "Cubao",
                    destination = "Baguio",
                    baseFare = 450.0,
                    stops = emptyList(),
                    isSynced = true,
                    updatedAt = 1L
                )
            ),
            users = listOf(
                User(
                    id = "cond-1",
                    name = "Ramon Magsaysay",
                    email = "ramon@aetranzit.ph",
                    role = UserRole.CONDUCTOR,
                    isActive = true,
                    isSynced = true,
                    updatedAt = 1L
                )
            ),
            timeZone = utcTz
        )

        val lines = csv.split("\r\n")
        assertEquals(3, lines.size)
        assertEquals(
            "Date/Time,Ticket Ref #,Bus #,Route,Seat #,Destination,Passenger Type,Fare,Cash,Change,Status,Conductor Name",
            lines[0]
        )
        assertTrue(lines[1].contains("#F9E8D7C6"))
        assertTrue(lines[1].contains("450.00,500.00,50.00"))
        assertTrue(lines[2].contains("#A1B2C3D4"))
        assertTrue(lines[2].contains("156.00,200.00,44.00"))
    }

    @Test
    fun `buildCsvContent escapes commas quotes and newlines per RFC-4180`() {
        val row = ReportTicketRow(
            ticketId = "t-special",
            referenceCode = "ABCD1234",
            formattedDateTime = "2026-11-26 08:30",
            busNumber = "AE-202",
            routeLabel = "Cubao, QC → Baguio",
            seatNumber = "4A",
            destination = "SM \"North\", Bay 2",
            passengerTypeLabel = "Senior Citizen",
            isDiscounted = true,
            fareAmount = 120.5,
            cashTendered = 200.0,
            changeAmount = 79.5,
            statusLabel = "On Board",
            conductorName = "Dela Cruz, Jose \"Pepe\"",
            createdAt = 1_000L
        )

        val csv = formatter.buildCsvContent(listOf(row))
        val lines = csv.split("\r\n")

        assertEquals(2, lines.size)
        val dataRow = lines[1]
        assertTrue(dataRow.contains("\"Cubao, QC → Baguio\""))
        assertTrue(dataRow.contains("\"SM \"\"North\"\", Bay 2\""))
        assertTrue(dataRow.contains("\"Dela Cruz, Jose \"\"Pepe\"\"\""))
    }

    @Test
    fun `escapeCsvField handles embedded line breaks`() {
        val escaped = formatter.escapeCsvField("Gate 1\nBay B")
        assertEquals("\"Gate 1\nBay B\"", escaped)
    }
}
