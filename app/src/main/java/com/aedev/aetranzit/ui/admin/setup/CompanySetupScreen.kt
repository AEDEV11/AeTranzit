package com.aedev.aetranzit.ui.admin.setup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.ui.components.AeIsometricBackground
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.AeTranzitBrandHeader
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle

@Composable
fun CompanySetupScreen(
    onSetupCompleted: (User) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompanySetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        // Block back navigation so Admin cannot enter Dashboard without completing Business Setup
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CompanySetupEffect.SetupCompleted -> onSetupCompleted(effect.user)
                CompanySetupEffect.LoggedOut -> onLoggedOut()
            }
        }
    }

    CompanySetupContent(
        uiState = uiState,
        onCompanyNameChanged = viewModel::onCompanyNameChanged,
        onSubmit = viewModel::onSubmitSetup,
        onLogout = viewModel::onLogout,
        modifier = modifier
    )
}

@Composable
fun CompanySetupContent(
    uiState: CompanySetupUiState,
    onCompanyNameChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val subtitleText = if (uiState.adminName.isNotBlank()) {
        "Welcome, ${uiState.adminName}. Name your bus company to unlock your fleet dashboard."
    } else {
        "Name your bus company to unlock your fleet dashboard."
    }

    AeIsometricBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AeDimens.SpaceLg, vertical = AeDimens.SpaceMd),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AeTranzitBrandHeader(
                    title = "Business Setup",
                    subtitle = subtitleText,
                    logoSize = AeDimens.LogoHeader
                )

                Spacer(modifier = Modifier.height(AeDimens.SpaceLg))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = AeDimens.ElevatedCardElevation
                    ),
                    border = BorderStroke(
                        AeDimens.BorderWidth,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeDimens.SpaceLg),
                        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceMd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
                        ) {
                            Surface(
                                modifier = Modifier.size(AeDimens.ActionRowIconBadgeSize),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(AeDimens.SpaceSm)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bus Company Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Appears on digital tickets, QR receipts, and reports.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.companyName,
                            onValueChange = onCompanyNameChanged,
                            label = { Text("Bus Company / Business Name") },
                            placeholder = { Text("e.g., Victory Liner") },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null)
                            },
                            shape = MaterialTheme.shapes.medium,
                            isError = uiState.companyNameError != null,
                            supportingText = {
                                Text(
                                    text = uiState.companyNameError
                                        ?: "Minimum 2 characters. Isolates buses, routes, staff, and tickets to your company."
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSubmit()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.companyCodePreview.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(
                                    AeDimens.BorderWidth,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = AeDimens.SpaceMd,
                                            vertical = AeDimens.SpaceSmPlus
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Company Identifier Prefix",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Auto-generated on setup",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        text = uiState.companyCodePreview,
                                        style = TabularAmountStyle.copy(
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (uiState.generalError != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(AeDimens.SpaceSmPlus),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(AeDimens.IconSm)
                                    )
                                    Text(
                                        text = uiState.generalError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        AePrimaryButton(
                            text = "Complete Business Setup",
                            isLoading = uiState.isSubmitting,
                            onClick = {
                                focusManager.clearFocus()
                                onSubmit()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                TextButton(
                    onClick = onLogout,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(AeDimens.SpaceXs))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Company Setup - Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Company Setup - Dark"
)
@Composable
private fun CompanySetupContentPreview() {
    AeTranzitTheme {
        CompanySetupContent(
            uiState = CompanySetupUiState(
                adminName = "Maria Santos",
                companyName = "Victory Liner",
                companyCodePreview = "VIC-XXXX"
            ),
            onCompanyNameChanged = {},
            onSubmit = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Company Setup - Error")
@Composable
private fun CompanySetupErrorPreview() {
    AeTranzitTheme {
        CompanySetupContent(
            uiState = CompanySetupUiState(
                adminName = "Maria Santos",
                companyName = "V",
                companyNameError = "Bus company / business name must be at least 2 characters."
            ),
            onCompanyNameChanged = {},
            onSubmit = {},
            onLogout = {}
        )
    }
}
