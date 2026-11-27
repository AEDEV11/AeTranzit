package com.aedev.aetranzit.ui.auth

import android.content.res.Configuration
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
fun RegisterScreen(
    onAuthenticated: (User) -> Unit,
    onNavigateBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.Authenticated -> onAuthenticated(effect.user)
            }
        }
    }

    RegisterContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBackToLogin = {
            viewModel.onEvent(AuthUiEvent.ClearErrors)
            onNavigateBackToLogin()
        }
    )
}

@Composable
fun RegisterContent(
    uiState: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    onNavigateBackToLogin: () -> Unit,
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
                    title = "AeTranzit",
                    subtitle = "Bus Operator Fleet & Ticketing Platform",
                    logoSize = AeDimens.LogoHeader
                )

                Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Create Admin Account",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(AeDimens.SpaceXs))
                        Text(
                            text = "Register as a bus operator Admin — set up your business next",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { onEvent(AuthUiEvent.NameChanged(it)) },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            isError = uiState.nameError != null,
                            supportingText = uiState.nameError?.let { error -> { Text(error) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))

                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            isError = uiState.emailError != null,
                            supportingText = uiState.emailError?.let { error -> { Text(error) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = {
                                IconButton(
                                    onClick = { onEvent(AuthUiEvent.TogglePasswordVisibility) },
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
                            isError = uiState.passwordError != null,
                            supportingText = uiState.passwordError?.let { error -> { Text(error) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(AeDimens.SpaceSm))

                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = { onEvent(AuthUiEvent.ConfirmPasswordChanged(it)) },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = if (uiState.isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.confirmPasswordError != null,
                            supportingText = uiState.confirmPasswordError?.let { error -> { Text(error) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onEvent(AuthUiEvent.SubmitRegister)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.generalError != null) {
                            Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
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

                        Spacer(modifier = Modifier.height(AeDimens.SpaceLg))

                        AePrimaryButton(
                            text = "Create Admin Account",
                            isLoading = uiState.isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                onEvent(AuthUiEvent.SubmitRegister)
                            }
                        )

                        Spacer(modifier = Modifier.height(AeDimens.SpaceMd))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(AeDimens.SpaceXs))

                        TextButton(
                            onClick = onNavigateBackToLogin,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = mutedColor)) {
                                        append("Already have an account? ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("Sign In")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Admin - Default")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Register Admin - Dark")
@Composable
private fun RegisterAdminPreview() {
    AeTranzitTheme {
        RegisterContent(
            uiState = AuthUiState(
                name = "Maria Santos",
                email = "maria@victoryliner.ph"
            ),
            onEvent = {},
            onNavigateBackToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "Register Admin - Validation Error")
@Composable
private fun RegisterAdminErrorPreview() {
    AeTranzitTheme {
        RegisterContent(
            uiState = AuthUiState(
                name = "Maria Santos",
                email = "maria@victoryliner.ph",
                confirmPasswordError = "Passwords do not match."
            ),
            onEvent = {},
            onNavigateBackToLogin = {}
        )
    }
}
