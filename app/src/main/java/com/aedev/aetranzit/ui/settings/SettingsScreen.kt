package com.aedev.aetranzit.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.SyncBadgeUiState
import com.aedev.aetranzit.ui.components.SyncStatusBadge
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TabularLabelStyle
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
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
                        .padding(AeDimens.SpaceLg)
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
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                modifier = Modifier.size(AeDimens.ActionRowIconBadgeSize),
                                shape = MaterialTheme.shapes.small,
                                color = if (uiState.isCloudSyncEnabled) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.isCloudSyncEnabled) {
                                            Icons.Default.CloudDone
                                        } else {
                                            Icons.Default.CloudOff
                                        },
                                        contentDescription = null,
                                        tint = if (uiState.isCloudSyncEnabled) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(AeDimens.IconSm)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Auto Cloud Sync (Firebase)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Cloud SSOT per company • Offline Room queue auto-syncs on reconnect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.sizeIn(
                                minWidth = AeDimens.MinTouchTarget,
                                minHeight = AeDimens.MinTouchTarget
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Switch(
                                checked = uiState.isCloudSyncEnabled,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleCloudSync(it)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    SyncStatusBadge(state = uiState.syncBadgeState)

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            AeDimens.BorderWidth,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceMd),
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                        ) {
                            SyncTelemetryRow(
                                label = "Network",
                                value = if (uiState.isOnline) {
                                    "Connected"
                                } else {
                                    "Offline (queuing locally)"
                                },
                                isNumeric = false
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SyncTelemetryRow(
                                label = "Pending Local Changes",
                                value = "${uiState.pendingSyncCount}",
                                isNumeric = true
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SyncTelemetryRow(
                                label = "Last Synced",
                                value = lastSyncLabel,
                                isNumeric = false
                            )
                        }
                    }

                    if (uiState.syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(AeDimens.SpaceSmPlus))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (uiState.isSyncError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = uiState.syncStatusMessage,
                                style = TabularLabelStyle.merge(MaterialTheme.typography.bodyMedium),
                                color = if (uiState.isSyncError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(AeDimens.SpaceSmPlus)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceLg))

                    AePrimaryButton(
                        text = "Sync Now",
                        leadingIcon = Icons.Default.Sync,
                        isLoading = uiState.isSyncing,
                        enabled = uiState.isCloudSyncEnabled,
                        onClick = { onEvent(SettingsUiEvent.SyncNow) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncTelemetryRow(
    label: String,
    value: String,
    isNumeric: Boolean
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
            style = if (isNumeric) {
                TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
            } else {
                TabularLabelStyle.merge(MaterialTheme.typography.bodyMedium)
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsContentPreview() {
    AeTranzitTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isCloudSyncEnabled = true,
                isOnline = true,
                pendingSyncCount = 2,
                syncStatusMessage = "Two-way sync complete: pushed 2, pulled 4 record(s).",
                syncBadgeState = SyncBadgeUiState(
                    syncStatus = SyncStatus.Synced,
                    isOnline = true,
                    isCloudSyncEnabled = true,
                    pendingCount = 0
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
