package com.aedev.aetranzit.ui.conductor

import androidx.lifecycle.SavedStateHandle
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.CalculateFareUseCase
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
class ConductorTicketingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTripRepository: FakeTripRepo
    private lateinit var fakeBusRepository: FakeBusRepo
    private lateinit var fakeRouteRepository: FakeRouteRepo
    private lateinit var fakeTicketRepository: FakeTicketRepo
    private lateinit var viewModel: ConductorTicketingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTripRepository = FakeTripRepo()
        fakeBusRepository = FakeBusRepo()
        fakeRouteRepository = FakeRouteRepo()
        fakeTicketRepository = FakeTicketRepo()

        viewModel = ConductorTicketingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AeTranzitRoutes.ARG_TRIP_ID to "trip-101")),
            tripRepository = fakeTripRepository,
            busRepository = fakeBusRepository,
            routeRepository = fakeRouteRepository,
            ticketRepository = fakeTicketRepository,
            calculateFareUseCase = CalculateFareUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `destination selection auto-prices stop fare and passenger type toggles 20 percent discount`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onEvent(ConductorTicketingUiEvent.SeatTapped("7"))
        advanceUntilIdle()

        // Defaults to final destination ("Dau" @ ₱195.00) and REGULAR
        var state = viewModel.uiState.value
        assertEquals("7", state.selectedSeatForIssue)
        assertEquals("Dau", state.selectedDestination?.name)
        assertEquals(195.0, state.fareCalculation.finalFare, 0.001)

        // Select intermediate stop ("San Fernando" @ ₱140.00)
        val sanFernando = state.destinationOptions.first { it.name == "San Fernando" }
        viewModel.onEvent(ConductorTicketingUiEvent.SelectDestination(sanFernando))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("San Fernando", state.selectedDestination?.name)
        assertEquals(140.0, state.fareCalculation.finalFare, 0.001)

        // Toggle Student (-20%) -> 140 * 0.80 = ₱112.00
        viewModel.onEvent(ConductorTicketingUiEvent.SelectPassengerType(PassengerType.STUDENT))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(112.0, state.fareCalculation.finalFare, 0.001)
        assertEquals(28.0, state.fareCalculation.discountAmount, 0.001)
    }

    @Test
    fun `cash and change updates validate shortfall and issue ticket updates occupancy and collection`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onEvent(ConductorTicketingUiEvent.SeatTapped("3"))
        viewModel.onEvent(ConductorTicketingUiEvent.SelectPassengerType(PassengerType.SENIOR))
        // Dau base 195 * 0.80 = 156.00
        viewModel.onEvent(ConductorTicketingUiEvent.SelectQuickCash(100.0))
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.fareCalculation.isInsufficientCash)
        assertEquals(56.0, state.fareCalculation.shortfallAmount, 0.001)
        assertFalse(state.fareCalculation.canIssueTicket)

        // Select ₱200 bill -> change should be ₱44.00
        viewModel.onEvent(ConductorTicketingUiEvent.SelectQuickCash(200.0))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.fareCalculation.isInsufficientCash)
        assertEquals(44.0, state.fareCalculation.changeAmount, 0.001)
        assertTrue(state.fareCalculation.canIssueTicket)

        // Issue Digital Ticket
        viewModel.onEvent(ConductorTicketingUiEvent.ConfirmIssueTicket)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.selectedSeatForIssue)
        assertNotNull(state.lastIssuedTicket)
        assertEquals(1, state.occupiedSeatsCount)
        assertEquals(156.0, state.totalTripCollection, 0.001)
        assertTrue(state.activeTicketsBySeat.containsKey("3"))

        // Tap occupied seat 3 -> opens details sheet -> mark dropped off frees seat while keeping collection
        viewModel.onEvent(ConductorTicketingUiEvent.SeatTapped("3"))
        advanceUntilIdle()
        val inspected = viewModel.uiState.value.inspectedTicket
        assertNotNull(inspected)

        viewModel.onEvent(ConductorTicketingUiEvent.MarkPassengerDroppedOff(inspected!!.id))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(0, state.occupiedSeatsCount)
        assertFalse(state.activeTicketsBySeat.containsKey("3"))
        assertEquals(156.0, state.totalTripCollection, 0.001)
    }
}

