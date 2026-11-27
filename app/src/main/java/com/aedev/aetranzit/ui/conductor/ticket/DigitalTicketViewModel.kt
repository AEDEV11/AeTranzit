package com.aedev.aetranzit.ui.conductor.ticket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.CompanyRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.TicketPayloadFormatter
import com.aedev.aetranzit.ui.components.formatPhp
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
import javax.inject.Inject

data class DigitalTicketUiState(
    val ticketId: String = "",
    val isLoading: Boolean = true,
    val companyName: String = Company.FALLBACK_BRAND_NAME,
    val ticket: Ticket? = null,
    val trip: Trip? = null,
    val bus: Bus? = null,
    val route: Route? = null,
    val busNumber: String = "N/A",
    val plateNumber: String = "N/A",
    val routeLabel: String = "N/A",
    val conductorName: String = "Conductor",
    val issuedTimestamp: String = "",
    val qrPayload: String = "",
    val smsReceiptText: String = "",
    val isSharingImage: Boolean = false,
    val errorMessage: String? = null
) {
    fun toRenderData(): DigitalTicketRenderData? {
        val currentTicket = ticket ?: return null
        if (qrPayload.isBlank()) return null
        return DigitalTicketRenderData(
            companyName = companyName,
            referenceCode = currentTicket.referenceCode,
            statusLabel = currentTicket.status.displayName,
            isDroppedOff = currentTicket.status == TicketStatus.DROPPED_OFF,
            seatNumber = currentTicket.seatNumber,
            routeLabel = routeLabel,
            destination = currentTicket.destination,
            busNumber = busNumber,
            plateNumber = plateNumber,
            passengerTypeLabel = currentTicket.passengerType.displayName,
            isDiscounted = currentTicket.passengerType.isDiscounted,
            fareFormatted = formatPhp(currentTicket.fareAmount),
            cashFormatted = formatPhp(currentTicket.cashTendered),
            changeFormatted = formatPhp(currentTicket.changeAmount),
            conductorName = conductorName,
            issuedTimestamp = issuedTimestamp,
            qrPayload = qrPayload
        )
    }
}

sealed interface DigitalTicketUiEvent {
    data object ShareImageRequested : DigitalTicketUiEvent
    data object ShareImageStarted : DigitalTicketUiEvent
    data class ShareImageCompleted(val errorMessage: String? = null) : DigitalTicketUiEvent
    data object SendSmsRequested : DigitalTicketUiEvent
    data class ActionFailed(val errorMessage: String) : DigitalTicketUiEvent
    data object ClearError : DigitalTicketUiEvent
}

private data class TicketMetadataBundle(
    val isLoaded: Boolean,
    val company: Company?,
    val ticket: Ticket?,
    val trip: Trip?,
    val bus: Bus?,
    val route: Route?
)

