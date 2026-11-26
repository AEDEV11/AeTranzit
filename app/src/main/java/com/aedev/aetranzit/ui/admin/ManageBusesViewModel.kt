package com.aedev.aetranzit.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.repository.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageBusesUiState(
    val buses: List<Bus> = emptyList(),
    val isEditorOpen: Boolean = false,
    val editingBusId: String? = null,
    val busNumberInput: String = "",
    val busNumberError: String? = null,
    val plateNumberInput: String = "",
    val plateNumberError: String? = null,
    val totalSeatsInput: String = "45",
    val totalSeatsError: String? = null,
    val selectedColumns: Int = 4,
    val isSaving: Boolean = false,
    val pendingDeleteBus: Bus? = null,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

sealed interface ManageBusesUiEvent {
    data object OpenAddDialog : ManageBusesUiEvent
    data class OpenEditDialog(val bus: Bus) : ManageBusesUiEvent
    data object DismissEditorDialog : ManageBusesUiEvent
    data class BusNumberChanged(val value: String) : ManageBusesUiEvent
    data class PlateNumberChanged(val value: String) : ManageBusesUiEvent
    data class TotalSeatsChanged(val value: String) : ManageBusesUiEvent
    data class ColumnsSelected(val columns: Int) : ManageBusesUiEvent
    data object SaveBus : ManageBusesUiEvent
    data class RequestDeleteBus(val bus: Bus) : ManageBusesUiEvent
    data object DismissDeleteDialog : ManageBusesUiEvent
    data object ConfirmDeleteBus : ManageBusesUiEvent
    data object ClearFeedback : ManageBusesUiEvent
}

private data class BusEditorDraftState(
    val isEditorOpen: Boolean = false,
    val editingBusId: String? = null,
    val busNumberInput: String = "",
    val busNumberError: String? = null,
    val plateNumberInput: String = "",
    val plateNumberError: String? = null,
    val totalSeatsInput: String = "45",
    val totalSeatsError: String? = null,
    val selectedColumns: Int = 4,
    val isSaving: Boolean = false,
    val pendingDeleteBus: Bus? = null,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

@HiltViewModel
class ManageBusesViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val draftState = MutableStateFlow(BusEditorDraftState())

    val uiState: StateFlow<ManageBusesUiState> = combine(
        busRepository.observeAllBuses(),
        draftState
    ) { buses, draft ->
        ManageBusesUiState(
            buses = buses,
            isEditorOpen = draft.isEditorOpen,
            editingBusId = draft.editingBusId,
            busNumberInput = draft.busNumberInput,
            busNumberError = draft.busNumberError,
            plateNumberInput = draft.plateNumberInput,
            plateNumberError = draft.plateNumberError,
            totalSeatsInput = draft.totalSeatsInput,
            totalSeatsError = draft.totalSeatsError,
            selectedColumns = draft.selectedColumns,
            isSaving = draft.isSaving,
            pendingDeleteBus = draft.pendingDeleteBus,
            feedbackMessage = draft.feedbackMessage,
            isErrorFeedback = draft.isErrorFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageBusesUiState()
    )

    fun onEvent(event: ManageBusesUiEvent) {
        when (event) {
            ManageBusesUiEvent.OpenAddDialog -> draftState.update {
                BusEditorDraftState(isEditorOpen = true)
            }
            is ManageBusesUiEvent.OpenEditDialog -> draftState.update {
                BusEditorDraftState(
                    isEditorOpen = true,
                    editingBusId = event.bus.id,
                    busNumberInput = event.bus.busNumber,
                    plateNumberInput = event.bus.plateNumber,
                    totalSeatsInput = event.bus.totalSeats.toString(),
                    selectedColumns = if (event.bus.columns == 5) 5 else 4
                )
            }
            ManageBusesUiEvent.DismissEditorDialog -> draftState.update {
                it.copy(isEditorOpen = false, isSaving = false)
            }
            is ManageBusesUiEvent.BusNumberChanged -> draftState.update {
                it.copy(busNumberInput = event.value, busNumberError = null)
            }
            is ManageBusesUiEvent.PlateNumberChanged -> draftState.update {
                it.copy(plateNumberInput = event.value, plateNumberError = null)
            }
            is ManageBusesUiEvent.TotalSeatsChanged -> draftState.update {
                it.copy(
                    totalSeatsInput = event.value.filter { ch -> ch.isDigit() },
                    totalSeatsError = null
                )
            }
            is ManageBusesUiEvent.ColumnsSelected -> {
                if (event.columns == 4 || event.columns == 5) {
                    draftState.update { it.copy(selectedColumns = event.columns) }
                }
            }
            ManageBusesUiEvent.SaveBus -> saveBus()
            is ManageBusesUiEvent.RequestDeleteBus -> draftState.update {
                it.copy(pendingDeleteBus = event.bus)
            }
            ManageBusesUiEvent.DismissDeleteDialog -> draftState.update {
                it.copy(pendingDeleteBus = null)
            }
            ManageBusesUiEvent.ConfirmDeleteBus -> confirmDeleteBus()
            ManageBusesUiEvent.ClearFeedback -> draftState.update {
                it.copy(feedbackMessage = null, isErrorFeedback = false)
            }
        }
    }

    private fun saveBus() {
        val current = draftState.value
        if (current.isSaving) return

        val trimmedBusNo = current.busNumberInput.trim()
        val trimmedPlate = current.plateNumberInput.trim()
        val seats = current.totalSeatsInput.trim().toIntOrNull()

        val busNoError = if (trimmedBusNo.isEmpty()) "Bus number is required." else null
        val plateError = if (trimmedPlate.isEmpty()) "Plate number is required." else null
        val seatsError = when {
            seats == null -> "Enter a valid seat count."
            seats !in MIN_SEATS..MAX_SEATS -> "Seat capacity must be between $MIN_SEATS and $MAX_SEATS."
            else -> null
        }

        if (busNoError != null || plateError != null || seatsError != null) {
            draftState.update {
                it.copy(
                    busNumberError = busNoError,
                    plateNumberError = plateError,
                    totalSeatsError = seatsError
                )
            }
            return
        }

        viewModelScope.launch {
            draftState.update { it.copy(isSaving = true) }
            busRepository.saveBus(
                id = current.editingBusId,
                busNumber = trimmedBusNo,
                plateNumber = trimmedPlate,
                totalSeats = seats!!,
                columns = current.selectedColumns
            ).onSuccess { saved ->
                draftState.update {
                    BusEditorDraftState(
                        feedbackMessage = "Bus ${saved.busNumber} saved.",
                        isErrorFeedback = false
                    )
                }
            }.onFailure { error ->
                draftState.update {
                    it.copy(
                        isSaving = false,
                        feedbackMessage = error.message ?: "Failed to save bus.",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    private fun confirmDeleteBus() {
        val target = draftState.value.pendingDeleteBus ?: return
        viewModelScope.launch {
            draftState.update { it.copy(pendingDeleteBus = null) }
            busRepository.deleteBus(target.id)
                .onSuccess {
                    draftState.update {
                        it.copy(
                            feedbackMessage = "Bus ${target.busNumber} deleted.",
                            isErrorFeedback = false
                        )
                    }
                }
                .onFailure { error ->
                    draftState.update {
                        it.copy(
                            feedbackMessage = error.message ?: "Unable to delete bus.",
                            isErrorFeedback = true
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_SEATS = 10
        const val MAX_SEATS = 80
    }
}
