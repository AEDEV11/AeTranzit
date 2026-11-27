package com.aedev.aetranzit.ui.conductor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.usecase.FareCalculationResult
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import com.aedev.aetranzit.ui.components.SyncStatusBadge
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import java.text.DateFormat
import java.util.Date

private val QUICK_CASH_DENOMINATIONS = listOf(50.0, 100.0, 200.0, 500.0, 1000.0)

private enum class SeatVisualState {
    AVAILABLE,
    SELECTED,
    OCCUPIED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorTicketingScreen(
    onNavigateBack: () -> Unit,
    onOpenDigitalTicket: ((String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState(),
    viewModel: ConductorTicketingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val handleEvent: (ConductorTicketingUiEvent) -> Unit = { event ->
        if (event is ConductorTicketingUiEvent.ViewDigitalTicket && onOpenDigitalTicket != null) {
            viewModel.onEvent(ConductorTicketingUiEvent.DismissInspectedTicket)
            onOpenDigitalTicket(event.ticket.id)
        } else {
            viewModel.onEvent(event)
        }
    }

    ConductorTicketingContent(
        uiState = uiState,
        syncBadgeState = syncBadgeState,
        onEvent = handleEvent,
        onNavigateBack = onNavigateBack,
        onOpenSettings = onOpenSettings
    )

    if (uiState.selectedSeatForIssue != null) {
        val issueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { handleEvent(ConductorTicketingUiEvent.DismissIssueSheet) },
            sheetState = issueSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            IssueTicketSheetContent(
                seatNumber = uiState.selectedSeatForIssue.orEmpty(),
                routeLabel = uiState.route?.displayName.orEmpty(),
                destinationOptions = uiState.destinationOptions,
                selectedDestination = uiState.selectedDestination,
                selectedPassengerType = uiState.selectedPassengerType,
                isManualFareOverrideEnabled = uiState.isManualFareOverrideEnabled,
                manualFareInput = uiState.manualFareInput,
                cashTenderedInput = uiState.cashTenderedInput,
                fareCalculation = uiState.fareCalculation,
                isIssuingTicket = uiState.isIssuingTicket,
                errorMessage = uiState.issueErrorMessage,
                onEvent = handleEvent
            )
        }
    }

    val inspectedTicket = uiState.inspectedTicket
    if (inspectedTicket != null) {
        val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { handleEvent(ConductorTicketingUiEvent.DismissInspectedTicket) },
            sheetState = detailsSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            OccupiedSeatDetailsSheetContent(
                ticket = inspectedTicket,
                isUpdating = uiState.isUpdatingTicketStatus,
                onEvent = handleEvent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorTicketingContent(
    uiState: ConductorTicketingUiState,
    onEvent: (ConductorTicketingUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState(),
    onOpenSettings: (() -> Unit)? = null
) {
    val bus = uiState.bus
    val columns = if (bus?.columns == 5) 5 else 4
    val seatRows = remember(uiState.totalSeats, columns) {
        if (uiState.totalSeats <= 0) {
            emptyList()
        } else {
            (1..uiState.totalSeats).map { it.toString() }.chunked(columns)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Seat Map & Ticketing",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.route != null) {
                            Text(
                                text = uiState.route.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    SyncStatusBadge(
                        state = syncBadgeState,
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(end = AeDimens.SpaceSm)
                    )
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.trip == null || bus == null || uiState.route == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(AeDimens.SpaceMd),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceLg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(AeDimens.IconLg)
                            )
                            Text(
                                text = "Active Trip Unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This trip could not be found or its bus/route metadata is missing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            AePrimaryButton(
                                text = "Return to Shift Console",
                                onClick = onNavigateBack
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(AeDimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    item(key = "trip_summary_bar") {
                        TripSummaryHeaderCard(
                            bus = bus,
                            route = uiState.route,
                            occupiedSeatsCount = uiState.occupiedSeatsCount,
                            totalSeats = uiState.totalSeats,
                            totalCollection = uiState.totalTripCollection
                        )
                    }

                    if (uiState.statusMessage != null) {
                        item(key = "status_banner") {
                            StatusFeedbackBanner(
                                message = uiState.statusMessage,
                                isError = uiState.isErrorMessage,
                                onDismiss = { onEvent(ConductorTicketingUiEvent.ClearStatusMessage) }
                            )
                        }
                    }

                    val confirmedTicket = uiState.lastIssuedTicket
                    if (confirmedTicket != null) {
                        item(key = "issued_confirmation_banner") {
                            IssuedTicketConfirmationCard(
                                ticket = confirmedTicket,
                                onViewDigitalTicket = {
                                    onEvent(ConductorTicketingUiEvent.ViewDigitalTicket(confirmedTicket))
                                },
                                onDismiss = {
                                    onEvent(ConductorTicketingUiEvent.DismissIssuedConfirmation)
                                }
                            )
                        }
                    }

                    item(key = "seat_map_header") {
                        SeatMapOrientationHeader(layoutLabel = bus.layoutLabel)
                    }

                    itemsIndexed(
                        items = seatRows,
                        key = { index, _ -> "seat_row_$index" }
                    ) { _, rowSeats ->
                        BusSeatRow(
                            rowSeats = rowSeats,
                            totalColumns = columns,
                            selectedSeat = uiState.selectedSeatForIssue,
                            activeTicketsBySeat = uiState.activeTicketsBySeat,
                            onSeatClick = { seat ->
                                onEvent(ConductorTicketingUiEvent.SeatTapped(seat))
                            }
                        )
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(AeDimens.SpaceLg))
                    }
                }
            }
        }
    }
}

@Composable
private fun TripSummaryHeaderCard(
    bus: Bus,
    route: Route,
    occupiedSeatsCount: Int,
    totalSeats: Int,
    totalCollection: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Bus #${bus.busNumber} • ${bus.plateNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${route.origin} → ${route.destination}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "${bus.layoutLabel} Layout",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSm,
                            vertical = AeDimens.SpaceXs
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OCCUPIED SEATS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$occupiedSeatsCount / $totalSeats",
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TRIP COLLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatPhp(totalCollection),
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss message",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun IssuedTicketConfirmationCard(
    ticket: Ticket,
    onViewDigitalTicket: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.secondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Ticket Issued • Ref #${ticket.referenceCode}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Seat ${ticket.seatNumber} → ${ticket.destination} (${ticket.passengerType.displayName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss ticket confirmation",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fare: ${formatPhp(ticket.fareAmount)} • Change: ${formatPhp(ticket.changeAmount)}",
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                TextButton(
                    onClick = onViewDigitalTicket,
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(AeDimens.IconSm)
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
                    Text("View Digital Ticket")
                }
            }
        }
    }
}

@Composable
private fun SeatMapOrientationHeader(layoutLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap an open seat to issue • Tap an occupied seat for details/drop-off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeatLegendBadge(
                    icon = Icons.Default.EventSeat,
                    label = "Available",
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outline
                )
                SeatLegendBadge(
                    icon = Icons.Default.CheckCircle,
                    label = "Selected",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary
                )
                SeatLegendBadge(
                    icon = Icons.Default.Person,
                    label = "Occupied",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    borderColor = MaterialTheme.colorScheme.secondary
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "▲ FRONT OF BUS ($layoutLabel AISLE LAYOUT)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AeDimens.SpaceXs)
                )
            }
        }
    }
}

