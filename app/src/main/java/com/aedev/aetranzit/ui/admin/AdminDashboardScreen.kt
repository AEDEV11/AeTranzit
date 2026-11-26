package com.aedev.aetranzit.ui.admin

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.usecase.AdminAnalyticsSummary
import com.aedev.aetranzit.domain.usecase.AnalyticsDateRange
import com.aedev.aetranzit.domain.usecase.ConductorPerformanceStat
import com.aedev.aetranzit.domain.usecase.DailyRevenuePoint
import com.aedev.aetranzit.domain.usecase.DestinationRevenueStat
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    userName: String,
    isCloudSyncEnabled: Boolean,
    onOpenReports: () -> Unit,
    onOpenManageStaff: () -> Unit,
    onOpenManageBuses: () -> Unit,
    onOpenManageRoutes: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AdminDashboardContent(
        userName = userName,
        isCloudSyncEnabled = isCloudSyncEnabled,
        uiState = uiState,
        onSelectDateRange = viewModel::onSelectDateRange,
        onOpenReports = onOpenReports,
        onOpenManageStaff = onOpenManageStaff,
        onOpenManageBuses = onOpenManageBuses,
        onOpenManageRoutes = onOpenManageRoutes,
        onOpenSettings = onOpenSettings,
        onLogout = onLogout,
        modifier = modifier
    )
}

@Composable
fun AdminDashboardContent(
    userName: String,
    isCloudSyncEnabled: Boolean,
    uiState: AdminDashboardUiState,
    onSelectDateRange: (AnalyticsDateRange) -> Unit,
    onOpenReports: () -> Unit,
    onOpenManageStaff: () -> Unit,
    onOpenManageBuses: () -> Unit,
    onOpenManageRoutes: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics = uiState.analytics

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Admin Operations Hub") },
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
            AdminWelcomeHeaderCard(
                userName = userName,
                isCloudSyncEnabled = isCloudSyncEnabled
            )

            DateRangeFilterBar(
                selectedRange = uiState.selectedDateRange,
                onSelectRange = onSelectDateRange
            )

            KpiSummaryGrid(analytics = analytics)

            RevenueChartCard(
                points = analytics.dailyRevenuePoints,
                rangeLabel = uiState.selectedDateRange.reportLabel
            )

            DestinationsAndPassengerBreakdownCard(analytics = analytics)

            if (analytics.conductorSummaries.isNotEmpty()) {
                ConductorPerformanceCard(conductors = analytics.conductorSummaries)
            }

            Text(
                text = "Operations & Administration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            AdminActionCard(
                title = "Reports & Exports",
                subtitle = "Filter ticket logs by bus or conductor and export RFC-4180 CSV or PDF",
                countBadge = "${analytics.totalTicketsSold}",
                countLabel = if (analytics.totalTicketsSold == 1) "Ticket" else "Tickets",
                icon = Icons.Default.Assessment,
                onClick = onOpenReports
            )

            AdminActionCard(
                title = "Manage Staff",
                subtitle = "View registered Admins & Conductors and activate or deactivate accounts",
                countBadge = "${uiState.totalStaff}",
                countLabel = "${uiState.activeConductorsCount} Active",
                icon = Icons.Default.Badge,
                onClick = onOpenManageStaff
            )

            AdminActionCard(
                title = "Manage Buses",
                subtitle = "Configure bus numbers, plate numbers, capacity, and 2x2 / 2x3 layouts",
                countBadge = "${uiState.totalBuses}",
                countLabel = if (uiState.totalBuses == 1) "Bus" else "Buses",
                icon = Icons.Default.DirectionsBus,
                onClick = onOpenManageBuses
            )

            AdminActionCard(
                title = "Manage Routes",
                subtitle = "Define origins, destinations, base fares, and intermediate stop fares",
                countBadge = "${uiState.totalRoutes}",
                countLabel = if (uiState.totalRoutes == 1) "Route" else "Routes",
                icon = Icons.Default.Route,
                onClick = onOpenManageRoutes
            )

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
}

