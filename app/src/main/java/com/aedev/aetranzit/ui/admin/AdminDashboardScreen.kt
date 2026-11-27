package com.aedev.aetranzit.ui.admin

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.usecase.AdminAnalyticsSummary
import com.aedev.aetranzit.domain.usecase.AnalyticsDateRange
import com.aedev.aetranzit.domain.usecase.ConductorPerformanceStat
import com.aedev.aetranzit.domain.usecase.DailyRevenuePoint
import com.aedev.aetranzit.domain.usecase.DestinationRevenueStat
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import com.aedev.aetranzit.ui.components.SyncStatusBadge
import com.aedev.aetranzit.ui.components.formatPhp
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.HeroHeaderBottomShape
import com.aedev.aetranzit.ui.theme.NavyHeaderBorder
import com.aedev.aetranzit.ui.theme.NavyHeaderSurface
import com.aedev.aetranzit.ui.theme.NavySubtleText
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TabularLabelStyle
import com.aedev.aetranzit.ui.theme.TransitTeal
import com.aedev.aetranzit.ui.theme.TransitTealDark
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
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState(),
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AdminDashboardContent(
        userName = userName,
        isCloudSyncEnabled = isCloudSyncEnabled,
        syncBadgeState = syncBadgeState,
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
    modifier: Modifier = Modifier,
    syncBadgeState: SyncBadgeUiState = SyncBadgeUiState()
) {
    val analytics = uiState.analytics

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminOperationsHeroHeader(
                companyName = uiState.companyName,
                userName = userName,
                analytics = analytics,
                syncBadgeState = syncBadgeState.copy(isCloudSyncEnabled = isCloudSyncEnabled),
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
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

            AdminOperationsDirectoryCard(
                ticketsSoldCount = analytics.totalTicketsSold,
                totalStaff = uiState.totalStaff,
                activeConductorsCount = uiState.activeConductorsCount,
                totalBuses = uiState.totalBuses,
                totalRoutes = uiState.totalRoutes,
                onOpenReports = onOpenReports,
                onOpenManageStaff = onOpenManageStaff,
                onOpenManageBuses = onOpenManageBuses,
                onOpenManageRoutes = onOpenManageRoutes,
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
        }
    }
}

