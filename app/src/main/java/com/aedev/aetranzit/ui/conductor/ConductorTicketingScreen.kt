package com.aedev.aetranzit.ui.conductor

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.model.Bus
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.Ticket
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.Trip
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.usecase.FareCalculationResult
import com.aedev.aetranzit.ui.components.AeEmptyStateCard
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import com.aedev.aetranzit.ui.components.SyncStatusBadge
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.ElectricBlueDark
import com.aedev.aetranzit.ui.theme.HeroHeaderBottomShape
import com.aedev.aetranzit.ui.theme.NavyHeaderBorder
import com.aedev.aetranzit.ui.theme.NavyHeaderSurface
import com.aedev.aetranzit.ui.theme.NavySubtleText
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TabularLabelStyle
import com.aedev.aetranzit.ui.theme.TransitTeal
import com.aedev.aetranzit.ui.theme.TransitTealDark
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
        val issueSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
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
        val detailsSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
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
    val route = uiState.route
    val columns = if (bus?.columns == 5) 5 else 4
    val seatRows = remember(uiState.totalSeats, columns) {
        if (uiState.totalSeats <= 0) {
            emptyList()
        } else {
            (1..uiState.totalSeats).map { it.toString() }.chunked(columns)
        }
    }
    val sortedActiveTickets = remember(uiState.activeTicketsBySeat) {
        uiState.activeTicketsBySeat.values.sortedWith(
            compareBy({ it.seatNumber.toIntOrNull() ?: Int.MAX_VALUE }, { it.seatNumber })
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState.trip != null && bus != null && route != null) {
                TripSummaryHeroHeader(
                    bus = bus,
                    route = route,
                    occupiedSeatsCount = uiState.occupiedSeatsCount,
                    totalSeats = uiState.totalSeats,
                    totalCollection = uiState.totalTripCollection,
                    syncBadgeState = syncBadgeState,
                    onNavigateBack = onNavigateBack,
                    onOpenSettings = onOpenSettings
                )
            } else {
                TopAppBar(
                    title = { Text("Seat Map & Ticketing") },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.sizeIn(
                                minWidth = AeDimens.MinTouchTarget,
                                minHeight = AeDimens.MinTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
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

            uiState.trip == null || bus == null || route == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(AeDimens.SpaceMd),
                    contentAlignment = Alignment.Center
                ) {
                    AeEmptyStateCard(
                        illustrationRes = R.drawable.empty_fleet_route,
                        title = "Active Trip Unavailable",
                        subtitle = "This trip could not be found or its bus and route metadata is missing.",
                        actionLabel = "Return to Shift Console",
                        onActionClick = onNavigateBack
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(AeDimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
                ) {
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

                    item(key = "seat_map_card") {
                        SeatMapContainerCard(
                            layoutLabel = bus.layoutLabel,
                            seatRows = seatRows,
                            totalColumns = columns,
                            selectedSeat = uiState.selectedSeatForIssue,
                            activeTicketsBySeat = uiState.activeTicketsBySeat,
                            onSeatClick = { seat ->
                                onEvent(ConductorTicketingUiEvent.SeatTapped(seat))
                            }
                        )
                    }

                    if (sortedActiveTickets.isNotEmpty()) {
                        item(key = "active_seats_manifest") {
                            ActiveSeatsManifestCard(
                                tickets = sortedActiveTickets,
                                onTicketClick = { ticket ->
                                    onEvent(ConductorTicketingUiEvent.SeatTapped(ticket.seatNumber))
                                }
                            )
                        }
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
private fun TripSummaryHeroHeader(
    bus: Bus,
    route: Route,
    occupiedSeatsCount: Int,
    totalSeats: Int,
    totalCollection: Double,
    syncBadgeState: SyncBadgeUiState,
    onNavigateBack: () -> Unit,
    onOpenSettings: (() -> Unit)?
) {
    val availableSeatsCount = (totalSeats - occupiedSeatsCount).coerceAtLeast(0)
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepNavy,
        shape = HeroHeaderBottomShape,
        shadowElevation = AeDimens.ElevatedCardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = AeDimens.SpaceMd,
                    end = AeDimens.SpaceMd,
                    top = AeDimens.SpaceSm,
                    bottom = AeDimens.SpaceMd
                ),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.sizeIn(
                            minWidth = AeDimens.MinTouchTarget,
                            minHeight = AeDimens.MinTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                    Column {
                        Text(
                            text = "Bus #${bus.busNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = bus.plateNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavySubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    SyncStatusBadge(
                        state = syncBadgeState,
                        onDarkHeader = true,
                        onClick = onOpenSettings
                    )
                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.sizeIn(
                                minWidth = AeDimens.MinTouchTarget,
                                minHeight = AeDimens.MinTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PureWhite
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
                Text(
                    text = "${route.origin}  →  ${route.destination}",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(AeDimens.StatusDotSize)
                            .clip(CircleShape)
                            .background(ElectricBlueDark)
                    )
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(AeDimens.BorderWidthThick)
                    ) {
                        val centerY = size.height / 2f
                        drawLine(
                            color = NavyHeaderBorder,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = size.height,
                            pathEffect = dashEffect
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = TransitTealDark,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(AeDimens.BorderWidthThick)
                    ) {
                        val centerY = size.height / 2f
                        drawLine(
                            color = NavyHeaderBorder,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = size.height,
                            pathEffect = dashEffect
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(AeDimens.StatusDotSize)
                            .clip(CircleShape)
                            .background(TransitTealDark)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = NavyHeaderSurface,
                border = BorderStroke(AeDimens.BorderWidth, NavyHeaderBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AeDimens.SpaceSmPlus,
                            vertical = AeDimens.SpaceSmPlus
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderMetricCell(
                        label = "Passengers",
                        value = "$occupiedSeatsCount/$totalSeats",
                        valueColor = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    HeaderMetricCell(
                        label = "Seats Available",
                        value = "$availableSeatsCount/$totalSeats",
                        valueColor = TransitTealDark,
                        modifier = Modifier.weight(1f),
                        align = Alignment.CenterHorizontally
                    )
                    HeaderMetricCell(
                        label = "Layout",
                        value = bus.layoutLabel,
                        valueColor = PureWhite,
                        modifier = Modifier.weight(1f),
                        align = Alignment.End
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trip Collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavySubtleText
                )
                Text(
                    text = formatPhp(totalCollection),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
                    ),
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
private fun HeaderMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = align
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NavySubtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = TabularAmountStyle.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight
            ),
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun SeatMapContainerCard(
    layoutLabel: String,
    seatRows: List<List<String>>,
    totalColumns: Int,
    selectedSeat: String?,
    activeTicketsBySeat: Map<String, Ticket>,
    onSeatClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.ElevatedCardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Seat Map",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap an open seat to issue • Tap occupied for receipt or drop-off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = layoutLabel,
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSmPlus,
                            vertical = AeDimens.SpaceXs
                        )
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "▲  FRONT OF BUS  ($layoutLabel AISLE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AeDimens.SpaceXs)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
                seatRows.forEach { rowSeats ->
                    BusSeatRow(
                        rowSeats = rowSeats,
                        totalColumns = totalColumns,
                        selectedSeat = selectedSeat,
                        activeTicketsBySeat = activeTicketsBySeat,
                        onSeatClick = onSeatClick
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeatLegendIndicator(
                    dotColor = MaterialTheme.colorScheme.outline,
                    label = "Available"
                )
                SeatLegendIndicator(
                    dotColor = ElectricBlue,
                    label = "Selected"
                )
                SeatLegendIndicator(
                    dotColor = TransitTeal,
                    label = "Occupied"
                )
            }
        }
    }
}

@Composable
private fun SeatLegendIndicator(
    dotColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
    ) {
        Box(
            modifier = Modifier
                .size(AeDimens.StatusDotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActiveSeatsManifestCard(
    tickets: List<Ticket>,
    onTicketClick: (Ticket) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
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
                    text = "Occupied Seats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${tickets.size} active",
                    style = TabularLabelStyle.merge(MaterialTheme.typography.labelMedium),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            tickets.forEachIndexed { index, ticket ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Surface(
                    onClick = { onTicketClick(ticket) },
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AeDimens.SpaceXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.sizeIn(minWidth = AeDimens.ActionRowIconBadgeSize)
                            ) {
                                Text(
                                    text = "#${ticket.seatNumber}",
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.labelLarge.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(
                                        horizontal = AeDimens.SpaceSm,
                                        vertical = AeDimens.SpaceXs
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = ticket.destination,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (ticket.passengerType.isDiscounted) {
                                        "${ticket.passengerType.displayName} (-20%)"
                                    } else {
                                        ticket.passengerType.displayName
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ticket.passengerType.isDiscounted) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                        ) {
                            Text(
                                text = formatPhp(ticket.fareAmount),
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                modifier = Modifier.sizeIn(
                    minWidth = AeDimens.MinTouchTarget,
                    minHeight = AeDimens.MinTouchTarget
                )
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
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.sizeIn(
                        minWidth = AeDimens.MinTouchTarget,
                        minHeight = AeDimens.MinTouchTarget
                    )
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
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
        SeatVisualState.SELECTED -> ElectricBlue
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.onSurface
        SeatVisualState.SELECTED -> PureWhite
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val subtitleColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        SeatVisualState.SELECTED -> PureWhite
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val borderColor = when (visualState) {
        SeatVisualState.AVAILABLE -> MaterialTheme.colorScheme.outlineVariant
        SeatVisualState.SELECTED -> ElectricBlue
        SeatVisualState.OCCUPIED -> MaterialTheme.colorScheme.secondary
    }
    val borderWidth = if (visualState == SeatVisualState.SELECTED) {
        AeDimens.BorderWidthThick
    } else {
        AeDimens.BorderWidth
    }
    val seatDrawableRes = when (visualState) {
        SeatVisualState.AVAILABLE -> R.drawable.ic_seat_available
        SeatVisualState.SELECTED -> R.drawable.ic_seat_selected
        SeatVisualState.OCCUPIED -> R.drawable.ic_seat_occupied
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
                .padding(horizontal = AeDimens.SpaceXs, vertical = AeDimens.SpaceXs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = seatDrawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(AeDimens.SeatArtworkSize)
                )
            }
            Text(
                text = seatNumber,
                style = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight
                ),
                color = contentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (visualState == SeatVisualState.SELECTED) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
                ),
                color = subtitleColor,
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
                    color = ElectricBlue
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
                            tint = PureWhite,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                        Text(
                            text = "Seat #$seatNumber",
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            ),
                            color = PureWhite
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
                modifier = Modifier.sizeIn(
                    minWidth = AeDimens.MinTouchTarget,
                    minHeight = AeDimens.MinTouchTarget
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close sheet"
                )
            }
        }

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
                                style = TabularLabelStyle.merge(MaterialTheme.typography.labelLarge)
                            )
                        },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }
        }

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
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
                                style = TabularLabelStyle.merge(MaterialTheme.typography.bodySmall),
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
                        shape = MaterialTheme.shapes.medium,
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
                shape = MaterialTheme.shapes.medium,
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
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                                )
                            )
                        },
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    )
                }
            }

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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.secondary)
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.secondary)
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
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            ),
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
                modifier = Modifier.sizeIn(
                    minWidth = AeDimens.MinTouchTarget,
                    minHeight = AeDimens.MinTouchTarget
                )
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close details")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
            shape = CircleShape,
            border = BorderStroke(AeDimens.BorderWidthThick, MaterialTheme.colorScheme.primary)
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
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = PureWhite
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
// Previews (2x2 Seat Map Light/Dark, 2x3 Seat Map, Issue Ticket Sheet States)
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

@Preview(showBackground = true, name = "Seat Map — 2x2 Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Seat Map — 2x2 Dark"
)
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
            onNavigateBack = {},
            onOpenSettings = {}
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
            onNavigateBack = {},
            onOpenSettings = {}
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
