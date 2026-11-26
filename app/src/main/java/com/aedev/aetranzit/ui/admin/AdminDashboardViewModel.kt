package com.aedev.aetranzit.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.repository.AuthRepository
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import com.aedev.aetranzit.domain.repository.TicketRepository
import com.aedev.aetranzit.domain.repository.TripRepository
import com.aedev.aetranzit.domain.usecase.AdminAnalyticsSummary
import com.aedev.aetranzit.domain.usecase.AnalyticsDateRange
import com.aedev.aetranzit.domain.usecase.BuildAdminAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AdminDashboardUiState(
    val selectedDateRange: AnalyticsDateRange = AnalyticsDateRange.LAST_7_DAYS,
    val analytics: AdminAnalyticsSummary = AdminAnalyticsSummary(),
    val totalBuses: Int = 0,
    val totalRoutes: Int = 0,
    val totalStaff: Int = 0,
    val activeConductorsCount: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    ticketRepository: TicketRepository,
    tripRepository: TripRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    authRepository: AuthRepository,
    private val buildAdminAnalyticsUseCase: BuildAdminAnalyticsUseCase
) : ViewModel() {

    private val selectedDateRange = MutableStateFlow(AnalyticsDateRange.LAST_7_DAYS)

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        ticketRepository.observeAllTickets(),
        tripRepository.observeAllTrips(),
        busRepository.observeAllBuses(),
        routeRepository.observeAllRoutes(),
        combine(authRepository.observeAllUsers(), selectedDateRange, ::Pair)
    ) { tickets, trips, buses, routes, (users, range) ->
        val summary = buildAdminAnalyticsUseCase.execute(
            tickets = tickets,
            trips = trips,
            buses = buses,
            routes = routes,
            users = users,
            dateRange = range
        )
        AdminDashboardUiState(
            selectedDateRange = range,
            analytics = summary,
            totalBuses = buses.size,
            totalRoutes = routes.size,
            totalStaff = users.size,
            activeConductorsCount = users.count { it.role == UserRole.CONDUCTOR && it.isActive }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminDashboardUiState()
    )

    fun onSelectDateRange(range: AnalyticsDateRange) {
        selectedDateRange.value = range
    }
}
