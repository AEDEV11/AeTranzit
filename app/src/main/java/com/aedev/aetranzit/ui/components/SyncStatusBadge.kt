package com.aedev.aetranzit.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aedev.aetranzit.domain.sync.SyncStatus
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlueDark
import com.aedev.aetranzit.ui.theme.ErrorContainerDark
import com.aedev.aetranzit.ui.theme.ErrorRedDark
import com.aedev.aetranzit.ui.theme.NavyHeaderBorder
import com.aedev.aetranzit.ui.theme.NavyHeaderSurface
import com.aedev.aetranzit.ui.theme.NavySubtleText
import com.aedev.aetranzit.ui.theme.NavySyncPillBg
import com.aedev.aetranzit.ui.theme.NavySyncPillBorder
import com.aedev.aetranzit.ui.theme.PureWhite
import com.aedev.aetranzit.ui.theme.TabularLabelStyle
import com.aedev.aetranzit.ui.theme.TransitTealDark
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
    onDarkHeader: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val visualKind = when {
        !state.isCloudSyncEnabled -> BadgeVisualKind.PAUSED
        state.syncStatus is SyncStatus.Syncing -> BadgeVisualKind.SYNCING
        !state.isOnline || state.syncStatus is SyncStatus.Offline -> BadgeVisualKind.OFFLINE
        state.syncStatus is SyncStatus.Error -> BadgeVisualKind.ERROR
        else -> BadgeVisualKind.SYNCED
    }

    val containerColor = if (onDarkHeader) {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> NavySyncPillBg
            BadgeVisualKind.SYNCING -> NavyHeaderSurface
            BadgeVisualKind.OFFLINE -> NavyHeaderSurface
            BadgeVisualKind.PAUSED -> NavyHeaderSurface
            BadgeVisualKind.ERROR -> ErrorContainerDark
        }
    } else {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.secondaryContainer
            BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.primaryContainer
            BadgeVisualKind.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant
            BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
            BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
    }

    val contentColor = if (onDarkHeader) {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> PureWhite
            BadgeVisualKind.SYNCING -> PureWhite
            BadgeVisualKind.OFFLINE -> PureWhite
            BadgeVisualKind.PAUSED -> NavySubtleText
            BadgeVisualKind.ERROR -> ErrorRedDark
        }
    } else {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.onSecondaryContainer
            BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.onPrimaryContainer
            BadgeVisualKind.OFFLINE -> MaterialTheme.colorScheme.onSurface
            BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
            BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }
    }

    val borderColor = if (onDarkHeader) {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> NavySyncPillBorder
            BadgeVisualKind.SYNCING -> ElectricBlueDark
            BadgeVisualKind.OFFLINE -> WarningAmber
            BadgeVisualKind.PAUSED -> NavyHeaderBorder
            BadgeVisualKind.ERROR -> ErrorRedDark
        }
    } else {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.secondary
            BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.primary
            BadgeVisualKind.OFFLINE -> WarningAmber
            BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.outline
            BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.error
        }
    }

    val indicatorAccent = if (onDarkHeader) {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> TransitTealDark
            BadgeVisualKind.SYNCING -> ElectricBlueDark
            BadgeVisualKind.OFFLINE -> WarningAmber
            BadgeVisualKind.PAUSED -> NavySubtleText
            BadgeVisualKind.ERROR -> ErrorRedDark
        }
    } else {
        when (visualKind) {
            BadgeVisualKind.SYNCED -> MaterialTheme.colorScheme.secondary
            BadgeVisualKind.SYNCING -> MaterialTheme.colorScheme.primary
            BadgeVisualKind.OFFLINE -> WarningAmber
            BadgeVisualKind.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
            BadgeVisualKind.ERROR -> MaterialTheme.colorScheme.error
        }
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
                        color = indicatorAccent
                    )
                }
                BadgeVisualKind.SYNCED -> {
                    Box(
                        modifier = Modifier
                            .size(AeDimens.StatusDotSize)
                            .clip(CircleShape)
                            .background(indicatorAccent)
                    )
                }
                BadgeVisualKind.OFFLINE -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = indicatorAccent,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                BadgeVisualKind.PAUSED -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = indicatorAccent,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
                BadgeVisualKind.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = indicatorAccent,
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                }
            }

            Text(
                text = labelText,
                style = TabularLabelStyle.merge(MaterialTheme.typography.labelMedium),
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
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(AeDimens.BorderWidth, borderColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = AeDimens.SpaceXxs)
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier.semantics { contentDescription = a11yDescription },
            shape = CircleShape,
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
                Surface(color = DeepNavy, shape = MaterialTheme.shapes.medium) {
                    Row(modifier = Modifier.padding(AeDimens.SpaceSm)) {
                        SyncStatusBadge(
                            state = SyncBadgeUiState(
                                syncStatus = SyncStatus.Synced,
                                isOnline = true,
                                isCloudSyncEnabled = true,
                                pendingCount = 0
                            ),
                            onDarkHeader = true,
                            onClick = {}
                        )
                    }
                }
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
            }
        }
    }
}