private class FakeTripRepo : TripRepository {
    private val trip = MutableStateFlow<Trip?>(
        Trip(
            id = "trip-101",
            busId = "bus-101",
            routeId = "route-101",
            conductorId = "cond-1",
            status = TripStatus.ACTIVE,
            startedAt = 1_000L,
            endedAt = null,
            isSynced = false,
            updatedAt = 1_000L
        )
    )

    override fun observeTripById(tripId: String): Flow<Trip?> =
        trip.map { it?.takeIf { t -> t.id == tripId } }

    override fun observeActiveTrip(conductorId: String): Flow<Trip?> = trip

    override fun observeTripsByConductor(conductorId: String): Flow<List<Trip>> =
        trip.map { listOfNotNull(it) }

    override fun observeUnsyncedCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun startTrip(conductorId: String, busId: String, routeId: String): Result<Trip> =
        Result.success(trip.value!!)

    override suspend fun endTrip(tripId: String): Result<Trip> =
        Result.success(trip.value!!)

    override suspend fun syncPendingTrips(): Result<Int> = Result.success(0)
}

private class FakeBusRepo : BusRepository {
    private val bus = Bus(
        id = "bus-101",
        busNumber = "101",
        plateNumber = "NCA-4821",
        totalSeats = 45,
        columns = 4,
        isSynced = true,
        updatedAt = 1L
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
    ): Result<Bus> = Result.success(bus)
    override suspend fun deleteBus(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun syncPendingBuses(): Result<Int> = Result.success(0)
}

private class FakeRouteRepo : RouteRepository {
    private val route = Route(
        id = "route-101",
        origin = "Cubao",
        destination = "Dau",
        baseFare = 195.0,
        stops = listOf(
            RouteStop("Bocaue", 65.0),
            RouteStop("San Fernando", 140.0)
        ),
        isSynced = true,
        updatedAt = 1L
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
    ): Result<Route> = Result.success(route)
    override suspend fun deleteRoute(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun syncPendingRoutes(): Result<Int> = Result.success(0)
}

private class FakeTicketRepo : TicketRepository {
    private val tickets = MutableStateFlow<List<Ticket>>(emptyList())

    override fun observeTicketById(ticketId: String): Flow<Ticket?> =
        tickets.map { list -> list.firstOrNull { it.id == ticketId } }

    override fun observeTicketsByTrip(tripId: String): Flow<List<Ticket>> =
        tickets.map { list -> list.filter { it.tripId == tripId } }

    override fun observeActiveTicketsByTrip(tripId: String): Flow<List<Ticket>> =
        tickets.map { list ->
            list.filter { it.tripId == tripId && it.status == TicketStatus.ACTIVE }
        }

    override fun observeUnsyncedCount(): Flow<Int> =
        tickets.map { list -> list.count { !it.isSynced } }

    override suspend fun issueTicket(
        tripId: String,
        seatNumber: String,
        destination: String,
        passengerType: PassengerType,
        fareAmount: Double,
        cashTendered: Double
    ): Result<Ticket> {
        val duplicate = tickets.value.any {
            it.tripId == tripId && it.seatNumber == seatNumber && it.status == TicketStatus.ACTIVE
        }
        if (duplicate) {
            return Result.failure(IllegalStateException("Seat already occupied"))
        }
        val created = Ticket(
            id = "tkt-${tickets.value.size + 1}0000000",
            tripId = tripId,
            busId = "bus-101",
            conductorId = "cond-1",
            seatNumber = seatNumber,
            destination = destination,
            passengerType = passengerType,
            fareAmount = fareAmount,
            cashTendered = cashTendered,
            changeAmount = cashTendered - fareAmount,
            status = TicketStatus.ACTIVE,
            createdAt = 2_000L,
            isSynced = false,
            updatedAt = 2_000L
        )
        tickets.value = tickets.value + created
        return Result.success(created)
    }

    override suspend fun markPassengerDroppedOff(ticketId: String): Result<Ticket> {
        val target = tickets.value.firstOrNull { it.id == ticketId }
            ?: return Result.failure(IllegalArgumentException("Not found"))
        val updated = target.copy(status = TicketStatus.DROPPED_OFF)
        tickets.value = tickets.value.map { if (it.id == ticketId) updated else it }
        return Result.success(updated)
    }

    override suspend fun syncPendingTickets(): Result<Int> = Result.success(0)
}
