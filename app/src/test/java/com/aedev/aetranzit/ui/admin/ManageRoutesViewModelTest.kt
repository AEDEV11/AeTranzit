package com.aedev.aetranzit.ui.admin

import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.repository.RouteRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageRoutesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRouteRepository: FakeRouteRepository
    private lateinit var viewModel: ManageRoutesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRouteRepository = FakeRouteRepository()
        viewModel = ManageRoutesViewModel(fakeRouteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveRoute rejects identical origin and destination, zero base fare, and invalid stop`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onEvent(ManageRoutesUiEvent.OpenAddDialog)
        viewModel.onEvent(ManageRoutesUiEvent.OriginChanged("Cubao"))
        viewModel.onEvent(ManageRoutesUiEvent.DestinationChanged("cubao"))
        viewModel.onEvent(ManageRoutesUiEvent.BaseFareChanged("0"))
        viewModel.onEvent(ManageRoutesUiEvent.AddStopDraft)
        advanceUntilIdle()

        val stopId = viewModel.uiState.value.stopDrafts.single().draftId
        viewModel.onEvent(ManageRoutesUiEvent.StopNameChanged(stopId, ""))
        viewModel.onEvent(ManageRoutesUiEvent.StopFareChanged(stopId, "0"))
        viewModel.onEvent(ManageRoutesUiEvent.SaveRoute)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.destinationError)
        assertNotNull(state.baseFareError)
        assertNotNull(state.stopDrafts.single().nameError)
        assertNotNull(state.stopDrafts.single().fareError)
    }

    @Test
    fun `saveRoute persists route with intermediate stops`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onEvent(ManageRoutesUiEvent.OpenAddDialog)
        viewModel.onEvent(ManageRoutesUiEvent.OriginChanged("Cubao"))
        viewModel.onEvent(ManageRoutesUiEvent.DestinationChanged("Dau"))
        viewModel.onEvent(ManageRoutesUiEvent.BaseFareChanged("195.0"))
        viewModel.onEvent(ManageRoutesUiEvent.AddStopDraft)
        advanceUntilIdle()

        val stopId = viewModel.uiState.value.stopDrafts.single().draftId
        viewModel.onEvent(ManageRoutesUiEvent.StopNameChanged(stopId, "Bocaue"))
        viewModel.onEvent(ManageRoutesUiEvent.StopFareChanged(stopId, "65"))
        viewModel.onEvent(ManageRoutesUiEvent.SaveRoute)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditorOpen)
        assertEquals(1, state.routes.size)
        assertEquals("Cubao → Dau", state.routes.single().displayName)
        assertEquals(listOf(RouteStop("Bocaue", 65.0)), state.routes.single().stops)
    }
}

private class FakeRouteRepository : RouteRepository {
    private val routes = MutableStateFlow<List<Route>>(emptyList())

    override fun observeAllRoutes(): Flow<List<Route>> = routes

    override fun observeRouteById(id: String): Flow<Route?> =
        routes.map { list -> list.firstOrNull { it.id == id } }

    override fun observeUnsyncedCount(): Flow<Int> =
        routes.map { list -> list.count { !it.isSynced } }

    override suspend fun saveRoute(
        id: String?,
        origin: String,
        destination: String,
        baseFare: Double,
        stops: List<RouteStop>
    ): Result<Route> {
        val route = Route(
            id = id ?: "route-${routes.value.size + 1}",
            origin = origin,
            destination = destination,
            baseFare = baseFare,
            stops = stops,
            isSynced = false,
            updatedAt = 1L
        )
        routes.value = routes.value.filterNot { it.id == route.id } + route
        return Result.success(route)
    }

    override suspend fun deleteRoute(id: String): Result<Unit> {
        routes.value = routes.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}
