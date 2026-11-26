package com.aedev.aetranzit.ui.navigation

import com.aedev.aetranzit.domain.model.UserRole

object AeTranzitRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val CONDUCTOR_HOME = "conductor_home"
    const val SETTINGS = "settings"

    fun homeRouteFor(role: UserRole): String = when (role) {
        UserRole.ADMIN -> ADMIN_DASHBOARD
        UserRole.CONDUCTOR -> CONDUCTOR_HOME
    }
}