@Composable
private fun AdminOperationsHeroHeader(
    companyName: String,
    userName: String,
    analytics: AdminAnalyticsSummary,
    syncBadgeState: SyncBadgeUiState,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
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
                    end = AeDimens.SpaceSm,
                    top = AeDimens.SpaceSmPlus,
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
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_brand_bus_emblem),
                        contentDescription = "AeTranzit Bus Emblem",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(AeDimens.HeaderEmblemSize)
                    )
                    Column {
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Admin Operations Hub • $userName",
                            style = MaterialTheme.typography.bodySmall,
                            color = NavySubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.sizeIn(
                            minWidth = AeDimens.MinTouchTarget,
                            minHeight = AeDimens.MinTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = PureWhite
                        )
                    }
                }
            }

            SyncStatusBadge(
                state = syncBadgeState,
                onDarkHeader = true,
                onClick = onOpenSettings
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = AeDimens.SpaceSm),
                shape = MaterialTheme.shapes.large,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroSummaryMetric(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Revenue",
                        value = formatPhp(analytics.totalRevenue),
                        modifier = Modifier.weight(1.25f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(AeDimens.IconLg),
                        thickness = AeDimens.BorderWidth,
                        color = NavyHeaderBorder
                    )
                    HeroSummaryMetric(
                        icon = Icons.Default.DirectionsBus,
                        label = "Tickets Sold",
                        value = "${analytics.totalTicketsSold}",
                        modifier = Modifier
                            .weight(0.95f)
                            .padding(start = AeDimens.SpaceSm)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(AeDimens.IconLg),
                        thickness = AeDimens.BorderWidth,
                        color = NavyHeaderBorder
                    )
                    HeroSummaryMetric(
                        icon = Icons.Default.PieChart,
                        label = "Occupancy",
                        value = String.format(
                            Locale.US,
                            "%.0f%%",
                            analytics.averageLoadFactorPercent
                        ),
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(start = AeDimens.SpaceSm)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryMetric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TransitTealDark,
            modifier = Modifier.size(AeDimens.IconSm)
        )
        Column {
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
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            Surface(
                onClick = { onSelectRange(range) },
                shape = CircleShape,
                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface,
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
                },
                shadowElevation = if (isSelected) AeDimens.CardElevation else AeDimens.SpaceXxs / 2,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Box(
                    modifier = Modifier.padding(
                        horizontal = AeDimens.SpaceMd,
                        vertical = AeDimens.SpaceSm
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.chipLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
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
                badgeColor = ElectricBlue,
                badgeIconTint = PureWhite,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Tickets Sold",
                valueText = "${analytics.totalTicketsSold}",
                subtitle = "${analytics.regularTicketsCount} Reg • ${analytics.discountedTicketsCount} Disc",
                icon = Icons.Default.ConfirmationNumber,
                badgeColor = TransitTeal,
                badgeIconTint = DeepNavy,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
        ) {
            KpiStatCard(
                title = "Discounted",
                valueText = String.format(Locale.US, "%.1f%%", analytics.discountedSharePercent),
                subtitle = "20% Student / Senior / PWD",
                icon = Icons.Default.Discount,
                badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                badgeIconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Avg Occupancy",
                valueText = String.format(Locale.US, "%.1f%%", analytics.averageLoadFactorPercent),
                subtitle = "${analytics.activeTripsCount} active ${if (analytics.activeTripsCount == 1) "trip" else "trips"}",
                icon = Icons.Default.EventSeat,
                badgeColor = MaterialTheme.colorScheme.primaryContainer,
                badgeIconTint = MaterialTheme.colorScheme.primary,
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
    badgeColor: Color,
    badgeIconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
            ) {
                Surface(
                    modifier = Modifier.size(AeDimens.KpiIconBadgeSize),
                    shape = CircleShape,
                    color = badgeColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeIconTint,
                            modifier = Modifier.size(AeDimens.IconSm)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = valueText,
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = subtitle,
                style = TabularLabelStyle.merge(MaterialTheme.typography.bodySmall),
                color = MaterialTheme.colorScheme.secondary,
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
                        text = "Revenue Over Time",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gross fare collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        AeDimens.BorderWidth,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSmPlus,
                            vertical = AeDimens.SpaceXs
                        )
                    )
                }
            }

            if (points.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AeDimens.SpaceMd),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.empty_analytics_data),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(AeDimens.EmptyIllustrationSize)
                    )
                    Text(
                        text = "No revenue recorded in $rangeLabel",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Issued conductor tickets will automatically populate this chart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(AeDimens.StatusDotSize)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    )
                    Text(
                        text = "Daily Gross Fare Revenue (₱)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
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
                        style = TabularLabelStyle.merge(MaterialTheme.typography.labelMedium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Discounted (-20%): ${analytics.discountedTicketsCount}",
                        style = TabularLabelStyle.merge(MaterialTheme.typography.labelMedium),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                LinearProgressIndicator(
                    progress = { regularRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AeDimens.LinearBarHeight)
                        .clip(CircleShape),
                    color = ElectricBlue,
                    trackColor = if (analytics.totalTicketsSold > 0) {
                        TransitTeal
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
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        ),
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
                        style = TabularLabelStyle.merge(MaterialTheme.typography.bodySmall),
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
                .height(AeDimens.SpaceXs)
                .clip(CircleShape),
            color = TransitTeal,
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
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
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
                            style = TabularLabelStyle.merge(MaterialTheme.typography.bodySmall),
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
private fun AdminOperationsDirectoryCard(
    ticketsSoldCount: Int,
    totalStaff: Int,
    activeConductorsCount: Int,
    totalBuses: Int,
    totalRoutes: Int,
    onOpenReports: () -> Unit,
    onOpenManageStaff: () -> Unit,
    onOpenManageBuses: () -> Unit,
    onOpenManageRoutes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.ElevatedCardElevation),
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Operations & Administration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = AeDimens.SpaceMd,
                    end = AeDimens.SpaceMd,
                    top = AeDimens.SpaceMd,
                    bottom = AeDimens.SpaceXs
                )
            )

            AdminDirectoryRow(
                title = "Reports",
                subtitle = "CSV & PDF trip & fare exports",
                badgeValue = "$ticketsSoldCount",
                icon = Icons.Default.Assessment,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onOpenReports
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = AeDimens.SpaceMd)
            )
            AdminDirectoryRow(
                title = "Staff",
                subtitle = "$activeConductorsCount active conductors on roster",
                badgeValue = "$totalStaff",
                icon = Icons.Default.Badge,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenManageStaff
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = AeDimens.SpaceMd)
            )
            AdminDirectoryRow(
                title = "Buses",
                subtitle = "Fleet numbers, plates, and 2x2 / 2x3 layouts",
                badgeValue = "$totalBuses",
                icon = Icons.Default.DirectionsBus,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onOpenManageBuses
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = AeDimens.SpaceMd)
            )
            AdminDirectoryRow(
                title = "Routes",
                subtitle = "Corridors, base fares, and stop matrices",
                badgeValue = "$totalRoutes",
                icon = Icons.Default.Route,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenManageRoutes
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = AeDimens.SpaceMd)
            )
            AdminDirectoryRow(
                title = "Sync & Settings",
                subtitle = "Offline queue & cloud sync controls",
                badgeValue = null,
                icon = Icons.Default.Settings,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun AdminDirectoryRow(
    title: String,
    subtitle: String,
    badgeValue: String?,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AeDimens.PrimaryButtonHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AeDimens.SpaceMd,
                    vertical = AeDimens.SpaceSmPlus
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Surface(
                modifier = Modifier.size(AeDimens.ActionRowIconBadgeSize),
                shape = MaterialTheme.shapes.small,
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(AeDimens.IconSm)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeValue != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badgeValue,
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            horizontal = AeDimens.SpaceSmPlus,
                            vertical = AeDimens.SpaceXxs
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                companyName = "Victory Liner",
                companyJoinCode = "VIC-4821",
                selectedDateRange = AnalyticsDateRange.LAST_7_DAYS,
                totalBuses = 4,
                totalRoutes = 3,
                totalStaff = 6,
                activeConductorsCount = 4,
                analytics = AdminAnalyticsSummary(
                    dateRange = AnalyticsDateRange.LAST_7_DAYS,
                    totalRevenue = 42_680.0,
                    totalTicketsSold = 248,
                    regularTicketsCount = 182,
                    discountedTicketsCount = 66,
                    discountedSharePercent = 26.5,
                    activeTripsCount = 2,
                    averageLoadFactorPercent = 74.0,
                    dailyRevenuePoints = listOf(
                        DailyRevenuePoint(1L, "Mon", 5200.0, 32),
                        DailyRevenuePoint(2L, "Tue", 6850.0, 41),
                        DailyRevenuePoint(3L, "Wed", 4900.0, 29),
                        DailyRevenuePoint(4L, "Thu", 6150.0, 36),
                        DailyRevenuePoint(5L, "Fri", 7400.0, 44),
                        DailyRevenuePoint(6L, "Sat", 5980.0, 33),
                        DailyRevenuePoint(7L, "Sun", 6200.0, 33)
                    ),
                    topDestinations = listOf(
                        DestinationRevenueStat("Baguio", 84, 21_800.0, 51.1),
                        DestinationRevenueStat("Tarlac Junction", 92, 12_450.0, 29.2),
                        DestinationRevenueStat("Dau", 72, 8_430.0, 19.7)
                    ),
                    conductorSummaries = listOf(
                        ConductorPerformanceStat("c1", "Ramon Magsaysay", 134, 24_400.0, 6),
                        ConductorPerformanceStat("c2", "Juan Luna", 114, 18_280.0, 5)
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
