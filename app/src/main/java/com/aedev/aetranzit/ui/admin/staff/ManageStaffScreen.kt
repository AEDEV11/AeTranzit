package com.aedev.aetranzit.ui.admin.staff

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle

@Composable
fun ManageStaffScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManageStaffContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleUserActive = viewModel::onToggleUserActive,
        onDismissMessage = viewModel::onDismissMessage,
        modifier = modifier
    )
}

@Composable
fun ManageStaffContent(
    uiState: ManageStaffUiState,
    onNavigateBack: () -> Unit,
    onToggleUserActive: (String, Boolean) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Manage Staff") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(AeDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
        ) {
            item(key = "staff_summary") {
                StaffSummaryCard(uiState = uiState)
            }

            if (uiState.statusMessage != null) {
                item(key = "staff_status_banner") {
                    StaffFeedbackBanner(
                        message = uiState.statusMessage,
                        isError = uiState.isErrorMessage,
                        onDismiss = onDismissMessage
                    )
                }
            }

            if (uiState.staffMembers.isEmpty()) {
                item(key = "empty_staff") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceXl),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No registered staff accounts found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.staffMembers,
                    key = { it.id }
                ) { member ->
                    StaffMemberCard(
                        member = member,
                        isUpdating = uiState.updatingUserId == member.id,
                        onToggleActive = { targetActive ->
                            onToggleUserActive(member.id, targetActive)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffSummaryCard(uiState: ManageStaffUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = AeDimens.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeDimens.SpaceMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StaffStatColumn(label = "Admins", value = "${uiState.totalAdmins}")
            StaffStatColumn(label = "Conductors", value = "${uiState.totalConductors}")
            StaffStatColumn(label = "Active Staff", value = "${uiState.activeCount}")
        }
    }
}

@Composable
private fun StaffStatColumn(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TabularAmountStyle.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StaffFeedbackBanner(
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
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
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
private fun StaffMemberCard(
    member: StaffMemberItem,
    isUpdating: Boolean,
    onToggleActive: (Boolean) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (member.isCurrentUser) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = AeDimens.SpaceSm,
                                        vertical = AeDimens.SpaceXxs
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = member.isActive,
                        onCheckedChange = onToggleActive,
                        enabled = !isUpdating,
                        modifier = Modifier.semantics {
                            contentDescription = "Toggle active status for ${member.name}"
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                ) {
                    RoleBadge(role = member.role)
                    ActiveStatusBadge(isActive = member.isActive)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    Icon(
                        imageVector = if (member.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = if (member.isSynced) "Synced" else "Pending local sync",
                        tint = if (member.isSynced) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(AeDimens.IconXs)
                    )
                    Text(
                        text = if (member.isSynced) "Synced" else "Local Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (member.isCurrentUser) {
                Text(
                    text = "Currently signed-in Admin account (self-deactivation blocked).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(role: UserRole) {
    val isAdmin = role == UserRole.ADMIN
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isAdmin) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AeDimens.SpaceSm,
                vertical = AeDimens.SpaceXxs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                contentDescription = null,
                tint = if (isAdmin) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(AeDimens.IconXs)
            )
            Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
            Text(
                text = role.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAdmin) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ActiveStatusBadge(isActive: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Text(
            text = if (isActive) "Active" else "Deactivated",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.padding(
                horizontal = AeDimens.SpaceSm,
                vertical = AeDimens.SpaceXxs
            )
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManageStaffContentPreview() {
    AeTranzitTheme {
        ManageStaffContent(
            uiState = ManageStaffUiState(
                staffMembers = listOf(
                    StaffMemberItem(
                        id = "admin-1",
                        name = "Maria Santos",
                        email = "maria@aetranzit.ph",
                        role = UserRole.ADMIN,
                        isActive = true,
                        isSynced = true,
                        isCurrentUser = true
                    ),
                    StaffMemberItem(
                        id = "cond-1",
                        name = "Ramon Magsaysay",
                        email = "ramon@aetranzit.ph",
                        role = UserRole.CONDUCTOR,
                        isActive = true,
                        isSynced = true,
                        isCurrentUser = false
                    ),
                    StaffMemberItem(
                        id = "cond-2",
                        name = "Andres Bonifacio",
                        email = "andres@aetranzit.ph",
                        role = UserRole.CONDUCTOR,
                        isActive = false,
                        isSynced = false,
                        isCurrentUser = false
                    )
                ),
                currentAdminId = "admin-1",
                totalAdmins = 1,
                totalConductors = 2,
                activeCount = 2,
                statusMessage = ManageStaffViewModel.SELF_DEACTIVATION_MESSAGE,
                isErrorMessage = true
            ),
            onNavigateBack = {},
            onToggleUserActive = { _, _ -> },
            onDismissMessage = {}
        )
    }
}
