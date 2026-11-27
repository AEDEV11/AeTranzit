package com.aedev.aetranzit.ui.admin.staff

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.components.AePrimaryButton
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
    val context = LocalContext.current

    ManageStaffContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onOpenAddStaff = viewModel::onOpenAddStaffSheet,
        onDismissAddStaff = viewModel::onDismissAddStaffSheet,
        onStaffNameChanged = viewModel::onStaffNameChanged,
        onStaffEmailChanged = viewModel::onStaffEmailChanged,
        onStaffRoleSelected = viewModel::onStaffRoleSelected,
        onTempPasswordChanged = viewModel::onTemporaryPasswordChanged,
        onGenerateTempPassword = viewModel::onGenerateTemporaryPassword,
        onToggleTempPasswordVisibility = viewModel::onToggleTempPasswordVisibility,
        onSubmitAddStaff = viewModel::onSubmitCreateStaff,
        onCopyCredentials = { credentials ->
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                ClipData.newPlainText(
                    "AeTranzit Staff Credentials",
                    credentials.toClipboardSummary()
                )
            )
            viewModel.onCredentialsCopied(credentials.name)
        },
        onDismissCredentials = viewModel::onDismissCreatedCredentials,
        onToggleUserActive = viewModel::onToggleUserActive,
        onDismissMessage = viewModel::onDismissMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffContent(
    uiState: ManageStaffUiState,
    onNavigateBack: () -> Unit,
    onOpenAddStaff: () -> Unit,
    onDismissAddStaff: () -> Unit,
    onStaffNameChanged: (String) -> Unit,
    onStaffEmailChanged: (String) -> Unit,
    onStaffRoleSelected: (UserRole) -> Unit,
    onTempPasswordChanged: (String) -> Unit,
    onGenerateTempPassword: () -> Unit,
    onToggleTempPasswordVisibility: () -> Unit,
    onSubmitAddStaff: () -> Unit,
    onCopyCredentials: (ProvisionedStaffCredentials) -> Unit,
    onDismissCredentials: () -> Unit,
    onToggleUserActive: (String, Boolean) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Staff",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.companyName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddStaff,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "Add Staff",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AeDimens.SpaceMd,
                top = AeDimens.SpaceMd,
                end = AeDimens.SpaceMd,
                bottom = AeDimens.PrimaryButtonHeight + AeDimens.SpaceXl
            ),
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeDimens.SpaceXl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                        ) {
                            Text(
                                text = "No staff accounts found yet.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap \"Add Staff\" below to provision Conductor or Co-Admin accounts with a temporary password.",
                                style = MaterialTheme.typography.bodySmall,
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

    if (uiState.addStaffForm.isSheetVisible) {
        ModalBottomSheet(onDismissRequest = onDismissAddStaff) {
            AddStaffSheetContent(
                formState = uiState.addStaffForm,
                companyName = uiState.companyName,
                onNameChanged = onStaffNameChanged,
                onEmailChanged = onStaffEmailChanged,
                onRoleSelected = onStaffRoleSelected,
                onTempPasswordChanged = onTempPasswordChanged,
                onGenerateTempPassword = onGenerateTempPassword,
                onToggleTempPasswordVisibility = onToggleTempPasswordVisibility,
                onSubmit = onSubmitAddStaff,
                onCancel = onDismissAddStaff
            )
        }
    }

    uiState.createdCredentials?.let { credentials ->
        ProvisionedCredentialsDialog(
            credentials = credentials,
            onCopyCredentials = { onCopyCredentials(credentials) },
            onDismiss = onDismissCredentials
        )
    }
}

@Composable
private fun AddStaffSheetContent(
    formState: AddStaffFormState,
    companyName: String,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onTempPasswordChanged: (String) -> Unit,
    onGenerateTempPassword: () -> Unit,
    onToggleTempPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AeDimens.SpaceLg,
                end = AeDimens.SpaceLg,
                bottom = AeDimens.SpaceXl
            ),
        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
    ) {
        Text(
            text = "Add Staff Member",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Provision a staff account under $companyName. They must set a new password on first login.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AeDimens.SpaceXxs))

        Text(
            text = "Role",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            StaffRoleChip(
                role = UserRole.CONDUCTOR,
                selected = formState.selectedRole == UserRole.CONDUCTOR,
                onClick = { onRoleSelected(UserRole.CONDUCTOR) },
                modifier = Modifier.weight(1f)
            )
            StaffRoleChip(
                role = UserRole.ADMIN,
                selected = formState.selectedRole == UserRole.ADMIN,
                onClick = { onRoleSelected(UserRole.ADMIN) },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChanged,
            label = { Text("Full Name") },
            placeholder = { Text("e.g., Ramon Magsaysay") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let { err -> { Text(err) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = formState.email,
            onValueChange = onEmailChanged,
            label = { Text("Email Address") },
            placeholder = { Text("e.g., ramon@buscompany.ph") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = formState.emailError != null,
            supportingText = formState.emailError?.let { err -> { Text(err) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
        ) {
            OutlinedTextField(
                value = formState.temporaryPassword,
                onValueChange = onTempPasswordChanged,
                label = { Text("Temporary Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = onToggleTempPasswordVisibility,
                        modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = if (formState.isPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (formState.isPasswordVisible) {
                                "Hide temporary password"
                            } else {
                                "Show temporary password"
                            }
                        )
                    }
                },
                visualTransformation = if (formState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = formState.passwordError != null,
                supportingText = {
                    Text(
                        text = formState.passwordError
                            ?: "Min 6 chars. Staff will be forced to change this on first sign-in."
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onGenerateTempPassword,
                enabled = !formState.isCreating,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(top = AeDimens.SpaceXs)
                    .heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(AeDimens.IconXs)
                )
                Spacer(modifier = Modifier.width(AeDimens.SpaceXxs))
                Text(
                    text = "Generate",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (formState.generalError != null) {
            Text(
                text = formState.generalError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(AeDimens.SpaceXs))

        AePrimaryButton(
            text = "Create ${formState.selectedRole.displayName} Account",
            isLoading = formState.isCreating,
            onClick = onSubmit
        )

        TextButton(
            onClick = onCancel,
            enabled = !formState.isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AeDimens.MinTouchTarget)
        ) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StaffRoleChip(
    role: UserRole,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (selected) {
        BorderStroke(AeDimens.BorderWidthThick, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outline)
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        modifier = modifier.heightIn(min = AeDimens.MinTouchTarget)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeDimens.SpaceMd, vertical = AeDimens.SpaceSm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (role == UserRole.ADMIN) {
                    Icons.Default.AdminPanelSettings
                } else {
                    Icons.Default.Badge
                },
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(AeDimens.SpaceSm))
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProvisionedCredentialsDialog(
    credentials: ProvisionedStaffCredentials,
    onCopyCredentials: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                text = "Staff Account Created",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)) {
                Text(
                    text = "Share these credentials with ${credentials.name}. They will be required to change this temporary password on first sign-in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
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
                        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                    ) {
                        CredentialLine(label = "Role", value = credentials.role.displayName)
                        CredentialLine(label = "Email", value = credentials.email)
                        CredentialLine(
                            label = "Temporary Password",
                            value = credentials.temporaryPassword,
                            isMonospaceHighlight = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCopyCredentials,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(AeDimens.IconXs)
                )
                Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
                Text("Copy Credentials")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun CredentialLine(
    label: String,
    value: String,
    isMonospaceHighlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isMonospaceHighlight) {
                TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = if (isMonospaceHighlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
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
                    if (member.mustChangePassword) {
                        TempPasswordBadge()
                    }
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
private fun TempPasswordBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = "Temp Password",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(
                horizontal = AeDimens.SpaceSm,
                vertical = AeDimens.SpaceXxs
            )
        )
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
                companyName = "Victory Liner",
                staffMembers = listOf(
                    StaffMemberItem(
                        id = "admin-1",
                        name = "Maria Santos",
                        email = "maria@aetranzit.ph",
                        role = UserRole.ADMIN,
                        isActive = true,
                        mustChangePassword = false,
                        isSynced = true,
                        isCurrentUser = true
                    ),
                    StaffMemberItem(
                        id = "cond-1",
                        name = "Ramon Magsaysay",
                        email = "ramon@aetranzit.ph",
                        role = UserRole.CONDUCTOR,
                        isActive = true,
                        mustChangePassword = true,
                        isSynced = true,
                        isCurrentUser = false
                    ),
                    StaffMemberItem(
                        id = "cond-2",
                        name = "Andres Bonifacio",
                        email = "andres@aetranzit.ph",
                        role = UserRole.CONDUCTOR,
                        isActive = false,
                        mustChangePassword = false,
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
            onOpenAddStaff = {},
            onDismissAddStaff = {},
            onStaffNameChanged = {},
            onStaffEmailChanged = {},
            onStaffRoleSelected = {},
            onTempPasswordChanged = {},
            onGenerateTempPassword = {},
            onToggleTempPasswordVisibility = {},
            onSubmitAddStaff = {},
            onCopyCredentials = {},
            onDismissCredentials = {},
            onToggleUserActive = { _, _ -> },
            onDismissMessage = {}
        )
    }
}
