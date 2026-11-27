package com.aedev.aetranzit.ui.conductor.ticket

import androidx.lifecycle.SavedStateHandle
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.TicketPayloadFormatter
import com.aedev.aetranzit.ui.navigation.AeTranzitRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DigitalTicketViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var ticketRepository: FakeDigitalTicketRepo
    private lateinit var tripRepository: FakeDigitalTripRepo
    private lateinit var busRepository: FakeDigitalBusRepo
    private lateinit var routeRepository: FakeDigitalRouteRepo
    private lateinit var companyRepository: FakeDigitalCompanyRepo
    private lateinit var authRepository: FakeDigitalAuthRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ticketRepository = FakeDigitalTicketRepo()
        tripRepository = FakeDigitalTripRepo()
        busRepository = FakeDigitalBusRepo()
        routeRepository = FakeDigitalRouteRepo()
        companyRepository = FakeDigitalCompanyRepo()
        authRepository = FakeDigitalAuthRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads company branded ticket metadata and reacts to status updates`() = runTest {
        val viewModel = createViewModel("d49a12ef-1111-2222-3333-444455556666")
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.ticket)
        assertEquals("Victory Liner", state.companyName)
        assertEquals("D49A12EF", state.ticket?.referenceCode)
        assertEquals("AE-305", state.busNumber)
        assertEquals("NAA 3050", state.plateNumber)
        assertEquals("Cubao → Baguio", state.routeLabel)
        assertEquals("Ramon Magsaysay", state.conductorName)
        assertEquals(
            "${TicketPayloadFormatter.WEB_TICKET_BASE_URL}d49a12ef-1111-2222-3333-444455556666",
            state.qrPayload
        )
        assertTrue(state.smsReceiptText.startsWith("Victory Liner Digital Ticket"))
        assertTrue(state.smsReceiptText.contains("Ref #: #D49A12EF (ACTIVE)"))
        assertTrue(state.smsReceiptText.contains("Student (-20% Discount)"))
        assertTrue(state.smsReceiptText.contains("Powered by AeTranzit"))
        assertTrue(
            state.smsReceiptText.endsWith(
                "Verify/View Ticket: ${TicketPayloadFormatter.WEB_TICKET_BASE_URL}d49a12ef-1111-2222-3333-444455556666"
            )
        )

        val renderData = state.toRenderData()
        assertNotNull(renderData)
        assertEquals("Victory Liner", renderData?.companyName)
        assertEquals("₱128.00", renderData?.fareFormatted)
        assertEquals("₱200.00", renderData?.cashFormatted)
        assertEquals("₱72.00", renderData?.changeFormatted)
        assertFalse(renderData?.isDroppedOff ?: true)

        // Live reaction when passenger is marked dropped off
        ticketRepository.markPassengerDroppedOff("d49a12ef-1111-2222-3333-444455556666")
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(TicketStatus.DROPPED_OFF, updatedState.ticket?.status)
        assertTrue(updatedState.toRenderData()?.isDroppedOff == true)
        assertTrue(updatedState.smsReceiptText.contains("(DROPPED OFF)"))

        collector.cancel()
    }

    @Test
    fun `falls back to AeTranzit brand name when ticket company record is missing`() = runTest {
        val viewModel = createViewModel("legacy-ticket-1111-2222-3333-444455556666")
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("AeTranzit", state.companyName)
        assertEquals("AeTranzit", state.toRenderData()?.companyName)
        assertTrue(state.smsReceiptText.startsWith("AeTranzit Digital Ticket"))

        collector.cancel()
    }

    @Test
    fun `handles missing ticketId cleanly with actionable error`() = runTest {
        val viewModel = createViewModel("")
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.ticket)
        assertEquals("Missing ticket ID.", state.errorMessage)
        assertNull(state.toRenderData())

        collector.cancel()
    }

    @Test
    fun `handles unknown ticketId cleanly when ticket does not exist`() = runTest {
        val viewModel = createViewModel("non-existent-ticket-id")
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.ticket)
        assertEquals("Ticket not found.", state.errorMessage)
        assertNull(state.toRenderData())

        collector.cancel()
    }

    @Test
    fun `tracks share image loading and error state transitions`() = runTest {
        val viewModel = createViewModel("d49a12ef-1111-2222-3333-444455556666")
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onEvent(DigitalTicketUiEvent.ShareImageStarted)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSharingImage)

        viewModel.onEvent(DigitalTicketUiEvent.ShareImageCompleted("Failed to write PNG"))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSharingImage)
        assertEquals("Failed to write PNG", viewModel.uiState.value.errorMessage)

        viewModel.onEvent(DigitalTicketUiEvent.ClearError)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)

        collector.cancel()
    }

    private fun createViewModel(ticketId: String): DigitalTicketViewModel {
        val handle = if (ticketId.isEmpty()) {
            SavedStateHandle()
        } else {
            SavedStateHandle(mapOf(AeTranzitRoutes.ARG_TICKET_ID to ticketId))
        }
        return DigitalTicketViewModel(
            savedStateHandle = handle,
            ticketRepository = ticketRepository,
            tripRepository = tripRepository,
            busRepository = busRepository,
            routeRepository = routeRepository,
            companyRepository = companyRepository,
            authRepository = authRepository,
            payloadFormatter = TicketPayloadFormatter()
        )
    }

    private class FakeDigitalCompanyRepo : CompanyRepository {
        private val company = Company(
            id = "company-900",
            name = "Victory Liner",
            joinCode = "VIC-4821",
            createdAt = 1_764_140_000_000L,
            isSynced = true,
            updatedAt = 1_764_140_000_000L
        )

        override fun observeActiveCompany(): Flow<Company?> = MutableStateFlow(company)
        override fun observeCompanyById(companyId: String): Flow<Company?> =
            MutableStateFlow(company.takeIf { it.id == companyId })
        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun getActiveCompany(): Company = company
        override suspend fun getCompanyById(companyId: String): Company? =
            company.takeIf { it.id == companyId }
        override suspend fun findByJoinCode(joinCode: String): Result<Company?> =
            Result.success(company.takeIf { it.joinCode == joinCode })
        override suspend fun syncPendingCompanies(): Result<Int> = Result.success(0)
    }

    private class FakeDigitalTicketRepo : TicketRepository {
        private val tickets = MutableStateFlow(
            listOf(
                Ticket(
                    id = "d49a12ef-1111-2222-3333-444455556666",
                    tripId = "trip-900",
                    busId = "bus-900",
                    conductorId = "cond-900",
                    companyId = "company-900",
                    seatNumber = "8",
                    destination = "Tarlac Junction",
                    passengerType = PassengerType.STUDENT,
                    fareAmount = 128.0,
                    cashTendered = 200.0,
                    changeAmount = 72.0,
                    status = TicketStatus.ACTIVE,
                    createdAt = 1_764_145_000_000L,
                    isSynced = false,
                    updatedAt = 1_764_145_000_000L
                ),
                Ticket(
                    id = "legacy-ticket-1111-2222-3333-444455556666",
                    tripId = "trip-900",
                    busId = "bus-900",
                    conductorId = "cond-900",
                    companyId = "missing-company-id",
                    seatNumber = "9",
                    destination = "Baguio",
                    passengerType = PassengerType.REGULAR,
                    fareAmount = 450.0,
                    cashTendered = 500.0,
                    changeAmount = 50.0,
                    status = TicketStatus.ACTIVE,
                    createdAt = 1_764_145_000_000L,
                    isSynced = false,
                    updatedAt = 1_764_145_000_000L
                )
            )
        )

        override fun observeAllTickets(): Flow<List<Ticket>> = tickets

        override fun observeTicketById(ticketId: String): Flow<Ticket?> =
            tickets.map { list -> list.firstOrNull { it.id == ticketId } }

        override fun observeTicketsByTrip(tripId: String): Flow<List<Ticket>> =
            tickets.map { list -> list.filter { it.tripId == tripId } }

        override fun observeActiveTicketsByTrip(tripId: String): Flow<List<Ticket>> =
            tickets.map { list ->
                list.filter { it.tripId == tripId && it.status == TicketStatus.ACTIVE }
            }

        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)

        override suspend fun issueTicket(
            tripId: String,
            seatNumber: String,
            destination: String,
            passengerType: PassengerType,
            fareAmount: Double,
            cashTendered: Double
        ): Result<Ticket> = Result.failure(UnsupportedOperationException())

        override suspend fun markPassengerDroppedOff(ticketId: String): Result<Ticket> {
            val current = tickets.value.firstOrNull { it.id == ticketId }
                ?: return Result.failure(IllegalArgumentException("Not found"))
            val updated = current.copy(status = TicketStatus.DROPPED_OFF)
            tickets.value = tickets.value.map { if (it.id == ticketId) updated else it }
            return Result.success(updated)
        }

        override suspend fun syncPendingTickets(): Result<Int> = Result.success(0)
    }

    private class FakeDigitalTripRepo : TripRepository {
        private val trip = Trip(
            id = "trip-900",
            busId = "bus-900",
            routeId = "route-900",
            conductorId = "cond-900",
            companyId = "company-900",
            status = TripStatus.ACTIVE,
            startedAt = 1_764_140_000_000L,
            endedAt = null,
            isSynced = true,
            updatedAt = 1_764_140_000_000L
        )

        override fun observeAllTrips(): Flow<List<Trip>> = MutableStateFlow(listOf(trip))

        override fun observeTripById(tripId: String): Flow<Trip?> =
            MutableStateFlow(trip.takeIf { it.id == tripId })

        override fun observeActiveTrip(conductorId: String): Flow<Trip?> = MutableStateFlow(trip)
        override fun observeTripsByConductor(conductorId: String): Flow<List<Trip>> =
            MutableStateFlow(listOf(trip))
        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun startTrip(conductorId: String, busId: String, routeId: String) =
            Result.success(trip)
        override suspend fun endTrip(tripId: String) = Result.success(trip)
        override suspend fun syncPendingTrips(): Result<Int> = Result.success(0)
    }

    private class FakeDigitalBusRepo : BusRepository {
        private val bus = Bus(
            id = "bus-900",
            busNumber = "AE-305",
            plateNumber = "NAA 3050",
            totalSeats = 45,
            columns = 5,
            companyId = "company-900",
            isSynced = true,
            updatedAt = 1_764_140_000_000L
        )

        override fun observeAllBuses(): Flow<List<Bus>> = MutableStateFlow(listOf(bus))
        override fun observeBusById(id: String): Flow<Bus?> =
            MutableStateFlow(bus.takeIf { it.id == id })
        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun saveBus(
            id: String?,
            busNumber: String,
            plateNumber: String,
            totalSeats: Int,
            columns: Int
        ) = Result.success(bus)
        override suspend fun deleteBus(id: String) = Result.success(Unit)
        override suspend fun syncPendingBuses(): Result<Int> = Result.success(0)
    }

    private class FakeDigitalRouteRepo : RouteRepository {
        private val route = Route(
            id = "route-900",
            origin = "Cubao",
            destination = "Baguio",
            baseFare = 450.0,
            stops = listOf(RouteStop(name = "Tarlac Junction", fare = 160.0)),
            companyId = "company-900",
            isSynced = true,
            updatedAt = 1_764_140_000_000L
        )

        override fun observeAllRoutes(): Flow<List<Route>> = MutableStateFlow(listOf(route))
        override fun observeRouteById(id: String): Flow<Route?> =
            MutableStateFlow(route.takeIf { it.id == id })
        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun saveRoute(
            id: String?,
            origin: String,
            destination: String,
            baseFare: Double,
            stops: List<RouteStop>
        ) = Result.success(route)
        override suspend fun deleteRoute(id: String) = Result.success(Unit)
        override suspend fun syncPendingRoutes(): Result<Int> = Result.success(0)
    }

    private class FakeDigitalAuthRepo : AuthRepository {
        private val conductor = User(
            id = "cond-900",
            name = "Ramon Magsaysay",
            email = "ramon@aetranzit.ph",
            role = UserRole.CONDUCTOR,
            companyId = "company-900",
            isActive = true,
            isSynced = true,
            updatedAt = 1_764_140_000_000L
        )

        override fun observeCurrentUser(): Flow<User?> = MutableStateFlow(conductor)
        override fun observeAllUsers(): Flow<List<User>> = MutableStateFlow(listOf(conductor))
        override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun getCurrentUser(): User = conductor
        override suspend fun login(email: String, password: String) = Result.success(conductor)
        override suspend fun register(
            name: String,
            email: String,
            password: String
        ) = Result.success(conductor)
        override suspend fun completeCompanySetup(companyName: String) = Result.success(conductor)
        override suspend fun createStaffAccount(
            name: String,
            email: String,
            temporaryPassword: String,
            role: UserRole
        ) = Result.success(conductor)
        override suspend fun completeForcedPasswordChange(newPassword: String) =
            Result.success(conductor)
        override suspend fun setUserActiveStatus(userId: String, isActive: Boolean) =
            Result.success(Unit)
        override suspend fun syncPendingUsers(): Result<Int> = Result.success(0)
        override suspend fun logout() = Unit
    }
}
