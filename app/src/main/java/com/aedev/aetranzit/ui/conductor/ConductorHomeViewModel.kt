package com.aedev.aetranzit.ui.conductor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TripRepository
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
import javax.inject.Inject

data class ActiveTripDetails(
    val trip: Trip,
    val bus: Bus?,
    val route: Route?
)

data class ConductorHomeUiState(
    val conductorId: String? = null,
    val buses: List<Bus> = emptyList(),
    val routes: List<Route> = emptyList(),
    val selectedBusId: String? = null,
    val selectedRouteId: String? = null,
    val activeTripDetails: ActiveTripDetails? = null,
    val isSubmitting: Boolean = false,
    val isEndTripConfirmVisible: Boolean = false,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

sealed interface ConductorHomeUiEvent {
    data class SelectBus(val busId: String) : ConductorHomeUiEvent
    data class SelectRoute(val routeId: String) : ConductorHomeUiEvent
    data object StartTrip : ConductorHomeUiEvent
    data object RequestEndTrip : ConductorHomeUiEvent
    data object DismissEndTripDialog : ConductorHomeUiEvent
    data object ConfirmEndTrip : ConductorHomeUiEvent
    data object SeedSampleFleetAndRoute : ConductorHomeUiEvent
    data object ClearMessage : ConductorHomeUiEvent
}

private data class ShiftControlState(
    val selectedBusId: String? = null,
    val selectedRouteId: String? = null,
    val isSubmitting: Boolean = false,
    val isEndTripConfirmVisible: Boolean = false,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

@HiltViewModel
class ConductorHomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val controlState = MutableStateFlow(ShiftControlState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeTripFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user == null) {
            flowOf(null)
        } else {
            tripRepository.observeActiveTrip(user.id)
        }
    }

    val uiState: StateFlow<ConductorHomeUiState> = combine(
        authRepository.observeCurrentUser(),
        busRepository.observeAllBuses(),
        routeRepository.observeAllRoutes(),
        activeTripFlow,
        controlState
    ) { user, buses, routes, activeTrip, control ->
        val resolvedBusId = control.selectedBusId?.takeIf { id -> buses.any { it.id == id } }
            ?: buses.firstOrNull()?.id
        val resolvedRouteId = control.selectedRouteId?.takeIf { id -> routes.any { it.id == id } }
            ?: routes.firstOrNull()?.id

        val details = activeTrip?.let { trip ->
            ActiveTripDetails(
                trip = trip,
                bus = buses.firstOrNull { it.id == trip.busId },
                route = routes.firstOrNull { it.id == trip.routeId }
            )
        }

        ConductorHomeUiState(
            conductorId = user?.id,
            buses = buses,
            routes = routes,
            selectedBusId = resolvedBusId,
            selectedRouteId = resolvedRouteId,
            activeTripDetails = details,
            isSubmitting = control.isSubmitting,
            isEndTripConfirmVisible = control.isEndTripConfirmVisible,
            statusMessage = control.statusMessage,
            isErrorMessage = control.isErrorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConductorHomeUiState()
    )

    fun onEvent(event: ConductorHomeUiEvent) {
        when (event) {
            is ConductorHomeUiEvent.SelectBus -> controlState.update {
                it.copy(selectedBusId = event.busId, statusMessage = null)
            }
            is ConductorHomeUiEvent.SelectRoute -> controlState.update {
                it.copy(selectedRouteId = event.routeId, statusMessage = null)
            }
            ConductorHomeUiEvent.StartTrip -> startTrip()
            ConductorHomeUiEvent.RequestEndTrip -> controlState.update {
                it.copy(isEndTripConfirmVisible = true)
            }
            ConductorHomeUiEvent.DismissEndTripDialog -> controlState.update {
                it.copy(isEndTripConfirmVisible = false)
            }
            ConductorHomeUiEvent.ConfirmEndTrip -> confirmEndTrip()
            ConductorHomeUiEvent.SeedSampleFleetAndRoute -> seedSampleFleetAndRoute()
            ConductorHomeUiEvent.ClearMessage -> controlState.update {
                it.copy(statusMessage = null, isErrorMessage = false)
            }
        }
    }

    private fun startTrip() {
        val snapshot = uiState.value
        if (snapshot.isSubmitting) return

        val conductorId = snapshot.conductorId
        if (conductorId.isNullOrBlank()) {
            controlState.update {
                it.copy(statusMessage = "Session expired. Please sign in again.", isErrorMessage = true)
            }
            return
        }
        val busId = snapshot.selectedBusId
        if (busId.isNullOrBlank()) {
            controlState.update {
                it.copy(statusMessage = "Please select an available bus.", isErrorMessage = true)
            }
            return
        }
        val routeId = snapshot.selectedRouteId
        if (routeId.isNullOrBlank()) {
            controlState.update {
                it.copy(statusMessage = "Please select an available route.", isErrorMessage = true)
            }
            return
        }

        viewModelScope.launch {
            controlState.update { it.copy(isSubmitting = true, statusMessage = null) }
            tripRepository.startTrip(
                conductorId = conductorId,
                busId = busId,
                routeId = routeId
            ).onSuccess {
                controlState.update {
                    it.copy(
                        isSubmitting = false,
                        statusMessage = "Shift started! Seat map is ready for ticketing.",
                        isErrorMessage = false
                    )
                }
            }.onFailure { error ->
                controlState.update {
                    it.copy(
                        isSubmitting = false,
                        statusMessage = error.message ?: "Unable to start trip.",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    private fun confirmEndTrip() {
        val activeTrip = uiState.value.activeTripDetails?.trip ?: return
        if (controlState.value.isSubmitting) return

        viewModelScope.launch {
            controlState.update {
                it.copy(isSubmitting = true, isEndTripConfirmVisible = false)
            }
            tripRepository.endTrip(activeTrip.id)
                .onSuccess {
                    controlState.update {
                        it.copy(
                            isSubmitting = false,
                            statusMessage = "Trip completed and saved to Room.",
                            isErrorMessage = false
                        )
                    }
                }
                .onFailure { error ->
                    controlState.update {
                        it.copy(
                            isSubmitting = false,
                            statusMessage = error.message ?: "Unable to complete trip.",
                            isErrorMessage = true
                        )
                    }
                }
        }
    }

    private fun seedSampleFleetAndRoute() {
        val snapshot = uiState.value
        if (snapshot.isSubmitting) return

        viewModelScope.launch {
            controlState.update { it.copy(isSubmitting = true, statusMessage = null) }
            var errorMessage: String? = null

            if (snapshot.buses.isEmpty()) {
                busRepository.saveBus(
                    busNumber = "101",
                    plateNumber = "NCA-4821",
                    totalSeats = 45,
                    columns = 4
                ).onFailure { errorMessage = it.message }

                busRepository.saveBus(
                    busNumber = "204",
                    plateNumber = "NDB-9910",
                    totalSeats = 55,
                    columns = 5
                ).onFailure { errorMessage = it.message }
            }

            if (snapshot.routes.isEmpty()) {
                routeRepository.saveRoute(
                    origin = "Cubao",
                    destination = "Dau",
                    baseFare = 195.0,
                    stops = listOf(
                        RouteStop(name = "Bocaue", fare = 65.0),
                        RouteStop(name = "San Fernando", fare = 140.0)
                    )
                ).onFailure { errorMessage = it.message }

                routeRepository.saveRoute(
                    origin = "PITX",
                    destination = "Tagaytay",
                    baseFare = 150.0,
                    stops = listOf(
                        RouteStop(name = "Dasmariñas", fare = 75.0),
                        RouteStop(name = "Silang", fare = 110.0)
                    )
                ).onFailure { errorMessage = it.message }
            }

            controlState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = errorMessage ?: "Sample fleet and routes added for quick setup.",
                    isErrorMessage = errorMessage != null
                )
            }
        }
    }
}