private data class ShareActionState(
    val isSharingImage: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DigitalTicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    ticketRepository: TicketRepository,
    private val tripRepository: TripRepository,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository,
    private val companyRepository: CompanyRepository,
    authRepository: AuthRepository,
    private val payloadFormatter: TicketPayloadFormatter
) : ViewModel() {

    private val ticketId: String =
        savedStateHandle.get<String>(AeTranzitRoutes.ARG_TICKET_ID)?.trim().orEmpty()

    private val actionState = MutableStateFlow(ShareActionState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ticketMetadataFlow = if (ticketId.isBlank()) {
        flowOf(
            TicketMetadataBundle(
                isLoaded = true,
                company = null,
                ticket = null,
                trip = null,
                bus = null,
                route = null
            )
        )
    } else {
        ticketRepository.observeTicketById(ticketId).flatMapLatest { ticket ->
            if (ticket == null) {
                flowOf(
                    TicketMetadataBundle(
                        isLoaded = true,
                        company = null,
                        ticket = null,
                        trip = null,
                        bus = null,
                        route = null
                    )
                )
            } else {
                val companyFlow = if (ticket.companyId.isBlank()) {
                    companyRepository.observeActiveCompany()
                } else {
                    companyRepository.observeCompanyById(ticket.companyId)
                }

                tripRepository.observeTripById(ticket.tripId).flatMapLatest { trip ->
                    val resolvedBusId = trip?.busId?.ifBlank { ticket.busId } ?: ticket.busId
                    val busFlow = if (resolvedBusId.isBlank()) {
                        flowOf(null)
                    } else {
                        busRepository.observeBusById(resolvedBusId)
                    }
                    val routeFlow = if (trip == null || trip.routeId.isBlank()) {
                        flowOf(null)
                    } else {
                        routeRepository.observeRouteById(trip.routeId)
                    }
                    combine(companyFlow, busFlow, routeFlow) { company, bus, route ->
                        TicketMetadataBundle(
                            isLoaded = true,
                            company = company,
                            ticket = ticket,
                            trip = trip,
                            bus = bus,
                            route = route
                        )
                    }
                }
            }
        }
    }

    val uiState: StateFlow<DigitalTicketUiState> = combine(
        ticketMetadataFlow,
        authRepository.observeCurrentUser(),
        actionState
    ) { bundle, currentUser, action ->
        val ticket = bundle.ticket
        val companyName = bundle.company?.name?.trim()?.ifBlank { Company.FALLBACK_BRAND_NAME }
            ?: Company.FALLBACK_BRAND_NAME
        val busNumber = bundle.bus?.busNumber?.trim()?.ifBlank { "N/A" } ?: "N/A"
        val plateNumber = bundle.bus?.plateNumber?.trim()?.ifBlank { "N/A" } ?: "N/A"
        val routeLabel = bundle.route?.displayName?.trim()?.ifBlank { "N/A" } ?: "N/A"
        val conductorName = currentUser?.name?.trim()?.ifBlank { "Conductor" } ?: "Conductor"

        if (!bundle.isLoaded) {
            DigitalTicketUiState(
                ticketId = ticketId,
                isLoading = true
            )
        } else if (ticket == null) {
            DigitalTicketUiState(
                ticketId = ticketId,
                isLoading = false,
                ticket = null,
                errorMessage = action.errorMessage ?: if (ticketId.isBlank()) {
                    "Missing ticket ID."
                } else {
                    "Ticket not found."
                }
            )
        } else {
            val formattedTimestamp = payloadFormatter.formatTimestamp(ticket.createdAt)
            val qrPayload = payloadFormatter.buildQrPayload(
                ticket = ticket,
                busNumber = busNumber,
                routeLabel = routeLabel
            )
            val smsReceiptText = payloadFormatter.buildSmsReceiptText(
                ticket = ticket,
                companyName = companyName,
                busNumber = busNumber,
                plateNumber = plateNumber,
                routeLabel = routeLabel,
                conductorName = conductorName
            )
            DigitalTicketUiState(
                ticketId = ticketId,
                isLoading = false,
                companyName = companyName,
                ticket = ticket,
                trip = bundle.trip,
                bus = bundle.bus,
                route = bundle.route,
                busNumber = busNumber,
                plateNumber = plateNumber,
                routeLabel = routeLabel,
                conductorName = conductorName,
                issuedTimestamp = formattedTimestamp,
                qrPayload = qrPayload,
                smsReceiptText = smsReceiptText,
                isSharingImage = action.isSharingImage,
                errorMessage = action.errorMessage
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DigitalTicketUiState(
            ticketId = ticketId,
            isLoading = ticketId.isNotBlank(),
            errorMessage = if (ticketId.isBlank()) "Missing ticket ID." else null
        )
    )

    fun onEvent(event: DigitalTicketUiEvent) {
        when (event) {
            DigitalTicketUiEvent.ShareImageRequested,
            DigitalTicketUiEvent.SendSmsRequested -> Unit
            DigitalTicketUiEvent.ShareImageStarted -> {
                actionState.update { it.copy(isSharingImage = true, errorMessage = null) }
            }
            is DigitalTicketUiEvent.ShareImageCompleted -> {
                actionState.update {
                    it.copy(
                        isSharingImage = false,
                        errorMessage = event.errorMessage
                    )
                }
            }
            is DigitalTicketUiEvent.ActionFailed -> {
                actionState.update {
                    it.copy(
                        isSharingImage = false,
                        errorMessage = event.errorMessage
                    )
                }
            }
            DigitalTicketUiEvent.ClearError -> {
                actionState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
