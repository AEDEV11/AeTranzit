package com.aedev.aetranzit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.admin.AdminDashboardScreen
import com.aedev.aetranzit.ui.admin.ManageBusesScreen
import com.aedev.aetranzit.ui.admin.ManageRoutesScreen
import com.aedev.aetranzit.ui.auth.LoginScreen
import com.aedev.aetranzit.ui.auth.RegisterScreen
import com.aedev.aetranzit.ui.auth.SplashScreen
import com.aedev.aetranzit.ui.conductor.ConductorHomeScreen
import com.aedev.aetranzit.ui.conductor.ConductorTicketingScreen
import com.aedev.aetranzit.ui.conductor.ticket.DigitalTicketScreen
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
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.ADMIN) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            AdminDashboardScreen(
                userName = sessionState.currentUser?.name ?: "Admin",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                onOpenManageBuses = {
                    navController.navigate(AeTranzitRoutes.ADMIN_BUSES) {
                        launchSingleTop = true
                    }
                },
                onOpenManageRoutes = {
                    navController.navigate(AeTranzitRoutes.ADMIN_ROUTES) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(AeTranzitRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onLogout = { sessionViewModel.logout(navigateToLoginReset) }
            )
        }

        composable(AeTranzitRoutes.ADMIN_BUSES) {
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.ADMIN) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            ManageBusesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.ADMIN_ROUTES) {
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.ADMIN) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            ManageRoutesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.CONDUCTOR_HOME) {
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.CONDUCTOR) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            ConductorHomeScreen(
                userName = sessionState.currentUser?.name ?: "Conductor",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                onOpenTicketing = { tripId ->
                    navController.navigate(AeTranzitRoutes.conductorTicketingRoute(tripId)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(AeTranzitRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onLogout = { sessionViewModel.logout(navigateToLoginReset) }
            )
        }

        composable(
            route = AeTranzitRoutes.CONDUCTOR_TICKETING,
            arguments = listOf(navArgument(AeTranzitRoutes.ARG_TRIP_ID) { type = NavType.StringType })
        ) {
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.CONDUCTOR) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            ConductorTicketingScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDigitalTicket = { ticketId ->
                    navController.navigate(AeTranzitRoutes.conductorDigitalTicketRoute(ticketId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AeTranzitRoutes.CONDUCTOR_DIGITAL_TICKET,
            arguments = listOf(navArgument(AeTranzitRoutes.ARG_TICKET_ID) { type = NavType.StringType })
        ) {
            val currentRole = sessionState.currentUser?.role
            if (currentRole != null && currentRole != UserRole.CONDUCTOR) {
                LaunchedEffect(currentRole) { navigateToRoleHome(currentRole) }
                return@composable
            }
            DigitalTicketScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
