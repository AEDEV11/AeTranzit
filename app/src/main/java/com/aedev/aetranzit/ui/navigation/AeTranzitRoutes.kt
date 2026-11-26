package com.aedev.aetranzit.ui.navigation

import com.aedev.aetranzit.domain.model.UserRole

object AeTranzitRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_BUSES = "admin_buses"
    const val ADMIN_ROUTES = "admin_routes"

    const val CONDUCTOR_HOME = "conductor_home"
    const val ARG_TRIP_ID = "tripId"
    const val CONDUCTOR_TICKETING = "conductor_ticketing/{$ARG_TRIP_ID}"
    const val ARG_TICKET_ID = "ticketId"
    const val CONDUCTOR_DIGITAL_TICKET = "conductor_digital_ticket/{$ARG_TICKET_ID}"

    const val SETTINGS = "settings"

    fun conductorTicketingRoute(tripId: String): String = "conductor_ticketing/$tripId"

    fun conductorDigitalTicketRoute(ticketId: String): String = "conductor_digital_ticket/$ticketId"

    fun homeRouteFor(role: UserRole): String = when (role) {
        UserRole.ADMIN -> ADMIN_DASHBOARD
        UserRole.CONDUCTOR -> CONDUCTOR_HOME
    }
}
