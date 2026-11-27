package com.aedev.aetranzit.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.WarningAmber

data class SyncBadgeUiState(
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isOnline: Boolean = true,
    val isCloudSyncEnabled: Boolean = true,
    val pendingCount: Int = 0
)

private enum class BadgeVisualKind {
    SYNCED,
    SYNCING,
    OFFLINE,
    PAUSED,
    ERROR
}

@Composable
fun SyncStatusBadge(
    state: SyncBadgeUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val visualKind = when {
        !state.isCloudSyncEnabled -> BadgeVisualKind.PAUSED
        state.syncStatus is SyncStatus.Syncing -> BadgeVisualKind.SYNCING
        !state.isOnline || state.syncStatus is SyncStatus.Offline -> BadgeVisualKind.OFFLINE
        state.syncStatus is SyncStatus.Error -> BadgeVisualKind.ERROR
        else -> BadgeVisualKind.SYNCED
    }

    val containerColor = when (visualKind) {
        BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.secondaryContainer
        BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.primaryContainer
        BadgeVisualKind.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant
        BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
        BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (visualKind) {
        BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.onSecondaryContainer
        BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.onPrimaryContainer
        BadgeVisualKind.OFFLINE -> MaterialTheme.colorScheme.onSurface
        BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }

    val borderColor = when (visualKind) {
        BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.secondary
        BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.primary
        BadgeVisualKind.OFFLINE -> WarningAmber
        BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.outline
        BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.error
    }

    val labelText = when (visualKind) {
        BadgeVisualKind.SYNCED -> if (state.pendingCount > 0) {
            "Online • ${state.pendingCount} pending"
        } else {
            "Online • Synced"
        }
        BadgeVisualKind.SYNCING -> "Syncing..."
        BadgeVisualKind.OFFLINE -> "Offline • ${state.pendingCount} pending"
        BadgeVisualKind.PAUSED -> "Sync Paused • ${state.pendingCount} pending"
        BadgeVisualKind.ERROR -> if (onClick != null) {
            "Sync Error • Tap to view"
        } else {
            "Sync Error"
        }
    }

    val a11yDescription = when (val status = state.syncStatus) {
        is SyncStatus.Error -> "Cloud sync error: ${status.message}. Tap to open sync settings."
        else -> "Cloud sync status: $labelText"
    }

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                horizontal = AeDimens.SpaceSmPlus,
                vertical = AeDimens.SpaceXs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
        ) {
            when (visualKind) {
                BadgeVisualKind.SYNCING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AeDimens.IconXs),
                        strokeWidth = AeDimens.BorderWidthThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                BadgeVisualKind.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                BadgeVisualKind.OFFLINE -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                BadgeVisualKind.PAUSED -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                BadgeVisualKind.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
            }

            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = AeDimens.MinTouchTarget)
                .semantics { contentDescription = a11yDescription },
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            border = BorderStroke(AeDimens.BorderWidth, borderColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = AeDimens.SpaceXs)
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier.semantics { contentDescription = a11yDescription },
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            border = BorderStroke(AeDimens.BorderWidth, borderColor),
            content = content
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncStatusBadgePreview() {
    AeTranzitTheme {
        Surface {
            Column(
                modifier = Modifier.padding(AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                SyncStatusBadge(
                    state = SyncBadgeUiState(
                        syncStatus = SyncStatus.Synced,
                        isOnline = true,
                        isCloudSyncEnabled = true,
                        pendingCount = 0
                    ),
                    onClick = {}
                )
                SyncStatusBadge(
                    state = SyncBadgeUiState(
                        syncStatus = SyncStatus.Syncing,
                        isOnline = true,
                        isCloudSyncEnabled = true,
                        pendingCount = 2
                    ),
                    onClick = {}
                )
                SyncStatusBadge(
                    state = SyncBadgeUiState(
                        syncStatus = SyncStatus.Offline,
                        isOnline = false,
                        isCloudSyncEnabled = true,
                        pendingCount = 4
                    ),
                    onClick = {}
                )
                SyncStatusBadge(
                    state = SyncBadgeUiState(
                        syncStatus = SyncStatus.Error("Permission denied during ticket sync."),
                        isOnline = true,
                        isCloudSyncEnabled = true,
                        pendingCount = 1
                    ),
                    onClick = {}
                )
            }
        }
    }
}