@Composable
private fun AdminWelcomeHeaderCard(
    userName: String,
    isCloudSyncEnabled: Boolean
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
                .padding(AeDimens.SpaceMd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Welcome, $userName",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
            Text(
                text = "Role: Admin • Sync: ${if (isCloudSyncEnabled) "Cloud Enabled" else "Offline Only (Room SSOT)"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DateRangeFilterBar(
    selectedRange: AnalyticsDateRange,
    onSelectRange: (AnalyticsDateRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
    ) {
        AnalyticsDateRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            FilterChip(
                selected = isSelected,
                onClick = { onSelectRange(range) },
                label = {
                    Text(
                        text = range.chipLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(AeDimens.IconXs)
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            )
        }
    }
}

@Composable
private fun KpiSummaryGrid(analytics: AdminAnalyticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            KpiStatCard(
                title = "Total Revenue",
                valueText = formatPhp(analytics.totalRevenue),
                subtitle = analytics.dateRange.reportLabel,
                icon = Icons.Default.Payments,
                highlightPrimary = true,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Tickets Sold",
                valueText = "${analytics.totalTicketsSold}",
                subtitle = "${analytics.regularTicketsCount} Reg • ${analytics.discountedTicketsCount} Disc",
                icon = Icons.Default.ConfirmationNumber,
                highlightPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            KpiStatCard(
                title = "Discounted Share",
                valueText = String.format(Locale.US, "%.1f%%", analytics.discountedSharePercent),
                subtitle = "20% Student / Senior / PWD",
                icon = Icons.Default.Discount,
                highlightPrimary = false,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Avg Occupancy",
                valueText = String.format(Locale.US, "%.1f%%", analytics.averageLoadFactorPercent),
                subtitle = "${analytics.activeTripsCount} active ${if (analytics.activeTripsCount == 1) "trip" else "trips"}",
                icon = Icons.Default.EventSeat,
                highlightPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    valueText: String,
    subtitle: String,
    icon: ImageVector,
    highlightPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (highlightPrimary) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (highlightPrimary) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (highlightPrimary) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlightPrimary) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(AeDimens.IconSm)
                )
            }
            Text(
                text = valueText,
                style = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RevenueChartCard(
    points: List<DailyRevenuePoint>,
    rangeLabel: String
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
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Revenue Trend",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gross fare collections ($rangeLabel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (points.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AeDimens.ChartHeight),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AeDimens.SpaceMd),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(AeDimens.IconLg)
                            )
                            Text(
                                text = "No revenue recorded in $rangeLabel",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Issued conductor tickets will automatically populate this chart.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val chartModel = remember(points) {
                    CartesianChartModel(
                        ColumnCartesianLayerModel.build {
                            series(points.map { it.revenue })
                        }
                    )
                }

                val bottomValueFormatter = remember(points) {
                    CartesianValueFormatter { _, value, _ ->
                        val index = value.roundToInt()
                        points.getOrNull(index)?.label ?: " "
                    }
                }

                val startValueFormatter = remember {
                    CartesianValueFormatter { _, value, _ ->
                        if (value >= 1_000.0) {
                            String.format(Locale.US, "₱%.1fk", value / 1_000.0)
                        } else {
                            String.format(Locale.US, "₱%.0f", value)
                        }
                    }
                }

                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = startValueFormatter
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = bottomValueFormatter
                            )
                        ),
                        model = chartModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AeDimens.ChartHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationsAndPassengerBreakdownCard(
    analytics: AdminAnalyticsSummary
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
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Text(
                text = "Passenger Mix & Top Drop-Offs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val regularRatio = if (analytics.totalTicketsSold == 0) {
                0f
            } else {
                (analytics.regularTicketsCount.toFloat() / analytics.totalTicketsSold.toFloat())
                    .coerceIn(0f, 1f)
            }

            Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Regular: ${analytics.regularTicketsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Discounted (-20%): ${analytics.discountedTicketsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                LinearProgressIndicator(
                    progress = { regularRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AeDimens.LinearBarHeight),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = if (analytics.totalTicketsSold > 0) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Top Drop-Off Destinations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (analytics.topDestinations.isEmpty()) {
                Text(
                    text = "No destination breakdown available for the selected range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)) {
                    analytics.topDestinations.forEachIndexed { index, stat ->
                        DestinationRankRow(rank = index + 1, stat = stat)
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationRankRow(
    rank: Int,
    stat: DestinationRevenueStat
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSm,
                            vertical = AeDimens.SpaceXxs
                        )
                    )
                }
                Column {
                    Text(
                        text = stat.destination,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stat.ticketCount} ${if (stat.ticketCount == 1) "ticket" else "tickets"} • ${String.format(Locale.US, "%.1f%%", stat.revenueSharePercent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = formatPhp(stat.totalRevenue),
                style = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { (stat.revenueSharePercent.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(AeDimens.SpaceXs),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ConductorPerformanceCard(
    conductors: List<ConductorPerformanceStat>
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
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            Text(
                text = "Conductor Shift Performance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            conductors.take(4).forEachIndexed { index, stat ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.conductorName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stat.ticketsIssued} tickets • ${stat.tripsCount} ${if (stat.tripsCount == 1) "trip" else "trips"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatPhp(stat.totalRevenue),
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    countBadge: String,
    countLabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(AeDimens.SpaceSm)
                        .size(AeDimens.IconLg)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = countBadge,
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminDashboardContentPreview() {
    AeTranzitTheme {
        AdminDashboardContent(
            userName = "Maria Santos",
            isCloudSyncEnabled = true,
            uiState = AdminDashboardUiState(
                selectedDateRange = AnalyticsDateRange.LAST_7_DAYS,
                totalBuses = 4,
                totalRoutes = 3,
                totalStaff = 6,
                activeConductorsCount = 4,
                analytics = AdminAnalyticsSummary(
                    dateRange = AnalyticsDateRange.LAST_7_DAYS,
                    totalRevenue = 18_450.0,
                    totalTicketsSold = 96,
                    regularTicketsCount = 72,
                    discountedTicketsCount = 24,
                    discountedSharePercent = 25.0,
                    activeTripsCount = 2,
                    averageLoadFactorPercent = 68.9,
                    dailyRevenuePoints = listOf(
                        DailyRevenuePoint(1L, "Nov 22", 3200.0, 18),
                        DailyRevenuePoint(2L, "Nov 23", 4150.0, 21),
                        DailyRevenuePoint(3L, "Nov 24", 2900.0, 15),
                        DailyRevenuePoint(4L, "Nov 25", 3850.0, 20),
                        DailyRevenuePoint(5L, "Nov 26", 4350.0, 22)
                    ),
                    topDestinations = listOf(
                        DestinationRevenueStat("Baguio", 34, 9_800.0, 53.1),
                        DestinationRevenueStat("Tarlac Junction", 38, 5_450.0, 29.5),
                        DestinationRevenueStat("Dau", 24, 3_200.0, 17.4)
                    ),
                    conductorSummaries = listOf(
                        ConductorPerformanceStat("c1", "Ramon Magsaysay", 54, 10_400.0, 4),
                        ConductorPerformanceStat("c2", "Juan Luna", 42, 8_050.0, 3)
                    )
                )
            ),
            onSelectDateRange = {},
            onOpenReports = {},
            onOpenManageStaff = {},
            onOpenManageBuses = {},
            onOpenManageRoutes = {},
            onOpenSettings = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminDashboardEmptyPreview() {
    AeTranzitTheme {
        AdminDashboardContent(
            userName = "Maria Santos",
            isCloudSyncEnabled = false,
            uiState = AdminDashboardUiState(
                selectedDateRange = AnalyticsDateRange.TODAY,
                totalBuses = 2,
                totalRoutes = 2,
                totalStaff = 2,
                activeConductorsCount = 1
            ),
            onSelectDateRange = {},
            onOpenReports = {},
            onOpenManageStaff = {},
            onOpenManageBuses = {},
            onOpenManageRoutes = {},
            onOpenSettings = {},
            onLogout = {}
        )
    }
}
