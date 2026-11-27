package com.aedev.aetranzit.ui.admin

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.ui.components.AeEmptyStateCard
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle

@Composable
fun ManageBusesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageBusesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManageBusesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBusesContent(
    uiState: ManageBusesUiState,
    onEvent: (ManageBusesUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Buses") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.sizeIn(
                            minWidth = AeDimens.MinTouchTarget,
                            minHeight = AeDimens.MinTouchTarget
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(ManageBusesUiEvent.OpenAddDialog) },
                containerColor = ElectricBlue,
                contentColor = PureWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bus")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.feedbackMessage != null) {
                Surface(
                    color = if (uiState.isErrorFeedback) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.feedbackMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isErrorFeedback) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onEvent(ManageBusesUiEvent.ClearFeedback) },
                            modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (uiState.buses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AeDimens.SpaceLg),
                    contentAlignment = Alignment.Center
                ) {
                    AeEmptyStateCard(
                        illustrationRes = R.drawable.empty_fleet_route,
                        title = "No Buses Configured Yet",
                        subtitle = "Add your fleet buses with seat capacity and column layout (2x2 or 2x3) so conductors can start trips.",
                        actionLabel = "Add First Bus",
                        onActionClick = { onEvent(ManageBusesUiEvent.OpenAddDialog) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AeDimens.SpaceMd,
                        end = AeDimens.SpaceMd,
                        top = AeDimens.SpaceSm,
                        bottom = AeDimens.FabBottomClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
                ) {
                    items(items = uiState.buses, key = { it.id }) { bus ->
                        BusItemCard(
                            bus = bus,
                            onEdit = { onEvent(ManageBusesUiEvent.OpenEditDialog(bus)) },
                            onDelete = { onEvent(ManageBusesUiEvent.RequestDeleteBus(bus)) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isEditorOpen) {
        BusEditorDialog(
            uiState = uiState,
            onEvent = onEvent
        )
    }

    val pendingDelete = uiState.pendingDeleteBus
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { onEvent(ManageBusesUiEvent.DismissDeleteDialog) },
            title = { Text("Delete Bus ${pendingDelete.busNumber}?") },
            text = {
                Text(
                    "Plate ${pendingDelete.plateNumber} (${pendingDelete.totalSeats} seats) will be removed from the local fleet."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ManageBusesUiEvent.ConfirmDeleteBus) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ManageBusesUiEvent.DismissDeleteDialog) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BusItemCard(
    bus: Bus,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            Surface(
                modifier = Modifier.size(AeDimens.ActionRowIconBadgeSize),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AeDimens.IconSm)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    Text(
                        text = "Bus ${bus.busNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${bus.layoutLabel} (${bus.columns} cols)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(
                                horizontal = AeDimens.SpaceSm,
                                vertical = AeDimens.SpaceXxs
                            )
                        )
                    }
                    Icon(
                        imageVector = if (bus.isSynced) {
                            Icons.Default.CloudDone
                        } else {
                            Icons.Default.CloudQueue
                        },
                        contentDescription = if (bus.isSynced) "Synced" else "Local only",
                        tint = if (bus.isSynced) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
                ) {
                    Text(
                        text = "Plate: ${bus.plateNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${bus.totalSeats} seats",
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.sizeIn(
                        minWidth = AeDimens.MinTouchTarget,
                        minHeight = AeDimens.MinTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Bus ${bus.busNumber}"
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.sizeIn(
                        minWidth = AeDimens.MinTouchTarget,
                        minHeight = AeDimens.MinTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Bus ${bus.busNumber}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BusEditorDialog(
    uiState: ManageBusesUiState,
    onEvent: (ManageBusesUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(ManageBusesUiEvent.DismissEditorDialog) },
        title = {
            Text(if (uiState.editingBusId == null) "Add Bus" else "Edit Bus")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
                OutlinedTextField(
                    value = uiState.busNumberInput,
                    onValueChange = { onEvent(ManageBusesUiEvent.BusNumberChanged(it)) },
                    label = { Text("Bus Number (e.g., 101)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.busNumberError != null,
                    supportingText = uiState.busNumberError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.plateNumberInput,
                    onValueChange = { onEvent(ManageBusesUiEvent.PlateNumberChanged(it)) },
                    label = { Text("Plate Number (e.g., NCA-4821)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.plateNumberError != null,
                    supportingText = uiState.plateNumberError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.totalSeatsInput,
                    onValueChange = { onEvent(ManageBusesUiEvent.TotalSeatsChanged(it)) },
                    label = { Text("Total Seats (10 – 80)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.totalSeatsError != null,
                    supportingText = uiState.totalSeatsError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Column Layout",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    FilterChip(
                        selected = uiState.selectedColumns == 4,
                        onClick = { onEvent(ManageBusesUiEvent.ColumnsSelected(4)) },
                        label = { Text("2x2 (4 cols)") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = AeDimens.MinTouchTarget)
                    )
                    FilterChip(
                        selected = uiState.selectedColumns == 5,
                        onClick = { onEvent(ManageBusesUiEvent.ColumnsSelected(5)) },
                        label = { Text("2x3 (5 cols)") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ManageBusesUiEvent.SaveBus) },
                enabled = !uiState.isSaving,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Text(if (uiState.editingBusId == null) "Add Bus" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(ManageBusesUiEvent.DismissEditorDialog) },
                enabled = !uiState.isSaving,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManageBusesContentPreview() {
    AeTranzitTheme {
        ManageBusesContent(
            uiState = ManageBusesUiState(
                buses = listOf(
                    Bus(
                        id = "b1",
                        busNumber = "101",
                        plateNumber = "NCA-4821",
                        totalSeats = 45,
                        columns = 4,
                        isSynced = true,
                        updatedAt = 1L
                    ),
                    Bus(
                        id = "b2",
                        busNumber = "204",
                        plateNumber = "NDB-9910",
                        totalSeats = 55,
                        columns = 5,
                        isSynced = false,
                        updatedAt = 2L
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
