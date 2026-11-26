package com.aedev.aetranzit.ui.admin

import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.repository.BusRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageBusesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeBusRepository: FakeBusRepository
    private lateinit var viewModel: ManageBusesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeBusRepository = FakeBusRepository()
        viewModel = ManageBusesViewModel(fakeBusRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveBus rejects empty bus and plate numbers and out-of-range seats`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onEvent(ManageBusesUiEvent.OpenAddDialog)
        viewModel.onEvent(ManageBusesUiEvent.BusNumberChanged("  "))
        viewModel.onEvent(ManageBusesUiEvent.PlateNumberChanged(""))
        viewModel.onEvent(ManageBusesUiEvent.TotalSeatsChanged("85"))
        viewModel.onEvent(ManageBusesUiEvent.SaveBus)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.busNumberError)
        assertNotNull(state.plateNumberError)
        assertNotNull(state.totalSeatsError)
        assertEquals(0, state.buses.size)
    }

    @Test
    fun `saveBus persists valid 5-column bus and closes dialog`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onEvent(ManageBusesUiEvent.OpenAddDialog)
        viewModel.onEvent(ManageBusesUiEvent.BusNumberChanged("204"))
        viewModel.onEvent(ManageBusesUiEvent.PlateNumberChanged("ndb-9910"))
        viewModel.onEvent(ManageBusesUiEvent.TotalSeatsChanged("55"))
        viewModel.onEvent(ManageBusesUiEvent.ColumnsSelected(5))
        viewModel.onEvent(ManageBusesUiEvent.SaveBus)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditorOpen)
        assertNull(state.busNumberError)
        assertEquals(1, state.buses.size)
        assertEquals(5, state.buses.first().columns)
        assertEquals("2x3", state.buses.first().layoutLabel)
    }
}

private class FakeBusRepository : BusRepository {
    private val buses = MutableStateFlow<List<Bus>>(emptyList())

    override fun observeAllBuses(): Flow<List<Bus>> = buses

    override fun observeBusById(id: String): Flow<Bus?> =
        buses.map { list -> list.firstOrNull { it.id == id } }

    override fun observeUnsyncedCount(): Flow<Int> =
        buses.map { list -> list.count { !it.isSynced } }

    override suspend fun saveBus(
        id: String?,
        busNumber: String,
        plateNumber: String,
        totalSeats: Int,
        columns: Int
    ): Result<Bus> {
        val bus = Bus(
            id = id ?: "bus-${buses.value.size + 1}",
            busNumber = busNumber,
            plateNumber = plateNumber.uppercase(),
            totalSeats = totalSeats,
            columns = columns,
            isSynced = false,
            updatedAt = 1L
        )
        buses.value = buses.value.filterNot { it.id == bus.id } + bus
        return Result.success(bus)
    }

    override suspend fun deleteBus(id: String): Result<Unit> {
        buses.value = buses.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun syncPendingBuses(): Result<Int> = Result.success(0)
}
