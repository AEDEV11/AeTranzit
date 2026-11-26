package com.aedev.aetranzit.ui.auth

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.components.AeTranzitBrandHeader
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme

@Composable
fun RegisterScreen(
    onAuthenticated: (UserRole) -> Unit,
    onNavigateBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.Authenticated -> onAuthenticated(effect.user.role)
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

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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
                title = "Create Staff Account",
                subtitle = "Works 100% offline with optional cloud sync"
            )

            Spacer(modifier = Modifier.height(AeDimens.SpaceLg))

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
                    Text(
                        text = "Select Role",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AeDimens.SpaceSm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
                    ) {
                        RoleSelectorChip(
                            role = UserRole.CONDUCTOR,
                            selected = uiState.selectedRole == UserRole.CONDUCTOR,
                            onClick = { onEvent(AuthUiEvent.RoleSelected(UserRole.CONDUCTOR)) },
                            modifier = Modifier.weight(1f)
                        )
                        RoleSelectorChip(
                            role = UserRole.ADMIN,
                            selected = uiState.selectedRole == UserRole.ADMIN,
                            onClick = { onEvent(AuthUiEvent.RoleSelected(UserRole.ADMIN)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(AuthUiEvent.NameChanged(it)) },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { error -> { Text(error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AeDimens.SpaceSm))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
                        trailingIcon = {
                            IconButton(
                                onClick = { onEvent(AuthUiEvent.TogglePasswordVisibility) },
                                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
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
                        Text(
                            text = uiState.generalError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

                    AePrimaryButton(
                        text = "Register ${uiState.selectedRole.displayName}",
                        isLoading = uiState.isLoading,
                        onClick = {
                            focusManager.clearFocus()
                            onEvent(AuthUiEvent.SubmitRegister)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

            TextButton(
                onClick = onNavigateBackToLogin,
                enabled = !uiState.isLoading,
                modifier = Modifier.heightIn(min = AeDimens.MinTouchTarget)
            ) {
                Text(
                    text = "Already registered? Sign In",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RoleSelectorChip(
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
        BorderStroke(AeDimens.BorderWidth * 2, MaterialTheme.colorScheme.primary)
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

@Preview(showBackground = true)
@Composable
private fun RegisterContentPreview() {
    AeTranzitTheme {
        RegisterContent(
            uiState = AuthUiState(
                name = "Juan Dela Cruz",
                email = "juan@aetranzit.com",
                selectedRole = UserRole.ADMIN
            ),
            onEvent = {},
            onNavigateBackToLogin = {}
        )
    }
}
