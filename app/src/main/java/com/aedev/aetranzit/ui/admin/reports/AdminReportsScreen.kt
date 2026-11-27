package com.aedev.aetranzit.ui.admin.reports

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.usecase.AnalyticsDateRange
import com.aedev.aetranzit.domain.usecase.ReportTicketRow
import com.aedev.aetranzit.ui.components.AeChipTone
import com.aedev.aetranzit.ui.components.AeEmptyStateCard
import com.aedev.aetranzit.ui.components.AeFilterChip
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.NavyHeaderBorder
import com.aedev.aetranzit.ui.theme.NavyHeaderSurface
import com.aedev.aetranzit.ui.theme.NavySubtleText
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TabularLabelStyle
import com.aedev.aetranzit.ui.theme.TransitTealDark

@Composable
fun AdminReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AdminReportsContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSelectDateRange = viewModel::onSelectDateRange,
        onSelectBus = viewModel::onSelectBus,
        onSelectConductor = viewModel::onSelectConductor,
        onExportCsv = { viewModel.onExportCsv(context) },
        onExportPdf = { viewModel.onExportPdf(context) },
        onDismissFeedback = viewModel::onDismissFeedback,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsContent(
    uiState: AdminReportsUiState,
    onNavigateBack: () -> Unit,
    onSelectDateRange: (AnalyticsDateRange) -> Unit,
    onSelectBus: (String?) -> Unit,
    onSelectConductor: (String?) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reports & Exports") },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            item(key = "filters_card") {
                ReportsFilterSection(
                    uiState = uiState,
                    onSelectDateRange = onSelectDateRange,
                    onSelectBus = onSelectBus,
                    onSelectConductor = onSelectConductor
                )
            }

            item(key = "summary_export_card") {
                ReportSummaryAndExportCard(
                    uiState = uiState,
                    onExportCsv = onExportCsv,
                    onExportPdf = onExportPdf
                )
            }

            if (uiState.feedbackMessage != null) {
                item(key = "feedback_banner") {
                    ReportFeedbackBanner(
                        message = uiState.feedbackMessage,
                        isError = uiState.isErrorFeedback,
                        onDismiss = onDismissFeedback
                    )
                }
            }

            item(key = "log_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered Ticket Log",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.totalTicketsCount} ${if (uiState.totalTicketsCount == 1) "record" else "records"}",
                        style = TabularLabelStyle.merge(MaterialTheme.typography.labelMedium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.filteredRows.isEmpty()) {
                item(key = "empty_log") {
                    EmptyReportRecordsCard()
                }
            } else {
                items(
                    items = uiState.filteredRows,
                    key = { it.ticketId }
                ) { row ->
                    TicketReportRowCard(row = row)
                }
            }
        }
    }
}

