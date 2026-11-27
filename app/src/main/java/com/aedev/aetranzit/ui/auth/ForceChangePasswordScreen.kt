package com.aedev.aetranzit.ui.auth

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.ui.components.AeIsometricBackground
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.AeTranzitBrandHeader
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme

@Composable
fun ForceChangePasswordScreen(
    onPasswordChanged: (User) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForceChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        // Block back navigation until user completes mandatory password change or signs out
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ForceChangePasswordEffect.PasswordUpdated -> onPasswordChanged(effect.user)
                ForceChangePasswordEffect.LoggedOut -> onLoggedOut()
            }
        }
    }

    ForceChangePasswordContent(
        uiState = uiState,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onToggleVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmitNewPassword,
        onLogout = viewModel::onLogout,
        modifier = modifier
    )
}

@Composable
fun ForceChangePasswordContent(
    uiState: ForceChangePasswordUiState,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

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
                    title = "Update Your Password",
                    subtitle = "For security, replace the temporary password provided by your Admin",
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
                        verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSmPlus)
                    ) {
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
                                    .padding(AeDimens.SpaceSmPlus),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.userName.ifBlank { "Staff Account" },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (uiState.userEmail.isNotBlank()) {
                                        Text(
                                            text = uiState.userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.newPassword,
                            onValueChange = onNewPasswordChanged,
                            label = { Text("New Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = {
                                IconButton(
                                    onClick = onToggleVisibility,
                                    modifier = Modifier.sizeIn(
                                        minWidth = AeDimens.MinTouchTarget,
                                        minHeight = AeDimens.MinTouchTarget
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPasswordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (uiState.isPasswordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.newPasswordError != null,
                            supportingText = {
                                Text(
                                    text = uiState.newPasswordError
                                        ?: "At least 6 characters. Must differ from your temporary password."
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = onConfirmPasswordChanged,
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = if (uiState.isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.confirmPasswordError != null,
                            supportingText = uiState.confirmPasswordError?.let { error ->
                                { Text(error) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
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

                        Spacer(modifier = Modifier.height(AeDimens.SpaceXs))

                        AePrimaryButton(
                            text = "Save New Password & Continue",
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
                        text = "Sign Out / Switch Account",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Force Change Password - Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Force Change Password - Dark"
)
@Composable
private fun ForceChangePasswordPreview() {
    AeTranzitTheme {
        ForceChangePasswordContent(
            uiState = ForceChangePasswordUiState(
                userName = "Ramon Magsaysay",
                userEmail = "ramon@victoryliner.ph"
            ),
            onNewPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onToggleVisibility = {},
            onSubmit = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Force Change Password - Error")
@Composable
private fun ForceChangePasswordErrorPreview() {
    AeTranzitTheme {
        ForceChangePasswordContent(
            uiState = ForceChangePasswordUiState(
                userName = "Ramon Magsaysay",
                userEmail = "ramon@victoryliner.ph",
                newPasswordError = "New password must be different from your temporary password."
            ),
            onNewPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onToggleVisibility = {},
            onSubmit = {},
            onLogout = {}
        )
    }
}
