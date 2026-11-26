package com.aedev.aetranzit.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle

@Composable
fun AdminDashboardScreen(
    userName: String,
    isCloudSyncEnabled: Boolean,
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
        onOpenManageBuses = onOpenManageBuses,
        onOpenManageRoutes = onOpenManageRoutes,
        onOpenSettings = onOpenSettings,
        onLogout = onLogout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardContent(
    userName: String,
    isCloudSyncEnabled: Boolean,
    uiState: AdminDashboardUiState,
    onOpenManageBuses: () -> Unit,
    onOpenManageRoutes: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
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
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
                    Text(
                        text = "Role: Admin • Sync: ${if (isCloudSyncEnabled) "Cloud Enabled" else "Offline Only (Room SSOT)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Fleet & Route Operations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
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
@Composable
private fun AdminDashboardContentPreview() {
    AeTranzitTheme {
        AdminDashboardContent(
            userName = "Maria Santos",
            isCloudSyncEnabled = false,
            uiState = AdminDashboardUiState(totalBuses = 4, totalRoutes = 3),
            onOpenManageBuses = {},
            onOpenManageRoutes = {},
            onOpenSettings = {},
            onLogout = {}
        )
    }
}
