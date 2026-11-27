package com.aedev.aetranzit.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.ui.components.AeTranzitBrandHeader
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onAuthenticated: (User) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (val current = destination) {
            SplashDestination.Loading -> Unit
            SplashDestination.Login -> onNavigateToLogin()
            is SplashDestination.Authenticated -> onAuthenticated(current.user)
        }
    }

    SplashContent()
}

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AeDimens.SpaceLg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AeTranzitBrandHeader(
            title = "AeTranzit",
            subtitle = "Offline-First Bus Ticketing",
            logoSize = AeDimens.LogoSplash
        )
        Spacer(modifier = Modifier.height(AeDimens.SpaceXl))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashContentPreview() {
    AeTranzitTheme {
        SplashContent()
    }
}
