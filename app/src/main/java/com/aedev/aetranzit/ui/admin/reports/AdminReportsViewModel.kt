package com.aedev.aetranzit.ui.admin.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.AnalyticsDateRange
import com.aedev.aetranzit.domain.usecase.CalculateFareUseCase
import com.aedev.aetranzit.domain.usecase.ReportExportFormatter
import com.aedev.aetranzit.domain.usecase.ReportTicketRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportFilterOption(
    val id: String,
    val label: String
)

data class AdminReportsUiState(
    val selectedDateRange: AnalyticsDateRange = AnalyticsDateRange.LAST_30_DAYS,
    val selectedBusId: String? = null,
    val selectedConductorId: String? = null,
    val busOptions: List<ReportFilterOption> = emptyList(),
    val conductorOptions: List<ReportFilterOption> = emptyList(),
    val filteredRows: List<ReportTicketRow> = emptyList(),
    val totalTicketsCount: Int = 0,
    val discountedTicketsCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val filterSummaryLabel: String = "Last 30 Days • All Buses • All Conductors",
    val isExportingCsv: Boolean = false,
    val isExportingPdf: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

private data class ReportFiltersState(
    val dateRange: AnalyticsDateRange = AnalyticsDateRange.LAST_30_DAYS,
    val busId: String? = null,
    val conductorId: String? = null,
    val isExportingCsv: Boolean = false,
    val isExportingPdf: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    ticketRepository: TicketRepository,
    tripRepository: TripRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    authRepository: AuthRepository,
    private val reportExportFormatter: ReportExportFormatter,
    private val reportFileExporter: ReportFileExporter
) : ViewModel() {

    private val filtersState = MutableStateFlow(ReportFiltersState())

    private val domainDataFlow = combine(
        ticketRepository.observeAllTickets(),
        tripRepository.observeAllTrips(),
        busRepository.observeAllBuses(),
        routeRepository.observeAllRoutes(),
        authRepository.observeAllUsers()
    ) { tickets, trips, buses, routes, users ->
        DomainSnapshot(tickets, trips, buses, routes, users)
    }

    val uiState: StateFlow<AdminReportsUiState> = combine(
        domainDataFlow,
        filtersState
    ) { snapshot, filters ->
        val nowMillis = System.currentTimeMillis()
        val filteredTickets = snapshot.tickets.filter { ticket ->
            filters.dateRange.contains(ticket.createdAt, nowMillis) &&
                (filters.busId == null || ticket.busId == filters.busId) &&
                (filters.conductorId == null || ticket.conductorId == filters.conductorId)
        }

        val rows = reportExportFormatter.buildReportRows(
            tickets = filteredTickets,
            trips = snapshot.trips,
            buses = snapshot.buses,
            routes = snapshot.routes,
            users = snapshot.users
        )

        val busOptions = snapshot.buses
            .sortedBy { it.busNumber }
            .map { ReportFilterOption(id = it.id, label = "Bus ${it.busNumber}") }

        val conductorOptions = snapshot.users
            .filter { user ->
                user.role == UserRole.CONDUCTOR ||
                    snapshot.tickets.any { it.conductorId == user.id }
            }
            .sortedBy { it.name }
            .map { ReportFilterOption(id = it.id, label = it.name) }

        val selectedBusLabel = busOptions.firstOrNull { it.id == filters.busId }?.label ?: "All Buses"
        val selectedConductorLabel =
            conductorOptions.firstOrNull { it.id == filters.conductorId }?.label ?: "All Conductors"

        AdminReportsUiState(
            selectedDateRange = filters.dateRange,
            selectedBusId = filters.busId,
            selectedConductorId = filters.conductorId,
            busOptions = busOptions,
            conductorOptions = conductorOptions,
            filteredRows = rows,
            totalTicketsCount = rows.size,
            discountedTicketsCount = rows.count { it.isDiscounted },
            totalRevenue = CalculateFareUseCase.roundCurrency(rows.sumOf { it.fareAmount }),
            filterSummaryLabel = "${filters.dateRange.reportLabel} • $selectedBusLabel • $selectedConductorLabel",
            isExportingCsv = filters.isExportingCsv,
            isExportingPdf = filters.isExportingPdf,
            feedbackMessage = filters.feedbackMessage,
            isErrorFeedback = filters.isErrorFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminReportsUiState()
    )

    fun onSelectDateRange(range: AnalyticsDateRange) {
        filtersState.update { it.copy(dateRange = range, feedbackMessage = null) }
    }

    fun onSelectBus(busId: String?) {
        filtersState.update { it.copy(busId = busId, feedbackMessage = null) }
    }

    fun onSelectConductor(conductorId: String?) {
        filtersState.update { it.copy(conductorId = conductorId, feedbackMessage = null) }
    }

    fun onDismissFeedback() {
        filtersState.update { it.copy(feedbackMessage = null, isErrorFeedback = false) }
    }

    fun onExportCsv(context: Context) {
        val snapshot = uiState.value
        if (snapshot.isExportingCsv || snapshot.isExportingPdf) return

        viewModelScope.launch {
            filtersState.update {
                it.copy(isExportingCsv = true, feedbackMessage = null, isErrorFeedback = false)
            }
            val result = reportFileExporter.exportCsvAndShare(
                context = context,
                rows = snapshot.filteredRows,
                filterSummary = snapshot.filterSummaryLabel
            )
            filtersState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            isExportingCsv = false,
                            feedbackMessage = null,
                            isErrorFeedback = false
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isExportingCsv = false,
                            feedbackMessage = error.message ?: "Unable to export CSV report.",
                            isErrorFeedback = true
                        )
                    }
                )
            }
        }
    }

    fun onExportPdf(context: Context) {
        val snapshot = uiState.value
        if (snapshot.isExportingCsv || snapshot.isExportingPdf) return

        viewModelScope.launch {
            filtersState.update {
                it.copy(isExportingPdf = true, feedbackMessage = null, isErrorFeedback = false)
            }
            val result = reportFileExporter.exportPdfAndShare(
                context = context,
                rows = snapshot.filteredRows,
                filterSummary = snapshot.filterSummaryLabel,
                totalRevenue = snapshot.totalRevenue
            )
            filtersState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            isExportingPdf = false,
                            feedbackMessage = null,
                            isErrorFeedback = false
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isExportingPdf = false,
                            feedbackMessage = error.message ?: "Unable to export PDF report.",
                            isErrorFeedback = true
                        )
                    }
                )
            }
        }
    }

    private data class DomainSnapshot(
        val tickets: List<com.aedev.aetranzit.domain.model.Ticket>,
        val trips: List<com.aedev.aetranzit.domain.model.Trip>,
        val buses: List<com.aedev.aetranzit.domain.model.Bus>,
        val routes: List<com.aedev.aetranzit.domain.model.Route>,
        val users: List<com.aedev.aetranzit.domain.model.User>
    )
}
