package com.aedev.aetranzit.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RouteStopDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val nameInput: String = "",
    val fareInput: String = "",
    val nameError: String? = null,
    val fareError: String? = null
)

data class ManageRoutesUiState(
    val routes: List<Route> = emptyList(),
    val isEditorOpen: Boolean = false,
    val editingRouteId: String? = null,
    val originInput: String = "",
    val originError: String? = null,
    val destinationInput: String = "",
    val destinationError: String? = null,
    val baseFareInput: String = "",
    val baseFareError: String? = null,
    val stopDrafts: List<RouteStopDraft> = emptyList(),
    val isSaving: Boolean = false,
    val pendingDeleteRoute: Route? = null,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

sealed interface ManageRoutesUiEvent {
    data object OpenAddDialog : ManageRoutesUiEvent
    data class OpenEditDialog(val route: Route) : ManageRoutesUiEvent
    data object DismissEditorDialog : ManageRoutesUiEvent
    data class OriginChanged(val value: String) : ManageRoutesUiEvent
    data class DestinationChanged(val value: String) : ManageRoutesUiEvent
    data class BaseFareChanged(val value: String) : ManageRoutesUiEvent
    data object AddStopDraft : ManageRoutesUiEvent
    data class RemoveStopDraft(val draftId: String) : ManageRoutesUiEvent
    data class StopNameChanged(val draftId: String, val value: String) : ManageRoutesUiEvent
    data class StopFareChanged(val draftId: String, val value: String) : ManageRoutesUiEvent
    data object SaveRoute : ManageRoutesUiEvent
    data class RequestDeleteRoute(val route: Route) : ManageRoutesUiEvent
    data object DismissDeleteDialog : ManageRoutesUiEvent
    data object ConfirmDeleteRoute : ManageRoutesUiEvent
    data object ClearFeedback : ManageRoutesUiEvent
}

private data class RouteEditorDraftState(
    val isEditorOpen: Boolean = false,
    val editingRouteId: String? = null,
    val originInput: String = "",
    val originError: String? = null,
    val destinationInput: String = "",
    val destinationError: String? = null,
    val baseFareInput: String = "",
    val baseFareError: String? = null,
    val stopDrafts: List<RouteStopDraft> = emptyList(),
    val isSaving: Boolean = false,
    val pendingDeleteRoute: Route? = null,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

@HiltViewModel
class ManageRoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val draftState = MutableStateFlow(RouteEditorDraftState())

    val uiState: StateFlow<ManageRoutesUiState> = combine(
        routeRepository.observeAllRoutes(),
        draftState
    ) { routes, draft ->
        ManageRoutesUiState(
            routes = routes,
            isEditorOpen = draft.isEditorOpen,
            editingRouteId = draft.editingRouteId,
            originInput = draft.originInput,
            originError = draft.originError,
            destinationInput = draft.destinationInput,
            destinationError = draft.destinationError,
            baseFareInput = draft.baseFareInput,
            baseFareError = draft.baseFareError,
            stopDrafts = draft.stopDrafts,
            isSaving = draft.isSaving,
            pendingDeleteRoute = draft.pendingDeleteRoute,
            feedbackMessage = draft.feedbackMessage,
            isErrorFeedback = draft.isErrorFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageRoutesUiState()
    )

    fun onEvent(event: ManageRoutesUiEvent) {
        when (event) {
            ManageRoutesUiEvent.OpenAddDialog -> draftState.update {
                RouteEditorDraftState(isEditorOpen = true)
            }
            is ManageRoutesUiEvent.OpenEditDialog -> draftState.update {
                RouteEditorDraftState(
                    isEditorOpen = true,
                    editingRouteId = event.route.id,
                    originInput = event.route.origin,
                    destinationInput = event.route.destination,
                    baseFareInput = formatEditableAmount(event.route.baseFare),
                    stopDrafts = event.route.stops.map { stop ->
                        RouteStopDraft(
                            nameInput = stop.name,
                            fareInput = formatEditableAmount(stop.fare)
                        )
                    }
                )
            }
            ManageRoutesUiEvent.DismissEditorDialog -> draftState.update {
                it.copy(isEditorOpen = false, isSaving = false)
            }
            is ManageRoutesUiEvent.OriginChanged -> draftState.update {
                it.copy(originInput = event.value, originError = null)
            }
            is ManageRoutesUiEvent.DestinationChanged -> draftState.update {
                it.copy(destinationInput = event.value, destinationError = null)
            }
            is ManageRoutesUiEvent.BaseFareChanged -> draftState.update {
                it.copy(baseFareInput = sanitizeDecimalInput(event.value), baseFareError = null)
            }
            ManageRoutesUiEvent.AddStopDraft -> draftState.update {
                it.copy(stopDrafts = it.stopDrafts + RouteStopDraft())
            }
            is ManageRoutesUiEvent.RemoveStopDraft -> draftState.update {
                it.copy(stopDrafts = it.stopDrafts.filterNot { stop -> stop.draftId == event.draftId })
            }
            is ManageRoutesUiEvent.StopNameChanged -> draftState.update { state ->
                state.copy(
                    stopDrafts = state.stopDrafts.map { stop ->
                        if (stop.draftId == event.draftId) {
                            stop.copy(nameInput = event.value, nameError = null)
                        } else {
                            stop
                        }
                    }
                )
            }
            is ManageRoutesUiEvent.StopFareChanged -> draftState.update { state ->
                state.copy(
                    stopDrafts = state.stopDrafts.map { stop ->
                        if (stop.draftId == event.draftId) {
                            stop.copy(
                                fareInput = sanitizeDecimalInput(event.value),
                                fareError = null
                            )
                        } else {
                            stop
                        }
                    }
                )
            }
            ManageRoutesUiEvent.SaveRoute -> saveRoute()
            is ManageRoutesUiEvent.RequestDeleteRoute -> draftState.update {
                it.copy(pendingDeleteRoute = event.route)
            }
            ManageRoutesUiEvent.DismissDeleteDialog -> draftState.update {
                it.copy(pendingDeleteRoute = null)
            }
            ManageRoutesUiEvent.ConfirmDeleteRoute -> confirmDeleteRoute()
            ManageRoutesUiEvent.ClearFeedback -> draftState.update {
                it.copy(feedbackMessage = null, isErrorFeedback = false)
            }
        }
    }

    private fun saveRoute() {
        val current = draftState.value
        if (current.isSaving) return

        val trimmedOrigin = current.originInput.trim()
        val trimmedDest = current.destinationInput.trim()
        val baseFare = current.baseFareInput.trim().toDoubleOrNull()

        val originError = if (trimmedOrigin.isEmpty()) "Origin is required." else null
        val destinationError = when {
            trimmedDest.isEmpty() -> "Destination is required."
            trimmedOrigin.isNotEmpty() && trimmedOrigin.equals(trimmedDest, ignoreCase = true) ->
                "Destination must differ from origin."
            else -> null
        }
        val baseFareError = when {
            baseFare == null || baseFare <= 0.0 -> "Enter a base fare greater than 0."
            else -> null
        }

        var hasStopError = false
        val validatedDrafts = current.stopDrafts.map { draft ->
            val nameErr = if (draft.nameInput.trim().isEmpty()) "Stop name required." else null
            val parsedFare = draft.fareInput.trim().toDoubleOrNull()
            val fareErr = if (parsedFare == null || parsedFare <= 0.0) "Fare > 0 required." else null
            if (nameErr != null || fareErr != null) {
                hasStopError = true
            }
            draft.copy(nameError = nameErr, fareError = fareErr)
        }

        if (originError != null || destinationError != null || baseFareError != null || hasStopError) {
            draftState.update {
                it.copy(
                    originError = originError,
                    destinationError = destinationError,
                    baseFareError = baseFareError,
                    stopDrafts = validatedDrafts
                )
            }
            return
        }

        val stops = validatedDrafts.map { draft ->
            RouteStop(
                name = draft.nameInput.trim(),
                fare = draft.fareInput.trim().toDouble()
            )
        }

        viewModelScope.launch {
            draftState.update { it.copy(isSaving = true) }
            routeRepository.saveRoute(
                id = current.editingRouteId,
                origin = trimmedOrigin,
                destination = trimmedDest,
                baseFare = baseFare!!,
                stops = stops
            ).onSuccess { saved ->
                draftState.update {
                    RouteEditorDraftState(
                        feedbackMessage = "Route ${saved.displayName} saved.",
                        isErrorFeedback = false
                    )
                }
            }.onFailure { error ->
                draftState.update {
                    it.copy(
                        isSaving = false,
                        feedbackMessage = error.message ?: "Failed to save route.",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    private fun confirmDeleteRoute() {
        val target = draftState.value.pendingDeleteRoute ?: return
        viewModelScope.launch {
            draftState.update { it.copy(pendingDeleteRoute = null) }
            routeRepository.deleteRoute(target.id)
                .onSuccess {
                    draftState.update {
                        it.copy(
                            feedbackMessage = "Route ${target.displayName} deleted.",
                            isErrorFeedback = false
                        )
                    }
                }
                .onFailure { error ->
                    draftState.update {
                        it.copy(
                            feedbackMessage = error.message ?: "Unable to delete route.",
                            isErrorFeedback = true
                        )
                    }
                }
        }
    }

    private fun sanitizeDecimalInput(raw: String): String {
        var dotSeen = false
        return buildString {
            for (ch in raw) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !dotSeen -> {
                        dotSeen = true
                        append(ch)
                    }
                }
            }
        }
    }

    private fun formatEditableAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}
