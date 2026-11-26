package com.aedev.aetranzit.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSyncLabel = remember(uiState.lastSyncedAt) {
        uiState.lastSyncedAt?.let { timestamp ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
        } ?: "Never synced"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync") },
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AeDimens.MinTouchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (uiState.isCloudSyncEnabled) {
                                    Icons.Default.CloudDone
                                } else {
                                    Icons.Default.CloudOff
                                },
                                contentDescription = null,
                                tint = if (uiState.isCloudSyncEnabled) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Column {
                                Text(
                                    text = "Cloud Sync (Firebase)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Syncs Users, Buses, Routes, Trips & Tickets • Room stays SSOT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isCloudSyncEnabled,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleCloudSync(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    Text(
                        text = "Pending local changes: ${uiState.pendingSyncCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(AeDimens.SpaceXs))

                    Text(
                        text = "Last synced: $lastSyncLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
                        Text(
                            text = uiState.syncStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isSyncError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    AePrimaryButton(
                        text = "Sync Now",
                        isLoading = uiState.isSyncing,
                        enabled = uiState.isCloudSyncEnabled,
                        onClick = { onEvent(SettingsUiEvent.SyncNow) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    AeTranzitTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isCloudSyncEnabled = true,
                pendingSyncCount = 3,
                syncStatusMessage = "Everything is up to date (Users, Buses, Routes, Trips & Tickets)."
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
