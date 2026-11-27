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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.ui.components.AeEmptyStateCard
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle

@Composable
fun ManageRoutesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageRoutesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManageRoutesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRoutesContent(
    uiState: ManageRoutesUiState,
    onEvent: (ManageRoutesUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Routes") },
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
                onClick = { onEvent(ManageRoutesUiEvent.OpenAddDialog) },
                containerColor = ElectricBlue,
                contentColor = PureWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Route")
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
                            onClick = { onEvent(ManageRoutesUiEvent.ClearFeedback) },
                            modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (uiState.routes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AeDimens.SpaceLg),
                    contentAlignment = Alignment.Center
                ) {
                    AeEmptyStateCard(
                        illustrationRes = R.drawable.empty_fleet_route,
                        title = "No Routes Configured Yet",
                        subtitle = "Add origin-to-destination corridors with base fares and intermediate stop fares.",
                        actionLabel = "Add First Route",
                        onActionClick = { onEvent(ManageRoutesUiEvent.OpenAddDialog) }
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
                    items(items = uiState.routes, key = { it.id }) { route ->
                        RouteItemCard(
                            route = route,
                            onEdit = { onEvent(ManageRoutesUiEvent.OpenEditDialog(route)) },
                            onDelete = { onEvent(ManageRoutesUiEvent.RequestDeleteRoute(route)) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isEditorOpen) {
        RouteEditorDialog(
            uiState = uiState,
            onEvent = onEvent
        )
    }

    val pendingDelete = uiState.pendingDeleteRoute
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { onEvent(ManageRoutesUiEvent.DismissDeleteDialog) },
            title = { Text("Delete Route?") },
            text = {
                Text("${pendingDelete.displayName} (${formatPhp(pendingDelete.baseFare)}) will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ManageRoutesUiEvent.ConfirmDeleteRoute) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ManageRoutesUiEvent.DismissDeleteDialog) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RouteItemCard(
    route: Route,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
            ) {
                Surface(
                    modifier = Modifier.size(AeDimens.ActionRowIconBadgeSize),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
                            text = route.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (route.isSynced) {
                                Icons.Default.CloudDone
                            } else {
                                Icons.Default.CloudQueue
                            },
                            contentDescription = if (route.isSynced) "Synced" else "Local only",
                            tint = if (route.isSynced) {
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
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
                    ) {
                        Text(
                            text = "Base: ${formatPhp(route.baseFare)}",
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${route.stops.size} ${if (route.stops.size == 1) "stop" else "stops"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = AeDimens.SpaceSm,
                                    vertical = AeDimens.SpaceXxs
                                )
                            )
                        }
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
                            contentDescription = "Edit Route ${route.displayName}"
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
                            contentDescription = "Delete Route ${route.displayName}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (route.stops.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
                Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
                    route.stops.forEach { stop ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${stop.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatPhp(stop.fare),
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteEditorDialog(
    uiState: ManageRoutesUiState,
    onEvent: (ManageRoutesUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(ManageRoutesUiEvent.DismissEditorDialog) },
        title = {
            Text(if (uiState.editingRouteId == null) "Add Route" else "Edit Route")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                OutlinedTextField(
                    value = uiState.originInput,
                    onValueChange = { onEvent(ManageRoutesUiEvent.OriginChanged(it)) },
                    label = { Text("Origin (e.g., Cubao)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.originError != null,
                    supportingText = uiState.originError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.destinationInput,
                    onValueChange = { onEvent(ManageRoutesUiEvent.DestinationChanged(it)) },
                    label = { Text("Destination (e.g., Dau)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.destinationError != null,
                    supportingText = uiState.destinationError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.baseFareInput,
                    onValueChange = { onEvent(ManageRoutesUiEvent.BaseFareChanged(it)) },
                    label = { Text("Full-Route Base Fare (₱)") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = uiState.baseFareError != null,
                    supportingText = uiState.baseFareError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = AeDimens.SpaceXs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Intermediate Stops (${uiState.stopDrafts.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        onClick = { onEvent(ManageRoutesUiEvent.AddStopDraft) },
                        shape = CircleShape,
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Stop", modifier = Modifier.padding(start = AeDimens.SpaceXs))
                    }
                }

                uiState.stopDrafts.forEachIndexed { index, draft ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceSm),
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Stop #${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        onEvent(ManageRoutesUiEvent.RemoveStopDraft(draft.draftId))
                                    },
                                    modifier = Modifier.sizeIn(
                                        minWidth = AeDimens.MinTouchTarget,
                                        minHeight = AeDimens.MinTouchTarget
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Stop #${index + 1}"
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = draft.nameInput,
                                onValueChange = {
                                    onEvent(ManageRoutesUiEvent.StopNameChanged(draft.draftId, it))
                                },
                                label = { Text("Stop Name") },
                                shape = MaterialTheme.shapes.medium,
                                singleLine = true,
                                isError = draft.nameError != null,
                                supportingText = draft.nameError?.let { msg -> { Text(msg) } },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = draft.fareInput,
                                onValueChange = {
                                    onEvent(ManageRoutesUiEvent.StopFareChanged(draft.draftId, it))
                                },
                                label = { Text("Stop Fare (₱)") },
                                shape = MaterialTheme.shapes.medium,
                                singleLine = true,
                                isError = draft.fareError != null,
                                supportingText = draft.fareError?.let { msg -> { Text(msg) } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ManageRoutesUiEvent.SaveRoute) },
                enabled = !uiState.isSaving,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Text(if (uiState.editingRouteId == null) "Add Route" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(ManageRoutesUiEvent.DismissEditorDialog) },
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
private fun ManageRoutesContentPreview() {
    AeTranzitTheme {
        ManageRoutesContent(
            uiState = ManageRoutesUiState(
                routes = listOf(
                    Route(
                        id = "r1",
                        origin = "Cubao",
                        destination = "Dau",
                        baseFare = 195.0,
                        stops = listOf(
                            RouteStop("Bocaue", 65.0),
                            RouteStop("San Fernando", 140.0)
                        ),
                        isSynced = true,
                        updatedAt = 1L
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
