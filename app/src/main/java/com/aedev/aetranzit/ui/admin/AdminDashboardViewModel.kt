package com.aedev.aetranzit.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.domain.repository.BusRepository
import com.aedev.aetranzit.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AdminDashboardUiState(
    val totalBuses: Int = 0,
    val totalRoutes: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    busRepository: BusRepository,
    routeRepository: RouteRepository
) : ViewModel() {

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        busRepository.observeAllBuses(),
        routeRepository.observeAllRoutes()
    ) { buses, routes ->
        AdminDashboardUiState(
            totalBuses = buses.size,
            totalRoutes = routes.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminDashboardUiState()
    )
}