@Composable
private fun SeatLegendBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(AeDimens.BorderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AeDimens.SpaceSm, vertical = AeDimens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(AeDimens.IconXs)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun BusSeatRow(
    rowSeats: List<String>,
    totalColumns: Int,
    selectedSeat: String?,
    activeTicketsBySeat: Map<String, Ticket>,
    onSeatClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
    ) {
        for (colIndex in 0 until totalColumns) {
            if (colIndex == 2) {
                Spacer(modifier = Modifier.width(AeDimens.AisleWidth))
            }
            val seatNumber = rowSeats.getOrNull(colIndex)
            if (seatNumber != null) {
                val occupiedTicket = activeTicketsBySeat[seatNumber]
                val visualState = when {
                    occupiedTicket != null -> SeatVisualState.OCCUPIED
                    selectedSeat == seatNumber -> SeatVisualState.SELECTED
                    else -> SeatVisualState.AVAILABLE
                }
                SeatTile(
                    seatNumber = seatNumber,
                    visualState = visualState,
                    destinationTag = occupiedTicket?.destination,
                    onClick = { onSeatClick(seatNumber) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SeatTile(
    seatNumber: String,
    visualState: SeatVisualState,
    destinationTag: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.surface
        SeatVisualState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.onSurface
        SeatVisualState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val borderColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.outline
        SeatVisualState.SELECTED -> MaterialTheme.colorScheme.primary
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.secondary
    }
    val borderWidth = if (visualState == SeatVisualState.SELECTED) {
        AeDimens.BorderWidthThick
    } else {
        AeDimens.BorderWidth
    }
    val icon = when (visualState) {
        SeatVisualState.AVAILABLE -> Icons.Default.EventSeat
        SeatVisualState.SELECTED -> Icons.Default.CheckCircle
        SeatVisualState.OCCUPIED -> Icons.Default.Person
    }
    val subtitle = when (visualState) {
        SeatVisualState.AVAILABLE -> "Open"
        SeatVisualState.SELECTED -> "Selected"
        SeatVisualState.OCCUPIED -> destinationTag.orEmpty()
    }
    val a11yDescription = when (visualState) {
        SeatVisualState.AVAILABLE -> "Seat $seatNumber, available"
        SeatVisualState.SELECTED -> "Seat $seatNumber, selected for ticket issuance"
        SeatVisualState.OCCUPIED -> "Seat $seatNumber, occupied, destination $subtitle"
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AeDimens.SeatTileMinHeight)
            .semantics { contentDescription = a11yDescription },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeDimens.SpaceXs, vertical = AeDimens.SpaceSm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(AeDimens.IconXs)
                )
                Text(
                    text = seatNumber,
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        lineHeight = MaterialTheme.typography.titleSmall.lineHeight
                    ),
                    color = contentColor,
                    maxLines = 1
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun IssueTicketSheetContent(
    seatNumber: String,
    routeLabel: String,
    destinationOptions: List<DestinationOption>,
    selectedDestination: DestinationOption?,
    selectedPassengerType: PassengerType,
    isManualFareOverrideEnabled: Boolean,
    manualFareInput: String,
    cashTenderedInput: String,
    fareCalculation: FareCalculationResult,
    isIssuingTicket: Boolean,
    errorMessage: String?,
    onEvent: (ConductorTicketingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
    ) {
        // Seat Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceMd,
                            vertical = AeDimens.SpaceSm
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                        Text(
                            text = "Seat #$seatNumber",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "Issue Digital Ticket",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (routeLabel.isNotBlank()) {
                        Text(
                            text = routeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = { onEvent(ConductorTicketingUiEvent.DismissIssueSheet) },
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close sheet"
                )
            }
        }

        // 1. Destination Selector
        Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
            Text(
                text = "1. Destination",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
            ) {
                destinationOptions.forEach { option ->
                    val isSelected = selectedDestination?.name == option.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(ConductorTicketingUiEvent.SelectDestination(option)) },
                        label = {
                            Text(
                                text = "${option.name} (${formatPhp(option.fare)})",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }
        }

        // 2. Passenger Type Selector
        Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
            Text(
                text = "2. Passenger Type",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
            ) {
                PassengerType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedPassengerType == type,
                        onClick = { onEvent(ConductorTicketingUiEvent.SelectPassengerType(type)) },
                        label = {
                            Text(
                                text = type.chipLabel,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }
        }

        // 3. Calculated Fare & Optional Manual Override
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOTAL FARE DUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatPhp(fareCalculation.finalFare),
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (fareCalculation.discountAmount > 0.0 && !fareCalculation.isManualOverride) {
                            Text(
                                text = "20% ${selectedPassengerType.displayName} discount applied (-${formatPhp(fareCalculation.discountAmount)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else if (fareCalculation.isManualOverride) {
                            Text(
                                text = "Manual fare override active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilterChip(
                        selected = isManualFareOverrideEnabled,
                        onClick = {
                            onEvent(
                                ConductorTicketingUiEvent.ToggleManualFareOverride(
                                    !isManualFareOverrideEnabled
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(AeDimens.IconXs)
                            )
                        },
                        label = { Text("Override") },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }

                if (isManualFareOverrideEnabled) {
                    OutlinedTextField(
                        value = manualFareInput,
                        onValueChange = { onEvent(ConductorTicketingUiEvent.ManualFareChanged(it)) },
                        label = { Text("Custom Fare Amount (₱)") },
                        placeholder = { Text("e.g. 120.00") },
                        singleLine = true,
                        textStyle = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Cash Tendered & Change Calculator
        Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
            Text(
                text = "3. Cash Tendered & Change",
                style = MaterialTheme.typography.titleSmall
            )

            OutlinedTextField(
                value = cashTenderedInput,
                onValueChange = { onEvent(ConductorTicketingUiEvent.CashTenderedChanged(it)) },
                label = { Text("Cash Tendered (₱)") },
                placeholder = { Text("Tap Exact / quick bill or type amount") },
                singleLine = true,
                isError = fareCalculation.isInsufficientCash,
                textStyle = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
            ) {
                val isExactSelected = fareCalculation.cashTendered != null &&
                    fareCalculation.finalFare > 0.0 &&
                    fareCalculation.cashTendered == fareCalculation.finalFare
                FilterChip(
                    selected = isExactSelected,
                    onClick = { onEvent(ConductorTicketingUiEvent.SelectExactCash) },
                    label = { Text("Exact", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                )

                QUICK_CASH_DENOMINATIONS.forEach { bill ->
                    val isBillSelected = fareCalculation.cashTendered == bill
                    FilterChip(
                        selected = isBillSelected,
                        onClick = { onEvent(ConductorTicketingUiEvent.SelectQuickCash(bill)) },
                        label = {
                            Text(
                                text = "₱${bill.toInt()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }

            // Live Change / Insufficient Cash Warning Box
            when {
                fareCalculation.isInsufficientCash -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceMd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = "Insufficient Cash Tendered",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Short by ${formatPhp(fareCalculation.shortfallAmount)}",
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                fareCalculation.cashTendered != null && fareCalculation.finalFare > 0.0 -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceMd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHANGE DUE",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = formatPhp(fareCalculation.changeAmount),
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                    lineHeight = MaterialTheme.typography.headlineSmall.lineHeight
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        AePrimaryButton(
            text = "Issue Digital Ticket",
            isLoading = isIssuingTicket,
            enabled = fareCalculation.canIssueTicket,
            onClick = { onEvent(ConductorTicketingUiEvent.ConfirmIssueTicket) }
        )

        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
    }
}

@Composable
fun OccupiedSeatDetailsSheetContent(
    ticket: Ticket,
    isUpdating: Boolean,
    onEvent: (ConductorTicketingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val issuedAtLabel = remember(ticket.createdAt) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ticket.createdAt))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(AeDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceMd,
                            vertical = AeDimens.SpaceSm
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                        Text(
                            text = "Seat #${ticket.seatNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "Passenger Ticket",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ref #${ticket.referenceCode} • Issued $issuedAtLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { onEvent(ConductorTicketingUiEvent.DismissInspectedTicket) },
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close details")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                TicketDetailRow(label = "Destination", value = ticket.destination, isAmount = false)
                TicketDetailRow(
                    label = "Passenger Type",
                    value = if (ticket.passengerType.isDiscounted) {
                        "${ticket.passengerType.displayName} (-20%)"
                    } else {
                        ticket.passengerType.displayName
                    },
                    isAmount = false
                )
                HorizontalDivider()
                TicketDetailRow(
                    label = "Fare Paid",
                    value = formatPhp(ticket.fareAmount),
                    isAmount = true
                )
                TicketDetailRow(
                    label = "Cash Tendered",
                    value = formatPhp(ticket.cashTendered),
                    isAmount = true
                )
                TicketDetailRow(
                    label = "Change Given",
                    value = formatPhp(ticket.changeAmount),
                    isAmount = true
                )
                TicketDetailRow(
                    label = "Sync State",
                    value = if (ticket.isSynced) "Synced to Cloud" else "Saved Offline in Room",
                    isAmount = false
                )
            }
        }

        OutlinedButton(
            onClick = { onEvent(ConductorTicketingUiEvent.ViewDigitalTicket(ticket)) },
            enabled = !isUpdating,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AeDimens.PrimaryButtonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(AeDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
            Text("View Digital Ticket", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = { onEvent(ConductorTicketingUiEvent.MarkPassengerDroppedOff(ticket.id)) },
            enabled = !isUpdating,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AeDimens.PrimaryButtonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(AeDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
            Text(
                text = if (isUpdating) "Updating Seat..." else "Mark Dropped Off (Free Seat)",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
    }
}

@Composable
private fun TicketDetailRow(
    label: String,
    value: String,
    isAmount: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isAmount) {
                TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            } else {
                MaterialTheme.typography.titleSmall
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================================
// Previews (2x2 Seat Map, 2x3 Seat Map, Issue Ticket Sheet States)
// ============================================================================

private val previewRoute = Route(
    id = "route-cubao-dau",
    origin = "Cubao",
    destination = "Dau",
    baseFare = 195.0,
    stops = listOf(
        RouteStop(name = "Bocaue", fare = 65.0),
        RouteStop(name = "San Fernando", fare = 140.0)
    ),
    isSynced = true,
    updatedAt = 1L
)

private val previewSampleTicket1 = Ticket(
    id = "a8f3b210-1111-2222-3333-444455556666",
    tripId = "trip-1",
    busId = "bus-1",
    conductorId = "cond-1",
    seatNumber = "1",
    destination = "Dau",
    passengerType = PassengerType.REGULAR,
    fareAmount = 195.0,
    cashTendered = 200.0,
    changeAmount = 5.0,
    status = TicketStatus.ACTIVE,
    createdAt = 1_700_000_000_000L,
    isSynced = false,
    updatedAt = 1_700_000_000_000L
)

private val previewSampleTicket4 = Ticket(
    id = "c49e107b-1111-2222-3333-444455556666",
    tripId = "trip-1",
    busId = "bus-1",
    conductorId = "cond-1",
    seatNumber = "4",
    destination = "San Fernando",
    passengerType = PassengerType.STUDENT,
    fareAmount = 112.0,
    cashTendered = 200.0,
    changeAmount = 88.0,
    status = TicketStatus.ACTIVE,
    createdAt = 1_700_000_060_000L,
    isSynced = false,
    updatedAt = 1_700_000_060_000L
)

@Preview(showBackground = true, name = "Seat Map — 2x2 Layout")
@Composable
private fun ConductorTicketing2x2Preview() {
    AeTranzitTheme {
        ConductorTicketingContent(
            uiState = ConductorTicketingUiState(
                tripId = "trip-1",
                isLoading = false,
                trip = Trip(
                    id = "trip-1",
                    busId = "bus-1",
                    routeId = previewRoute.id,
                    conductorId = "cond-1",
                    status = TripStatus.ACTIVE,
                    startedAt = 1_700_000_000_000L,
                    endedAt = null,
                    isSynced = false,
                    updatedAt = 1_700_000_000_000L
                ),
                bus = Bus(
                    id = "bus-1",
                    busNumber = "101",
                    plateNumber = "NCA-4821",
                    totalSeats = 12,
                    columns = 4,
                    isSynced = true,
                    updatedAt = 1L
                ),
                route = previewRoute,
                activeTicketsBySeat = mapOf(
                    "1" to previewSampleTicket1,
                    "4" to previewSampleTicket4
                ),
                occupiedSeatsCount = 2,
                totalSeats = 12,
                totalTripCollection = 307.0,
                selectedSeatForIssue = "2",
                lastIssuedTicket = previewSampleTicket4
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Seat Map — 2x3 Layout")
@Composable
private fun ConductorTicketing2x3Preview() {
    AeTranzitTheme {
        ConductorTicketingContent(
            uiState = ConductorTicketingUiState(
                tripId = "trip-2",
                isLoading = false,
                trip = Trip(
                    id = "trip-2",
                    busId = "bus-2",
                    routeId = previewRoute.id,
                    conductorId = "cond-1",
                    status = TripStatus.ACTIVE,
                    startedAt = 1_700_000_000_000L,
                    endedAt = null,
                    isSynced = false,
                    updatedAt = 1_700_000_000_000L
                ),
                bus = Bus(
                    id = "bus-2",
                    busNumber = "204",
                    plateNumber = "NDB-9910",
                    totalSeats = 15,
                    columns = 5,
                    isSynced = true,
                    updatedAt = 1L
                ),
                route = previewRoute,
                activeTicketsBySeat = mapOf("1" to previewSampleTicket1),
                occupiedSeatsCount = 1,
                totalSeats = 15,
                totalTripCollection = 195.0
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Issue Ticket Sheet — Student 20% Discount")
@Composable
private fun IssueTicketSheetDiscountedPreview() {
    val options = listOf(
        DestinationOption("Bocaue", 65.0, false),
        DestinationOption("San Fernando", 140.0, false),
        DestinationOption("Dau", 195.0, true)
    )
    AeTranzitTheme {
        Surface {
            IssueTicketSheetContent(
                seatNumber = "7",
                routeLabel = "Cubao → Dau",
                destinationOptions = options,
                selectedDestination = options[2],
                selectedPassengerType = PassengerType.STUDENT,
                isManualFareOverrideEnabled = false,
                manualFareInput = "",
                cashTenderedInput = "200",
                fareCalculation = FareCalculationResult(
                    baseFare = 195.0,
                    finalFare = 156.0,
                    discountAmount = 39.0,
                    isManualOverride = false,
                    cashTendered = 200.0,
                    changeAmount = 44.0,
                    shortfallAmount = 0.0,
                    isInsufficientCash = false,
                    canIssueTicket = true
                ),
                isIssuingTicket = false,
                errorMessage = null,
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Issue Ticket Sheet — Insufficient Cash")
@Composable
private fun IssueTicketSheetInsufficientCashPreview() {
    val options = listOf(
        DestinationOption("Bocaue", 65.0, false),
        DestinationOption("Dau", 195.0, true)
    )
    AeTranzitTheme {
        Surface {
            IssueTicketSheetContent(
                seatNumber = "11",
                routeLabel = "Cubao → Dau",
                destinationOptions = options,
                selectedDestination = options[1],
                selectedPassengerType = PassengerType.REGULAR,
                isManualFareOverrideEnabled = false,
                manualFareInput = "",
                cashTenderedInput = "100",
                fareCalculation = FareCalculationResult(
                    baseFare = 195.0,
                    finalFare = 195.0,
                    discountAmount = 0.0,
                    isManualOverride = false,
                    cashTendered = 100.0,
                    changeAmount = 0.0,
                    shortfallAmount = 95.0,
                    isInsufficientCash = true,
                    canIssueTicket = false
                ),
                isIssuingTicket = false,
                errorMessage = null,
                onEvent = {}
            )
        }
    }
}
