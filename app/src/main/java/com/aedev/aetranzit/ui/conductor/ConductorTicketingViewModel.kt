package com.aedev.aetranzit.ui.conductor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.CalculateFareUseCase
import com.aedev.aetranzit.domain.usecase.FareCalculationResult
import com.aedev.aetranzit.ui.navigation.AeTranzitRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class DestinationOption(
    val name: String,
    val fare: Double,
    val isFinalDestination: Boolean
)

data class ConductorTicketingUiState(
    val tripId: String = "",
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val bus: Bus? = null,
    val route: Route? = null,
    val activeTicketsBySeat: Map<String, Ticket> = emptyMap(),
    val occupiedSeatsCount: Int = 0,
    val totalSeats: Int = 0,
    val totalTripCollection: Double = 0.0,
    val destinationOptions: List<DestinationOption> = emptyList(),
    val selectedSeatForIssue: String? = null,
    val selectedDestination: DestinationOption? = null,
    val selectedPassengerType: PassengerType = PassengerType.REGULAR,
    val isManualFareOverrideEnabled: Boolean = false,
    val manualFareInput: String = "",
    val cashTenderedInput: String = "",
    val fareCalculation: FareCalculationResult = FareCalculationResult(
        baseFare = 0.0,
        finalFare = 0.0,
        discountAmount = 0.0,
        isManualOverride = false,
        cashTendered = null,
        changeAmount = 0.0,
        shortfallAmount = 0.0,
        isInsufficientCash = false,
        canIssueTicket = false
    ),
    val isIssuingTicket: Boolean = false,
    val issueErrorMessage: String? = null,
    val inspectedTicket: Ticket? = null,
    val isUpdatingTicketStatus: Boolean = false,
    val lastIssuedTicket: Ticket? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

sealed interface ConductorTicketingUiEvent {
    data class SeatTapped(val seatNumber: String) : ConductorTicketingUiEvent
    data object DismissIssueSheet : ConductorTicketingUiEvent
    data class SelectDestination(val option: DestinationOption) : ConductorTicketingUiEvent
    data class SelectPassengerType(val passengerType: PassengerType) : ConductorTicketingUiEvent
    data class ToggleManualFareOverride(val enabled: Boolean) : ConductorTicketingUiEvent
    data class ManualFareChanged(val input: String) : ConductorTicketingUiEvent
    data class CashTenderedChanged(val input: String) : ConductorTicketingUiEvent
    data object SelectExactCash : ConductorTicketingUiEvent
    data class SelectQuickCash(val amount: Double) : ConductorTicketingUiEvent
    data object ConfirmIssueTicket : ConductorTicketingUiEvent
    data object DismissInspectedTicket : ConductorTicketingUiEvent
    data class MarkPassengerDroppedOff(val ticketId: String) : ConductorTicketingUiEvent
    data class ViewDigitalTicket(val ticket: Ticket) : ConductorTicketingUiEvent
    data object DismissIssuedConfirmation : ConductorTicketingUiEvent
    data object ClearStatusMessage : ConductorTicketingUiEvent
}

private data class TripContextBundle(
    val isLoaded: Boolean,
    val trip: Trip?,
    val bus: Bus?,
    val route: Route?
)

private data class TicketFormControlState(
    val selectedSeatForIssue: String? = null,
    val selectedDestinationName: String? = null,
    val selectedPassengerType: PassengerType = PassengerType.REGULAR,
    val isManualFareOverrideEnabled: Boolean = false,
    val manualFareInput: String = "",
    val cashTenderedInput: String = "",
    val isIssuingTicket: Boolean = false,
    val issueErrorMessage: String? = null,
    val inspectedTicketId: String? = null,
    val isUpdatingTicketStatus: Boolean = false,
    val lastIssuedTicket: Ticket? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

@HiltViewModel
class ConductorTicketingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tripRepository: TripRepository,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository,
    private val ticketRepository: TicketRepository,
    private val calculateFareUseCase: CalculateFareUseCase
) : ViewModel() {

    private val tripId: String = savedStateHandle.get<String>(AeTranzitRoutes.ARG_TRIP_ID).orEmpty()

    private val formState = MutableStateFlow(TicketFormControlState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tripContextFlow = if (tripId.isBlank()) {
        flowOf(TripContextBundle(isLoaded = true, trip = null, bus = null, route = null))
    } else {
        tripRepository.observeTripById(tripId).flatMapLatest { trip ->
            if (trip == null) {
                flowOf(TripContextBundle(isLoaded = true, trip = null, bus = null, route = null))
            } else {
                combine(
                    busRepository.observeBusById(trip.busId),
                    routeRepository.observeRouteById(trip.routeId)
                ) { bus, route ->
                    TripContextBundle(
                        isLoaded = true,
                        trip = trip,
                        bus = bus,
                        route = route
                    )
                }
            }
        }
    }

    private val allTicketsFlow = if (tripId.isBlank()) {
        flowOf(emptyList())
    } else {
        ticketRepository.observeTicketsByTrip(tripId)
    }

    private val activeTicketsFlow = if (tripId.isBlank()) {
        flowOf(emptyList())
    } else {
        ticketRepository.observeActiveTicketsByTrip(tripId)
    }

    val uiState: StateFlow<ConductorTicketingUiState> = combine(
        tripContextFlow,
        allTicketsFlow,
        activeTicketsFlow,
        formState
    ) { context, allTickets, activeTickets, form ->
        val destinationOptions = buildDestinationOptions(context.route)
        val resolvedDestination = destinationOptions.firstOrNull {
            it.name == form.selectedDestinationName
        } ?: destinationOptions.lastOrNull()

        val activeBySeat = activeTickets.associateBy { it.seatNumber }
        val totalCollection = CalculateFareUseCase.roundCurrency(
            allTickets.sumOf { it.fareAmount }
        )

        val manualOverrideValue = if (form.isManualFareOverrideEnabled) {
            form.manualFareInput.trim().toDoubleOrNull() ?: 0.0
        } else {
            null
        }
        val parsedCash = form.cashTenderedInput.trim()
            .takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()

        val calculation = calculateFareUseCase(
            baseFare = resolvedDestination?.fare ?: 0.0,
            passengerType = form.selectedPassengerType,
            manualFareOverride = manualOverrideValue,
            cashTendered = parsedCash
        )

        val inspected = form.inspectedTicketId?.let { targetId ->
            activeTickets.firstOrNull { it.id == targetId }
        }

        ConductorTicketingUiState(
            tripId = tripId,
            isLoading = !context.isLoaded,
            trip = context.trip,
            bus = context.bus,
            route = context.route,
            activeTicketsBySeat = activeBySeat,
            occupiedSeatsCount = activeBySeat.size,
            totalSeats = context.bus?.totalSeats ?: 0,
            totalTripCollection = totalCollection,
            destinationOptions = destinationOptions,
            selectedSeatForIssue = form.selectedSeatForIssue,
            selectedDestination = resolvedDestination,
            selectedPassengerType = form.selectedPassengerType,
            isManualFareOverrideEnabled = form.isManualFareOverrideEnabled,
            manualFareInput = form.manualFareInput,
            cashTenderedInput = form.cashTenderedInput,
            fareCalculation = calculation,
            isIssuingTicket = form.isIssuingTicket,
            issueErrorMessage = form.issueErrorMessage,
            inspectedTicket = inspected,
            isUpdatingTicketStatus = form.isUpdatingTicketStatus,
            lastIssuedTicket = form.lastIssuedTicket,
            statusMessage = form.statusMessage,
            isErrorMessage = form.isErrorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConductorTicketingUiState(tripId = tripId)
    )

    fun onEvent(event: ConductorTicketingUiEvent) {
        when (event) {
            is ConductorTicketingUiEvent.SeatTapped -> handleSeatTapped(event.seatNumber)
            ConductorTicketingUiEvent.DismissIssueSheet -> formState.update {
                it.copy(
                    selectedSeatForIssue = null,
                    issueErrorMessage = null
                )
            }
            is ConductorTicketingUiEvent.SelectDestination -> formState.update {
                it.copy(
                    selectedDestinationName = event.option.name,
                    isManualFareOverrideEnabled = false,
                    manualFareInput = "",
                    issueErrorMessage = null
                )
            }
            is ConductorTicketingUiEvent.SelectPassengerType -> formState.update {
                it.copy(
                    selectedPassengerType = event.passengerType,
                    issueErrorMessage = null
                )
            }
            is ConductorTicketingUiEvent.ToggleManualFareOverride -> formState.update {
                it.copy(
                    isManualFareOverrideEnabled = event.enabled,
                    manualFareInput = if (event.enabled) it.manualFareInput else "",
                    issueErrorMessage = null
                )
            }
            is ConductorTicketingUiEvent.ManualFareChanged -> {
                val sanitized = sanitizeDecimalInput(event.input)
                formState.update {
                    it.copy(
                        isManualFareOverrideEnabled = true,
                        manualFareInput = sanitized,
                        issueErrorMessage = null
                    )
                }
            }
            is ConductorTicketingUiEvent.CashTenderedChanged -> {
                val sanitized = sanitizeDecimalInput(event.input)
                formState.update {
                    it.copy(
                        cashTenderedInput = sanitized,
                        issueErrorMessage = null
                    )
                }
            }
            ConductorTicketingUiEvent.SelectExactCash -> {
                val exactFare = uiState.value.fareCalculation.finalFare
                if (exactFare > 0.0) {
                    formState.update {
                        it.copy(
                            cashTenderedInput = formatAmountInput(exactFare),
                            issueErrorMessage = null
                        )
                    }
                }
            }
            is ConductorTicketingUiEvent.SelectQuickCash -> {
                formState.update {
                    it.copy(
                        cashTenderedInput = formatAmountInput(event.amount),
                        issueErrorMessage = null
                    )
                }
            }
            ConductorTicketingUiEvent.ConfirmIssueTicket -> confirmIssueTicket()
            ConductorTicketingUiEvent.DismissInspectedTicket -> formState.update {
                it.copy(inspectedTicketId = null)
            }
            is ConductorTicketingUiEvent.MarkPassengerDroppedOff ->
                markPassengerDroppedOff(event.ticketId)
            is ConductorTicketingUiEvent.ViewDigitalTicket -> formState.update {
                it.copy(
                    inspectedTicketId = null,
                    statusMessage = "Digital Ticket #${event.ticket.referenceCode} (Seat ${event.ticket.seatNumber}) ready — QR & Share screen connects in Phase 4.",
                    isErrorMessage = false
                )
            }
            ConductorTicketingUiEvent.DismissIssuedConfirmation -> formState.update {
                it.copy(lastIssuedTicket = null)
            }
            ConductorTicketingUiEvent.ClearStatusMessage -> formState.update {
                it.copy(statusMessage = null, isErrorMessage = false)
            }
        }
    }

    private fun handleSeatTapped(seatNumber: String) {
        val snapshot = uiState.value
        val existingTicket = snapshot.activeTicketsBySeat[seatNumber]
        if (existingTicket != null) {
            formState.update {
                it.copy(
                    selectedSeatForIssue = null,
                    inspectedTicketId = existingTicket.id,
                    issueErrorMessage = null
                )
            }
        } else {
            val defaultDestination = snapshot.destinationOptions.lastOrNull()?.name
            formState.update {
                it.copy(
                    selectedSeatForIssue = seatNumber,
                    selectedDestinationName = defaultDestination,
                    selectedPassengerType = PassengerType.REGULAR,
                    isManualFareOverrideEnabled = false,
                    manualFareInput = "",
                    cashTenderedInput = "",
                    inspectedTicketId = null,
                    issueErrorMessage = null
                )
            }
        }
    }

    private fun confirmIssueTicket() {
        val snapshot = uiState.value
        if (snapshot.isIssuingTicket) return

        val seatNumber = snapshot.selectedSeatForIssue
        if (seatNumber.isNullOrBlank()) {
            formState.update {
                it.copy(issueErrorMessage = "Please select an available seat.")
            }
            return
        }

        if (snapshot.activeTicketsBySeat.containsKey(seatNumber)) {
            formState.update {
                it.copy(issueErrorMessage = "Seat $seatNumber is already occupied.")
            }
            return
        }

        val destination = snapshot.selectedDestination
        if (destination == null || destination.name.isBlank()) {
            formState.update {
                it.copy(issueErrorMessage = "Please select a destination.")
            }
            return
        }

        val calc = snapshot.fareCalculation
        if (calc.finalFare <= 0.0) {
            formState.update {
                it.copy(issueErrorMessage = "Enter a valid fare greater than ₱0.00.")
            }
            return
        }

        val cashTendered = calc.cashTendered
        if (cashTendered == null) {
            formState.update {
                it.copy(issueErrorMessage = "Enter cash tendered or tap Exact / quick cash.")
            }
            return
        }

        if (calc.isInsufficientCash) {
            formState.update {
                it.copy(issueErrorMessage = "Cash tendered is insufficient for this fare.")
            }
            return
        }

        viewModelScope.launch {
            formState.update { it.copy(isIssuingTicket = true, issueErrorMessage = null) }
            ticketRepository.issueTicket(
                tripId = snapshot.tripId,
                seatNumber = seatNumber,
                destination = destination.name,
                passengerType = snapshot.selectedPassengerType,
                fareAmount = calc.finalFare,
                cashTendered = cashTendered
            ).onSuccess { issued ->
                formState.update {
                    it.copy(
                        isIssuingTicket = false,
                        selectedSeatForIssue = null,
                        cashTenderedInput = "",
                        manualFareInput = "",
                        isManualFareOverrideEnabled = false,
                        issueErrorMessage = null,
                        lastIssuedTicket = issued
                    )
                }
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isIssuingTicket = false,
                        issueErrorMessage = error.message ?: "Unable to issue ticket."
                    )
                }
            }
        }
    }

    private fun markPassengerDroppedOff(ticketId: String) {
        if (formState.value.isUpdatingTicketStatus) return
        viewModelScope.launch {
            formState.update { it.copy(isUpdatingTicketStatus = true) }
            ticketRepository.markPassengerDroppedOff(ticketId)
                .onSuccess { updated ->
                    formState.update { current ->
                        current.copy(
                            isUpdatingTicketStatus = false,
                            inspectedTicketId = null,
                            lastIssuedTicket = current.lastIssuedTicket
                                ?.takeUnless { it.id == updated.id },
                            statusMessage = "Seat ${updated.seatNumber} freed — passenger dropped off at ${updated.destination}.",
                            isErrorMessage = false
                        )
                    }
                }
                .onFailure { error ->
                    formState.update {
                        it.copy(
                            isUpdatingTicketStatus = false,
                            statusMessage = error.message ?: "Unable to update seat status.",
                            isErrorMessage = true
                        )
                    }
                }
        }
    }

    private fun buildDestinationOptions(route: Route?): List<DestinationOption> {
        if (route == null) return emptyList()
        val stops = route.stops.map { stop ->
            DestinationOption(
                name = stop.name,
                fare = stop.fare,
                isFinalDestination = false
            )
        }
        val finalStop = DestinationOption(
            name = route.destination,
            fare = route.baseFare,
            isFinalDestination = true
        )
        return stops + finalStop
    }

    private fun sanitizeDecimalInput(raw: String): String {
        val filtered = buildString {
            var dotSeen = false
            var decimals = 0
            for (ch in raw) {
                if (ch.isDigit()) {
                    if (!dotSeen || decimals < 2) {
                        append(ch)
                        if (dotSeen) decimals++
                    }
                } else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    append(ch)
                }
            }
        }
        return filtered
    }

    private fun formatAmountInput(amount: Double): String {
        val asLong = amount.toLong()
        return if (amount == asLong.toDouble()) {
            asLong.toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}
