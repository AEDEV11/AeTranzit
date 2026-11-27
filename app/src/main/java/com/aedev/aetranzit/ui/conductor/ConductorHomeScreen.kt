package com.aedev.aetranzit.ui.conductor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import com.aedev.aetranzit.ui.components.SyncStatusBadge
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import java.text.DateFormat
import java.util.Date

@Composable
fun ConductorHomeScreen(
    userName: String,
    isCloudSyncEnabled: Boolean,
    onOpenTicketing: (tripId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState(),
    viewModel: ConductorHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConductorHomeContent(
        userName = userName,
        isCloudSyncEnabled = isCloudSyncEnabled,
        syncBadgeState = syncBadgeState,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenTicketing = onOpenTicketing,
        onOpenSettings = onOpenSettings,
        onLogout = onLogout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorHomeContent(
    userName: String,
    isCloudSyncEnabled: Boolean,
    uiState: ConductorHomeUiState,
    onEvent: (ConductorHomeUiEvent) -> Unit,
    onOpenTicketing: (tripId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Conductor Console") },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            ConductorDutyHeaderCard(
                userName = userName,
                syncBadgeState = syncBadgeState.copy(isCloudSyncEnabled = isCloudSyncEnabled),
                onOpenSettings = onOpenSettings
            )

            if (uiState.statusMessage != null) {
                Surface(
                    color = if (uiState.isErrorMessage) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isErrorMessage) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onEvent(ConductorHomeUiEvent.ClearMessage) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            val activeDetails = uiState.activeTripDetails
            if (activeDetails != null) {
                ActiveTripSummaryCard(
                    details = activeDetails,
                    isSubmitting = uiState.isSubmitting,
                    onOpenSeatMap = { onOpenTicketing(activeDetails.trip.id) },
                    onRequestEndTrip = { onEvent(ConductorHomeUiEvent.RequestEndTrip) }
                )
            } else {
                StartNewTripCard(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }

            OutlinedButton(
                onClick = onOpenSettings,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AeDimens.PrimaryButtonHeight)
            ) {
                Text("Open Sync & Settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (uiState.isEndTripConfirmVisible && uiState.activeTripDetails != null) {
        val busLabel = uiState.activeTripDetails.bus?.busNumber?.let { "Bus $it" } ?: "current bus"
        val routeLabel = uiState.activeTripDetails.route?.displayName ?: "current route"
        AlertDialog(
            onDismissRequest = { onEvent(ConductorHomeUiEvent.DismissEndTripDialog) },
            title = { Text("End Active Trip?") },
            text = {
                Text(
                    "Mark $busLabel ($routeLabel) as COMPLETED? No new tickets can be issued for this trip once ended."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ConductorHomeUiEvent.ConfirmEndTrip) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("End Trip", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ConductorHomeUiEvent.DismissEndTripDialog) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Text("Keep Active")
                }
            }
        )
    }
}

@Composable
private fun ConductorDutyHeaderCard(
    userName: String,
    syncBadgeState: SyncBadgeUiState,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "On Duty: $userName",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Role: Conductor • Seamless Offline Queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SyncStatusBadge(
                state = syncBadgeState,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun StartNewTripCard(
    uiState: ConductorHomeUiState,
    onEvent: (ConductorHomeUiEvent) -> Unit
) {
    val missingFleetOrRoutes = uiState.buses.isEmpty() || uiState.routes.isEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Start New Trip",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (missingFleetOrRoutes) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeDimens.SpaceMd),
                        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        Text(
                            text = when {
                                uiState.buses.isEmpty() && uiState.routes.isEmpty() ->
                                    "No buses or routes are configured on this device yet."
                                uiState.buses.isEmpty() ->
                                    "No buses are configured yet."
                                else ->
                                    "No routes are configured yet."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Have an Admin add your fleet and routes from the Admin Dashboard, or seed sample fleet & route data now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onEvent(ConductorHomeUiEvent.SeedSampleFleetAndRoute) },
                            enabled = !uiState.isSubmitting,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = AeDimens.MinTouchTarget)
                        ) {
                            Text("Seed Sample Fleet & Route")
                        }
                    }
                }
            } else {
                Text(
                    text = "1. Select Bus",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
                    uiState.buses.forEach { bus ->
                        val isSelected = bus.id == uiState.selectedBusId
                        SelectableOptionRow(
                            selected = isSelected,
                            title = "Bus ${bus.busNumber} • ${bus.plateNumber}",
                            subtitle = "${bus.totalSeats} seats • ${bus.layoutLabel} layout",
                            trailingBadge = null,
                            onClick = { onEvent(ConductorHomeUiEvent.SelectBus(bus.id)) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "2. Select Route",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
                    uiState.routes.forEach { route ->
                        val isSelected = route.id == uiState.selectedRouteId
                        SelectableOptionRow(
                            selected = isSelected,
                            title = route.displayName,
                            subtitle = "${route.stops.size} intermediate ${if (route.stops.size == 1) "stop" else "stops"}",
                            trailingBadge = formatPhp(route.baseFare),
                            onClick = { onEvent(ConductorHomeUiEvent.SelectRoute(route.id)) }
                        )
                    }
                }

                AePrimaryButton(
                    text = "Start Trip",
                    isLoading = uiState.isSubmitting,
                    enabled = uiState.selectedBusId != null && uiState.selectedRouteId != null,
                    onClick = { onEvent(ConductorHomeUiEvent.StartTrip) }
                )
            }
        }
    }
}

@Composable
private fun SelectableOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    trailingBadge: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = AeDimens.BorderWidth,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AeDimens.MinTouchTarget)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                if (trailingBadge != null) {
                    Text(
                        text = trailingBadge,
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTripSummaryCard(
    details: ActiveTripDetails,
    isSubmitting: Boolean,
    onOpenSeatMap: () -> Unit,
    onRequestEndTrip: () -> Unit
) {
    val startedAtFormatted = remember(details.trip.startedAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(details.trip.startedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Trip",
                    style = MaterialTheme.typography.titleLarge
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSm,
                            vertical = AeDimens.SpaceXs
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = details.trip.status.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AeDimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = details.bus?.let {
                                "Bus ${it.busNumber} • Plate ${it.plateNumber} (${it.totalSeats} seats, ${it.layoutLabel})"
                            } ?: "Bus ID: ${details.trip.busId}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = details.route?.displayName ?: "Route ID: ${details.trip.routeId}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (details.route != null) {
                            Text(
                                text = formatPhp(details.route.baseFare),
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Started: $startedAtFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AePrimaryButton(
                text = "Open Seat Map & Issue Ticket",
                enabled = !isSubmitting,
                onClick = onOpenSeatMap
            )

            OutlinedButton(
                onClick = onRequestEndTrip,
                enabled = !isSubmitting,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(
                    width = AeDimens.BorderWidth,
                    color = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AeDimens.PrimaryButtonHeight)
            ) {
                Icon(
                    imageVector = Icons.Default.StopCircle,
                    contentDescription = null
                )
                Text(
                    text = "End Trip",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = AeDimens.SpaceSm)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConductorHomeNoActiveTripPreview() {
    AeTranzitTheme {
        ConductorHomeContent(
            userName = "Juan Dela Cruz",
            isCloudSyncEnabled = false,
            uiState = ConductorHomeUiState(
                buses = listOf(
                    Bus(
                        id = "b1",
                        busNumber = "101",
                        plateNumber = "NCA-4821",
                        totalSeats = 45,
                        columns = 4,
                        isSynced = false,
                        updatedAt = 1L
                    )
                ),
                routes = listOf(
                    Route(
                        id = "r1",
                        origin = "Cubao",
                        destination = "Dau",
                        baseFare = 195.0,
                        stops = listOf(RouteStop("Bocaue", 65.0)),
                        isSynced = false,
                        updatedAt = 1L
                    )
                ),
                selectedBusId = "b1",
                selectedRouteId = "r1"
            ),
            onEvent = {},
            onOpenTicketing = {},
            onOpenSettings = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConductorHomeActiveTripPreview() {
    val bus = Bus(
        id = "b1",
        busNumber = "101",
        plateNumber = "NCA-4821",
        totalSeats = 45,
        columns = 4,
        isSynced = true,
        updatedAt = 1L
    )
    val route = Route(
        id = "r1",
        origin = "Cubao",
        destination = "Dau",
        baseFare = 195.0,
        stops = listOf(RouteStop("Bocaue", 65.0)),
        isSynced = true,
        updatedAt = 1L
    )
    val trip = Trip(
        id = "t1",
        busId = "b1",
        routeId = "r1",
        conductorId = "c1",
        status = TripStatus.ACTIVE,
        startedAt = 1_700_000_000_000L,
        endedAt = null,
        isSynced = false,
        updatedAt = 1L
    )
    AeTranzitTheme {
        ConductorHomeContent(
            userName = "Juan Dela Cruz",
            isCloudSyncEnabled = true,
            uiState = ConductorHomeUiState(
                activeTripDetails = ActiveTripDetails(trip = trip, bus = bus, route = route)
            ),
            onEvent = {},
            onOpenTicketing = {},
            onOpenSettings = {},
            onLogout = {}
        )
    }
}
