package com.aedev.aetranzit.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularLabelStyle

enum class AeDialogTone {
    PRIMARY,
    POSITIVE,
    DESTRUCTIVE
}

@Composable
fun AeModalDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    useTabularSubtitle: Boolean = false,
    dismissText: String? = "Cancel",
    onDismissAction: (() -> Unit)? = null,
    icon: ImageVector? = null,
    tone: AeDialogTone = AeDialogTone.PRIMARY,
    confirmLeadingIcon: ImageVector? = null,
    isConfirmEnabled: Boolean = true,
    isConfirmLoading: Boolean = false,
    isDismissEnabled: Boolean = true,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        AeModalDialogSurface(
            title = title,
            subtitle = subtitle,
            useTabularSubtitle = useTabularSubtitle,
            confirmText = confirmText,
            onConfirm = onConfirm,
            dismissText = dismissText,
            onDismiss = onDismissAction ?: onDismissRequest,
            icon = icon,
            tone = tone,
            confirmLeadingIcon = confirmLeadingIcon,
            isConfirmEnabled = isConfirmEnabled,
            isConfirmLoading = isConfirmLoading,
            isDismissEnabled = isDismissEnabled,
            modifier = modifier
                .padding(horizontal = AeDimens.SpaceMd)
                .widthIn(max = AeDimens.DialogMaxWidth),
            content = content
        )
    }
}

@Composable
fun AeModalDialogSurface(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    useTabularSubtitle: Boolean = false,
    dismissText: String? = "Cancel",
    icon: ImageVector? = null,
    tone: AeDialogTone = AeDialogTone.PRIMARY,
    confirmLeadingIcon: ImageVector? = null,
    isConfirmEnabled: Boolean = true,
    isConfirmLoading: Boolean = false,
    isDismissEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val badgeContainerColor = when (tone) {
        AeDialogTone.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        AeDialogTone.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
        AeDialogTone.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
    }
    val badgeIconTint = when (tone) {
        AeDialogTone.PRIMARY -> MaterialTheme.colorScheme.primary
        AeDialogTone.POSITIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        AeDialogTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
    }
    val confirmContainerColor = when (tone) {
        AeDialogTone.PRIMARY, AeDialogTone.POSITIVE -> MaterialTheme.colorScheme.primary
        AeDialogTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
    }
    val confirmContentColor = when (tone) {
        AeDialogTone.PRIMARY, AeDialogTone.POSITIVE -> MaterialTheme.colorScheme.onPrimary
        AeDialogTone.DESTRUCTIVE -> MaterialTheme.colorScheme.onError
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = AeDimens.ZeroElevation,
        shadowElevation = AeDimens.ElevatedCardElevation,
        border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(AeDimens.DialogIconBadgeSize),
                        shape = MaterialTheme.shapes.medium,
                        color = badgeContainerColor,
                        tonalElevation = AeDimens.ZeroElevation
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeIconTint,
                                modifier = Modifier.size(AeDimens.IconMd)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXxs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null && content != null) {
                        Text(
                            text = subtitle,
                            style = if (useTabularSubtitle) {
                                TabularLabelStyle.merge(MaterialTheme.typography.bodySmall)
                            } else {
                                MaterialTheme.typography.bodySmall
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (subtitle != null && content == null) {
                Text(
                    text = subtitle,
                    style = if (useTabularSubtitle) {
                        TabularLabelStyle.merge(MaterialTheme.typography.bodyMedium)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (content != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus),
                    content = content
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissText != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = isDismissEnabled && !isConfirmLoading,
                        shape = CircleShape,
                        border = BorderStroke(
                            AeDimens.BorderWidth,
                            MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Button(
                    onClick = onConfirm,
                    enabled = isConfirmEnabled && !isConfirmLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmContainerColor,
                        contentColor = confirmContentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    if (isConfirmLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AeDimens.IconSm),
                            strokeWidth = AeDimens.BorderWidthThick,
                            color = confirmContentColor
                        )
                    } else {
                        if (confirmLeadingIcon != null) {
                            Icon(
                                imageVector = confirmLeadingIcon,
                                contentDescription = null,
                                modifier = Modifier.size(AeDimens.IconXs)
                            )
                            Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
                        }
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "AeModalDialog - Destructive & Positive Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "AeModalDialog - Destructive & Positive Dark"
)
@Composable
private fun AeModalDialogSurfacePreview() {
    AeTranzitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
            ) {
                AeModalDialogSurface(
                    title = "Delete Bus 101?",
                    subtitle = "Plate NCA-4821 (45 seats) will be removed from the local fleet.",
                    useTabularSubtitle = true,
                    confirmText = "Delete",
                    dismissText = "Cancel",
                    icon = Icons.Default.DeleteOutline,
                    tone = AeDialogTone.DESTRUCTIVE,
                    onConfirm = {},
                    onDismiss = {}
                )

                AeModalDialogSurface(
                    title = "Staff Account Created",
                    subtitle = "Share these credentials securely. Password change is enforced on first login.",
                    confirmText = "Copy",
                    dismissText = "Done",
                    icon = Icons.Default.CheckCircle,
                    tone = AeDialogTone.POSITIVE,
                    confirmLeadingIcon = Icons.Default.ContentCopy,
                    onConfirm = {},
                    onDismiss = {}
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            AeDimens.BorderWidth,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceMd),
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Conductor • ramon@aetranzit.ph",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
