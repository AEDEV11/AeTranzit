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
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.ui.admin.AdminDashboardScreen
import com.aedev.aetranzit.ui.admin.ManageBusesScreen
import com.aedev.aetranzit.ui.admin.ManageRoutesScreen
import com.aedev.aetranzit.ui.admin.reports.AdminReportsScreen
import com.aedev.aetranzit.ui.admin.setup.CompanySetupScreen
import com.aedev.aetranzit.ui.admin.staff.ManageStaffScreen
import com.aedev.aetranzit.ui.auth.ForceChangePasswordScreen
import com.aedev.aetranzit.ui.auth.LoginScreen
import com.aedev.aetranzit.ui.auth.RegisterScreen
import com.aedev.aetranzit.ui.auth.SplashScreen
import com.aedev.aetranzit.ui.onboarding.OnboardingScreen
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

    val navigateToPostAuth: (User) -> Unit = { user ->
        val target = AeTranzitRoutes.postAuthRouteFor(user)
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
                onNavigateToOnboarding = {
                    navController.navigate(AeTranzitRoutes.ONBOARDING) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onAuthenticated = navigateToPostAuth
            )
        }

        composable(AeTranzitRoutes.ONBOARDING) {
            OnboardingScreen(
                onOnboardingCompleted = navigateToLoginReset
            )
        }

        composable(AeTranzitRoutes.LOGIN) {
            LoginScreen(
                onAuthenticated = navigateToPostAuth,
                onNavigateToRegister = {
                    navController.navigate(AeTranzitRoutes.REGISTER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AeTranzitRoutes.REGISTER) {
            RegisterScreen(
                onAuthenticated = navigateToPostAuth,
                onNavigateBackToLogin = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.COMPANY_SETUP) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                (currentUser.role != UserRole.ADMIN ||
                    currentUser.mustChangePassword ||
                    currentUser.companyId.isNotBlank())
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            CompanySetupScreen(
                onSetupCompleted = navigateToPostAuth,
                onLoggedOut = navigateToLoginReset
            )
        }

        composable(AeTranzitRoutes.FORCE_CHANGE_PASSWORD) {
            val currentUser = sessionState.currentUser
            if (currentUser != null && !currentUser.mustChangePassword) {
                LaunchedEffect(currentUser.id) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ForceChangePasswordScreen(
                onPasswordChanged = navigateToPostAuth,
                onLoggedOut = navigateToLoginReset
            )
        }

        composable(AeTranzitRoutes.ADMIN_DASHBOARD) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.ADMIN_DASHBOARD
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            AdminDashboardScreen(
                userName = currentUser?.name ?: "Admin",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                syncBadgeState = sessionState.syncBadgeState,
                onOpenReports = {
                    navController.navigate(AeTranzitRoutes.ADMIN_REPORTS) {
                        launchSingleTop = true
                    }
                },
                onOpenManageStaff = {
                    navController.navigate(AeTranzitRoutes.ADMIN_STAFF) {
                        launchSingleTop = true
                    }
                },
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

        composable(AeTranzitRoutes.ADMIN_REPORTS) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.ADMIN_DASHBOARD
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            AdminReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.ADMIN_STAFF) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.ADMIN_DASHBOARD
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ManageStaffScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.ADMIN_BUSES) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.ADMIN_DASHBOARD
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ManageBusesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.ADMIN_ROUTES) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.ADMIN_DASHBOARD
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ManageRoutesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.CONDUCTOR_HOME) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.CONDUCTOR_HOME
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ConductorHomeScreen(
                userName = currentUser?.name ?: "Conductor",
                isCloudSyncEnabled = sessionState.isCloudSyncEnabled,
                syncBadgeState = sessionState.syncBadgeState,
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
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.CONDUCTOR_HOME
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            ConductorTicketingScreen(
                syncBadgeState = sessionState.syncBadgeState,
                onNavigateBack = { navController.popBackStack() },
                onOpenDigitalTicket = { ticketId ->
                    navController.navigate(AeTranzitRoutes.conductorDigitalTicketRoute(ticketId)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(AeTranzitRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AeTranzitRoutes.CONDUCTOR_DIGITAL_TICKET,
            arguments = listOf(navArgument(AeTranzitRoutes.ARG_TICKET_ID) { type = NavType.StringType })
        ) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                AeTranzitRoutes.postAuthRouteFor(currentUser) != AeTranzitRoutes.CONDUCTOR_HOME
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.role,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            DigitalTicketScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AeTranzitRoutes.SETTINGS) {
            val currentUser = sessionState.currentUser
            if (currentUser != null &&
                (currentUser.mustChangePassword ||
                    (currentUser.role == UserRole.ADMIN && currentUser.companyId.isBlank()))
            ) {
                LaunchedEffect(
                    currentUser.id,
                    currentUser.companyId,
                    currentUser.mustChangePassword
                ) {
                    navigateToPostAuth(currentUser)
                }
                return@composable
            }
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
