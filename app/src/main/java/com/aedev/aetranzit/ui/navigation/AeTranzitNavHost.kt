package com.aedev.aetranzit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.admin.AdminDashboardScreen
import com.aedev.aetranzit.ui.auth.LoginScreen
import com.aedev.aetranzit.ui.auth.RegisterScreen
import com.aedev.aetranzit.ui.auth.SplashScreen
import com.aedev.aetranzit.ui.conductor.ConductorHomeScreen
import com.aedev.aetranzit.ui.settings.SettingsScreen

@Composable
fun AeTranzitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    val navigateToRoleHome: (UserRole) -> Unit = { role ->
        val target = AeTranzitRoutes.homeRouteFor(role)
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    val navigateToLoginReset: () -> Unit = {
        navController.navigate(AeTranzitRoutes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AeTranzitRoutes.SPLASH,
        modifier = modifier
    ) {
        composable(AeTranzitRoutes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = navigateToLoginReset,
                onNavigateToRoleHome = navigateToRoleHome
            )
        }

        composable(AeTranzitRoutes.LOGIN) {
            LoginScreen(
                onAuthenticated = navigateToRoleHome,
                onNavigateToRegister = {
                    navController.navigate(AeTranzitRoutes.REGISTER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AeTranzitRoutes.REGISTER) {
            RegisterScreen(
                onAuthenticated = navigateToRoleHome,
                onNavigateBackToLogin = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                userName = sessionState.currentUser?.name ?: "Admin",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                onOpenSettings = {
                    navController.navigate(AeTranzitRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onLogout = { sessionViewModel.logout(navigateToLoginReset) }
            )
        }

        composable(AeTranzitRoutes.CONDUCTOR_HOME) {
            ConductorHomeScreen(
                userName = sessionState.currentUser?.name ?: "Conductor",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                onOpenSettings = {
                    navController.navigate(AeTranzitRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onLogout = { sessionViewModel.logout(navigateToLoginReset) }
            )
        }

        composable(AeTranzitRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