@Composable
private fun ReportsFilterSection(
    uiState: AdminReportsUiState,
    onSelectDateRange: (AnalyticsDateRange) -> Unit,
    onSelectBus: (String?) -> Unit,
    onSelectConductor: (String?) -> Unit
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
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                AnalyticsDateRange.entries.forEach { range ->
                    AeFilterChip(
                        label = range.chipLabel,
                        selected = range == uiState.selectedDateRange,
                        showCheckWhenSelected = true,
                        useTabularStyle = true,
                        onClick = { onSelectDateRange(range) }
                    )
                }
            }

            if (uiState.busOptions.isNotEmpty()) {
                Text(
                    text = "Bus Filter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    AeFilterChip(
                        label = "All Buses",
                        selected = uiState.selectedBusId == null,
                        tone = AeChipTone.PRIMARY_CONTAINER,
                        showCheckWhenSelected = true,
                        onClick = { onSelectBus(null) }
                    )
                    uiState.busOptions.forEach { option ->
                        AeFilterChip(
                            label = option.label,
                            selected = uiState.selectedBusId == option.id,
                            tone = AeChipTone.PRIMARY_CONTAINER,
                            showCheckWhenSelected = true,
                            useTabularStyle = true,
                            onClick = { onSelectBus(option.id) }
                        )
                    }
                }
            }

            if (uiState.conductorOptions.isNotEmpty()) {
                Text(
                    text = "Conductor Filter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    AeFilterChip(
                        label = "All Conductors",
                        selected = uiState.selectedConductorId == null,
                        tone = AeChipTone.PRIMARY_CONTAINER,
                        showCheckWhenSelected = true,
                        onClick = { onSelectConductor(null) }
                    )
                    uiState.conductorOptions.forEach { option ->
                        AeFilterChip(
                            label = option.label,
                            selected = uiState.selectedConductorId == option.id,
                            tone = AeChipTone.PRIMARY_CONTAINER,
                            showCheckWhenSelected = true,
                            onClick = { onSelectConductor(option.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryAndExportCard(
    uiState: AdminReportsUiState,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    val isBusy = uiState.isExportingCsv || uiState.isExportingPdf

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.ElevatedCardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = NavyHeaderSurface,
                border = BorderStroke(AeDimens.BorderWidth, NavyHeaderBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AeDimens.SpaceMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filtered Revenue",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavySubtleText
                        )
                        Text(
                            text = formatPhp(uiState.totalRevenue),
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            ),
                            color = PureWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Tickets",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavySubtleText
                        )
                        Text(
                            text = "${uiState.totalTicketsCount}",
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            ),
                            color = TransitTealDark
                        )
                    }
                }
            }

            Text(
                text = uiState.filterSummaryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = NavySubtleText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    enabled = !isBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    border = BorderStroke(AeDimens.BorderWidth, NavyHeaderBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    if (uiState.isExportingCsv) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AeDimens.IconSm),
                            strokeWidth = AeDimens.BorderWidthThick,
                            color = PureWhite
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.TableView,
                            contentDescription = null,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                    }
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                    Text("Export CSV", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onExportPdf,
                    enabled = !isBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    if (uiState.isExportingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AeDimens.IconSm),
                            strokeWidth = AeDimens.BorderWidthThick,
                            color = PureWhite
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                    }
                    Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                    Text("Export PDF", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ReportFeedbackBanner(
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
private fun EmptyReportRecordsCard() {
    AeEmptyStateCard(
        illustrationRes = R.drawable.empty_analytics_data,
        title = "No Tickets Match These Filters",
        subtitle = "Adjust the date range, bus, or conductor filter above to inspect matching passenger receipts."
    )
}

@Composable
private fun TicketReportRowCard(row: ReportTicketRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AeDimens.IconSm)
                    )
                    Text(
                        text = "#${row.referenceCode}",
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (row.isDiscounted) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (row.isDiscounted) "${row.passengerTypeLabel} -20%" else row.passengerTypeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (row.isDiscounted) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(
                                horizontal = AeDimens.SpaceSm,
                                vertical = AeDimens.SpaceXxs
                            )
                        )
                    }
                }

                Text(
                    text = formatPhp(row.fareAmount),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Seat ${row.seatNumber} → ${row.destination}",
                style = TabularLabelStyle.merge(
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bus ${row.busNumber} • ${row.conductorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
                Text(
                    text = "${row.statusLabel} • ${row.formattedDateTime}",
                    style = TabularLabelStyle.merge(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminReportsContentPreview() {
    AeTranzitTheme {
        AdminReportsContent(
            uiState = AdminReportsUiState(
                selectedDateRange = AnalyticsDateRange.LAST_30_DAYS,
                busOptions = listOf(
                    ReportFilterOption("b1", "Bus AE-101"),
                    ReportFilterOption("b2", "Bus AE-305")
                ),
                conductorOptions = listOf(
                    ReportFilterOption("c1", "Ramon Magsaysay"),
                    ReportFilterOption("c2", "Juan Luna")
                ),
                filteredRows = listOf(
                    ReportTicketRow(
                        ticketId = "t1",
                        referenceCode = "D49A12EF",
                        formattedDateTime = "2026-11-26 14:30",
                        busNumber = "AE-305",
                        routeLabel = "Cubao → Baguio",
                        seatNumber = "8",
                        destination = "Tarlac Junction",
                        passengerTypeLabel = "Student",
                        isDiscounted = true,
                        fareAmount = 128.0,
                        cashTendered = 200.0,
                        changeAmount = 72.0,
                        statusLabel = "On Board",
                        conductorName = "Ramon Magsaysay",
                        createdAt = 1L
                    ),
                    ReportTicketRow(
                        ticketId = "t2",
                        referenceCode = "A81C9001",
                        formattedDateTime = "2026-11-26 13:15",
                        busNumber = "AE-101",
                        routeLabel = "Cubao → Dau",
                        seatNumber = "12",
                        destination = "Dau",
                        passengerTypeLabel = "Regular",
                        isDiscounted = false,
                        fareAmount = 195.0,
                        cashTendered = 200.0,
                        changeAmount = 5.0,
                        statusLabel = "Dropped Off",
                        conductorName = "Juan Luna",
                        createdAt = 2L
                    )
                ),
                totalTicketsCount = 2,
                discountedTicketsCount = 1,
                totalRevenue = 323.0
            ),
            onNavigateBack = {},
            onSelectDateRange = {},
            onSelectBus = {},
            onSelectConductor = {},
            onExportCsv = {},
            onExportPdf = {},
            onDismissFeedback = {}
        )
    }
}

@Preview(showBackground = true, name = "Admin Reports - Empty")
@Composable
private fun AdminReportsEmptyPreview() {
    AeTranzitTheme {
        AdminReportsContent(
            uiState = AdminReportsUiState(),
            onNavigateBack = {},
            onSelectDateRange = {},
            onSelectBus = {},
            onSelectConductor = {},
            onExportCsv = {},
            onExportPdf = {},
            onDismissFeedback = {}
        )
    }
}
